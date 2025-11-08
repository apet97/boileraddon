# Production Deployment Guide

This guide covers deploying Clockify addons to production with security, reliability, and scalability best practices.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Configuration](#configuration)
3. [Security Hardening](#security-hardening)
4. [Database Setup](#database-setup)
5. [Deployment Options](#deployment-options)
6. [Monitoring & Observability](#monitoring--observability)
7. [Scaling](#scaling)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required

- **Java 17+** - Runtime environment
- **PostgreSQL 13+** or **MySQL 8+** - For persistent token storage
- **TLS Certificate** - HTTPS is required for webhooks
- **Public Domain/IP** - Addon must be accessible from Clockify servers

### Recommended

- **Redis** - For distributed rate limiting and caching
- **Prometheus + Grafana** - For metrics and monitoring
- **Docker** - For containerized deployment
- **Kubernetes** - For orchestrated deployment at scale

---

## Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Required
ADDON_BASE_URL=https://your-addon.example.com
ADDON_WEBHOOK_SECRET=<generate-with-openssl-rand-hex-32>
ADDON_PORT=8080

# Database (for persistent token storage)
DB_URL=jdbc:postgresql://localhost:5432/clockify_addons
DB_USER=addon_user
DB_PASSWORD=<secure-password>

# Optional
DEBUG=false
LOG_LEVEL=INFO
LOG_APPENDER=JSON  # Use JSON for production logging
```

### Generate Webhook Secret

```bash
openssl rand -hex 32
```

**CRITICAL**: Use a cryptographically secure secret (≥32 characters). Never commit secrets to version control!

### Validation

The SDK includes automatic configuration validation that fails fast with helpful errors:

```java
import com.clockify.addon.sdk.ConfigValidator;

Map<String, String> env = System.getenv();
ConfigValidator.AddonConfig config = ConfigValidator.validateAddonConfig(env);
```

---

## Security Hardening

### 1. Use DatabaseTokenStore

Replace in-memory token storage with persistent database storage:

```java
import com.clockify.addon.sdk.security.DatabaseTokenStore;

// From environment variables
ITokenStore tokenStore = DatabaseTokenStore.fromEnvironment();

// Or with custom config
ITokenStore tokenStore = new DatabaseTokenStore(
    "jdbc:postgresql://localhost:5432/db",
    "user",
    "password"
);
```

### 2. Enable Rate Limiting

Add rate limiting to prevent abuse:

```java
import com.clockify.addon.sdk.middleware.RateLimiter;
import jakarta.servlet.DispatcherType;

// In EmbeddedServer setup
ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

// Add rate limiter: 10 requests/sec per IP
RateLimiter rateLimiter = new RateLimiter(10.0, "ip");
context.addFilter(new FilterHolder(rateLimiter), "/*",
    EnumSet.of(DispatcherType.REQUEST));
```

### 3. Configure HTTPS Only

Update Jetty to redirect HTTP → HTTPS:

```java
// HTTP connector (redirect to HTTPS)
ServerConnector httpConnector = new ServerConnector(server);
httpConnector.setPort(80);

// HTTPS connector
HttpConfiguration httpsConfig = new HttpConfiguration();
httpsConfig.addCustomizer(new SecureRequestCustomizer());

SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
sslContextFactory.setKeyStorePath("/path/to/keystore.jks");
sslContextFactory.setKeyStorePassword(keystorePassword);

ServerConnector httpsConnector = new ServerConnector(server,
    new SslConnectionFactory(sslContextFactory, "http/1.1"),
    new HttpConnectionFactory(httpsConfig));
httpsConnector.setPort(443);

server.addConnector(httpConnector);
server.addConnector(httpsConnector);
```

### 4. Input Validation

All paths are automatically sanitized to prevent:
- Path traversal attacks (`../`)
- Null byte injection
- Invalid characters

Custom validation using the SDK:

```java
import com.clockify.addon.sdk.util.PathSanitizer;

String safePath = PathSanitizer.sanitize(userInput);
```

### 5. Security Headers

Add security headers to responses:

```java
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("X-Frame-Options", "DENY");
response.setHeader("X-XSS-Protection", "1; mode=block");
response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
response.setHeader("Content-Security-Policy", "default-src 'self'");
```

---

## Database Setup

### PostgreSQL Schema

```sql
CREATE DATABASE clockify_addons;
\c clockify_addons;

CREATE TABLE addon_tokens (
    workspace_id VARCHAR(255) PRIMARY KEY,
    auth_token TEXT NOT NULL,
    api_base_url VARCHAR(512) NOT NULL,
    created_at BIGINT NOT NULL,
    last_accessed_at BIGINT NOT NULL
);

CREATE INDEX idx_tokens_created ON addon_tokens(created_at);
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);

-- Create user with limited permissions
CREATE USER addon_user WITH ENCRYPTED PASSWORD 'your-secure-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON addon_tokens TO addon_user;
```

For a comprehensive PostgreSQL setup guide (Docker, pooling, migrations, security), see docs/POSTGRESQL_GUIDE.md.

### MySQL Schema

```sql
CREATE DATABASE clockify_addons;
USE clockify_addons;

CREATE TABLE addon_tokens (
    workspace_id VARCHAR(255) PRIMARY KEY,
    auth_token TEXT NOT NULL,
    api_base_url VARCHAR(512) NOT NULL,
    created_at BIGINT NOT NULL,
    last_accessed_at BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tokens_created ON addon_tokens(created_at);
CREATE INDEX idx_tokens_accessed ON addon_tokens(last_accessed_at);

-- Create user with limited permissions
CREATE USER 'addon_user'@'localhost' IDENTIFIED BY 'your-secure-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON clockify_addons.addon_tokens TO 'addon_user'@'localhost';
FLUSH PRIVILEGES;
```

### Connection Pooling

Add HikariCP for production connection pooling:

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl(jdbcUrl);
config.setUsername(username);
config.setPassword(password);
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
config.setConnectionTimeout(30000);

HikariDataSource dataSource = new HikariDataSource(config);
```

---

## Deployment Options

### Option 1: Docker Deployment

**Dockerfile:**

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR
COPY target/your-addon-jar-with-dependencies.jar app.jar

# Non-root user
RUN addgroup -S addon && adduser -S addon -G addon
USER addon

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  addon:
    build: .
    ports:
      - "8080:8080"
    environment:
      - ADDON_BASE_URL=https://your-addon.example.com
      - ADDON_WEBHOOK_SECRET=${WEBHOOK_SECRET}
      - DB_URL=jdbc:postgresql://db:5432/clockify_addons
      - DB_USER=addon_user
      - DB_PASSWORD=${DB_PASSWORD}
      - LOG_LEVEL=INFO
      - LOG_APPENDER=JSON
    depends_on:
      - db
    restart: unless-stopped

  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=clockify_addons
      - POSTGRES_USER=addon_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

**Deploy:**

```bash
docker-compose up -d
```

### Option 2: Kubernetes Deployment

**deployment.yaml:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: clockify-addon
spec:
  replicas: 3
  selector:
    matchLabels:
      app: clockify-addon
  template:
    metadata:
      labels:
        app: clockify-addon
    spec:
      containers:
      - name: addon
        image: your-registry/clockify-addon:latest
        ports:
        - containerPort: 8080
        env:
        - name: ADDON_BASE_URL
          value: "https://your-addon.example.com"
        - name: ADDON_WEBHOOK_SECRET
          valueFrom:
            secretKeyRef:
              name: addon-secrets
              key: webhook-secret
        - name: DB_URL
          value: "jdbc:postgresql://postgres:5432/clockify_addons"
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: addon-secrets
              key: db-user
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: addon-secrets
              key: db-password
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: clockify-addon
spec:
  selector:
    app: clockify-addon
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

**Deploy:**

```bash
kubectl apply -f deployment.yaml
```

### Option 3: Traditional VM Deployment

**systemd service file** (`/etc/systemd/system/clockify-addon.service`):

```ini
[Unit]
Description=Clockify Addon Service
After=network.target postgresql.service

[Service]
Type=simple
User=addon
WorkingDirectory=/opt/clockify-addon
EnvironmentFile=/opt/clockify-addon/.env
ExecStart=/usr/bin/java -jar /opt/clockify-addon/addon.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Deploy:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable clockify-addon
sudo systemctl start clockify-addon
sudo systemctl status clockify-addon
```

---

## Monitoring & Observability

### Health Check Endpoint

Built-in health check at `/health`:

```java
import com.clockify.addon.sdk.health.HealthCheck;

HealthCheck healthCheck = new HealthCheck("my-addon", "1.0.0");

// Add custom checks
healthCheck.addHealthCheckProvider(new HealthCheck.HealthCheckProvider() {
    @Override
    public String getName() {
        return "database";
    }

    @Override
    public HealthCheck.HealthCheckResult check() {
        try {
            // Test database connection
            tokenStore.count();
            return new HealthCheck.HealthCheckResult("database", true, "Connected");
        } catch (Exception e) {
            return new HealthCheck.HealthCheckResult("database", false, e.getMessage());
        }
    }
});

addon.registerCustomEndpoint("/health", healthCheck);
```

Consider adding a lightweight DB check to the health response (e.g., call `tokenStore.count()` and return DOWN if it fails). This allows external monitors to catch DB outages even if the JVM is alive.

**Response:**

```json
{
  "status": "UP",
  "application": "my-addon",
  "version": "1.0.0",
  "timestamp": 1699564800000,
  "runtime": {
    "uptime": 3600000,
    "startTime": 1699561200000
  },
  "memory": {
    "heapUsed": 52428800,
    "heapMax": 536870912,
    "heapUsagePercent": 9.76
  },
  "checks": {
    "database": {
      "status": "UP",
      "message": "Connected"
    }
  }
}
```

### Structured Logging

Configure JSON logging for production:

```bash
export LOG_APPENDER=JSON
export LOG_LEVEL=INFO
```

Logs will be in JSON format for easy parsing by log aggregators (ELK, Datadog, etc.):

```json
{"timestamp":"2024-11-08T12:00:00.000Z","level":"INFO","logger":"com.clockify.addon","message":"Request processed","thread":"http-thread-1"}
```

### Metrics with Prometheus

Add Micrometer for Prometheus metrics:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.13.0</version>
</dependency>
```

Expose metrics at `/metrics`:

```java
PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
addon.registerCustomEndpoint("/metrics", request ->
    HttpResponse.ok(registry.scrape(), "text/plain"));
```

---

## Scaling

### Horizontal Scaling

The addon is stateless (with database token storage) and can scale horizontally:

1. **Load Balancer**: Use nginx, HAProxy, or cloud load balancer
2. **Multiple Instances**: Run 3+ instances for high availability
3. **Session Affinity**: Not required - addon is stateless

### Vertical Scaling

**JVM Tuning:**

```bash
java -XX:+UseG1GC \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/heapdump.hprof \
     -jar addon.jar
```

### Performance Tips

1. **Connection Pooling**: Use HikariCP (10-20 connections per instance)
2. **HTTP Client Pooling**: Reuse HTTP clients (already done in SDK)
3. **Rate Limiting**: Implement per-workspace rate limiting
4. **Caching**: Cache frequently accessed data (tags, projects)
5. **Async Processing**: Process webhooks asynchronously for high throughput

---

## Troubleshooting

### Common Issues

#### 1. Webhook Signature Validation Fails

**Symptom:** 401 errors on webhook endpoints

**Solution:**
- Verify `ADDON_WEBHOOK_SECRET` matches Clockify configuration
- Check webhook signature validation logic
- Enable debug logging: `LOG_LEVEL=DEBUG`

#### 2. Token Not Found

**Symptom:** `No token found for workspace` errors

**Solution:**
- Verify database connectivity
- Check `INSTALLED` lifecycle handler is registered
- Confirm token is saved on installation

#### 3. Rate Limit Exceeded

**Symptom:** 429 errors from Clockify API

**Solution:**
- Implement exponential backoff (SDK does this automatically)
- Reduce request frequency
- Cache API responses

#### 4. Memory Leaks

**Symptom:** Increasing heap usage, eventual OOM

**Solution:**
- Enable heap dumps: `-XX:+HeapDumpOnOutOfMemoryError`
- Analyze with VisualVM or MAT
- Check for unclosed HTTP clients or database connections

### Debug Mode

Enable verbose logging:

```bash
export DEBUG=true
export LOG_LEVEL=DEBUG
```

### Health Check Monitoring

Monitor `/health` endpoint:

```bash
curl https://your-addon.example.com/health | jq .
```

---

## Checklist

Before going to production:

- [ ] Generate strong webhook secret (≥32 chars)
- [ ] Configure database with connection pooling
- [ ] Enable HTTPS with valid TLS certificate
- [ ] Set up rate limiting
- [ ] Configure structured logging (JSON)
- [ ] Set up health check monitoring
- [ ] Configure alerts for errors/downtime
- [ ] Test backup and restore procedures
- [ ] Document runbook for common issues
- [ ] Load test addon with expected traffic
- [ ] Set up automated backups (database)
- [ ] Configure log rotation/retention
- [ ] Review security headers
- [ ] Test disaster recovery plan

---

## Support

For issues or questions:

1. Check [BUILD_VERIFICATION.md](../BUILD_VERIFICATION.md)
2. Review [ARCHITECTURE.md](ARCHITECTURE.md)
3. Enable debug logging
4. Check health endpoint
5. Review application logs

---

## License

See [LICENSE](../LICENSE)

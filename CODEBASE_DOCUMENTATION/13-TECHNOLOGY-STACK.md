# Technology Stack

Complete technology stack and dependencies for the Clockify Add-on Boilerplate.

## Core Technologies

### Language & Platform

**Java 17+**
- Minimum required version: Java 17
- LTS (Long Term Support) version
- Features used: Records, Text Blocks, Pattern Matching
- License: GPL v2 with Classpath Exception

**Maven 3.6+**
- Build tool and dependency management
- Multi-module project structure
- Plugin ecosystem (Surefire, Failsafe, JaCoCo, Flyway)

---

## Web Framework

### Eclipse Jetty 11.0.24

**Purpose:** Embedded HTTP server

**Components:**
- `jetty-server` - Core server
- `jetty-servlet` - Servlet container
- Jakarta Servlet API 6.1.0 (provided scope)

**Features:**
- Embedded mode (no external server)
- Filter chain support
- Servlet 6.x specification
- WebSocket support (not used)
- HTTP/2 support (not enabled by default)

**Configuration:**

```xml
<dependency>
    <groupId>org.eclipse.jetty</groupId>
    <artifactId>jetty-server</artifactId>
    <version>11.0.24</version>
</dependency>

<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.1.0</version>
    <scope>provided</scope>
</dependency>
```

**Why Jetty?**
- Fast startup (< 2 seconds)
- Low memory footprint (~50MB base)
- Production-ready
- No application server needed

---

## JSON Processing

### Jackson 2.18.2

**Components:**
- `jackson-databind` - Object mapping
- `jackson-core` - Core streaming API
- `jackson-annotations` - Annotations

**Features:**
- Fast serialization/deserialization
- Annotation-based configuration
- Custom serializers/deserializers
- Tree model (`JsonNode`)
- Streaming API

**Usage:**

```java
ObjectMapper mapper = new ObjectMapper();

// Serialize
String json = mapper.writeValueAsString(object);

// Deserialize
MyClass obj = mapper.readValue(json, MyClass.class);

// Tree model
JsonNode node = mapper.readTree(json);
String value = node.get("field").asText();
```

**Configuration:**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
```

---

## Logging

### SLF4J 2.0.16 + Logback 1.5.12

**SLF4J** - Logging facade (API)
**Logback** - Logging implementation

**Features:**
- Structured logging
- Multiple appenders (console, file, etc.)
- Log levels (TRACE, DEBUG, INFO, WARN, ERROR)
- MDC (Mapped Diagnostic Context) support
- Async logging

**Configuration:**

```xml
<!-- SLF4J API -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
</dependency>

<!-- Logback implementation -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.12</version>
</dependency>
```

**logback.xml:**

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="info">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

---

## Utilities

### Google Guava 33.3.1

**Purpose:** Utility library

**Used Features:**
- `RateLimiter` - Token bucket rate limiting
- `Cache` - In-memory caching (not heavily used)
- Collections utilities
- Preconditions

**Configuration:**

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.3.1</version>
</dependency>
```

**Usage:**

```java
// Rate limiting
RateLimiter limiter = RateLimiter.create(10.0); // 10 permits/sec
if (limiter.tryAcquire()) {
    // Process request
}
```

---

## Metrics & Observability

### Micrometer 1.13.0

**Purpose:** Metrics collection and export

**Components:**
- `micrometer-core` - Core metrics API
- `micrometer-registry-prometheus` - Prometheus exporter

**Metrics Types:**
- Counter - Monotonically increasing value
- Gauge - Current value snapshot
- Timer - Duration measurement
- Summary - Distribution summary

**Configuration:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.13.0</version>
</dependency>
```

**Usage:**

```java
PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

// Counter
Counter.builder("webhook_requests_total")
    .tag("event", "TIME_ENTRY_CREATED")
    .register(registry)
    .increment();

// Timer
Timer.Sample sample = Timer.start(registry);
// ... do work ...
Timer timer = Timer.builder("request_duration_seconds")
    .register(registry);
sample.stop(timer);

// Export
String metrics = registry.scrape();
```

---

## Validation

### Jakarta Validation API 3.0.2 + Hibernate Validator 8.0.1

**Purpose:** Bean validation

**Features:**
- Annotation-based validation
- Custom validators
- Groups and sequences
- Method validation

**Configuration:**

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>

<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>8.0.1.Final</version>
</dependency>
```

**Usage:**

```java
public class Rule {
    @NotBlank
    private String name;

    @NotNull
    @Size(min = 1)
    private List<Condition> conditions;
}

Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
Set<ConstraintViolation<Rule>> violations = validator.validate(rule);
```

---

## Database

### PostgreSQL 42.7.4 (JDBC Driver)

**Purpose:** Database connectivity

**Configuration:**

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version>
</dependency>
```

**Connection:**

```java
Connection conn = DriverManager.getConnection(
    "jdbc:postgresql://localhost:5432/addons",
    "addons",
    "addons"
);
```

### Flyway 10.18.2

**Purpose:** Database migrations

**Features:**
- Version-based migrations
- SQL and Java migrations
- Rollback support (commercial)
- Validation

**Configuration:**

```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>10.18.2</version>
    <configuration>
        <url>${env.DB_URL}</url>
        <user>${env.DB_USER}</user>
        <password>${env.DB_PASSWORD}</password>
        <locations>
            <location>filesystem:db/migrations</location>
        </locations>
    </configuration>
</plugin>
```

---

## Testing

### JUnit Jupiter 5.11.3

**Purpose:** Unit testing framework

**Features:**
- Parameterized tests
- Nested tests
- Test lifecycle callbacks
- Assertions

**Configuration:**

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.3</version>
    <scope>test</scope>
</dependency>
```

### Mockito 5.14.2

**Purpose:** Mocking framework

**Features:**
- Mock creation
- Stubbing
- Verification
- Argument captors

**Configuration:**

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
```

### Testcontainers 1.20.2

**Purpose:** Integration testing with containers

**Features:**
- Docker container management
- PostgreSQL module
- Network configuration
- Automatic cleanup

**Configuration:**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.20.2</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.20.2</version>
    <scope>test</scope>
</dependency>
```

**Usage:**

```java
@Testcontainers
class DatabaseTokenStoreIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void testSaveAndGet() {
        DatabaseTokenStore store = new DatabaseTokenStore(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        // Test operations
    }
}
```

### JaCoCo 0.8.12

**Purpose:** Code coverage

**Configuration:**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## Build Tools

### Maven Assembly Plugin 3.7.1

**Purpose:** Create fat JARs (jar-with-dependencies)

**Configuration:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.7.1</version>
    <configuration>
        <archive>
            <manifest>
                <mainClass>com.example.addon.AddonApp</mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
</plugin>
```

### Maven Surefire Plugin 3.2.5

**Purpose:** Run unit tests

### Maven Failsafe Plugin 3.2.5

**Purpose:** Run integration tests

---

## Infrastructure

### Docker

**Base Images:**
- `maven:3.9.6-eclipse-temurin-17` - Build stage
- `eclipse-temurin:17-jre-jammy` - Runtime stage

**PostgreSQL:**
- `postgres:15-alpine` - Development database

### Docker Compose 3.8

**Purpose:** Local development environment

---

## Development Tools

### Python 3.x (for tooling)

**Scripts:**
- `validate-manifest.py` - Manifest validation
- `coverage_badge.py` - Coverage badges
- `check_briefing_links.py` - Link checking
- `verify-jwt-example.py` - JWT validation

**Dependencies:**
- `jsonschema` - JSON schema validation
- Standard library modules

### ngrok

**Purpose:** Local development tunneling
- Expose localhost to public internet
- HTTPS support
- Webhook testing

---

## CI/CD

### GitHub Actions

**Workflows:**
- Build and Test
- Smoke Tests
- Manifest Validation
- Documentation Deployment (Jekyll)
- Database Migrations

**Actions Used:**
- `actions/checkout@v4`
- `actions/setup-java@v4`
- `actions/setup-python@v5`
- `actions/upload-artifact@v4`
- `actions/deploy-pages@v4`

---

## Security Libraries

### Built-in Java Security

**HMAC-SHA256:**
```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
byte[] signature = mac.doFinal(data);
```

**Base64 (JWT):**
```java
Base64.getDecoder().decode(encodedString);
```

---

## Complete Dependency Tree

### Parent POM Dependencies

```xml
<properties>
    <jackson.version>2.18.2</jackson.version>
    <jetty.version>11.0.24</jetty.version>
    <slf4j.version>2.0.16</slf4j.version>
    <logback.version>1.5.12</logback.version>
    <guava.version>33.3.1-jre</guava.version>
    <micrometer.version>1.13.0</micrometer.version>
    <junit.version>5.11.3</junit.version>
    <mockito.version>5.14.2</mockito.version>
    <testcontainers.version>1.20.2</testcontainers.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Jetty -->
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
            <version>${jetty.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>

        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>

        <!-- Guava -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${guava.version}</version>
        </dependency>

        <!-- Metrics -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <version>${micrometer.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>${testcontainers.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Version Compatibility

| Component | Minimum | Recommended | Latest Tested |
|-----------|---------|-------------|---------------|
| Java | 17 | 17 | 21 |
| Maven | 3.6 | 3.9 | 3.9.6 |
| PostgreSQL | 12 | 15 | 15 |
| Docker | 20.10 | 24.0 | 24.0 |
| ngrok | 3.0 | 3.x | 3.5 |

---

## License Summary

| Library | License |
|---------|---------|
| Java 17 | GPL v2 + Classpath Exception |
| Maven | Apache 2.0 |
| Jetty | Apache 2.0 / EPL 2.0 |
| Jackson | Apache 2.0 |
| SLF4J | MIT |
| Logback | EPL 1.0 / LGPL 2.1 |
| Guava | Apache 2.0 |
| Micrometer | Apache 2.0 |
| PostgreSQL JDBC | BSD |
| JUnit | EPL 2.0 |
| Mockito | MIT |
| Testcontainers | MIT |

---

## Performance Characteristics

### Startup Time
- Template Addon: ~1-2 seconds
- Rules Addon: ~2-3 seconds (with database)

### Memory Footprint
- Base (no load): ~50MB
- Under load: ~100-200MB
- With caching: +50MB

### Throughput
- Simple endpoints: ~1000 req/s (single instance)
- With database: ~500 req/s
- Rate limited (default): 10 req/s per workspace

---

**Generated:** 2025-11-09 | **Version:** 1.0.0

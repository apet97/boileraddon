# k6 Load Testing Scenarios

Comprehensive load testing scenarios for Clockify Addon performance validation.

## Overview

This directory contains k6 load testing scripts designed to validate the performance, scalability, and reliability of the Clockify Addon under various load conditions.

## Test Scenarios

### 1. Smoke Testing (`smoke-testing.js`)
- **Purpose**: Quick validation that all critical endpoints are working
- **Load**: 1 virtual user for 1 minute
- **Focus**: Basic functionality and response times
- **Use Case**: CI/CD pipeline validation, deployment verification

### 2. Load Testing (`load-testing.js`)
- **Purpose**: Simulate realistic production load patterns
- **Load**: 50-200 users with gradual ramp-up/down
- **Focus**: Mixed user scenarios (health checks, settings, webhooks, APIs)
- **Use Case**: Performance baseline establishment, capacity planning

### 3. Stress Testing (`stress-testing.js`)
- **Purpose**: Push system to its limits and identify breaking points
- **Load**: Up to 2000 users with rapid ramp-up
- **Focus**: Extreme load conditions, resource utilization
- **Use Case**: Breaking point identification, resource scaling validation

### 4. Spike Testing (`spike-testing.js`)
- **Purpose**: Test system behavior under sudden traffic spikes
- **Load**: Sudden spikes from 50 to 1000+ users
- **Focus**: Recovery from traffic bursts, auto-scaling effectiveness
- **Use Case**: Traffic spike handling, auto-scaling validation

## Prerequisites

1. Install k6:
   ```bash
   # macOS
   brew install k6

   # Ubuntu/Debian
   sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update
   sudo apt-get install k6

   # Windows
   # Download from https://k6.io/docs/getting-started/installation/
   ```

2. Start your addon server:
   ```bash
   # From project root
   mvn spring-boot:run -pl addons/rules
   ```

## Running Tests

### Smoke Test
```bash
k6 run k6/smoke-testing.js
```

### Load Test
```bash
k6 run k6/load-testing.js
```

### Stress Test
```bash
k6 run k6/stress-testing.js
```

### Spike Test
```bash
k6 run k6/spike-testing.js
```

### Custom Configuration
```bash
# Test against different environment
BASE_URL=https://your-addon.example.com k6 run k6/load-testing.js

# Test with specific workspace
WORKSPACE_ID=your-workspace-id k6 run k6/load-testing.js

# Generate HTML report
k6 run --out json=results.json k6/load-testing.js
```

## Test Results Interpretation

### Key Metrics to Monitor

1. **Response Times**:
   - p(95) < 500ms for normal load
   - p(95) < 1000ms for stress conditions

2. **Error Rates**:
   - < 1% for normal load
   - < 5% for stress conditions

3. **Throughput**:
   - Requests per second (RPS)
   - Concurrent users handled

### Performance Thresholds

| Test Type | Response Time (p95) | Error Rate | Concurrent Users |
|-----------|---------------------|------------|------------------|
| Smoke     | < 200ms             | < 1%       | 1                |
| Load      | < 500ms             | < 1%       | 200              |
| Stress    | < 1000ms            | < 5%       | 2000             |
| Spike     | < 800ms             | < 3%       | 1000             |

## Integration with CI/CD

### GitHub Actions Example
```yaml
name: Load Testing
on:
  push:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: grafana/k6-action@v0.3.0
        with:
          filename: k6/load-testing.js
          flags: --out json=results.json
```

### Custom Thresholds for CI
```bash
# Fail build if performance degrades
k6 run --thresholds http_req_duration="p(95)<500" k6/load-testing.js
```

## Best Practices

1. **Run Tests Regularly**: Integrate into CI/CD pipeline
2. **Monitor Trends**: Track performance over time
3. **Test Realistic Scenarios**: Mimic production traffic patterns
4. **Document Findings**: Keep performance baselines updated
5. **Alert on Degradation**: Set up monitoring for performance drops

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure addon server is running
2. **High Response Times**: Check server resources and database performance
3. **High Error Rates**: Verify endpoint implementations and error handling
4. **Memory Issues**: Monitor JVM heap usage during tests

### Performance Optimization

1. **Database**: Ensure proper indexing and connection pooling
2. **Caching**: Implement appropriate caching strategies
3. **Connection Pooling**: Configure optimal pool sizes
4. **Resource Limits**: Set appropriate memory and CPU limits

## Contributing

When adding new test scenarios:

1. Follow existing naming conventions
2. Include appropriate thresholds
3. Document the purpose and use case
4. Test with realistic data patterns
5. Update this README with new scenarios
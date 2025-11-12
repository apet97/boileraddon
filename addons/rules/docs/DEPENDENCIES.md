# Third-Party Dependencies

This document lists all third-party dependencies used by the Rules addon,
including licenses and external service dependencies. This transparency is
required for Clockify marketplace review.

## Runtime Dependencies

### Core Framework
| Dependency | Version | License | Purpose |
|-----------|---------|---------|---------|
| **addon-sdk** | 0.1.0 | MIT | Clockify addon SDK (internal) |
| **Jackson Databind** | (managed) | Apache License 2.0 | JSON parsing and serialization |
| **Jackson Annotations** | (managed) | Apache License 2.0 | JSON annotation support |
| **Eclipse Jetty Server** | (managed) | Apache License 2.0 / Eclipse Public License 1.0 | Embedded HTTP server |
| **Eclipse Jetty Servlet** | (managed) | Apache License 2.0 / Eclipse Public License 1.0 | Servlet container |
| **Jakarta Servlet API** | (managed) | Eclipse Public License 2.0 | Servlet specification |

### Logging
| Dependency | Version | License | Purpose |
|-----------|---------|---------|---------|
| **SLF4J API** | (managed) | MIT | Logging facade |
| **Logback Classic** | (managed) | Eclipse Public License 1.0 / LGPL 2.1 | Logging implementation |

## Test Dependencies

| Dependency | Version | License | Purpose |
|-----------|---------|---------|---------|
| **JUnit Jupiter** | (managed) | Eclipse Public License 2.0 | Unit testing framework |
| **Mockito Core** | 5.2.0 | MIT | Mocking framework for tests |

## Build Dependencies

| Tool | Purpose |
|------|---------|
| **Maven** | Build automation and dependency management |
| **Maven Compiler Plugin** | Java compilation |
| **Maven Assembly Plugin** | Creating executable JAR with dependencies |
| **Maven Surefire Plugin** | Running unit tests |

## External Service Dependencies

### Required Services

#### Clockify API
- **URL**: `https://api.clockify.me/api` (or regional equivalents)
- **Purpose**: All time tracking operations (read/write time entries, tags, projects)
- **Authentication**: X-Addon-Token header (installation token or user token)
- **Availability**: 99.9% SLA
- **Documentation**: https://developer.clockify.me/api

### Optional Services

#### None
This addon does not depend on any external third-party services beyond Clockify itself.

## License Compliance

### Apache License 2.0
**Dependencies**: Jackson, Jetty

**Terms**:
- Commercial use allowed
- Modification allowed
- Distribution allowed
- Patent use allowed
- Private use allowed

**Conditions**:
- License and copyright notice
- State changes
- Attribution required

### MIT License
**Dependencies**: SLF4J, Mockito, addon-sdk

**Terms**:
- Very permissive
- Commercial use allowed
- Modification allowed
- Distribution allowed
- Private use allowed

**Conditions**:
- License and copyright notice

### Eclipse Public License
**Dependencies**: Jetty, Jakarta Servlet, JUnit, Logback

**Terms**:
- Commercial use allowed
- Modification allowed
- Distribution allowed
- Patent use allowed

**Conditions**:
- License and copyright notice
- State changes
- Source code disclosure (for modifications)

## Security Considerations

### Dependency Scanning

We use Maven dependency plugins to scan for known vulnerabilities:

```bash
# Check for security vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Check for outdated dependencies
mvn versions:display-dependency-updates
```

### Update Policy

- **Security patches**: Applied immediately upon disclosure
- **Minor version updates**: Reviewed and applied monthly
- **Major version updates**: Evaluated quarterly
- **Breaking changes**: Thoroughly tested before adoption

### Known Vulnerabilities

As of 2025-01-12:
- ✓ No known critical vulnerabilities
- ✓ All dependencies up-to-date with security patches
- ✓ Regular scans via GitHub Dependabot

## Version Management

All non-test dependency versions are managed by the parent POM to ensure consistency:

```xml
<parent>
  <groupId>com.clockify.boilerplate</groupId>
  <artifactId>clockify-addon-boilerplate</artifactId>
  <version>1.0.0</version>
</parent>
```

This provides:
- Consistent versions across all addon modules
- Easier security patch management
- Reduced version conflicts

## Data Privacy

### No Data Collection
This addon does NOT:
- Send data to third-party services
- Collect user analytics
- Use tracking pixels or cookies beyond CSRF protection
- Phone home to external servers

### Data Storage
- All data stored locally in addon's database (via TokenStore/RulesStore)
- Workspace data cached in-memory (refreshable)
- No PII exported outside Clockify ecosystem

### GDPR Compliance
- User data only accessed via Clockify API with proper auth
- Installation tokens stored securely (never logged or exposed)
- Data deleted on addon uninstall (DELETED lifecycle event)
- No cross-workspace data sharing

## Contact

For questions about dependencies or licensing:
- **Project**: Rules Automation Addon for Clockify
- **Repository**: https://github.com/apet97/boileraddon
- **Issues**: https://github.com/apet97/boileraddon/issues

## References

- [Maven Central Repository](https://mvnrepository.com/)
- [Clockify Developer Portal](https://developer.clockify.me)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [MIT License](https://opensource.org/licenses/MIT)
- [Eclipse Public License](https://www.eclipse.org/legal/epl-2.0/)

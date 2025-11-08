# Build Environment and Java 17 Tooling

This repository targets Java 17 across all modules and CI. Using newer JDKs (e.g., JDK 25) to execute or fork tests can cause the Maven test JVM to die early (Surefire/JaCoCo incompatibilities), which shows up as generic Maven hints like “resume with -rf :addon-sdk”.

Use this playbook to keep builds stable and green.

## 1) Use Java 17 for Maven itself

- macOS/Homebrew

```
brew install openjdk@17
export JAVA_HOME="$('/usr/libexec/java_home' -v 17)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

Both should report version 17.

## 2) Force the forked test JVM to be 17 (Toolchains)

Create `~/.m2/toolchains.xml` on your machine:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
    </provides>
    <configuration>
      <jdkHome>/Library/Java/JavaVirtualMachines/*17*/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
  
</toolchains>
```

The build uses `maven-toolchains-plugin` to select JDK 17 for the Surefire fork.

## 3) Pinned test plugins + JaCoCo binding

- Surefire/Failsafe are pinned to 3.2.5 in the parent POM.
- JaCoCo plugin is pinned to 0.8.12 and bound as:
  - prepare-agent (default)
  - report (phase: test)
  - check (phase: verify) – scoped to packages with unit tests in `addon-sdk`.

This ensures `verify` always has an execution data file, and coverage gates are meaningful but not brittle.

## 4) Quick sanity

```
# Clean caches if you switched JDKs
rm -rf ~/.m2/repository/org/jacoco ~/.m2/repository/org/apache/maven/surefire

# Module tests
mvn -e -DtrimStackTrace=false -pl addons/addon-sdk -am test

# Full build
mvn -e -DtrimStackTrace=false -fae verify
```

## 5) Optional: prove fork JVM is 17

Add a temporary test (do not commit) under `addons/addon-sdk/src/test/java/JvmPrintTest.java`:

```java
import org.junit.jupiter.api.Test;
public class JvmPrintTest {
  @Test public void printsJvm() {
    System.out.println("FORK JVM: " + System.getProperty("java.version"));
  }
}
```

Run:
```
mvn -pl addons/addon-sdk -am -Dtest=JvmPrintTest test
```
You should see `FORK JVM: 17.x` in the test output.

## 6) CI

- GitHub Actions are configured for Temurin JDK 17. No extra flags are required beyond that.
- The docs/Pages job builds docs and copies coverage with `-Djacoco.skip=true` since it doesn’t run tests.

If you still see test forks failing on a newer JDK locally, ensure `JAVA_HOME` and Toolchains are set as above.

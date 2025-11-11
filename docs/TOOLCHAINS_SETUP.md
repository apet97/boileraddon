# Maven Toolchains Setup Guide

## Why Toolchains?

Maven toolchains ensure that **all Maven plugins** (compiler, surefire, failsafe) use the **exact same Java version** during the build. Without toolchains, you may encounter hard-to-debug issues:

- **"Test JVM died unexpectedly"** - Surefire using different JDK than compiler
- **Mockito ClassNotFoundException** - JDK version mismatch between compile and test
- **Inconsistent behavior** - Tests pass on one machine, fail on another
- **Flaky tests** - Random failures that disappear on retry

## The Problem Without Toolchains

When you run `mvn test`:

1. **Maven Compiler Plugin** uses the JDK specified in `JAVA_HOME`
2. **Maven Surefire Plugin** (test runner) might use a different JDK
3. This creates bytecode incompatibility and classloader issues

Example failure:

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.0:test
[ERROR] Process Exit Code: 134
[ERROR] Crashed tests:
[ERROR]   com.clockify.addon.sdk.security.PooledDatabaseTokenStoreTest
```

## The Solution: Toolchains

Toolchains lock all Maven plugins to use the **same Java installation**. This is configured once in `~/.m2/toolchains.xml` and applies to all Maven projects that request it.

---

## Quick Setup

### Step 1: Create toolchains.xml

Create or edit `~/.m2/toolchains.xml`:

```bash
# macOS/Linux
mkdir -p ~/.m2
nano ~/.m2/toolchains.xml

# Windows
mkdir %USERPROFILE%\.m2
notepad %USERPROFILE%\.m2\toolchains.xml
```

### Step 2: Add Configuration

Choose the configuration for your platform:

#### macOS (Homebrew)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find your Homebrew JDK path:**

```bash
# Method 1: Use java_home utility
/usr/libexec/java_home -V

# Output example:
# 17 (x86_64) "Homebrew" - "OpenJDK 17" /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Method 2: Check Homebrew installation
brew --prefix openjdk@17
# Then append: /libexec/openjdk.jdk/Contents/Home
```

#### macOS (Intel - using /usr/local)

If you're on Intel Mac, the path is slightly different:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

#### Linux (Ubuntu/Debian)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-17-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find your Linux JDK path:**

```bash
# Method 1: Check alternatives
update-alternatives --config java
# Output: /usr/lib/jvm/java-17-openjdk-amd64/bin/java
# Use path WITHOUT /bin/java

# Method 2: List installed JVMs
ls /usr/lib/jvm/

# Method 3: Use JAVA_HOME if set
echo $JAVA_HOME
```

#### Linux (SDKMAN)

If you installed Java via SDKMAN:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/home/YOUR_USERNAME/.sdkman/candidates/java/17.0.x-open</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find SDKMAN JDK path:**

```bash
# List installed Java versions
sdk list java

# Current version path
sdk home java 17.0.x-open
```

#### Windows (AdoptOpenJDK / Eclipse Adoptium)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

**Find your Windows JDK path:**

```powershell
# Method 1: Check JAVA_HOME
echo %JAVA_HOME%

# Method 2: Find java.exe location
where java
# Output: C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot\bin\java.exe
# Use path WITHOUT \bin\java.exe

# Method 3: Check Program Files
dir "C:\Program Files\Eclipse Adoptium\"
dir "C:\Program Files\Java\"
```

### Step 3: Verify Configuration

Test that Maven recognizes your toolchain:

```bash
# This should NOT error
mvn toolchains:toolchain

# Expected output:
# [INFO] Toolchain (JDK): openjdk 17 [ /your/jdk/path ]
```

**If it fails**, check:
1. Path is correct and points to JDK home (not `bin/` directory)
2. XML syntax is valid (no typos)
3. Version matches available JDK

---

## Multiple Java Versions

If you have multiple Java versions installed, you can add all of them:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <!-- Java 11 -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>11</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-11-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>

  <!-- Java 17 -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-17-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>

  <!-- Java 21 -->
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>21</version>
      <vendor>openjdk</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/lib/jvm/java-21-openjdk-amd64</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

Maven will use the version requested by each project (this project requests Java 17).

---

## How It Works

### In pom.xml

The project specifies which toolchain to use:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-toolchains-plugin</artifactId>
  <configuration>
    <toolchains>
      <jdk>
        <version>17</version>
      </jdk>
    </toolchains>
  </configuration>
</plugin>
```

### During Build

1. **Maven Toolchains Plugin** executes early in the build lifecycle
2. It reads `~/.m2/toolchains.xml`
3. Finds JDK matching `<version>17</version>`
4. Sets this JDK for **all plugins** (compiler, surefire, failsafe)

### In Test Plugins

Surefire and Failsafe are explicitly configured to use toolchains:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <jdkToolchain>
      <version>17</version>
    </jdkToolchain>
  </configuration>
</plugin>
```

This ensures tests run with the **exact same JDK** as compilation.

---

## Troubleshooting

### "Could not find toolchain"

**Error:**
```
[ERROR] No toolchain found with specification [version:17, type:jdk]
```

**Causes & Fixes:**

1. **toolchains.xml doesn't exist**
   ```bash
   ls ~/.m2/toolchains.xml
   # If not found, create it using examples above
   ```

2. **Path is incorrect**
   ```bash
   # Verify path exists and is a directory
   ls -la "/path/from/toolchains.xml"

   # Should contain: bin/java, lib/, etc.
   ls "/path/from/toolchains.xml"/bin/
   ```

3. **Version mismatch**
   ```xml
   <!-- Wrong: version in toolchains.xml doesn't match pom.xml -->
   <provides>
     <version>11</version>  <!-- ← Should be 17! -->
   </provides>
   ```

4. **XML syntax error**
   ```bash
   # Validate XML
   xmllint ~/.m2/toolchains.xml
   # Or just try to open it in a browser
   ```

### "Test JVM died" even with toolchains

**Possible causes:**

1. **Old class files** - Clean before building:
   ```bash
   mvn clean test
   ```

2. **Multiple JDKs in PATH** - Check your environment:
   ```bash
   which java
   java -version
   echo $JAVA_HOME
   echo $PATH | tr ':' '\n' | grep java
   ```

3. **IDE interference** - If using IntelliJ/Eclipse:
   - Close IDE
   - Run from command line: `mvn clean test`
   - If it works, reconfigure IDE JDK settings

### "Unsupported class file version"

**Error:**
```
java.lang.UnsupportedClassVersionError: version 61.0
```

**Cause:** Bytecode compiled with Java 17 (version 61.0), but runtime using older Java.

**Fix:** Ensure toolchains.xml points to Java 17+:
```bash
# Check toolchain
mvn toolchains:toolchain

# Should show Java 17+
```

### macOS: "jdkHome does not exist"

**Cause:** Path doesn't include `/Contents/Home` for macOS.

**Fix:**
```xml
<!-- Wrong -->
<jdkHome>/opt/homebrew/opt/openjdk@17</jdkHome>

<!-- Correct -->
<jdkHome>/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home</jdkHome>
```

### Windows: Path with spaces

**Cause:** Path contains spaces and isn't recognized.

**Symptoms:**
```
[ERROR] No toolchain found
```

**Fix:** Ensure path is exactly as shown in filesystem (with spaces):
```xml
<jdkHome>C:\Program Files\Eclipse Adoptium\jdk-17.0.2+8</jdkHome>
```

Maven handles spaces correctly. Do **not** escape or quote them.

---

## Verification Checklist

Before running tests, verify your setup:

```bash
# 1. Java version
java -version
# Should show: openjdk version "17.x.x"

# 2. Maven version
mvn -version
# Should show: Java version: 17.x.x

# 3. Toolchains recognized
mvn toolchains:toolchain
# Should show: Toolchain (JDK): openjdk 17

# 4. Clean build succeeds
mvn clean compile
# Should show: BUILD SUCCESS

# 5. Tests succeed
mvn clean test
# Should show: BUILD SUCCESS, Tests run: 296+
```

**All 5 steps pass?** Your toolchains are correctly configured!

---

## Advanced Configuration

### Custom Vendor

If using a specific JDK vendor (Oracle, Amazon Corretto, Azul Zulu):

```xml
<toolchain>
  <type>jdk</type>
  <provides>
    <version>17</version>
    <vendor>Amazon</vendor>  <!-- or Oracle, Azul, etc. -->
  </provides>
  <configuration>
    <jdkHome>/path/to/amazon-corretto-17</jdkHome>
  </configuration>
</toolchain>
```

Then update pom.xml to request specific vendor:

```xml
<jdkToolchain>
  <version>17</version>
  <vendor>Amazon</vendor>
</jdkToolchain>
```

### Per-Project Override

To use different JDK for a specific run:

```bash
# Override toolchain version
mvn test -Djdk.toolchain.version=21

# Or disable toolchains entirely
mvn test -Dmaven.toolchain.skip=true
```

---

## Summary

**Toolchains solve critical stability issues** by ensuring all Maven plugins use the same Java version.

**Setup is simple:**
1. Create `~/.m2/toolchains.xml`
2. Add your JDK path(s)
3. Verify with `mvn toolchains:toolchain`

**Once configured:**
- Tests become stable and reproducible
- No more "Test JVM died" errors
- Consistent behavior across machines
- Team members use same Java version automatically

For more information:
- [FROM_ZERO_SETUP.md](../FROM_ZERO_SETUP.md) - Complete setup guide
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Testing best practices
- [Maven Toolchains Documentation](https://maven.apache.org/guides/mini/guide-using-toolchains.html)

---

**Questions?** Check the [troubleshooting section](#troubleshooting) or open an issue on GitHub.

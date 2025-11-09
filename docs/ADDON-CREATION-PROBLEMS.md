# Problems When Creating Addons - Comprehensive Analysis

**Purpose**: Document all identified issues and blockers when developers (human and AI) attempt to create new Clockify addons from the boilerplate

**Document Version**: 1.0.0
**Last Updated**: 2025-11-09

---

## Table of Contents

1. [Scaffolding Script Issues](#scaffolding-script-issues)
2. [Manual Creation Problems](#manual-creation-problems)
3. [Package & Naming Problems](#package--naming-problems)
4. [Maven Configuration Issues](#maven-configuration-issues)
5. [Manifest File Problems](#manifest-file-problems)
6. [Build & Compilation Issues](#build--compilation-issues)
7. [Runtime Configuration Problems](#runtime-configuration-problems)
8. [Token Storage & Authentication Issues](#token-storage--authentication-issues)
9. [Testing & Validation Problems](#testing--validation-problems)
10. [Documentation & Understanding Issues](#documentation--understanding-issues)
11. [Production Deployment Issues](#production-deployment-issues)
12. [Dependency & Environment Issues](#dependency--environment-issues)

---

## Scaffolding Script Issues

### Problem 1: Perl/Python3 Dependency Chain

**Location**: `scripts/new-addon.sh:128-191`

**Issue**: The scaffolding script requires either Perl OR Python3 for proper template variable substitution. Without either tool, package names are not automatically updated.

**Current Behavior**:
```bash
# What happens if perl is NOT available:
if command -v perl >/dev/null 2>&1; then
  # Use perl...
else
  # Fallback to sed (less reliable for multi-line but works for our case)
  sed -i.bak "..."  # ← Sed is unreliable for complex replacements
fi
```

**Symptoms**:
- Java source files still contain `com.example.templateaddon` package name
- Class files named `TemplateAddonApp.java` instead of `YourAddonApp.java`
- HTML files reference "Template Add-on" instead of custom display name
- Build fails with package mismatch errors

**Example Error Output**:
```
[ERROR] Failed to parse XML in file: addons/my-addon/pom.xml
[ERROR] Unexpected character in packageName: 'com.example.templateaddon'
```

**Why It Happens**:
- Perl is not available on all systems (especially Windows via WSL, minimal Docker images, or CI runners)
- Python3 fallback has different string replacement semantics than Perl
- Sed is a line-based tool, not suitable for multi-line XML/JSON replacements

**Fix Implementation**:
```bash
# Proposed: Use a more robust fallback strategy
if command -v perl >/dev/null 2>&1; then
  use_perl=true
elif command -v python3 >/dev/null 2>&1; then
  use_python=true
else
  # Last resort: jq + sed combination (more reliable)
  use_fallback=true
fi

# Or: Require Python3 as minimum (it's in all modern Python environments)
if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: Python3 is required but not found"
  exit 1
fi
```

---

### Problem 2: JQ Dependency for Manifest Updates

**Location**: `scripts/new-addon.sh:196-225`

**Issue**: Manifest JSON updates require `jq` for proper field replacement. Without jq, sed is used but can corrupt JSON structure.

**Symptoms**:
- Invalid JSON in generated `manifest.json`
- Quotes not properly escaped
- Arrays/objects malformed
- Manifest validation fails

**Example**:
```bash
# Without jq, this sed command can break JSON:
sed -i.bak "s#\"name\": \".*\"#\"name\": \"$DISPLAY_NAME\"#g" manifest.json
# If DISPLAY_NAME contains special chars or quotes, JSON breaks
```

**Why It Happens**:
- Sed treats the replacement string literally
- Special characters in addon names (`"`, `\`, `&`) require escaping
- Multiple fields on same line can cause partial replacements

**Better Solution**:
```bash
# Use Python's built-in json module (always available)
python3 << EOF
import json
with open('manifest.json', 'r') as f:
    manifest = json.load(f)
manifest['name'] = "$DISPLAY_NAME"
manifest['key'] = "$KEY"
manifest['baseUrl'] = "$BASE_URL"
with open('manifest.json', 'w') as f:
    json.dump(manifest, f, indent=2)
EOF
```

---

### Problem 3: Parent pom.xml Module Registration Inconsistency

**Location**: `scripts/new-addon.sh:242-255`

**Issue**: Adding the new module to parent `pom.xml` uses multi-line sed, which can fail or produce invalid XML.

**Current Code**:
```bash
if command -v perl >/dev/null 2>&1; then
  perl -0777 -pe "s#(</modules>)#    <module>addons/$NAME_RAW</module>\n    \$1#" -i pom.xml
else
  sed -i.bak "s#</modules>#    <module>addons/$NAME_RAW</module>\n    </modules>#" pom.xml
fi
```

**Problems**:
- Sed's `-i` behavior differs across macOS and Linux (`-i''` vs `-i''`)
- Escaped newlines (`\n`) are interpreted differently
- If `</modules>` appears multiple times, only first is replaced
- No validation that XML remains valid after change

**Symptoms**:
- Invalid XML in pom.xml
- Build fails with "XML parsing error"
- Module not registered, causing "missing module" errors

**Example Error**:
```
[ERROR] Failed to parse XML in file: pom.xml
[ERROR] Unexpected character 'n' at line 42, column 15
```

**Better Implementation**:
```bash
# Use xmlstarlet (if available) or Python's xml.etree
python3 << 'EOF'
import xml.etree.ElementTree as ET
tree = ET.parse('pom.xml')
root = tree.getroot()
# Register namespace
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
ET.register_namespace('', 'http://maven.apache.org/POM/4.0.0')
modules = root.find('.//m:modules', ns)
if modules is not None:
    new_module = ET.Element('module')
    new_module.text = "addons/my-addon"
    modules.append(new_module)
    tree.write('pom.xml')
EOF
```

---

### Problem 4: Missing Validation of Generated Addon

**Location**: `scripts/new-addon.sh:257-265`

**Issue**: Manifest validation is treated as non-fatal (warning only), so broken addons are created successfully.

**Current Code**:
```bash
echo "Validating manifest..."
if [ -f "tools/validate-manifest.py" ]; then
  python3 tools/validate-manifest.py "$DST_DIR/manifest.json" || {
    echo "Warning: Manifest validation failed (but addon was created)" >&2
  }
fi
```

**Why This Is Bad**:
- Developer gets false sense of success
- Error only discovered during first build attempt
- No smoke tests run (unlike `test-new-addon.sh`)
- Invalid addons pollute the repository

**Symptoms**:
- `scripts/new-addon.sh my-addon` reports success
- Next step (`mvn package`) fails with cryptic error
- User doesn't know which step went wrong

**Fix**: Make validation mandatory:
```bash
echo "Validating manifest..."
if [ -f "tools/validate-manifest.py" ]; then
  python3 tools/validate-manifest.py "$DST_DIR/manifest.json" || {
    echo "Error: Manifest validation failed" >&2
    cleanup_and_exit 1
  }
else
  echo "Warning: Validator not found, cannot validate manifest" >&2
fi
```

---

## Manual Creation Problems

### Problem 5: Manual Copy-Paste Errors

**Issue**: When developers manually copy `_template-addon` without using the script, they often miss renaming steps.

**Common Mistakes**:
1. Copy folder but forget to update:
   - `pom.xml` artifactId
   - `manifest.json` key and name
   - Java package names
   - Main class name in pom.xml

2. Partial updates:
   - Update pom.xml but not manifest.json
   - Rename folder but not package structure
   - Change package in some files but not others

**Example Scenario**:
```bash
# Developer does:
cp -r addons/_template-addon addons/my-addon
# Then manually edits files but misses one:
# ✓ Updated pom.xml
# ✓ Updated manifest.json
# ✓ Renamed Java package
# ✗ FORGOT: Update mainClass in pom.xml assembly plugin
```

**Result**:
```
Build succeeds but:
java -jar target/my-addon-0.1.0-jar-with-dependencies.jar

Error: Could not find or load main class com.example.templateaddon.TemplateAddonApp
```

**Prevention**: The README's manual instructions miss critical steps. Should include a verification checklist.

---

### Problem 6: Package Naming Conflicts

**Issue**: Package names can contain invalid Java identifier characters.

**Location**: `scripts/new-addon.sh:92`

**Code**:
```bash
PKG_NAME=$(echo "$NAME_RAW" | tr -cd '[:alnum:]_-' | tr '-' '_')
```

**Problems**:
- Hyphens in addon name (`my-cool-addon`) become underscores (`my_cool_addon`)
- Numbers at start are stripped but logic not explicit
- Special characters silently dropped, leading to confusing package names

**Example Issues**:
```bash
# Input: "my-addon-v2"
# Package becomes: "my_addon_v2" ✓ OK

# Input: "2nd-addon"
# Package becomes: "nd_addon" ✗ WRONG (number prefix stripped)

# Input: "add.on"
# Package becomes: "addon" ✗ loses structure
```

**Why It Matters**:
- Developers don't realize their addon name was transformed
- Generated code doesn't match their expectations
- Debugging harder when package name ≠ addon name

---

## Package & Naming Problems

### Problem 7: Class Name Derivation Algorithm Failure

**Location**: `scripts/new-addon.sh:76-81`

**Code**:
```bash
NAME_CLASS_SOURCE=$(echo "$NAME_RAW" | tr -cd '[:alnum:]_-')
CLASS_PREFIX=$(echo "$NAME_CLASS_SOURCE" | tr '_-' ' ' | awk '{...}')
if [ -z "$CLASS_PREFIX" ]; then
  CLASS_PREFIX="Addon"
fi
```

**Failure Cases**:
```bash
# Input: "123"
# CLASS_SOURCE: "123"
# CLASS_PREFIX: "123" (awk sees only numbers, produces empty, defaults to "Addon")
# Output class: "AddonApp" ✓ Actually works, but misleading

# Input: "my---addon"
# Transforms to "my   addon" (multiple spaces)
# Awk concatenates as "MyAddon" ✓ OK

# Input: "__private"
# Transforms to "  private"
# Awk produces "Private" ✓ OK

# Input: "a"
# CLASS_PREFIX: "A"
# Output class: "AApp" (weird but valid)
```

**Why This Matters**:
- Generated class names can be unexpected (single letter names)
- Multiple hyphens/underscores produce confusing class names
- No validation that resulting class name is sensible

---

## Maven Configuration Issues

### Problem 8: pom.xml Template Variables Not Updated

**Issue**: The template pom.xml contains hardcoded references that new-addon.sh may miss.

**Template pom.xml locations**:
```xml
<artifactId>_template-addon</artifactId>              <!-- Must be updated -->
<name>_template-addon</name>                          <!-- Must be updated -->
<mainClass>com.example.templateaddon.TemplateAddonApp</mainClass>  <!-- Must match -->
<include>com.example.templateaddon.*</include>        <!-- JaCoCo coverage -->
```

**Symptoms of Failure**:
```bash
# Build output shows original names:
[INFO] Building Template Add-on 0.1.0
# But you created "my-addon"!

# JAR file is named wrong:
ls -la target/
_template-addon-0.1.0-jar-with-dependencies.jar  ← Wrong!
```

**Why Problem 8 Happens**:
- Perl/Python fallback misses these replacements
- JaCoCo include path uses old package name
- No post-generation validation

---

### Problem 9: Incorrect Parent POM Reference

**Issue**: New addon modules don't properly inherit parent POM dependencies.

**Template pom.xml**:
```xml
<parent>
  <groupId>com.clockify.boilerplate</groupId>
  <artifactId>clockify-addon-boilerplate</artifactId>
  <version>1.0.0</version>
  <relativePath>../../pom.xml</relativePath>
</parent>
```

**Problems**:
- If addon is created in different depth, relativePath breaks
- If moved after creation, parent reference fails
- Version must match root pom.xml

**Symptoms**:
```
[ERROR] Failed to read the POM for com.clockify.boilerplate:clockify-addon-boilerplate:jar:1.0.0:
[ERROR] Project does not exist in the repository
```

---

### Problem 10: Missing Assembly Plugin Configuration

**Issue**: If the maven-assembly-plugin configuration is not correctly copied, fat JAR build fails.

**Expected**:
```xml
<plugin>
  <artifactId>maven-assembly-plugin</artifactId>
  <configuration>
    <archive>
      <manifest>
        <mainClass>com.example.myaddon.MyAddonApp</mainClass>
      </manifest>
    </archive>
    <descriptorRefs>
      <descriptorRef>jar-with-dependencies</descriptorRef>
    </descriptorRefs>
  </configuration>
  <executions>
    <execution>
      <id>make-assembly</id>
      <phase>package</phase>
      <goals>
        <goal>single</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

**If Missing**:
```bash
mvn package
# Produces ONLY: my-addon-0.1.0.jar (100 KB, no dependencies)
# Produces nothing: my-addon-0.1.0-jar-with-dependencies.jar

# When trying to run:
java -jar target/my-addon-0.1.0.jar
ClassNotFoundException: com.fasterxml.jackson.databind.ObjectMapper
```

---

## Manifest File Problems

### Problem 11: $schema Field in Runtime Manifest

**Location**: `addons/_template-addon/manifest.json`

**Issue**: Including `$schema` field causes Clockify API rejection.

**Current Template**:
```json
{
  "schemaVersion": "1.3",
  "key": "_template-addon",
  ...
}
```

**If Someone Adds**:
```json
{
  "$schema": "https://path/to/schema.json",
  "schemaVersion": "1.3",
  ...
}
```

**Result**:
```
POST /api/addons
HTTP/1.1 400 Bad Request
{
  "error": "Unexpected field: $schema"
}
```

**Why Developers Add It**:
- IDE autocomplete suggests it
- Other projects use it for validation
- Appears helpful for schema validation

---

### Problem 12: Manifest Validation Not Run After Generation

**Location**: `scripts/new-addon.sh:257-265`

**Issue**: The validation step warns but doesn't fail, so bad manifests propagate.

**Current Behavior**:
```bash
python3 tools/validate-manifest.py "$DST_DIR/manifest.json" || {
  echo "Warning: Manifest validation failed (but addon was created)" >&2
}
```

**Should Be**:
```bash
python3 tools/validate-manifest.py "$DST_DIR/manifest.json" || {
  echo "Error: Manifest validation failed!" >&2
  # Cleanup and exit
  rm -rf "$DST_DIR"
  git checkout -- pom.xml 2>/dev/null || true
  exit 1
}
```

---

### Problem 13: Manifest Components Array Empty or Missing

**Issue**: Generated manifest may have empty or malformed components array.

**Template Correctness**:
```json
{
  "components": [
    {
      "type": "sidebar",
      "path": "/settings",
      "label": "Template Add-on",
      "accessLevel": "ADMINS"
    }
  ]
}
```

**Failure Modes**:
```json
// Mode 1: Empty components
{
  "components": []  // ← Clockify rejects, needs at least one
}

// Mode 2: Missing components entirely
{
  "key": "my-addon",
  "webhooks": [...]
  // components field missing
}

// Mode 3: Malformed component
{
  "components": [
    {
      "type": "sidebar"
      // Missing required path, label
    }
  ]
}
```

---

## Build & Compilation Issues

### Problem 14: Java 17 Toolchain Not Available

**Location**: pom.xml (maven-surefire-plugin, maven-compiler-plugin)

**Issue**: Build requires Java 17, but many developers have different versions.

**pom.xml Configuration**:
```xml
<source>17</source>
<target>17</target>
```

**Symptoms**:
```
[ERROR] Fatal error compiling: invalid target release: 17
[ERROR] (you are running ${java.version}, but target is 17)
```

**Why Happens**:
- Developer has Java 11 or Java 8
- Maven picks wrong JDK
- CI environment might have multiple JDKs

**Modern Fix** (Maven 3.5+):
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <jdkToolchain>
      <version>17</version>
    </jdkToolchain>
  </configuration>
</plugin>
```

But this requires:
```bash
~/.m2/toolchains.xml
# Defining available JDKs
```

---

### Problem 15: Maven Dependency Resolution Failure

**Issue**: Dependencies not found if Maven Central is inaccessible or repository is misconfigured.

**Symptoms**:
```
[ERROR] Failed to execute goal on project my-addon:
[ERROR] Could not transfer artifact ...
[ERROR] Failure to find com.fasterxml.jackson.core:jackson-databind:jar:2.18.2 in https://repo.maven.apache.org/maven2
```

**Why Happens**:
- Network connectivity issues
- Maven cache corrupted
- Custom repository configuration interferes
- Proxy/firewall blocks Maven Central

---

### Problem 16: ClassPath Order Issues with Fat JAR

**Issue**: When multiple versions of the same library are on classpath, wrong version loads.

**Example**:
```bash
# If developer has jackson 2.15 globally installed
# And boilerplate specifies 2.18.2
# Fat JAR construction might use wrong version

# Results in:
NoSuchMethodError: ObjectMapper.constructType() doesn't exist in 2.15
```

**Why Happens**:
- Maven Assembly doesn't enforce reproducible ordering
- System classpath can interfere
- IDE classpath differs from Maven classpath

---

## Runtime Configuration Problems

### Problem 17: ADDON_BASE_URL Not Set or Incorrect

**Issue**: Application fails to start if ADDON_BASE_URL is missing or malformed.

**Location**: `_template-addon/TemplateAddonApp.java:8-9`

```java
String baseUrl = ConfigValidator.validateUrl(System.getenv("ADDON_BASE_URL"),
        "http://localhost:8080/_template-addon", "ADDON_BASE_URL");
```

**Failure Modes**:
```bash
# Not set at all:
ADDON_BASE_URL environment variable not provided, using default
# ↓ Falls back to template default, which is wrong

# Malformed URL:
ADDON_BASE_URL="localhost:8080/_template-addon"  # Missing http://
# ↓ Validation might pass but URLs don't match

# Trailing slash mismatch:
ADDON_BASE_URL="http://localhost:8080/_template-addon/"
# Manifest expects: http://localhost:8080/_template-addon/manifest.json
# Actual endpoint: http://localhost:8080/_template-addon//manifest.json (double slash)
```

**Why Developers Get This Wrong**:
- Template default is unhelpful (still says `_template-addon`)
- No error if environment variable used but misspelled
- Path extraction logic is complex (sanitize function)

---

### Problem 18: Port Conflicts

**Issue**: Default port 8080 conflicts with other services.

**Symptoms**:
```
java -jar my-addon.jar
java.net.BindException: Address already in use: bind
```

**New Addon Script Default**:
```bash
PORT=8080  # ← Hardcoded default
```

**Better Approach**:
```bash
# Use OS-provided ephemeral port
PORT=0  # Let OS assign available port
```

---

## Token Storage & Authentication Issues

### Problem 19: Token Store Not Initialized

**Issue**: Application doesn't properly initialize token storage, causing INSTALLED handler to fail silently.

**Expected in Lifecycle Handler**:
```java
tokenStore.save(workspaceId, token);
```

**If Token Store Missing**:
```
INSTALLED handler called
Token saved... (to nowhere)
No error visible
Later webhook handler:
tokenStore.get(workspaceId) → null
API call fails with 401 Unauthorized
Developer confused: "Why did installation succeed but webhooks don't work?"
```

**Why Happens**:
- Template provides InMemoryTokenStore
- Not suitable for production (lost on restart)
- DatabaseTokenStore requires configuration
- Documentation unclear which to use when

---

### Problem 20: WEBHOOK_SECRET Not Provided

**Issue**: Webhook signature validation fails if secret not configured.

**Expected Flow**:
```
1. addon.registerWebhookSecret("secret-value");
2. Webhook arrives with clockify-webhook-signature header
3. Signature validated against secret
```

**If Secret Missing**:
```bash
# Option 1: Skip validation (INSECURE)
ADDON_SKIP_SIGNATURE_VERIFY=true  # Never for production!

# Option 2: Validation fails
WebhookSignatureValidator.verify() → throws exception
Webhook handler returns 401
Clockify stops sending webhooks
```

---

## Testing & Validation Problems

### Problem 21: No Smoke Test Run After Generation

**Issue**: `scripts/new-addon.sh` doesn't run the smoke test like CI does.

**What Should Happen** (per `scripts/test-new-addon.sh`):
```bash
# After creating addon:
1. Verify manifest.json is valid JSON
2. Verify manifest has required fields (key, name, baseUrl)
3. Verify components array is non-empty
4. Verify template tokens removed from generated code
5. Verify TemplateAddonApp.java was renamed
```

**Current Behavior**:
```bash
# new-addon.sh just warns if validation fails:
echo "Warning: Manifest validation failed (but addon was created)" >&2
```

**Result**:
```bash
scripts/new-addon.sh my-addon "My Addon"
# ✓ Success message
# Developer thinks they're done
mvn package
# ✗ Fails due to issues caught by smoke test
```

---

### Problem 22: No Build Test After Generation

**Issue**: Script doesn't verify that generated addon can be built.

**What Should Be Tested**:
```bash
cd addons/my-addon
mvn clean package
# Should succeed without errors
```

**Current Behavior**: No build test performed

**Result**: Issues discovered only when developer tries to build manually later

---

## Documentation & Understanding Issues

### Problem 23: Template Addon is Confusing Starting Point

**Issues**:
- Has 3 endpoints (manifest, health, test)
- Has lifecycle handlers but no real logic
- Has webhook handler with no-op implementation
- README suggests manual steps but doesn't match script behavior
- No clear guidance on which parts to keep vs modify

**Confusing Code**:
```java
// TemplateAddonApp.java
addon.registerCustomEndpoint("/api/test", new TestController()::handle);
// What does TestController do? Why is it needed?
// When should I remove it?
```

**Better Approach**:
- Minimal template (only required endpoints)
- Clear TODO comments for "Customize here"
- Example addon implementations (auto-tag-assistant, rules) as learning resources

---

### Problem 24: Inconsistent Documentation Between README and Script

**Template README** (`addons/_template-addon/README.md:32-46`):
```markdown
## How to copy and rename

1. **Copy the folder** – duplicate addons/_template-addon...
2. **Update the Maven coordinates** – open pom.xml...
3. **Adjust the Java package** – rename from com.example.templateaddon...
4. **Rename the application class** – change TemplateAddonApp...
5. **Update manifest key and metadata**...
6. **Wire the new module in the parent build**...
7. **Search for TODOs**...
```

**But Script Documentation** (`scripts/new-addon.sh:5-14`):
```
This will:
1. Copy addons/_template-addon to addons/<addon-name>
2. Update package names, artifactId, manifest key, and baseUrl
3. Validate the manifest
4. Add the new module to the root pom.xml
```

**Problem**: They describe different processes! Developers don't know whether to use the script or follow README.

**Recommendation**: Have single source of truth (prefer script-based approach)

---

## Production Deployment Issues

### Problem 25: Database Token Store Not Configured

**Issue**: Production setup requires persistent token storage, but template uses in-memory storage.

**Template Default**:
```java
// Uses InMemoryTokenStore (lost on restart!)
// Need to switch to DatabaseTokenStore for production
```

**Problem**:
- No guidance on when to switch to DatabaseTokenStore
- Database setup not documented in template README
- Configuration complexity hidden until production

**Required Setup**:
```properties
# Database configuration needed:
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
# Plus: database migrations, schema setup
```

---

### Problem 26: No Production Checklist

**Issue**: Template doesn't include pre-deployment checklist.

**Missing Items**:
- [ ] WEBHOOK_SECRET configured
- [ ] Token store is persistent (not in-memory)
- [ ] Rate limiting enabled
- [ ] Logging configured for production
- [ ] Health endpoint responds
- [ ] Metrics endpoint works
- [ ] CORS properly configured for production domain
- [ ] Security headers set
- [ ] No debug mode enabled

---

## Dependency & Environment Issues

### Problem 27: Perl/Python3/jq/xmlstarlet Not Available

**Issue**: Script has cascading optional dependencies.

**Required Tools**:
- Perl (for string replacement)
- Python3 (fallback for string replacement)
- jq (for manifest.json updates)
- Sed (for pom.xml updates)

**Environments Where Issues Occur**:
- Alpine Linux (minimal image)
- Windows (WSL1/WSL2)
- GitHub Actions runners (sometimes missing)
- CI/CD containers (often stripped down)

**Error Output is Confusing**:
```bash
# If perl unavailable:
"Warning: Unable to rewrite Java packages automatically (missing perl/python3)."
# But it doesn't fail! Developer thinks it worked.
```

---

### Problem 28: Git Not Available When Running Script

**Issue**: `scripts/test-new-addon.sh` requires git to be available and in a git repo.

**Location**: `scripts/test-new-addon.sh:4`
```bash
REPO_ROOT=$(git rev-parse --show-toplevel)
```

**Fails If**:
- Running outside git repo
- Git not installed
- Not in a git checkout (downloaded as ZIP)

**Error**:
```
fatal: not a git repository (or any parent up to mount point)
```

---

### Problem 29: Incorrect Maven Version

**Issue**: Maven 3.9+ has different behaviors from earlier versions.

**Known Issues**:
- Dependency resolution changed
- Security restrictions changed
- Plugin execution timing different

**Symptoms**:
```
[ERROR] Invalid plugin descriptor for...
[ERROR] Failed to load goal...
```

---

## Summary of Root Causes

### By Category:

| Category | Problems | Severity |
|----------|----------|----------|
| **Tooling & Environment** | 27, 28, 14, 17 | HIGH |
| **Manifest Issues** | 11, 12, 13 | HIGH |
| **Package/Naming** | 6, 7, 8, 9 | MEDIUM |
| **Build Configuration** | 10, 15, 16 | MEDIUM |
| **Scripting Logic** | 1, 2, 3, 4, 5 | MEDIUM |
| **Testing & Validation** | 21, 22 | MEDIUM |
| **Documentation** | 23, 24 | LOW |
| **Configuration** | 17, 18, 19, 20 | MEDIUM |
| **Production Readiness** | 25, 26 | HIGH |

### By Impact:

**Block Addon Creation**:
- Problem 1 (Perl/Python dependency)
- Problem 3 (pom.xml registration)
- Problem 14 (Java 17 toolchain)

**Create Non-Functional Addon**:
- Problem 6 (Package naming)
- Problem 8 (pom.xml not updated)
- Problem 11 (manifest invalid)
- Problem 19 (token store)

**Discovered Too Late**:
- Problem 21 (no smoke test)
- Problem 22 (no build test)
- Problem 25 (database setup)
- Problem 26 (production checklist)

---

## Recommendations for Improvement

### Phase 1: Critical Fixes
1. **Make dependencies explicit**: Require Python3 only (remove Perl/jq dependency)
2. **Add mandatory smoke test**: Fail if addon doesn't pass validation
3. **Improve error messages**: Tell user exactly what went wrong and how to fix it
4. **Create minimal addon**: Simplify template to only essentials

### Phase 2: Quality Improvements
5. **Add build test**: Verify addon can build after generation
6. **Improve documentation**: Single source of truth (script-first)
7. **Add production checklist**: Template README includes pre-deployment steps
8. **Better package naming**: Validate and explain transformations

### Phase 3: Developer Experience
9. **Interactive scaffolding**: Ask user questions instead of command-line args
10. **Post-generation guide**: Print next steps and validation results
11. **Integrated testing**: `scripts/new-addon.sh` runs smoke + build tests
12. **Better error recovery**: If generation fails, clean up partial files

---

## Next Steps

This analysis should inform:
1. Priority of fixes in `scripts/new-addon.sh`
2. Updates to template addon
3. Improvements to documentation
4. New validation tools or checks
5. CI/CD pipeline updates

See related documents:
- `COMMON-MISTAKES.md` - Runtime errors after addon is created
- `SDK_OVERVIEW.md` - How to use the SDK
- `ARCHITECTURE.md` - How addons are structured

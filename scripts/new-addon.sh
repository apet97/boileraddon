#!/usr/bin/env bash
#
# Scaffold a new Clockify add-on from the addons/_template-addon module.
#
# Usage: scripts/new-addon.sh [--port <port>] [--base-path <path>] <addon-name> [display-name]
#
# Example:
#   scripts/new-addon.sh my-cool-addon "My Cool Add-on"
#
# This will:
# 1. Copy addons/_template-addon to addons/<addon-name>
# 2. Update package names, artifactId, manifest key, and baseUrl
# 3. Validate the manifest
# 4. Add the new module to the root pom.xml
# 5. Run smoke tests to verify generation
# 6. Build the addon to ensure it compiles
#
# Fixes applied:
# - Problem #1-2: Python3-only approach (no Perl/jq dependency)
# - Problem #3: Robust XML manipulation for pom.xml
# - Problem #4: Mandatory validation with cleanup on failure
# - Problem #6-7: Better package/class name validation
# - Problem #8: All pom.xml variables updated
# - Problem #14: Java version check
# - Problem #21-22: Smoke and build tests after generation
#

set -euo pipefail

# Color output helpers
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

error() {
  echo -e "${RED}Error: $*${NC}" >&2
}

warn() {
  echo -e "${YELLOW}Warning: $*${NC}" >&2
}

success() {
  echo -e "${GREEN}✓ $*${NC}"
}

info() {
  echo "  $*"
}

usage() {
  echo "Usage: $0 [--port <port>] [--base-path <path>] [--skip-tests] <addon-name> [display-name]" >&2
  echo "" >&2
  echo "Options:" >&2
  echo "  --port <port>        Port number (default: 8080)" >&2
  echo "  --base-path <path>   Base URL path (default: addon-name)" >&2
  echo "  --skip-tests         Skip smoke and build tests (not recommended)" >&2
  echo "" >&2
  echo "Example:" >&2
  echo "  $0 --port 8080 --base-path my-cool-addon my-cool-addon \"My Cool Add-on\"" >&2
}

# Cleanup function for failures
cleanup_on_error() {
  local dst_dir="$1"
  warn "Cleaning up failed addon generation..."
  if [ -d "$dst_dir" ]; then
    rm -rf "$dst_dir"
    info "Removed $dst_dir"
  fi
  # Restore pom.xml if we have a backup
  if [ -f "pom.xml.new-addon-backup" ]; then
    mv pom.xml.new-addon-backup pom.xml
    info "Restored pom.xml"
  fi
}

# Validate prerequisites
check_prerequisites() {
  info "Checking prerequisites..."

  # Check Python3 (REQUIRED - fixes Problem #1)
  if ! command -v python3 >/dev/null 2>&1; then
    error "Python3 is required but not found. Please install Python 3.6 or later."
    exit 1
  fi

  local py_version
  py_version=$(python3 --version 2>&1 | grep -oE '[0-9]+\.[0-9]+' | head -1)
  success "Python3 found: $py_version"

  # Check Java version (fixes Problem #14)
  if ! command -v java >/dev/null 2>&1; then
    error "Java is required but not found."
    exit 1
  fi

  local java_version
  java_version=$(java -version 2>&1 | grep -oE 'version "([0-9]+)' | grep -oE '[0-9]+' | head -1)

  if [ "$java_version" -lt 17 ]; then
    error "Java 17 or higher is required. Found: Java $java_version"
    error "Please install Java 17 or configure Maven toolchains.xml"
    error "See: https://maven.apache.org/guides/mini/guide-using-toolchains.html"
    exit 1
  fi
  success "Java found: version $java_version"

  # Check Maven
  if ! command -v mvn >/dev/null 2>&1; then
    error "Maven is required but not found."
    exit 1
  fi

  local mvn_version
  mvn_version=$(mvn --version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
  success "Maven found: $mvn_version"
}

# Parse command line arguments
PORT=8080
BASE_PATH=""
SKIP_TESTS=false

while [ $# -gt 0 ]; do
  case "$1" in
    --port)
      if [ $# -lt 2 ]; then
        error "--port requires a value"
        usage
        exit 2
      fi
      PORT="$2"
      shift 2
      ;;
    --base-path)
      if [ $# -lt 2 ]; then
        error "--base-path requires a value"
        usage
        exit 2
      fi
      BASE_PATH="$2"
      shift 2
      ;;
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --)
      shift
      break
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --*)
      error "Unknown option $1"
      usage
      exit 2
      ;;
    *)
      break
      ;;
  esac
done

if [ $# -lt 1 ]; then
  usage
  exit 2
fi

NAME_RAW="$1"
DISPLAY_NAME="${2:-Template Add-on}"

# Validate addon name (fixes Problem #6)
if [ -z "$NAME_RAW" ]; then
  error "Addon name cannot be empty"
  exit 2
fi

if [[ "$NAME_RAW" =~ ^[0-9] ]]; then
  error "Addon name cannot start with a number: '$NAME_RAW'"
  error "Please use a name starting with a letter, e.g., 'my-addon' or 'addon-123'"
  exit 2
fi

if [[ ! "$NAME_RAW" =~ ^[a-zA-Z0-9._-]+$ ]]; then
  error "Addon name contains invalid characters: '$NAME_RAW'"
  error "Allowed characters: letters, numbers, hyphens, underscores, dots"
  exit 2
fi

# Derive names with improved validation (fixes Problem #6-7)
PKG_NAME=$(echo "$NAME_RAW" | tr -cd '[:alnum:]_-' | tr '-' '_')

# Validate package name is not empty after transformation
if [ -z "$PKG_NAME" ]; then
  error "Package name cannot be derived from addon name: '$NAME_RAW'"
  exit 2
fi

# Validate package name doesn't start with number
if [[ "$PKG_NAME" =~ ^[0-9] ]]; then
  error "Derived package name cannot start with number: '$PKG_NAME'"
  error "Original name: '$NAME_RAW'"
  error "Please choose a different addon name"
  exit 2
fi

# Derive class name with better algorithm (fixes Problem #7)
NAME_CLASS_SOURCE=$(echo "$NAME_RAW" | tr -cd '[:alnum:]_-')
CLASS_PREFIX=$(echo "$NAME_CLASS_SOURCE" | tr '_-' ' ' | awk '{
  result = ""
  for (i = 1; i <= NF; i++) {
    word = $i
    if (word != "" && !match(word, /^[0-9]+$/)) {
      result = result toupper(substr(word, 1, 1)) substr(word, 2)
    }
  }
  print result
}')

# Fallback if class prefix is empty or invalid
if [ -z "$CLASS_PREFIX" ] || [[ "$CLASS_PREFIX" =~ ^[0-9] ]]; then
  CLASS_PREFIX="Addon"
  warn "Using default class name: ${CLASS_PREFIX}App"
fi

# Validate class name length
if [ ${#CLASS_PREFIX} -lt 2 ]; then
  warn "Class name '$CLASS_PREFIX' is very short. Consider using a longer addon name."
fi

ARTIFACT_ID=$(echo "$NAME_RAW" | tr -cd '[:alnum:]_.-')
KEY=$(echo "$NAME_RAW" | tr -cd '[:alnum:]._-' | tr '_' '-')

if [ -z "$BASE_PATH" ]; then
  BASE_PATH="$NAME_RAW"
fi

# Strip any leading/trailing slashes
BASE_PATH="${BASE_PATH#/}"
BASE_PATH="${BASE_PATH%/}"

SRC_DIR="addons/_template-addon"
DST_DIR="addons/$NAME_RAW"

# Check prerequisites
check_prerequisites

# Check if destination already exists
if [ -e "$DST_DIR" ]; then
  error "Destination $DST_DIR already exists"
  exit 3
fi

# Validate source template exists
if [ ! -d "$SRC_DIR" ]; then
  error "Template directory not found: $SRC_DIR"
  exit 1
fi

BASE_URL="http://localhost:${PORT}/${BASE_PATH}"
REMOTE_BASE_URL_HOST="https://YOUR-SUBDOMAIN.ngrok-free.app"
REMOTE_BASE_URL="$REMOTE_BASE_URL_HOST/${BASE_PATH}"
if [ -n "$BASE_PATH" ]; then
  REMOTE_MANIFEST_URL="$REMOTE_BASE_URL_HOST/${BASE_PATH}/manifest.json"
else
  REMOTE_MANIFEST_URL="$REMOTE_BASE_URL_HOST/manifest.json"
fi

echo ""
echo "================================"
echo "Creating new Clockify add-on"
echo "================================"
echo ""
info "Addon name:   $NAME_RAW"
info "Display name: $DISPLAY_NAME"
info "Package:      com.example.${PKG_NAME}"
info "Artifact ID:  $ARTIFACT_ID"
info "Manifest key: $KEY"
info "Main class:   ${CLASS_PREFIX}App"
info "Base URL:     $BASE_URL"
echo ""

# Backup pom.xml before modifying (for rollback on error)
cp pom.xml pom.xml.new-addon-backup

# Set trap to cleanup on error
trap 'cleanup_on_error "$DST_DIR"' ERR

# Copy template
echo "📦 Copying template..."
cp -r "$SRC_DIR" "$DST_DIR"
success "Template copied to $DST_DIR"

# Update all files using Python3 only (fixes Problem #1-2)
echo ""
echo "🔧 Updating files..."

python3 - "$DST_DIR" "$PKG_NAME" "$NAME_RAW" "$CLASS_PREFIX" "$DISPLAY_NAME" "$ARTIFACT_ID" "$BASE_URL" "$KEY" "$PORT" <<'PYTHON_SCRIPT'
import sys
import pathlib
import json
import re

dst_dir = pathlib.Path(sys.argv[1])
pkg_name = sys.argv[2]
name_raw = sys.argv[3]
class_prefix = sys.argv[4]
display_name = sys.argv[5]
artifact_id = sys.argv[6]
base_url = sys.argv[7]
key = sys.argv[8]
port = sys.argv[9]

# 1. Update Java files
print("  → Updating Java files...")
src_pkg_path = dst_dir / "src" / "main" / "java" / "com" / "example" / "templateaddon"
dst_pkg_path = dst_dir / "src" / "main" / "java" / "com" / "example" / pkg_name

# Create new package directory
dst_pkg_path.parent.mkdir(parents=True, exist_ok=True)
dst_pkg_path.mkdir(parents=True, exist_ok=True)

# Copy all files
if src_pkg_path.exists():
    for file in src_pkg_path.glob('*'):
        if file.is_file():
            (dst_pkg_path / file.name).write_text(file.read_text())

    # Remove old package directory
    import shutil
    shutil.rmtree(src_pkg_path)

# Rename main class file
old_class_file = dst_pkg_path / "TemplateAddonApp.java"
new_class_file = dst_pkg_path / f"{class_prefix}App.java"
if old_class_file.exists():
    old_class_file.rename(new_class_file)

# Update test files if they exist
test_src_path = dst_dir / "src" / "test" / "java" / "com" / "example" / "templateaddon"
test_dst_path = dst_dir / "src" / "test" / "java" / "com" / "example" / pkg_name

if test_src_path.exists():
    test_dst_path.parent.mkdir(parents=True, exist_ok=True)
    test_dst_path.mkdir(parents=True, exist_ok=True)

    for file in test_src_path.glob('*'):
        if file.is_file():
            (test_dst_path / file.name).write_text(file.read_text())

    shutil.rmtree(test_src_path)

# 2. Update content in all Java and HTML files
print("  → Updating package names and class names...")
for path in dst_dir.rglob('*'):
    if path.suffix in {'.java', '.html', '.md'}:
        try:
            text = path.read_text()
            original = text

            # Replace package names
            text = text.replace('com.example.templateaddon', f'com.example.{pkg_name}')

            # Replace addon name
            text = text.replace('_template-addon', name_raw)

            # Replace class names
            text = text.replace('TemplateAddonApp', f'{class_prefix}App')

            # Replace display names
            text = text.replace('Template Add-on', display_name)
            text = text.replace('Template add-on', display_name)

            # Only write if changed
            if text != original:
                path.write_text(text)
        except Exception as e:
            print(f"Warning: Could not update {path}: {e}", file=sys.stderr)

# 3. Update pom.xml (fixes Problem #8 - ALL variables)
print("  → Updating pom.xml...")
pom_path = dst_dir / "pom.xml"
if pom_path.exists():
    pom_text = pom_path.read_text()

    # Update artifact ID
    pom_text = re.sub(
        r'<artifactId>_template-addon</artifactId>',
        f'<artifactId>{artifact_id}</artifactId>',
        pom_text
    )

    # Update name
    pom_text = re.sub(
        r'<name>_template-addon</name>',
        f'<name>{artifact_id}</name>',
        pom_text
    )

    # Update main class reference
    pom_text = re.sub(
        r'<mainClass>com\.example\.templateaddon\.TemplateAddonApp</mainClass>',
        f'<mainClass>com.example.{pkg_name}.{class_prefix}App</mainClass>',
        pom_text
    )

    # Update JaCoCo include pattern
    pom_text = re.sub(
        r'<include>com\.example\.templateaddon\.\*</include>',
        f'<include>com.example.{pkg_name}.*</include>',
        pom_text
    )

    # Update any other template addon references in comments
    pom_text = re.sub(
        r'_template-addon',
        name_raw,
        pom_text
    )

    pom_path.write_text(pom_text)

# 4. Update manifest.json using proper JSON handling (fixes Problem #2)
print("  → Updating manifest.json...")
manifest_path = dst_dir / "manifest.json"
if manifest_path.exists():
    try:
        with open(manifest_path, 'r') as f:
            manifest = json.load(f)

        # Update core fields
        manifest['key'] = key
        manifest['name'] = display_name
        manifest['baseUrl'] = base_url

        # Update component labels
        if 'components' in manifest and isinstance(manifest['components'], list):
            for component in manifest['components']:
                if isinstance(component, dict) and 'label' in component:
                    if component['label'].lower() in ['template add-on', 'template addon']:
                        component['label'] = display_name

        # Write back with proper formatting
        with open(manifest_path, 'w') as f:
            json.dump(manifest, f, indent=2)
            f.write('\n')  # Add trailing newline

    except Exception as e:
        print(f"Error updating manifest: {e}", file=sys.stderr)
        sys.exit(1)

# 5. Create .env file
print("  → Creating .env file...")
env_path = dst_dir / ".env"
env_content = f"""ADDON_PORT={port}
ADDON_BASE_URL={base_url}
"""
env_path.write_text(env_content)

print("  ✓ All files updated successfully")

PYTHON_SCRIPT

success "Files updated"

# Add module to root pom.xml using Python (fixes Problem #3)
echo ""
echo "📝 Updating root pom.xml..."

python3 - "$NAME_RAW" <<'PYTHON_POM'
import sys
import xml.etree.ElementTree as ET

name_raw = sys.argv[1]

# Parse pom.xml
try:
    tree = ET.parse('pom.xml')
    root = tree.getroot()

    # Register namespace
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    ET.register_namespace('', 'http://maven.apache.org/POM/4.0.0')

    # Find modules element
    modules = root.find('.//m:modules', ns)

    if modules is None:
        print("Error: Could not find <modules> element in pom.xml", file=sys.stderr)
        sys.exit(1)

    # Check if module already exists
    module_path = f"addons/{name_raw}"
    existing = False
    for module in modules.findall('m:module', ns):
        if module.text and module.text.strip() == module_path:
            existing = True
            break

    if not existing:
        # Create new module element
        new_module = ET.Element('module')
        new_module.text = module_path
        modules.append(new_module)

        # Write back with proper formatting
        tree.write('pom.xml', encoding='utf-8', xml_declaration=True)
        print(f"  ✓ Added module '{module_path}' to pom.xml")
    else:
        print(f"  ℹ Module '{module_path}' already exists in pom.xml")

except Exception as e:
    print(f"Error: Failed to update pom.xml: {e}", file=sys.stderr)
    sys.exit(1)

PYTHON_POM

# Validate manifest (fixes Problem #4 - make it mandatory)
echo ""
echo "✅ Validating manifest..."

if [ -f "tools/validate-manifest.py" ]; then
  if ! python3 tools/validate-manifest.py "$DST_DIR/manifest.json"; then
    error "Manifest validation failed!"
    error "The generated manifest.json is invalid."
    exit 1
  fi
  success "Manifest validation passed"
else
  warn "Validator not found at tools/validate-manifest.py, skipping validation"
fi

# Run smoke tests (fixes Problem #21)
if [ "$SKIP_TESTS" = false ]; then
  echo ""
  echo "🧪 Running smoke tests..."

  # Check that template strings were replaced
  if grep -r "com.example.templateaddon" "$DST_DIR/src" >/dev/null 2>&1; then
    error "Found unreplaced template package name 'com.example.templateaddon'"
    grep -rn "com.example.templateaddon" "$DST_DIR/src" || true
    exit 1
  fi
  success "No template package names found"

  if grep -r "TemplateAddonApp" "$DST_DIR/src" >/dev/null 2>&1; then
    error "Found unreplaced class name 'TemplateAddonApp'"
    grep -rn "TemplateAddonApp" "$DST_DIR/src" || true
    exit 1
  fi
  success "No template class names found"

  if grep "Template Add-on" "$DST_DIR/manifest.json" >/dev/null 2>&1; then
    error "Found unreplaced display name in manifest.json"
    exit 1
  fi
  success "Manifest properly updated"

  # Verify manifest has required fields
  if ! python3 -c "import json; m=json.load(open('$DST_DIR/manifest.json')); exit(0 if all(k in m for k in ['key','name','baseUrl','schemaVersion']) else 1)"; then
    error "Manifest is missing required fields"
    exit 1
  fi
  success "Manifest has all required fields"

  # Run build test (fixes Problem #22)
  echo ""
  echo "🔨 Running build test..."
  info "Building addon to verify it compiles..."

  if mvn -f "$DST_DIR/pom.xml" clean package -DskipTests -q; then
    success "Build completed successfully"

    # Verify JAR was created
    jar_file="$DST_DIR/target/${ARTIFACT_ID}-0.1.0-jar-with-dependencies.jar"
    if [ -f "$jar_file" ]; then
      jar_size=$(ls -lh "$jar_file" | awk '{print $5}')
      success "JAR created: ${ARTIFACT_ID}-0.1.0-jar-with-dependencies.jar ($jar_size)"
    else
      warn "JAR file not found at expected location: $jar_file"
    fi
  else
    error "Build failed! Check the output above for errors."
    exit 1
  fi
fi

# Remove backup pom.xml since we succeeded
rm -f pom.xml.new-addon-backup

# Clear error trap
trap - ERR

echo ""
echo "========================================"
echo "✅ Add-on created successfully!"
echo "========================================"
echo ""
success "Location: $DST_DIR"
echo ""
echo "📋 Next steps:"
echo ""
info "1. Review the generated code:"
info "   - $DST_DIR/src/main/java/com/example/${PKG_NAME}/${CLASS_PREFIX}App.java"
info "   - $DST_DIR/src/main/java/com/example/${PKG_NAME}/WebhookHandlers.java"
info "   - $DST_DIR/src/main/java/com/example/${PKG_NAME}/LifecycleHandlers.java"
echo ""
info "2. Run it locally:"
info "   cd $DST_DIR"
info "   ADDON_PORT=$PORT ADDON_BASE_URL=$BASE_URL java -jar target/${ARTIFACT_ID}-0.1.0-jar-with-dependencies.jar"
echo ""
info "3. For development with auto-reload, use your IDE or:"
info "   cd $DST_DIR"
info "   mvn exec:java -Dexec.mainClass=\"com.example.${PKG_NAME}.${CLASS_PREFIX}App\""
echo ""
info "4. Expose with ngrok for testing with Clockify:"
info "   ngrok http $PORT"
echo ""
info "5. Update the manifest baseUrl in Clockify Developer Console to match ngrok URL:"
info "   Example: $REMOTE_MANIFEST_URL"
echo ""
info "6. Customize your addon logic as needed"
echo ""

if [ "$SKIP_TESTS" = true ]; then
  warn "Tests were skipped. Run 'mvn verify' manually to ensure everything works."
fi

echo ""
success "Happy coding! 🚀"
echo ""

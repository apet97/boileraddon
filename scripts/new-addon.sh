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
#

set -euo pipefail

usage() {
  echo "Usage: $0 [--port <port>] [--base-path <path>] <addon-name> [display-name]" >&2
  echo "" >&2
  echo "Example:" >&2
  echo "  $0 --port 8080 --base-path my-cool-addon my-cool-addon \"My Cool Add-on\"" >&2
}

PORT=8080
BASE_PATH=""

while [ $# -gt 0 ]; do
  case "$1" in
    --port)
      if [ $# -lt 2 ]; then
        echo "Error: --port requires a value" >&2
        usage
        exit 2
      fi
      PORT="$2"
      shift 2
      ;;
    --base-path)
      if [ $# -lt 2 ]; then
        echo "Error: --base-path requires a value" >&2
        usage
        exit 2
      fi
      BASE_PATH="$2"
      shift 2
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
      echo "Error: Unknown option $1" >&2
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

if [ -z "$BASE_PATH" ]; then
  BASE_PATH="$NAME_RAW"
fi

# Strip any leading/trailing slashes to avoid double slashes when constructing URLs
BASE_PATH="${BASE_PATH#/}"
BASE_PATH="${BASE_PATH%/}"

# Sanitize names for different contexts
PKG_NAME=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_' | tr '-' '_')
ARTIFACT_ID=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_.')
KEY=$(echo "$NAME_RAW" | tr -cd '[:alnum:].-_' | tr '_' '-')

SRC_DIR="addons/_template-addon"
DST_DIR="addons/$NAME_RAW"

# Check if destination already exists
if [ -e "$DST_DIR" ]; then
  echo "Error: Destination $DST_DIR already exists" >&2
  exit 3
fi

BASE_URL="http://localhost:${PORT}/${BASE_PATH}"
REMOTE_BASE_URL_HOST="https://YOUR-SUBDOMAIN.ngrok-free.app"
REMOTE_BASE_URL="$REMOTE_BASE_URL_HOST/${BASE_PATH}"
if [ -n "$BASE_PATH" ]; then
  REMOTE_MANIFEST_URL="$REMOTE_BASE_URL_HOST/${BASE_PATH}/manifest.json"
else
  REMOTE_MANIFEST_URL="$REMOTE_BASE_URL_HOST/manifest.json"
fi

echo "Creating new add-on: $NAME_RAW"
echo "  Package name: com.example.${PKG_NAME}"
echo "  Artifact ID:  $ARTIFACT_ID"
echo "  Manifest key: $KEY"
echo "  Base URL:     $BASE_URL"
echo ""

# Copy template using cp -r (POSIX compliant, no rsync needed)
echo "Copying template..."
cp -r "$SRC_DIR" "$DST_DIR"

# Update pom.xml
echo "Updating pom.xml..."
if command -v perl >/dev/null 2>&1; then
  # Use perl if available (most systems)
  perl -0777 -pe "s#<artifactId>_template-addon</artifactId>#<artifactId>${ARTIFACT_ID}</artifactId>#; s#<name>_template-addon</name>#<name>${ARTIFACT_ID}</name>#" -i "$DST_DIR/pom.xml"
else
  # Fallback to sed (less reliable for multi-line but works for our case)
  sed -i.bak "s#<artifactId>_template-addon</artifactId>#<artifactId>${ARTIFACT_ID}</artifactId>#g; s#<name>_template-addon</name>#<name>${ARTIFACT_ID}</name>#g" "$DST_DIR/pom.xml"
  rm -f "$DST_DIR/pom.xml.bak"
fi

# Update package structure
echo "Updating package names..."
SRC_PKG_PATH="com/example/templateaddon"
DST_PKG_PATH="com/example/${PKG_NAME}"

mkdir -p "$DST_DIR/src/main/java/$DST_PKG_PATH"
cp -R "$DST_DIR/src/main/java/$SRC_PKG_PATH"/* "$DST_DIR/src/main/java/$DST_PKG_PATH"/
rm -rf "$DST_DIR/src/main/java/$SRC_PKG_PATH"

if [ -d "$DST_DIR/src/test/java" ]; then
  mkdir -p "$DST_DIR/src/test/java/$DST_PKG_PATH"
  if [ -d "$DST_DIR/src/test/java/$SRC_PKG_PATH" ]; then
    cp -R "$DST_DIR/src/test/java/$SRC_PKG_PATH"/* "$DST_DIR/src/test/java/$DST_PKG_PATH"/ 2>/dev/null || true
    rm -rf "$DST_DIR/src/test/java/$SRC_PKG_PATH"
  fi
fi

if command -v perl >/dev/null 2>&1; then
  find "$DST_DIR/src" -type f -name '*.java' -exec perl -pi -e "s/com\\.example\\.templateaddon/com.example.${PKG_NAME}/g; s/_template-addon/${NAME_RAW}/g" {} +
elif command -v python3 >/dev/null 2>&1; then
  python3 - "$DST_DIR" "$PKG_NAME" "$NAME_RAW" <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1])
pkg = sys.argv[2]
name = sys.argv[3]
for path in root.rglob('*.java'):
    text = path.read_text()
    text = text.replace('com.example.templateaddon', f'com.example.{pkg}')
    text = text.replace('_template-addon', name)
    path.write_text(text)
PY
else
  echo "Warning: Unable to rewrite Java packages automatically (missing perl/python3)." >&2
  echo "         Update occurrences of 'com.example.templateaddon' and '_template-addon' manually." >&2
fi

# Update manifest.json
echo "Updating manifest.json..."

if command -v jq >/dev/null 2>&1; then
  # Use jq if available (recommended)
  jq \
    --arg key "$KEY" \
    --arg name "$DISPLAY_NAME" \
    --arg url "$BASE_URL" \
    '.key=$key | .name=$name | .baseUrl=$url' \
    "$DST_DIR/manifest.json" > "$DST_DIR/manifest.json.tmp" && mv "$DST_DIR/manifest.json.tmp" "$DST_DIR/manifest.json"
else
  # Fallback to sed (less reliable but works)
  sed -i.bak "s#\"key\": \".*\"#\"key\": \"$KEY\"#g; s#\"name\": \".*\"#\"name\": \"$DISPLAY_NAME\"#g; s#\"baseUrl\": \".*\"#\"baseUrl\": \"$BASE_URL\"#g" "$DST_DIR/manifest.json"
  rm -f "$DST_DIR/manifest.json.bak"
fi

echo "Writing .env seed file..."
cat > "$DST_DIR/.env" <<EOF
ADDON_PORT=$PORT
ADDON_BASE_URL=$BASE_URL
EOF

# Update main class reference in pom.xml
echo "Updating main class reference..."
if command -v perl >/dev/null 2>&1; then
  perl -0777 -pe "s#<mainClass>com.example.templateaddon.TemplateAddonApp</mainClass>#<mainClass>com.example.${PKG_NAME}.TemplateAddonApp</mainClass>#" -i "$DST_DIR/pom.xml"
else
  sed -i.bak "s#<mainClass>com.example.templateaddon.TemplateAddonApp</mainClass>#<mainClass>com.example.${PKG_NAME}.TemplateAddonApp</mainClass>#g" "$DST_DIR/pom.xml"
  rm -f "$DST_DIR/pom.xml.bak"
fi

# Add module to root pom.xml
echo "Adding module to root pom.xml..."
if grep -q "<module>addons/$NAME_RAW</module>" pom.xml 2>/dev/null; then
  echo "  (module already exists in pom.xml)"
else
  # Add before the closing </modules> tag
  if command -v perl >/dev/null 2>&1; then
    perl -0777 -pe "s#(</modules>)#    <module>addons/$NAME_RAW</module>\n    \$1#" -i pom.xml
  else
    sed -i.bak "s#</modules>#    <module>addons/$NAME_RAW</module>\n    </modules>#" pom.xml
    rm -f pom.xml.bak
  fi
  echo "  ✓ Added to pom.xml"
fi

# Validate manifest
echo "Validating manifest..."
if [ -f "tools/validate-manifest.py" ]; then
  python3 tools/validate-manifest.py "$DST_DIR/manifest.json" || {
    echo "Warning: Manifest validation failed (but addon was created)" >&2
  }
else
  echo "  (validator not found, skipping)"
fi

echo ""
echo "================================"
echo "✓ Add-on created successfully!"
echo "================================"
echo ""
echo "Location: $DST_DIR"
echo ""
echo "Next steps:"
echo ""
echo "  1. Build the add-on:"
echo "     mvn -f $DST_DIR/pom.xml clean package"
echo ""
echo "  2. Run it locally:"
echo "     ADDON_PORT=$PORT ADDON_BASE_URL=$BASE_URL java -jar $DST_DIR/target/${ARTIFACT_ID}-0.1.0-jar-with-dependencies.jar"
echo ""
echo "  3. Expose with ngrok:"
echo "     ngrok http $PORT"
echo ""
echo "  4. Update manifest baseUrl in $DST_DIR/manifest.json"
echo "     to: $REMOTE_BASE_URL"
echo ""
echo "  5. Install in Clockify using manifest URL:"
echo "     $REMOTE_MANIFEST_URL"
echo ""
echo "  6. Customize the logic in:"
echo "     - $DST_DIR/src/main/java/$DST_PKG_PATH/WebhookHandlers.java"
echo "     - $DST_DIR/src/main/java/$DST_PKG_PATH/LifecycleHandlers.java"
echo ""

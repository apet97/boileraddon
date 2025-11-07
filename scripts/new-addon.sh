#!/usr/bin/env bash
#
# Scaffold a new Clockify add-on from the java-basic-addon template.
#
# Usage: scripts/new-addon.sh <addon-name> [display-name]
#
# Example:
#   scripts/new-addon.sh my-cool-addon "My Cool Add-on"
#
# This will:
# 1. Copy templates/java-basic-addon to addons/<addon-name>
# 2. Update package names, artifactId, manifest key, and baseUrl
# 3. Validate the manifest
# 4. Add the new module to the root pom.xml
#

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <addon-name> [display-name]" >&2
  echo "" >&2
  echo "Example:" >&2
  echo "  $0 my-cool-addon \"My Cool Add-on\"" >&2
  exit 2
fi

NAME_RAW="$1"
DISPLAY_NAME="${2:-Java Basic Addon}"

# Sanitize names for different contexts
PKG_NAME=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_' | tr '-' '_')
ARTIFACT_ID=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_.')
KEY=$(echo "$NAME_RAW" | tr -cd '[:alnum:].-_' | tr '_' '-')

SRC_DIR="templates/java-basic-addon"
DST_DIR="addons/$NAME_RAW"

# Check if destination already exists
if [ -e "$DST_DIR" ]; then
  echo "Error: Destination $DST_DIR already exists" >&2
  exit 3
fi

echo "Creating new add-on: $NAME_RAW"
echo "  Package name: com.example.${PKG_NAME}"
echo "  Artifact ID:  $ARTIFACT_ID"
echo "  Manifest key: $KEY"
echo ""

# Copy template using cp -r (POSIX compliant, no rsync needed)
echo "Copying template..."
cp -r "$SRC_DIR" "$DST_DIR"

# Update pom.xml
echo "Updating pom.xml..."
if command -v perl >/dev/null 2>&1; then
  # Use perl if available (most systems)
  perl -0777 -pe "s#<artifactId>java-basic-addon</artifactId>#<artifactId>${ARTIFACT_ID}</artifactId>#; s#<name>java-basic-addon</name>#<name>${ARTIFACT_ID}</name>#" -i "$DST_DIR/pom.xml"
else
  # Fallback to sed (less reliable for multi-line but works for our case)
  sed -i.bak "s#<artifactId>java-basic-addon</artifactId>#<artifactId>${ARTIFACT_ID}</artifactId>#g; s#<name>java-basic-addon</name>#<name>${ARTIFACT_ID}</name>#g" "$DST_DIR/pom.xml"
  rm -f "$DST_DIR/pom.xml.bak"
fi

# Update package structure
echo "Updating package names..."
SRC_PKG_PATH="com/example/addon"
DST_PKG_PATH="com/example/${PKG_NAME}"

mkdir -p "$DST_DIR/src/main/java/$DST_PKG_PATH"
mkdir -p "$DST_DIR/src/test/java/$DST_PKG_PATH"

# Move and update Java files
for f in "$DST_DIR/src/main/java/$SRC_PKG_PATH"/*.java; do
  if [ -f "$f" ]; then
    filename=$(basename "$f")
    sed "s/package com.example.addon;/package com.example.${PKG_NAME};/g" "$f" > "$DST_DIR/src/main/java/$DST_PKG_PATH/$filename"
  fi
done
rm -rf "$DST_DIR/src/main/java/$SRC_PKG_PATH"

# Move and update test files if they exist
if [ -d "$DST_DIR/src/test/java/$SRC_PKG_PATH" ]; then
  for f in "$DST_DIR/src/test/java/$SRC_PKG_PATH"/*.java; do
    if [ -f "$f" ]; then
      filename=$(basename "$f")
      sed "s/package com.example.addon;/package com.example.${PKG_NAME};/g" "$f" > "$DST_DIR/src/test/java/$DST_PKG_PATH/$filename"
    fi
  done
  rm -rf "$DST_DIR/src/test/java/$SRC_PKG_PATH"
fi

# Update manifest.json
echo "Updating manifest.json..."
BASE_URL="http://localhost:8080/${NAME_RAW}"

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

# Update main class reference in pom.xml
echo "Updating main class reference..."
if command -v perl >/dev/null 2>&1; then
  perl -0777 -pe "s#<mainClass>com.example.addon.AddonApplication</mainClass>#<mainClass>com.example.${PKG_NAME}.AddonApplication</mainClass>#" -i "$DST_DIR/pom.xml"
else
  sed -i.bak "s#<mainClass>com.example.addon.AddonApplication</mainClass>#<mainClass>com.example.${PKG_NAME}.AddonApplication</mainClass>#g" "$DST_DIR/pom.xml"
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
echo "     java -jar $DST_DIR/target/${ARTIFACT_ID}-0.1.0-jar-with-dependencies.jar"
echo ""
echo "  3. Expose with ngrok:"
echo "     ngrok http 8080"
echo ""
echo "  4. Update manifest baseUrl in $DST_DIR/manifest.json"
echo "     to: https://YOUR-SUBDOMAIN.ngrok-free.app/${NAME_RAW}"
echo ""
echo "  5. Install in Clockify using manifest URL:"
echo "     https://YOUR-SUBDOMAIN.ngrok-free.app/${NAME_RAW}/manifest.json"
echo ""
echo "  6. Customize the logic in:"
echo "     - $DST_DIR/src/main/java/$DST_PKG_PATH/WebhookHandlers.java"
echo "     - $DST_DIR/src/main/java/$DST_PKG_PATH/LifecycleHandlers.java"
echo ""

#!/usr/bin/env bash
set -euo pipefail
if [ $# -lt 1 ]; then
  echo "Usage: scripts/new-addon.sh <addon-name> [display-name]" >&2
  exit 2
fi
NAME_RAW="$1"
DISPLAY_NAME="${2:-Java Basic Addon}"
PKG_NAME=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_' | tr '-' '_' )
ARTIFACT_ID=$(echo "$NAME_RAW" | tr -cd '[:alnum:]-_.' )
KEY=$(echo "$NAME_RAW" | tr -cd '[:alnum:].-_' | tr '_' '-' )
SRC_DIR="templates/java-basic-addon"
DST_DIR="addons/$NAME_RAW"

if [ -e "$DST_DIR" ]; then
  echo "Destination $DST_DIR already exists" >&2
  exit 3
fi

rsync -a "$SRC_DIR/" "$DST_DIR/"

# Rewrite pom.xml artifactId and name
perl -0777 -pe "s#<artifactId>java-basic-addon</artifactId>#<artifactId>${ARTIFACT_ID}</artifactId>#; s#<name>java-basic-addon</name>#<name>${ARTIFACT_ID}</name>#" -i "$DST_DIR/pom.xml"

# Rewrite package path by moving files and updating package declarations
SRC_PKG_PATH="com/example/addon"
DST_PKG_PATH="com/example/${PKG_NAME}"
mkdir -p "$DST_DIR/src/main/java/com/example/${PKG_NAME}"
mkdir -p "$DST_DIR/src/test/java/com/example/${PKG_NAME}"
for f in $(ls "$DST_DIR/src/main/java/${SRC_PKG_PATH}"); do
  sed "s/package com.example.addon;/package com.example.${PKG_NAME};/g" "$DST_DIR/src/main/java/${SRC_PKG_PATH}/$f" > "$DST_DIR/src/main/java/${DST_PKG_PATH}/$f"
  rm "$DST_DIR/src/main/java/${SRC_PKG_PATH}/$f"
done
rmdir "$DST_DIR/src/main/java/${SRC_PKG_PATH}" || true

if [ -f "$DST_DIR/src/test/java/${SRC_PKG_PATH}/ManifestValidationTest.java" ]; then
  sed "s/package com.example.addon;/package com.example.${PKG_NAME};/g" "$DST_DIR/src/test/java/${SRC_PKG_PATH}/ManifestValidationTest.java" > "$DST_DIR/src/test/java/${DST_PKG_PATH}/ManifestValidationTest.java"
  rm -rf "$DST_DIR/src/test/java/${SRC_PKG_PATH}"
fi

# Rewrite manifest.json key, name, baseUrl
BASE_URL="http://localhost:8080/${NAME_RAW}"
jq \
  --arg key "$KEY" \
  --arg name "$DISPLAY_NAME" \
  --arg url "$BASE_URL" \
  '.key=$key | .name=$name | .baseUrl=$url' \
  "$DST_DIR/manifest.json" > "$DST_DIR/manifest.json.tmp" && mv "$DST_DIR/manifest.json.tmp" "$DST_DIR/manifest.json"

# Validate
make validate

echo "Created $DST_DIR"

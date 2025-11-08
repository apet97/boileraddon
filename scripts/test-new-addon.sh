#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

TMP_NAME="smoke-addon-$$"
DISPLAY_NAME="Smoke Add-on"
STUB_ENV_DIR=$(mktemp -d)

cleanup() {
  rm -rf "addons/$TMP_NAME"
  git checkout -- pom.xml >/dev/null 2>&1 || true
  rm -rf "$STUB_ENV_DIR"
}
trap cleanup EXIT

mkdir -p "$STUB_ENV_DIR/jsonschema"
cat > "$STUB_ENV_DIR/jsonschema/__init__.py" <<'PY'
class Draft7Validator:
    def __init__(self, schema):
        self.schema = schema

    def iter_errors(self, instance):
        return []


def validate(instance, schema):
    return True
PY

PYTHONPATH="$STUB_ENV_DIR${PYTHONPATH:+:$PYTHONPATH}" scripts/new-addon.sh "$TMP_NAME" "$DISPLAY_NAME" >/dev/null

MANIFEST_PATH="addons/$TMP_NAME/manifest.json"

python3 - "$MANIFEST_PATH" "$DISPLAY_NAME" <<'PY'
import json
import sys

manifest_path, expected_label = sys.argv[1:3]

with open(manifest_path, "r", encoding="utf-8") as fh:
    manifest = json.load(fh)

missing = [field for field in ("key", "name", "baseUrl", "components") if field not in manifest]
if missing:
    print(
        "Smoke test failed: manifest missing required fields: " + ", ".join(missing),
        file=sys.stderr,
    )
    sys.exit(1)

components = manifest.get("components")
if not isinstance(components, list) or not components:
    print("Smoke test failed: manifest.components must be a non-empty list.", file=sys.stderr)
    sys.exit(1)

labels = [comp.get("label") for comp in components if isinstance(comp, dict) and "label" in comp]
if expected_label not in labels:
    print(
        f"Smoke test failed: expected component label '{expected_label}' not found in manifest.",
        file=sys.stderr,
    )
    sys.exit(1)
PY

CLASS_SOURCE=$(echo "$TMP_NAME" | tr -cd '[:alnum:]_-')
CLASS_PREFIX=$(echo "$CLASS_SOURCE" | tr '_-' ' ' | awk '{for (i = 1; i <= NF; i++) { if ($i != "") { printf "%s%s", toupper(substr($i, 1, 1)), substr($i, 2); } }}')
if [ -z "$CLASS_PREFIX" ]; then
  CLASS_PREFIX="Addon"
fi

if find "addons/$TMP_NAME" -type f -name 'TemplateAddonApp.java' | grep -q .; then
  echo "Smoke test failed: TemplateAddonApp.java should have been renamed." >&2
  exit 1
fi

if ! find "addons/$TMP_NAME" -type f -name "${CLASS_PREFIX}App.java" | grep -q .; then
  echo "Smoke test failed: ${CLASS_PREFIX}App.java was not created." >&2
  exit 1
fi

if rg -q 'Template Add-on|TemplateAddonApp|Template add-on' "addons/$TMP_NAME"; then
  echo "Smoke test failed: template tokens remain in generated add-on." >&2
  exit 1
fi

echo "✓ new-addon.sh smoke test passed"

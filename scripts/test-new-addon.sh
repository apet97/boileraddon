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

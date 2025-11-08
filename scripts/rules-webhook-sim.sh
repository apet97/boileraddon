#!/usr/bin/env bash
set -euo pipefail

# Simulate a Clockify webhook by computing HMAC signature and POSTing to /webhook.
# Requires:
#   - ADDON_BASE_URL (defaults to http://localhost:8080/rules)
#   - WORKSPACE_ID (required)
#   - CLOCKIFY_INSTALLATION_TOKEN (shared secret) (required)

BASE_URL="${ADDON_BASE_URL:-http://localhost:8080/rules}"
WS="${WORKSPACE_ID:-}"
SECRET="${CLOCKIFY_INSTALLATION_TOKEN:-}"

if [[ -z "$WS" || -z "$SECRET" ]]; then
  echo "WORKSPACE_ID and CLOCKIFY_INSTALLATION_TOKEN are required." >&2
  echo "Example: export WORKSPACE_ID=ws-1; export CLOCKIFY_INSTALLATION_TOKEN=raw-installation-jwt" >&2
  exit 1
fi

BODY='{
  "workspaceId": "'"$WS"'",
  "event": "TIME_ENTRY_CREATED",
  "timeEntry": {"id":"e1","description":"Client meeting","tagIds":[]}
}'

sig_hex=""
if command -v openssl >/dev/null 2>&1; then
  sig_hex=$(printf "%s" "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | xxd -p -c 256)
else
  # Fallback to Python
  sig_hex=$(python3 - << 'PY'
import hmac, hashlib, os, sys
secret = os.environ.get('CLOCKIFY_INSTALLATION_TOKEN','').encode('utf-8')
body = os.environ.get('BODY','').encode('utf-8')
print(hmac.new(secret, body, hashlib.sha256).hexdigest())
PY
  )
fi

HEADER="sha256=${sig_hex}"

echo "POST $BASE_URL/webhook with clockify-webhook-signature: $HEADER"
curl -sS -X POST "$BASE_URL/webhook" \
  -H "Content-Type: application/json" \
  -H "clockify-webhook-signature: $HEADER" \
  -d "$BODY" | sed -e 's/^/  /'
echo


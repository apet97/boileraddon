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

BODY='{"workspaceId":"'"$WS"'","event":"NEW_TIME_ENTRY","timeEntry":{"id":"e1","description":"Client meeting","tagIds":[]}}'

# Compute HMAC via Python for consistency
sig_hex=$(BODY="$BODY" CLOCKIFY_INSTALLATION_TOKEN="$SECRET" python3 - << 'PY'
import hmac, hashlib, os
secret = os.environ['CLOCKIFY_INSTALLATION_TOKEN'].encode('utf-8')
body = os.environ['BODY'].encode('utf-8')
print(hmac.new(secret, body, hashlib.sha256).hexdigest())
PY
)

HEADER="sha256=${sig_hex}"

echo "POST $BASE_URL/webhook with clockify-webhook-signature: $HEADER"
curl -sS -X POST "$BASE_URL/webhook" \
  -H "Content-Type: application/json" \
  -H "clockify-webhook-signature: $HEADER" \
  -d "$BODY" | sed -e 's/^/  /'
echo

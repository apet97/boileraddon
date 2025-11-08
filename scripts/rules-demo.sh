#!/usr/bin/env bash
set -euo pipefail

# Seed a demo rule and exercise the dry-run endpoint.
# Requires:
#   - ADDON_BASE_URL (defaults to http://localhost:8080/rules)
#   - WORKSPACE_ID (required)

BASE_URL="${ADDON_BASE_URL:-http://localhost:8080/rules}"
WS="${WORKSPACE_ID:-}"

if [[ -z "$WS" ]]; then
  echo "WORKSPACE_ID is required. Example: export WORKSPACE_ID=your-ws-id" >&2
  exit 1
fi

echo "Seeding demo rule at $BASE_URL for workspace $WS ..."

RULE_PAYLOAD='{
  "name": "Tag meetings",
  "enabled": true,
  "combinator": "AND",
  "conditions": [
    {"type": "descriptionContains", "operator": "CONTAINS", "value": "meeting"}
  ],
  "actions": [
    {"type": "add_tag", "args": {"tag": "billable"}}
  ]
}'

curl -sS -X POST "$BASE_URL/api/rules?workspaceId=$WS" \
  -H 'Content-Type: application/json' \
  -d "$RULE_PAYLOAD" | sed -e 's/^/  /'
echo

echo "Dry-run test (no side effects)..."
TEST_PAYLOAD=$(printf '{"workspaceId":"%s","timeEntry":{"id":"e1","description":"Client meeting","tagIds":[]}}' "$WS")
curl -sS -X POST "$BASE_URL/api/test" \
  -H 'Content-Type: application/json' \
  -d "$TEST_PAYLOAD" | sed -e 's/^/  /'
echo

echo "Done. To apply actions on real webhooks, export RULES_APPLY_CHANGES=true and send signed webhooks."


#!/usr/bin/env bash
set -euo pipefail
if ! command -v swagger-cli >/dev/null 2>&1; then
  echo "Install swagger-cli: npm i -g swagger-cli" >&2
  exit 1
fi
swagger-cli validate dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json

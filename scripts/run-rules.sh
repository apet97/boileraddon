#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"

PORT="${PORT:-8080}"
BASE_URL="${BASE_URL:-}"
APPLY="${APPLY:-false}"

usage() {
  cat <<EOF
Run the Rules add-on (builds jar and starts Jetty).

Usage:
  bash scripts/run-rules.sh [--port <port>] [--base-url <url>] [--use-ngrok] [--apply]

Options:
  --port <n>       Local port to listen on (default: 8080)
  --base-url <url> Base URL to advertise (e.g., http://localhost:8080/rules or https://<ngrok>/rules)
  --use-ngrok      Discover https public_url from local ngrok API and use <url>/rules
  --apply          Set RULES_APPLY_CHANGES=true to mutate Clockify entries

Examples:
  bash scripts/run-rules.sh                # local HTTP
  bash scripts/run-rules.sh --use-ngrok    # requires: ngrok http 8080 in another terminal
  bash scripts/run-rules.sh --base-url https://abc123.ngrok-free.app/rules
EOF
}

discover_ngrok() {
  local api="http://127.0.0.1:4040/api/tunnels"
  curl -fsSL "$api" | awk -F '"' '/public_url/ && $4 ~ /^https:/{print $4; exit}'
}

sanitize_base() {
  local in="$1"
  # Trim leading/trailing whitespace
  in="${in##*( )}"; in="${in%%*( )}"
  # Guard against spaces anywhere in URL
  if printf '%s' "$in" | grep -qE '\\s'; then
    echo "ERROR: Base URL contains spaces: '$in'" >&2
    echo "Hint: copy-paste without spaces, e.g. --base-url \"https://<sub>.ngrok-free.app/rules\"" >&2
    exit 2
  fi
  in="${in%/manifest.json}"
  in="${in%/}"
  if [[ "$in" != */rules ]]; then in="$in/rules"; fi
  echo "$in"
}

USE_NGROK=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --port) PORT="$2"; shift 2;;
    --base-url) BASE_URL="$2"; shift 2;;
    --use-ngrok) USE_NGROK=true; shift 1;;
    --apply) APPLY=true; shift 1;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1"; usage; exit 2;;
  esac
done

cd "$ROOT"
echo "Building addon-sdk (install to ~/.m2)…"
mvn -q -e -Dmaven.test.skip=true -pl addons/addon-sdk -am install
echo "Building rules addon…"
mvn -q -e -Dmaven.test.skip=true -pl addons/rules -am package

JAR=$(ls -1 addons/rules/target/rules-*-jar-with-dependencies.jar | head -n 1 || true)
if [[ -z "${JAR:-}" ]]; then
  echo "Could not find rules jar-with-dependencies under addons/rules/target" >&2
  exit 4
fi

if $USE_NGROK; then
  NGROK_BASE=$(discover_ngrok || true)
  if [[ -z "${NGROK_BASE:-}" ]]; then
    echo "Could not detect an https ngrok tunnel. Start it with: ngrok http ${PORT}" >&2
    exit 3
  fi
  BASE_URL=$(sanitize_base "$NGROK_BASE")
else
  BASE_URL=${BASE_URL:-"http://localhost:${PORT}/rules"}
  BASE_URL=$(sanitize_base "$BASE_URL")
fi

echo "================================"
echo "Starting Rules Add-on..."
echo "Base URL: ${BASE_URL}"
echo "Manifest: ${BASE_URL}/manifest.json"
echo "Rules API: ${BASE_URL}/api/rules"
echo "================================"

# Security defaults: lock embedding to Clockify and restrict CORS to Clockify origins unless overridden
FRAME_ANCESTORS_DEFAULT="'self' https://*.clockify.me"
if [[ -z "${ADDON_FRAME_ANCESTORS:-}" ]]; then
  ADDON_FRAME_ANCESTORS="$FRAME_ANCESTORS_DEFAULT"
  echo "Security: ADDON_FRAME_ANCESTORS not set → defaulting to: $ADDON_FRAME_ANCESTORS"
else
  echo "Security: ADDON_FRAME_ANCESTORS is set → $ADDON_FRAME_ANCESTORS"
fi

CORS_DEFAULT="https://app.clockify.me,https://developer.clockify.me"
if [[ -z "${ADDON_CORS_ORIGINS:-}" ]]; then
  ADDON_CORS_ORIGINS="$CORS_DEFAULT"
  echo "Security: ADDON_CORS_ORIGINS not set → defaulting to: $ADDON_CORS_ORIGINS"
else
  echo "Security: ADDON_CORS_ORIGINS is set → $ADDON_CORS_ORIGINS"
fi
# Credentials default to false (safe). Override by exporting ADDON_CORS_ALLOW_CREDENTIALS=true if needed.
ADDON_CORS_ALLOW_CREDENTIALS=${ADDON_CORS_ALLOW_CREDENTIALS:-false}
echo "Security: ADDON_CORS_ALLOW_CREDENTIALS=${ADDON_CORS_ALLOW_CREDENTIALS}"

if [[ "$APPLY" == "true" ]]; then
  echo "RULES_APPLY_CHANGES=true (mutations enabled)"
  RULES_APPLY_CHANGES=true ADDON_PORT="$PORT" ADDON_BASE_URL="$BASE_URL" \
    ADDON_FRAME_ANCESTORS="$ADDON_FRAME_ANCESTORS" \
    ADDON_CORS_ORIGINS="$ADDON_CORS_ORIGINS" \
    ADDON_CORS_ALLOW_CREDENTIALS="$ADDON_CORS_ALLOW_CREDENTIALS" \
    java -jar "$JAR"
else
  ADDON_PORT="$PORT" ADDON_BASE_URL="$BASE_URL" \
    ADDON_FRAME_ANCESTORS="$ADDON_FRAME_ANCESTORS" \
    ADDON_CORS_ORIGINS="$ADDON_CORS_ORIGINS" \
    ADDON_CORS_ALLOW_CREDENTIALS="$ADDON_CORS_ALLOW_CREDENTIALS" \
    java -jar "$JAR"
fi

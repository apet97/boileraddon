#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"

PORT="${PORT:-8080}"

if ! command -v ngrok >/dev/null 2>&1; then
  echo "ngrok is required. Install from https://ngrok.com/ and ensure 'ngrok' is on PATH." >&2
  exit 2
fi

# If 4040 is not serving, start ngrok in background
if ! curl -fsS http://127.0.0.1:4040/api/tunnels >/dev/null 2>&1; then
  echo "Starting ngrok http ${PORT} ..."
  # Best-effort background start; suppress noisy output
  (ngrok http "${PORT}" >/dev/null 2>&1 &) || true
  # Wait up to ~10s for API to come up
  for i in {1..50}; do
    if curl -fsS http://127.0.0.1:4040/api/tunnels >/dev/null 2>&1; then break; fi
    sleep 0.2
  done
fi

echo "Launching Rules with ngrok base URL..."
exec bash "$ROOT/scripts/run-rules.sh" --use-ngrok


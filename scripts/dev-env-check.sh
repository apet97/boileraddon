#!/usr/bin/env bash
set -e

fail() { echo "[x] $1"; exit 1; }
pass() { echo "[✓] $1"; }
warn() { echo "[!] $1"; }

command -v java >/dev/null 2>&1 && pass "Java: $(java -version 2>&1 | head -n1)" || fail "Java not found"
command -v mvn >/dev/null 2>&1 && pass "Maven: $(mvn -v 2>/dev/null | head -n1)" || fail "Maven not found"
command -v ngrok >/dev/null 2>&1 && pass "ngrok: $(ngrok version 2>/dev/null | head -n1)" || warn "ngrok not found (required for external install)"

PORT=${ADDON_PORT:-8080}
if lsof -i :$PORT >/dev/null 2>&1; then
  pass "Port $PORT available for add-on"
else
  echo "[i] Port $PORT not busy (good)"
fi

echo "Next steps:"
echo "  make build-template   # or make build"
echo "  make run-auto-tag-assistant"
echo "  ngrok http $PORT      # then update ADDON_BASE_URL and install"

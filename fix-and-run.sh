#!/usr/bin/env bash
set -euo pipefail

# Convenience wrapper: applies quick sanity checks, builds modules, then
# delegates to scripts/run-rules.sh with your provided arguments.

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$HERE"

cd "$ROOT"

echo "[fix-and-run] Quick build of SDK + Rules…"
mvn -q -e -Dmaven.test.skip=true -pl addons/addon-sdk -am install
mvn -q -e -Dmaven.test.skip=true -pl addons/rules -am package

echo "[fix-and-run] Delegating to scripts/run-rules.sh $*"
exec bash scripts/run-rules.sh "$@"

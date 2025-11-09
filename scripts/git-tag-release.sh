#!/usr/bin/env bash
set -euo pipefail

# Create and push a git tag for a release
# Usage: bash scripts/git-tag-release.sh v0.2 "Rules IFTTT builder stabilized"

TAG="${1:-}"
MESSAGE="${2:-Release}"

if [ -z "$TAG" ]; then
  echo "Usage: $0 <tag> [message]"
  echo "Example: $0 v0.2 'Rules IFTTT builder stabilized'"
  exit 1
fi

echo "[git-tag-release] Ensuring git repo…"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Not a git repo" >&2; exit 2; }

echo "[git-tag-release] Creating tag $TAG with message: $MESSAGE"
git tag -a "$TAG" -m "$MESSAGE"

echo "[git-tag-release] Pushing tag to origin…"
git push origin "$TAG"

echo "[git-tag-release] Done. Tag $TAG created and pushed."

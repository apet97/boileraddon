#!/usr/bin/env bash
set -euo pipefail

# Push current workspace state directly to main (no PR).
# - Commits any staged/unstaged changes with provided message
# - Switches to main (creates or resets local main to origin/main)
# - Fast-forwards from origin/main if possible, then pushes

MSG="${1:-fix: rules addon updates}"

echo "[git-push-main] Ensuring git repo…"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Not a git repo" >&2; exit 2; }

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "[git-push-main] Current branch: $CURRENT_BRANCH"

echo "[git-push-main] Staging all changes…"
git add -A

if ! git diff --cached --quiet; then
  echo "[git-push-main] Committing staged changes…"
  git commit -m "$MSG"
else
  echo "[git-push-main] No staged changes to commit."
fi

echo "[git-push-main] Fetching origin…"
git fetch origin --prune

if git show-ref --verify --quiet refs/heads/main; then
  echo "[git-push-main] Checking out local main…"
  git checkout main
else
  echo "[git-push-main] Creating local main…"
  git checkout -B main
fi

if git show-ref --verify --quiet refs/remotes/origin/main; then
  echo "[git-push-main] Fast-forwarding from origin/main (if possible)…"
  git merge --ff-only origin/main || true
fi

echo "[git-push-main] Pushing to origin main…"
git push -u origin main

echo "[git-push-main] Done."


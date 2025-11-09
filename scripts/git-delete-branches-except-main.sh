#!/usr/bin/env bash
set -euo pipefail

# Dangerous helper: deletes all remote branches except main.
# Prompts for confirmation. Also prunes local tracking branches.

REMOTE="origin"
CONFIRM="${1:-}"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Not a git repo" >&2; exit 2; }

echo "[git-clean] Fetching $REMOTE…"
git fetch "$REMOTE" --prune

echo "[git-clean] Remote branches on $REMOTE (excluding main):"
TO_DELETE=$(git for-each-ref refs/remotes/$REMOTE --format='%(refname:short)' | awk -F'/' '$2 != "HEAD" && $2 != "main" {print $2}' | sort -u)

if [[ -z "${TO_DELETE}" ]]; then
  echo "[git-clean] Nothing to delete."
  exit 0
fi

echo "$TO_DELETE" | sed 's/^/  - /'

if [[ "$CONFIRM" != "--yes" ]]; then
  echo
  read -r -p "Delete these remote branches on $REMOTE? Type 'delete' to proceed: " ANSWER
  if [[ "$ANSWER" != "delete" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

echo "[git-clean] Deleting on $REMOTE…"
while read -r BR; do
  [[ -z "$BR" ]] && continue
  echo "  deleting $REMOTE/$BR"
  git push "$REMOTE" --delete "$BR" || true
done <<< "$TO_DELETE"

echo "[git-clean] Pruning local tracking branches…"
git fetch "$REMOTE" --prune

echo "[git-clean] Done."


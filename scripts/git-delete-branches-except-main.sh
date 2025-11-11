#!/usr/bin/env bash
set -euo pipefail

# Dangerous helper: deletes remote branches. Now defaults to dry-run.
# Usage: ./git-delete-branches-except-main.sh [--remote origin] [--prefix feature/john] [--yes]

REMOTE="origin"
ALLOW_PREFIX=""
CONFIRM=false
PROTECTED_REGEX="^(main|develop|release/.+|prod|production|staging)$"

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]
  --remote <name>      Git remote to clean (default: origin)
  --prefix <pattern>   Required allowlist prefix (e.g., feature/john)
  --yes                Execute deletions (otherwise dry-run)
  --help               Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --remote)
      REMOTE="${2:-}"
      shift 2
      ;;
    --prefix)
      if [[ -z "${2:-}" ]]; then
        echo "--prefix requires a non-empty value" >&2
        exit 1
      fi
      ALLOW_PREFIX="${2}"
      shift 2
      ;;
    --yes)
      CONFIRM=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Not a git repo" >&2; exit 2; }

echo "[git-clean] Fetching $REMOTE…"
git fetch "$REMOTE" --prune

ALL_BRANCHES=$(git for-each-ref "refs/remotes/$REMOTE" --format='%(refname:short)' | sed "s#^$REMOTE/##" | grep -vE "$PROTECTED_REGEX" || true)

if [[ -n "$ALLOW_PREFIX" ]]; then
  FILTERED=$(echo "$ALL_BRANCHES" | grep -E "^${ALLOW_PREFIX}" || true)
else
  FILTERED="$ALL_BRANCHES"
fi

if [[ -z "${FILTERED}" ]]; then
  echo "[git-clean] No branches match filters."
  exit 0
fi

echo "[git-clean] Branches eligible for deletion on $REMOTE:"
echo "$FILTERED" | sed 's/^/  - /'

if ! $CONFIRM; then
  echo
  echo "[git-clean] Dry-run only. Re-run with --yes and --prefix <pattern> to delete."
  exit 0
fi

if [[ -z "$ALLOW_PREFIX" ]]; then
  echo "[git-clean] Refusing to delete without --prefix allowlist." >&2
  exit 1
fi

echo "[git-clean] Deleting on $REMOTE…"
while read -r BR; do
  [[ -z "$BR" ]] && continue
  echo "  deleting $REMOTE/$BR"
  git push "$REMOTE" --delete "$BR" || true
done <<< "$FILTERED"

echo "[git-clean] Pruning local tracking branches…"
git fetch "$REMOTE" --prune

echo "[git-clean] Done."

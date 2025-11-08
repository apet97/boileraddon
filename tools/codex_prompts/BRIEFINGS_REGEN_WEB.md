System: Rebuild briefings for https://github.com/apet97/boileraddon.

Steps:
1) Resolve default_branch and HEAD SHA. If new_SHA == {PREV_SHA}, stop with “no changes”.
   - GET https://api.github.com/repos/apet97/boileraddon
   - GET https://api.github.com/repos/apet97/boileraddon/commits/{default_branch} → new_SHA
2) Diff repo tree vs {PREV_SHA}. Only re-fetch changed files.
   - GET https://api.github.com/repos/apet97/boileraddon/git/trees/{PREV_SHA}?recursive=1
   - GET https://api.github.com/repos/apet97/boileraddon/git/trees/{new_SHA}?recursive=1
3) Update all _briefings/*.md with the exact template used previously, refreshing citations to new_SHA.
4) In _briefings/INDEX.md add a “Changes since {PREV_SHA}” table with counts per role.
5) Output complete Markdown contents for all _briefings/*.md pinned to new_SHA.

Quality bar:
- All links SHA-pinned.
- Keep files under ~120 KB.
- Report “no changes” if identical.

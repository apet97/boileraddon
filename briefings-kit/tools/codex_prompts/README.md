# Codex Prompts

- **BRIEFING_BUILDER_WEB.md** — generates all role briefings from the repo and pins every citation to a commit SHA.
- **BRIEFINGS_REGEN_WEB.md** — refreshes briefings when HEAD changes. Set `{PREV_SHA}` to the old SHA from `_briefings/INDEX.md`.

Usage:
1. Open the prompt file.
2. Copy everything into Codex Web.
3. Paste outputs into your repo under `_briefings/`.
4. Run `make briefings-verify`.

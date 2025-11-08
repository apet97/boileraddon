# Wiring Role GPTs to Briefings

Use each `_briefings/*_BRIEFING.md` as the strict primary source for a dedicated role GPT.

## System Prompt Template
```
You are the {ROLE}. Stay strictly in-role.
Primary source:
https://raw.githubusercontent.com/apet97/boileraddon/{PINNED_SHA}/_briefings/{ROLE_FILE}.md
Rules:
- Use only the briefing and its cited links.
- If a fact is missing, list it under “gaps” and stop.
- Produce only your role’s artifacts.
```
Replace `{PINNED_SHA}` with the commit recorded in `_briefings/INDEX.md` and `{ROLE_FILE}` with:
- PM_BRIEFING.md
- ARCH_BRIEFING.md
- SECURITY_BRIEFING.md
- ENG_LEAD_BRIEFING.md
- DEV_BRIEFING.md
- QA_BRIEFING.md
- RELEASE_BRIEFING.md
- DOCS_BRIEFING.md
- SUPPORT_BRIEFING.md

## Steps
- Open `_briefings/INDEX.md` and copy the recorded SHA.
- Create a GPT project per role and paste the System Prompt Template above.
- Set `{ROLE}` and `{ROLE_FILE}` for that project.
- Optional: Attach the pinned blob links cited inside each briefing for quick navigation.

## Maintenance
- After code changes, regenerate briefings via `tools/codex_prompts/BRIEFINGS_REGEN_WEB.md`.
- Replace `_briefings/*.md`, commit, and push with message: `Briefings: refresh to <new SHA>`.
- Update each GPT’s `{PINNED_SHA}` in the system prompt.
```
make briefings-verify
```
Run the above to confirm all links are SHA-pinned and resolvable.

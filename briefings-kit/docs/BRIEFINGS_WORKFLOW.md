# Briefings Workflow

Goal: generate, verify, and maintain stakeholder briefings pinned to a Git commit.

## 0) Prerequisites
- Repo: https://github.com/apet97/boileraddon
- Optional: Python 3.10+ for link checks.
- Optional: `make` for convenience targets.

## 1) Add this kit to your repo
Unzip this archive into the **repo root**. Commit it:
```bash
unzip briefings-kit.zip -d .
git add docs scripts tools _briefings
git commit -m "Add briefings workflow kit"
git push
```

## 2) Build briefings with Codex Web
Open **tools/codex_prompts/BRIEFING_BUILDER_WEB.md**. Copy the whole prompt into Codex Web.

- `{PROJECT_NAME}` = `Clockify Add-on Boilerplate`
- `{PLATFORM_NAME}` = `Clockify`

Run it. Codex will return the full contents for:
```
_briefings/INDEX.md
_briefings/PM_BRIEFING.md
_briefings/ARCH_BRIEFING.md
_briefings/SECURITY_BRIEFING.md
_briefings/ENG_LEAD_BRIEFING.md
_briefings/DEV_BRIEFING.md
_briefings/QA_BRIEFING.md
_briefings/RELEASE_BRIEFING.md
_briefings/DOCS_BRIEFING.md
_briefings/SUPPORT_BRIEFING.md
```

Create these files in your repo under `_briefings/` and paste the content.

Commit:
```bash
git add _briefings
git commit -m "Briefings: initial pin"
git push
```

## 3) Verify pinned links
Use the checker script or Make target:
```bash
python3 tools/check_briefing_links.py _briefings
# or
make briefings-verify
```
Fix any report about `/blob/main/` not pinned to a SHA.

## 4) Wire GPT projects
For each role GPT, set **System** to:
```
You are the {ROLE}. Stay strictly in-role.
Primary source:
<raw GitHub or blob URL to>/_briefings/{ROLE_FILE}.md
Rules:
- Use only the briefing and its cited links.
- If a fact is missing, list it under “gaps” and stop.
- Produce only your role’s artifacts.
```
Attach the pinned `_briefings/{ROLE_FILE}.md` or use its blob URL.

## 5) Regenerate after code changes
When HEAD changes, use **tools/codex_prompts/BRIEFINGS_REGEN_WEB.md**:
- Set `{PREV_SHA}` to the SHA recorded in `_briefings/INDEX.md`.
- Run the prompt in Codex Web.
- Replace the `_briefings/*.md` files with the new output.
- Commit with message: `Briefings: refresh to <new SHA>`.

## 6) Quick commands
```bash
make briefings-open         # prints where INDEX.md is
make briefings-verify       # checks links resolve and are SHA-pinned
```

## 7) Maintenance tips
- Keep `_briefings/INDEX.md` authoritative for the commit SHA and build timestamp.
- Enforce SHA-pinned links only; never link to `main`.
- Keep each briefing readable in under 10 minutes.

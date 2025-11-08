# Briefings convenience targets
.PHONY: briefings-open briefings-verify

briefings-open:
	@echo "Open _briefings/INDEX.md in your editor or viewer."

briefings-verify:
	@python3 tools/check_briefing_links.py _briefings || (echo 'Link check failed'; exit 1)

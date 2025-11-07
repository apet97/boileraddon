# Clockify UI Kit / Storybook

- Entry point: https://resources.developer.clockify.me/ui/latest/showcase/?path=/story/introduction--page
- Saved snapshot: `extras/ui-kit-storybook.html` (shell page; Storybook content loads via dynamic assets).

If you need component names/props programmatically, fetch the Storybook JSON index (often `sb.json` or `index.json`) from the same base. Note: direct JSON fetch returned 403 from this environment. Using a browser or a headless runtime with proper headers may be required to retrieve it.


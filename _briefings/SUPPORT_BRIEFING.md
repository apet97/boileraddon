Support & Developer Relations Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Provide accurate lifecycle and webhook payload guidance using request/response samples and webhook JSON archive.

Support developers through Quickstart, troubleshooting notes, and API expectations in Auto-Tag README.

Relay common mistakes and manifest requirements to community/partners.

Coordinate with engineering on signature validation, token handling, and environment claims for escalations.

Primary artifacts in repo:

Request/Response Examples

Webhook JSON Samples

Common Mistakes guide

How to do your job:

Use Request/Response Examples to reproduce customer reports about lifecycle or webhook payloads; verify headers (signature, workspace) are present.

Reference Webhook JSON Samples when developers need full payload context for less common events (approvals, expenses, assignments).

Guide developers through Quickstart workflow (build, run, ngrok, install) and share troubleshooting steps from Auto-Tag README.

Share Common Mistakes document (manifest schema, auth headers, webhook handling) during office hours or forum responses.

Educate community on signature validation using WebhookSignatureValidator example; encourage storing installation tokens securely.

Explain JWT environment claim usage for region-specific API routing using JwtTokenDecoder docs.

Escalate missing token scenarios noted in lifecycle handler logs to engineering to avoid support loops.

Critical decisions already made:

Support documentation centers on Quickstart, Common Mistakes, and detailed request/response samples for accurate troubleshooting.

Webhook signature validation and JWT decoding patterns are canonical references for community guidance.

Troubleshooting guidance emphasizes token storage, ngrok exposure, and manifest alignment.

Open questions and risks:

Owner	Source	Link
Addon Engineering	Lifecycle handler still logs TODO when auth token missing; support needs documented remediation to share with customers.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java#L47-L63
Docs Team	Template manifest description placeholder may confuse community templates; provide finished example before broad distribution.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/manifest.json#L2-L18
Commands or APIs you will call (if any):

curl http://localhost:8080/auto-tag-assistant/manifest.json

References:

Request/Response examples.

Webhook JSON samples.

Common Mistakes guide.

Auto-Tag Assistant troubleshooting tips.

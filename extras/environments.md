# Clockify Environments and Regions

- Resolve API base URLs from token claims (URL claims). Use values in JWT claims to target the correct region and product APIs.
- Regional server prefixes per OpenAPI:
  - EU (Germany): euc1
  - USA: use2
  - UK: euw2
  - AU: apse2
- Examples (from OpenAPI description):
  - Global Regular: https://api.clockify.me/api/v1
  - Regional Regular (EU): https://euc1.clockify.me/api/v1
  - PTO and Reports have separate hosts (see clockify-openapi.json).

Always prefer values/hosts indicated by claims over hardcoding.

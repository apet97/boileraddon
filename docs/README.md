# Clockify Addon Documentation

**Complete guide for building Clockify addons with zero-shot AI development**

This directory contains comprehensive documentation designed to enable AI assistants to develop Clockify addons without prior context.

---

## Quick Navigation

### 🚀 Getting Started

- **[Quick Reference](QUICK-REFERENCE.md)** - One-page cheat sheet with all essential info
- **[Building Your Own Addon](BUILDING-YOUR-OWN-ADDON.md)** - Step-by-step guide
- **[Architecture](ARCHITECTURE.md)** - Technical overview of addon system

### 📚 Development Guides

- **[API Cookbook](API-COOKBOOK.md)** - Copy-paste code examples for Clockify API
- **[Request/Response Examples](REQUEST-RESPONSE-EXAMPLES.md)** - Complete HTTP exchange examples
- **[Common Patterns](PATTERNS.md)** - Reusable code patterns and best practices
- **[Data Models](DATA-MODELS.md)** - Complete entity schemas and field definitions

### 🔧 Reference

- **[Build Verification](../BUILD_VERIFICATION.md)** - Validate your build setup
- **[Clockify Parameters](CLOCKIFY_PARAMETERS.md)** - Manifest fields, headers, webhooks, env flags
- **[ngrok Testing](NGROK_TESTING.md)** - Expose your add-on and install in Clockify

### 📦 Product Specs

- **[Overtime Add-on (PM)](ADDON_OVERTIME.md)** - Scope, MVP, flows, KPIs

---

## Documentation Overview

### For AI Assistants

This documentation is optimized for zero-shot addon development:

1. **[QUICK-REFERENCE.md](QUICK-REFERENCE.md)** - Start here for fast context
   - Manifest structure
   - All available scopes
   - Component types
   - Webhook events
   - Common API endpoints
   - Troubleshooting

2. **[API-COOKBOOK.md](API-COOKBOOK.md)** - Ready-to-use code snippets
   - Authentication patterns
   - CRUD operations for all entities
   - Error handling
   - Rate limiting
   - Complete working examples

3. **[REQUEST-RESPONSE-EXAMPLES.md](REQUEST-RESPONSE-EXAMPLES.md)** - Real HTTP exchanges
   - Lifecycle callbacks (INSTALLED, DELETED)
   - Webhook payloads (all event types)
   - Settings UI requests
   - API call examples with real data

4. **[PATTERNS.md](PATTERNS.md)** - Production-ready patterns
   - Token storage (in-memory, file-based, database-ready)
   - Webhook signature validation
   - API client with retry logic
   - Caching strategies
   - Multi-workspace management
   - Async processing

5. **[DATA-MODELS.md](DATA-MODELS.md)** - Entity reference
   - Complete schemas for all entities
   - Field validation rules
   - Example JSON objects
   - Relationships between entities

### For Humans

- Start with **[Building Your Own Addon](BUILDING-YOUR-OWN-ADDON.md)**
- Reference **[Quick Reference](QUICK-REFERENCE.md)** while coding
- Use **[API Cookbook](API-COOKBOOK.md)** for specific operations
- Check **[Architecture](ARCHITECTURE.md)** to understand the system

---

## What's Available

### ✅ Complete Documentation

- [x] Manifest structure and validation rules
- [x] All available scopes and their permissions
- [x] All component types with examples
- [x] All webhook events with payloads
- [x] Complete API reference with real examples
- [x] Security best practices (JWT, webhook signatures)
- [x] Error handling and rate limiting
- [x] Token storage patterns
- [x] Multi-workspace management
- [x] Build and deployment guides

### 📦 Code Examples

- **Working Addon**: [auto-tag-assistant](../addons/auto-tag-assistant/) - Complete reference implementation
- **Template**: [_template-addon](../addons/_template-addon/) - Minimal starter
- **Component Examples**: [examples/component-types](../examples/component-types/) - UI components
- **Specs**: [examples/](../examples/) - Multiple addon specifications

### 🛠️ Tools

- **Scaffolding**: `scripts/new-addon.sh` - Create new addon
- **Validation**: `tools/validate-manifest.py` - Validate manifest files
- **Makefile**: Build, run, test, and deploy commands

---

## Development Workflow

### 1. Quick Start (1 minute)

```bash
# Create new addon
./scripts/new-addon.sh --port 8080 my-addon "My Addon"

# Build and run
cd addons/my-addon
make build && make run
```

### 2. Local Development with ngrok

```bash
# Terminal 1: Run addon
make run-my-addon

# Terminal 2: Expose with ngrok
ngrok http 8080

# Update manifest.json with ngrok URL
# Install in Clockify using manifest URL
```

### 3. Reference Documentation

While developing, keep these open:
- [QUICK-REFERENCE.md](QUICK-REFERENCE.md) - Quick lookups
- [API-COOKBOOK.md](API-COOKBOOK.md) - Code snippets
- [REQUEST-RESPONSE-EXAMPLES.md](REQUEST-RESPONSE-EXAMPLES.md) - HTTP exchanges

---

## Key Concepts

### Manifest File

Your `manifest.json` defines:
- Addon metadata (name, description)
- Required scopes (permissions)
- Components (UI elements)
- Webhooks (event subscriptions)
- Lifecycle callbacks (INSTALLED, DELETED)

**Critical**: Never include `$schema` in runtime manifest!

### Lifecycle Events

1. **INSTALLED** - User installs addon
   - **Action Required**: Store `installationToken`
   - Use token for all API calls

2. **DELETED** - User uninstalls addon
   - **Action Required**: Clean up workspace data

### Webhooks

Subscribe to real-time events:
- `NEW_TIME_ENTRY` - New entry created
- `NEW_TIMER_STARTED` - Timer started
- `TIMER_STOPPED` - Timer stopped
- `TIME_ENTRY_UPDATED` - Entry modified
- `TIME_ENTRY_DELETED` - Entry removed

**Critical**: Always validate webhook signatures!

### Components

UI elements in Clockify:
- **Settings Sidebar** - Addon configuration
- **Time Entry Sidebar** - Entry context panel
- **Project Sidebar** - Project context panel
- **Report Tab** - Custom reports
- **Widget** - Dashboard widgets

All components receive JWT token with user context.

---

## Security Checklist

- ✅ Validate webhook signatures (HMAC-SHA256)
- ✅ Verify JWT tokens for UI components
- ✅ Store installation tokens securely
- ✅ Never log sensitive data
- ✅ Use HTTPS in production
- ✅ Sanitize user inputs
- ✅ Implement rate limiting
- ✅ Handle errors gracefully

---

## Common Patterns

### Store Installation Token

```java
public HttpResponse handleInstalled(Map<String, Object> body) {
    String workspaceId = (String) body.get("workspaceId");
    String token = (String) body.get("installationToken");
    tokenStore.saveToken(workspaceId, token); // ← CRITICAL!
    return HttpResponse.ok("{\"success\": true}");
}
```

### Make Authenticated API Call

```java
HttpURLConnection conn = new URL(apiBaseUrl + endpoint).openConnection();
conn.setRequestProperty("X-Addon-Token", installationToken);
conn.setRequestProperty("Content-Type", "application/json");
```

### Validate Webhook Signature

```java
String signature = request.getHeader("x-clockify-signature");
String rawBody = readRequestBody(request);
if (!signatureValidator.validate(rawBody, signature)) {
    return HttpResponse.unauthorized();
}
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Manifest rejected | Remove `$schema` field, use `schemaVersion` |
| 401 Unauthorized | Check installation token is stored |
| 403 Forbidden | Add required scope to manifest |
| Webhook not received | Verify signature validation |
| Settings page blank | Check JWT token validation |

See [QUICK-REFERENCE.md](QUICK-REFERENCE.md) for complete troubleshooting guide.

---

## Additional Resources

### External Documentation

- [Clockify OpenAPI Spec](../dev-docs-marketplace-cake-snapshot/extras/clockify-openapi.json)
- [Full Marketplace Docs](../dev-docs-marketplace-cake-snapshot/cake_marketplace_dev_docs.md)
- [Manifest Schemas](../dev-docs-marketplace-cake-snapshot/extras/) - v1.0 to v1.3

### Example Addons

- [Auto Tag Assistant](../addons/auto-tag-assistant/) - Complete working addon
- [Cost Center](../examples/cost-center-addon/) - Specification
- [Jira Sync](../examples/jira-sync-addon/) - Specification
- [Tag Enforcer](../examples/tag-enforcer-addon/) - Specification

---

## FAQ

**Q: Where do I start?**
A: Run `./scripts/new-addon.sh my-addon "My Addon"` and read [BUILDING-YOUR-OWN-ADDON.md](BUILDING-YOUR-OWN-ADDON.md)

**Q: How do I test locally?**
A: Use ngrok to expose your local server. See [README.md](../README.md#local-development)

**Q: Where are webhook signatures validated?**
A: See [PATTERNS.md](PATTERNS.md#webhook-signature-validation) for complete implementation

**Q: How do I store installation tokens?**
A: See [PATTERNS.md](PATTERNS.md#token-storage) for production-ready patterns

**Q: What scopes do I need?**
A: See [QUICK-REFERENCE.md](QUICK-REFERENCE.md#available-scopes) for scope-to-operation mapping

**Q: How do I handle rate limits?**
A: See [API-COOKBOOK.md](API-COOKBOOK.md#rate-limiting) for retry logic with exponential backoff

---

## Contributing

Found an issue or want to improve the documentation?

1. Check existing issues
2. Submit a pull request with improvements
3. Update relevant examples

---

**Quick Command Reference**

```bash
# Create addon
./scripts/new-addon.sh my-addon "My Addon"

# Build
make build-my-addon

# Run locally
make run-my-addon

# Validate manifest
make validate

# Run in Docker
make docker-run TEMPLATE=my-addon ADDON_BASE_URL=https://example.com
```

---

**Documentation Last Updated**: 2025-11-08

For the most up-to-date information, see the [main README](../README.md).

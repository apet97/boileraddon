# `_template-addon`  
[AI START HERE](../../docs/AI_START_HERE.md)

This module is a copy-ready starting point for building a new Clockify add-on with Jetty and Jackson. It includes:

- Runtime manifest endpoint (`/manifest.json`) via the SDK’s DefaultManifestController
- Health endpoint (`/health`)
- Lifecycle handlers (`/lifecycle/installed`, `/lifecycle/deleted`) that persist/remove the installation token
- Webhook handler skeleton with signature verification (`clockify-webhook-signature`)
- A dry‑run test endpoint (`/api/test`) you can use to exercise logic without side effects

> 🚀 Quick start: run `scripts/new-addon.sh my-addon "My Add-on"` from the repo root to clone this module automatically. The script requires `perl`, `jq`, and `python3` and updates Maven coordinates, package names, and the parent `pom.xml` for you.

## Local development

1. Copy the environment defaults from the repo root: `cp .env.example .env`
2. Update `.env` with your preferred `ADDON_PORT` and `ADDON_BASE_URL` values.
3. Run `make dev` from the repo root to build and launch the template using those settings.
4. Verify endpoints:
   - `curl $ADDON_BASE_URL/health`
   - `curl $ADDON_BASE_URL/manifest.json`
   - `curl -X POST $ADDON_BASE_URL/api/test -H 'Content-Type: application/json' -d '{"hello":"world"}'`

The application reads from `.env` first and still honors variables exported in your shell, so you can temporarily override values without editing the file.

## How to copy and rename

1. **Copy the folder** – duplicate `addons/_template-addon` to your new add-on folder name (for example `addons/my-addon`).
2. **Update the Maven coordinates** – open the new module's `pom.xml` and update:
   - `<artifactId>` to your add-on slug (for example `my-addon`).
   - `<name>` to match the add-on name you want to display.
   - Update the `<mainClass>` inside the `maven-assembly-plugin` section so it points to your renamed package/class.
3. **Adjust the Java package** – rename the Java package from `com.example.templateaddon` to your desired package. Update the folder structure under `src/main/java` to match.
4. **Rename the application class** – change `TemplateAddonApp` (and its file name) to your add-on specific entry point. Update references inside the class accordingly.
5. **Update manifest key and metadata** – edit both `manifest.json` and the programmatic manifest inside `TemplateAddonApp` so that `key`, `name`, `description`, `baseUrl`, and `scopes` reflect your add-on. Make sure the key matches the folder name you expose at runtime.
6. **Wire the new module in the parent build** – add your new module path to the root `pom.xml` `<modules>` section if it is not already there.
7. **Search for TODOs** – follow the TODO comments across controllers to plug in your actual business logic, persistence, and UI.
8. **Routing note** – the SDK matches endpoint paths exactly (no wildcards). Pass identifiers via query/body or register additional exact paths.

After renaming, run `mvn clean package -pl <your-module> -am` to produce a `*-jar-with-dependencies.jar` that you can deploy.

## Pattern: add a dry‑run test endpoint

The SDK matches endpoint paths exactly (no wildcards). If you need a utility endpoint to test your logic without side effects,
register a dedicated path and POST a sample payload to it. For example:

```
// In your App wiring
addon.registerCustomEndpoint("/api/test", request -> {
  if ("POST".equals(request.getMethod())) {
    // parse request JSON and exercise your logic (do not mutate external state)
    return HttpResponse.ok("{\"status\":\"ok\"}", "application/json");
  }
  return HttpResponse.error(405, "Method not allowed", "application/json");
});
```

When designing CRUD endpoints that operate on identifiers, prefer query/body parameters with the exact registered path:

```
// Register once
addon.registerCustomEndpoint("/api/items", handler);

// Client deletes by id
DELETE /api/items?id=<ID>
// or JSON body {"id":"..."}
```

## Security note

- Always verify webhooks with `clockify-webhook-signature` using the SDK `WebhookSignatureValidator.verify(request, workspaceId)`
- Use the SDK `TokenStore` to store the installation token (persist for production)

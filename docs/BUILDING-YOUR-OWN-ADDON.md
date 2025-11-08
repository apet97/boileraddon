# Building Your Own Clockify Add-on

Use this repository as a starting point for your production add-on. Follow the checklist below to clone the Auto-Tag Assistant or template module, customize the manifest, manage tokens correctly, and prepare for deployment.

## 1. Copy & Rename Checklist

1. **Duplicate the module** – Prefer the automated scaffold:
   ```bash
   scripts/new-addon.sh my-addon "My Add-on"
   ```
   The helper script (requires `perl`, `jq`, and `python3`) copies the template module, rewrites the Maven coordinates, updates package/class names, and wires the new module into the parent build. If you need to customize the process manually, copy either `addons/auto-tag-assistant/` (full example) or `addons/_template-addon/` (minimal skeleton) into a new directory such as `addons/<your-addon>/`. The legacy scaffold remains available under `templates/java-basic-addon/` if you need it for reference.
2. **Update package names** – Replace `com.example.autotagassistant` with your own reversed domain in both source directories and `pom.xml`.
3. **Rename entry points** – Rename `AutoTagAssistantApp` (or the template’s `Main` class) and adjust the `mainClass` in the module POM.
4. **Adjust artifact IDs** – Update `<artifactId>` in the copied `pom.xml` so the generated JAR name matches your add-on key.
5. **Refresh manifest file** – If you copied the static `manifest.json` for reference, rename it and keep it in sync with your runtime manifest builder.
6. **Wire into the parent build** – Add the new module path to the `<modules>` section of the root `pom.xml` so `mvn clean package` builds it alongside the existing examples.

> Tip: Use `rg autotagassistant` to find lingering references to the original package name.

## 2. Customize the Manifest

The runtime manifest is generated programmatically via `ClockifyManifest`. Update the builder inside your app’s `main` method to match your new add-on.

```java
ClockifyManifest manifest = ClockifyManifest
        .v1_3Builder()
        .key("my-addon-key")
        .name("My Add-on")
        .description("What it does")
        .baseUrl(baseUrl)
        .minimalSubscriptionPlan("FREE")
        .scopes(new String[]{"TIME_ENTRY_READ"})
        .build();
```

* Add or remove `components`, `webhooks`, and `lifecycle` entries using the manifest getters before starting the server.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java†L35-L63】
* Keep the runtime manifest lean—Clockify rejects unknown fields (such as `$schema`).
* When exposing additional endpoints, ensure the shared SDK helpers `ClockifyAddon.registerCustomEndpoint`, `registerLifecycleHandler`, or `registerWebhookHandler` are invoked so the manifest stays synchronized.【F:addons/addon-sdk/src/main/java/com/clockify/addon/sdk/ClockifyAddon.java†L37-L135】 The helpers now trim extra whitespace and normalize leading slashes, so `registerCustomEndpoint("settings", handler)` and `registerCustomEndpoint("/settings", handler)` target the same route.

## 3. Manage Tokens & Claims

Clockify sends workspace-specific credentials and environment hints. Treat them as secrets.

* **Persist securely** – Replace the demo `TokenStore` with a persistent service (database, secret manager). Capture both the `authToken` and `apiUrl` fields from the INSTALLED lifecycle payload.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java†L23-L93】
* **Normalize API roots** – Use `TokenStore.normalizeApiBaseUrl` as a reference when constructing your own logic so partial URLs (e.g., `/api`) become fully qualified REST endpoints.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/TokenStore.java†L58-L89】
* **Validate claims** – When verifying JWTs (webhook signatures, iframe tokens), confirm that the workspace ID, add-on key, and issuer match expectations before executing business logic.
* **Rotate on events** – Update stored secrets if Clockify reissues tokens or base URLs during later lifecycle events.

## 4. Deployment Pointers

* **Local development** – Build with `mvn clean package -DskipTests` and run `java -jar <module>/target/<name>-jar-with-dependencies.jar`. Pair it with `ngrok http 8080` (or your chosen port) and install the manifest via the forwarded HTTPS URL. Start the server first, then install the manifest to avoid caching a stale URL in Developer.
* **Configuration** – Provide sensible defaults via environment variables (`ADDON_BASE_URL`, `ADDON_PORT`) and propagate them to your manifest builder so the runtime manifest always matches the deployed host.【F:addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/AutoTagAssistantApp.java†L25-L63】
* **Logging & monitoring** – Forward structured logs to your platform of choice and tag entries with workspace IDs to simplify debugging.
* **Health checks** – Register a `/health` endpoint that returns quickly so platform monitors and Clockify can detect outages.
* **Packaging** – Keep using the shaded JAR build (the parent POM is already configured). Upload the resulting archive or containerize the Java process for your runtime environment.

## 5. Next Steps

Once your add-on logic is ready:

1. Validate manifests with `python3 tools/validate-manifest.py`. The script now walks every `addons/*/manifest.json` file by default, so newly added modules are checked automatically (pass an explicit path only if you need to validate a specific file).
2. Deploy to a stable host (cloud VM, container platform, or serverless Java runtime).
3. Configure HTTPS (ngrok is fine for local demos; use a production-grade TLS endpoint for customers).
4. Reinstall the add-on in Clockify using the production manifest URL and monitor lifecycle/webhook logs for any issues.

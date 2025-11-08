# Create Your First Add‑on (5 steps)

Follow these steps to clone the template and have a working add‑on with a dry‑run endpoint.

1) Scaffold
```
make new-addon NAME=my-addon DISPLAY="My Add‑on"
```
This copies the template module to `addons/my-addon`, updates Maven coordinates, packages, and parent POM.

2) Build and run locally
```
make build  # or: make -f addons/my-addon/pom.xml package -DskipTests
ADDON_BASE_URL=http://localhost:8080/my-addon \
java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar
```

3) Verify endpoints
```
curl http://localhost:8080/my-addon/health
curl http://localhost:8080/my-addon/manifest.json
curl -X POST http://localhost:8080/my-addon/api/test -H 'Content-Type: application/json' -d '{"hello":"world"}'
```

4) Add logic
- Register a new endpoint:
```
// in YourApp.java after creating ClockifyAddon addon
addon.registerCustomEndpoint("/api/items", request -> HttpResponse.ok("{}","application/json"));
```
- Add a webhook handler and validate signatures (`clockify-webhook-signature`) via SDK:
```
addon.registerWebhookHandler("TIME_ENTRY_UPDATED", request -> {
  var v = com.clockify.addon.sdk.security.WebhookSignatureValidator.verify(request, /*workspaceId*/ "your-ws-id");
  if (!v.isValid()) return v.response();
  return HttpResponse.ok("{\"processed\":true}", "application/json");
});
```

5) Install via ngrok
```
ngrok http 8080
# restart with https base URL
ADDON_BASE_URL=https://YOUR-NGROK.ngrok-free.app/my-addon \
java -jar addons/my-addon/target/my-addon-0.1.0-jar-with-dependencies.jar
# install using the runtime manifest URL:
#   https://YOUR-NGROK.ngrok-free.app/my-addon/manifest.json
```

Tips
- Exact‑match routing: pass IDs via query/body.
- Use TokenStore to persist installation tokens; in production, use a DB implementation.
- Use SDK ClockifyHttpClient for safe API calls; always send `x-addon-token`.
- Add a dry‑run `/api/test` endpoint as your first feature to iterate quickly.

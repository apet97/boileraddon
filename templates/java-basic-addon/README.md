# Java Basic Add-on Template

This is a minimal, runnable Clockify add-on using the addon-java-sdk.

## Prerequisites
- Java 17+
- Maven
- GitHub Packages auth configured (see `dev-docs-marketplace-cake-snapshot/extras/maven-setup.md`)

## Run locally
```bash
export ADDON_KEY=example.addon
export ADDON_BASE_URL=http://localhost:8080
mvn -q -f pom.xml -DskipTests package
java -jar target/java-basic-addon-0.1.0-jar-with-dependencies.jar
```

## Expose with ngrok
```bash
ngrok http 8080
export ADDON_BASE_URL=https://<your-ngrok-domain>
```

## Register in Marketplace (dev)
- Use `manifest.json` (validates against `manifest-schema-latest.json`).
- Lifecycle: points to `/lifecycle`.
- Webhook: points to `/webhook`.
- Health: `GET /health`.


## Zero to Installed (Hello World)

```bash
# 1) Clone
git clone https://github.com/apet97/boileraddon.git
cd boileraddon

# 2) Validate manifest (optional)
make validate

# 3) Build
make build-java

# 4) Run
make run-java
# Server listens on :8080

# 5) Expose via ngrok (in another terminal)
ngrok http 8080
# export ADDON_BASE_URL to the ngrok URL, rebuild/run if needed

# 6) Create addon in dev Marketplace UI
# - Use templates/java-basic-addon/manifest.json
# - Lifecycle -> /lifecycle, Webhook -> /webhook, Health -> /health

# 7) Trigger and confirm
# - Reinstall addon to see lifecycle
# - Hit /health and observe OK
```

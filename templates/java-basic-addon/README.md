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


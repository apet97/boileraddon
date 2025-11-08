# Java Basic Add-on Template

A minimal Clockify add-on that uses the same inline SDK shipped with the Auto-Tag Assistant example. Start here when you want a clean slate without business logic. For an architectural overview of the shared SDK pieces, see [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md).

## Requirements

Install the following tools:

- **Java 17+** – Verify with `java -version`.
- **Maven 3.6+** – Verify with `mvn -version`.
- **ngrok** – Only needed when exposing localhost to Clockify (https://ngrok.com/download).
- **(Optional) Make** – Provides shortcuts defined in the repository `Makefile`.

## Build & Run Locally

```bash
# From the repository root
mvn -f templates/java-basic-addon/pom.xml clean package -DskipTests
java -jar templates/java-basic-addon/target/java-basic-addon-0.1.0-jar-with-dependencies.jar
```

The application listens on port `8080` by default. Override the runtime configuration as needed:

```bash
ADDON_PORT=3000 ADDON_BASE_URL=http://localhost:3000/my-addon \
java -jar templates/java-basic-addon/target/java-basic-addon-0.1.0-jar-with-dependencies.jar
```

## Expose to Clockify

1. In a second terminal, forward the local port with ngrok:

   ```bash
   ngrok http 8080
   ```

   Copy the HTTPS URL that ngrok prints (for example `https://abc123.ngrok-free.app`).
2. Restart the add-on with `ADDON_BASE_URL` set to the ngrok domain so the manifest points to the public URL:

   ```bash
   ADDON_PORT=8080 ADDON_BASE_URL=https://abc123.ngrok-free.app/my-addon \
   java -jar templates/java-basic-addon/target/java-basic-addon-0.1.0-jar-with-dependencies.jar
   ```

   Stop any instance you launched with the default `http://localhost:8080` URL before running this command.
3. Install in Clockify:

   - Go to **Admin → Add-ons → Install Custom Add-on**.
   - Enter `https://abc123.ngrok-free.app/my-addon/manifest.json` (replace `my-addon` with your `ADDON_BASE_URL` suffix).

## Makefile Shortcuts

Prefer Make?

```bash
make build-template        # Packages only the template module
make run-auto-tag-assistant # Run the full example (for comparison)
```

## Next Steps

Follow the [Building Your Own Add-on](../../docs/BUILDING-YOUR-OWN-ADDON.md) guide for a copy/rename checklist, manifest customization advice, token management, and deployment tips. The template uses the same manifest builder and routing utilities as the Auto-Tag Assistant, so you can reference both modules interchangeably while extending your add-on.

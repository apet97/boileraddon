# `_template-addon`

This module is a copy-ready starting point for building a new Clockify add-on with Jetty and Jackson. Use it as a template by duplicating the folder and renaming the identifiers that ship with the scaffold.

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

After renaming, run `mvn clean package -pl <your-module> -am` to produce a `*-jar-with-dependencies.jar` that you can deploy.

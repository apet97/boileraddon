# Combined Contents: addon-examples-main + addon-java-sdk-main

Generated: 2025-11-12 18:11:29 UTC

This file aggregates the folder structure and file contents of the two directories: `addon-examples-main` and `addon-java-sdk-main`. Binary files are listed with metadata and omitted by default.

## Directory Tree

- addon-examples-main/
- .DS_Store
- README.md
- addon-examples-main
- hello-world-go
  - go.mod
  - main.go
  - public
    - manifest.json
  - readme.md
  - templates
    - ui.html
- hello-world-java
  - Dockerfile
  - configure-maven.sh
  - docker-compose.yml
  - pom.xml
  - readme.md
  - src
    - main
      - java
        - com
          - cake
            - clockify
              - helloworld
                - HelloWorldAddon.java
                - Server.java
                - model
                  - Installation.java
      - resources
        - ui.html
- iframe-example
  - Caddyfile
  - Dockerfile
  - Dockerfile.dev
  - README.md
  - docker-compose.yml
  - package-lock.json
  - package.json
  - src
    - config.js
    - createWebserver.js
    - dev
    - endpoints
      - index.js
      - manifest.js
    - index.js
    - manifest-v0.1.json
    - printServerInfo.js
  - static
    - sidebar.html
    - tab_icon.svg
- pumble-notifications-java
  - Dockerfile
  - configure-maven.sh
  - docker-compose.yml
  - pom.xml
  - readme.md
  - src
    - main
      - java
        - com
          - cake
            - clockify
              - pumblenotifications
                - AddonRequest.java
                - NotificationsAddon.java
                - Repository.java
                - Server.java
                - handler
                  - WebhookHandler.java
                - model
                  - Installation.java
- ui-example
  - .dockerignore
  - .env.example
  - .gitignore
  - Dockerfile
  - README.md
  - docker-compose.yml
  - package-lock.json
  - package.json
  - src
    - config.js
    - getPublicUrlFromNgrok.js
    - index.js
    - manifest-v0.1.json
    - routes.js
  - static
    - chart.html
    - chat.html
    - index.html
    - tab_icon.svg
- weather-example-serverless
  - README.md
  - icon.png
  - index.html
  - manifest.json

- addon-java-sdk-main/
- .DS_Store
- .github
  - workflows
    - publish-processor.yml
    - publish-sdk.yml
- addon-java-sdk-main
- addon-sdk
  - .DS_Store
  - pom.xml
  - publish.sh
  - src
    - .DS_Store
    - main
      - .DS_Store
      - java
        - .DS_Store
        - com
          - .DS_Store
          - cake
            - .DS_Store
            - clockify
              - .DS_Store
              - addonsdk
                - .DS_Store
                - clockify
                  - ClockifyAddon.java
                  - ClockifySignatureParser.java
                  - model
                    - ClockifyManifest.java
                    - ClockifyResource.java
                - shared
                  - Addon.java
                  - AddonServlet.java
                  - EmbeddedServer.java
                  - RequestHandler.java
                  - ValidationException.java
                  - response
                    - HttpResponse.java
                  - utils
                    - Utils.java
                    - ValidationUtils.java
    - test
      - java
        - com
          - cake
            - clockify
              - AddonTests.java
              - ManifestTests.java
              - ServletTests.java
              - Utils.java
  - target
    - classes
    - generated-sources
      - annotations
    - maven-status
      - maven-compiler-plugin
        - compile
          - default-compile
            - createdFiles.lst
            - inputFiles.lst
- annotation-processor
  - pom.xml
  - src
    - main
      - java
        - com
          - cake
            - clockify
              - annotationprocessor
                - Constants.java
                - ManifestExtensionProcessor.java
                - NodeConstants.java
                - Utils.java
                - clockify
                  - ClockifyManifestProcessor.java
                  - DefinitionProcessor.java
                  - EnumConstantsProcessor.java
                  - ExtendClockifyManifest.java
      - resources
        - clockify-manifests
          - 1.2.json
          - 1.3.json
          - 1.4.json
  - target
    - addon-sdk-annotation-processor-1.0.10.jar
    - classes
      - clockify-manifests
        - 1.2.json
        - 1.3.json
        - 1.4.json
      - com
        - cake
          - clockify
            - annotationprocessor
              - Constants.class
              - ManifestExtensionProcessor.class
              - NodeConstants.class
              - Utils.class
              - clockify
                - ClockifyManifestProcessor.class
                - DefinitionProcessor.class
                - EnumConstantsProcessor.class
                - ExtendClockifyManifest.class
    - generated-sources
      - annotations
    - maven-archiver
      - pom.properties
    - maven-status
      - maven-compiler-plugin
        - compile
          - default-compile
            - createdFiles.lst
            - inputFiles.lst
- configure-maven.sh
- readme.md


## Files

### addon-examples-main/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-examples-main/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-examples-main/README.md

- Size: 134 bytes
- MIME: text/plain; charset=us-ascii

```markdown
# addon-examples
Working examples of addons for the CAKE.com Marketplace for Clockify. Refer each project for the setup instructions.

```

### addon-examples-main/hello-world-go/go.mod

- Size: 27 bytes
- MIME: text/plain; charset=us-ascii

```
module hello-world

go 1.19
```

### addon-examples-main/hello-world-go/main.go

- Size: 827 bytes
- MIME: text/x-c; charset=us-ascii

```go
package main

import (
	"log"
	"net/http"
	"os"
)

func main() {
	// register a handler to serve all the static files under the public directory
	http.Handle("/public/", http.StripPrefix("/public/", http.FileServer(http.Dir("./public"))))
	handleUiComponent()

	// start a http server on port 8080
	err := http.ListenAndServe(":8080", nil)
	if err != nil {
		panic(err)
	}
}

func handleUiComponent() {
	// load the html content from the template
	uiBytes, err := os.ReadFile("./templates/ui.html")
	if err != nil {
		log.Fatalf("unable to read file: %v", err)
	}

	// register a handler that writes the template bytes into the response writer
	http.HandleFunc("/ui", func(w http.ResponseWriter, r *http.Request) {
		_, err := w.Write(uiBytes)
		if err != nil {
			w.WriteHeader(500)
		} else {
			w.WriteHeader(200)
		}
	})
}

```

### addon-examples-main/hello-world-go/public/manifest.json

- Size: 729 bytes
- MIME: text/plain; charset=us-ascii

```json
{
  "schemaVersion": "1.2",
  "key": "helloworld",
  "name": "Hello World",
  "description": "A sample addon that renders a UI component",
  "baseUrl": "",
  "lifecycle": [],
  "webhooks": [],
  "components": [
    {
      "type": "widget",
      "accessLevel": "EVERYONE",
      "path": "/ui",
      "label": "widget"
    }
  ],
  "settings": {
    "tabs": [
      {
        "id": "settings",
        "name": "Settings",
        "settings": [
          {
            "id": "displayed-text",
            "name": "Displayed text",
            "type": "TXT",
            "accessLevel": "EVERYONE",
            "value": "Hello World!"
          }
        ]
      }
    ]
  },
  "minimalSubscriptionPlan": "FREE",
  "scopes": [
  ]
}
```

### addon-examples-main/hello-world-go/readme.md

- Size: 1241 bytes
- MIME: text/html; charset=us-ascii

```markdown
## Hello World

This is a simple addon which will render a widget component that when clicked will display a 'Hello World!' message (or a custom one).

### Getting started
This addon example statically serves the manifest.json file under the following path:
```
{baseUrl}/public/manifest.json
```

The only required change before getting started is to update the 'baseUrl' value inside the ./public/manifest.json file.
See below for a quickstart on how to retrieve a public URL for our example.

Running the addon app:
```shell
go run main.go
```

The above command will run a http server on port 8080.

#### Retrieving a public URL
For this example we made use of a free service called <a href="https://ngrok.com">ngrok</a>.

After downloading the binary, we can execute the following command which will expose the server running on our local port through a public URL.
```shell
ngrok http 8080
```

Last, set the public URL that ngrok provides inside the manifest.json file:
```json
{
  "baseUrl": ""
}
```

### Settings
```displayed-text```
The custom text to be displayed.

### Structure
#### Handlers
The addon makes use of the following HTTP handlers:
- Static files handler (for serving the 'public' directory)
- UI Component handler

```

### addon-examples-main/hello-world-go/templates/ui.html

- Size: 1909 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Index</title>
  <style>
    body {
      background-color: #F2F6F8;
      padding: 20px;
    }
  </style>
</head>

<body>
<h1 id="displayed-text"></h1>


<script>
  // warning: the signature is not validated here
  // this serves only as an example
  function parseJwtClaims (token) {
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    var jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
  }

  const url = new URL(location.href);
  // UI components receive a parameter containing the JWT token whenever they are rendered
  // this JWT token is specific to the current user that is viewing the iframe
  // and will only be able to access data for this user
  const token = url.searchParams.get("auth_token");
  // parse claims from the JWT
  const claims = parseJwtClaims(token);
  // get the workspaceId claim
  const workspaceId = claims.workspaceId;

  let endpoint = `https://developer.clockify.me/api/addon/workspaces/${workspaceId}/settings`;
  fetch(endpoint, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-Addon-Token': token
    }
  })
          .then((response) => response.json())
          .then((data) => {
            const setting = data['tabs'][0]['settings'].filter(s => s['id'] === 'displayed-text')[0];
            const value = setting['value'];

            document.getElementById('displayed-text').textContent=value;
          })
          .catch((error) => {
            console.log('An error occurred.');
            console.log(error);
          });
</script>
</body>
</html>

```

### addon-examples-main/hello-world-java/Dockerfile

- Size: 265 bytes
- MIME: text/plain; charset=us-ascii

```dockerfile
FROM maven:3.8-eclipse-temurin-18 AS build
ADD pom.xml .
ADD configure-maven.sh .
ADD src ./src

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

RUN ./configure-maven.sh $GITHUB_USERNAME $GITHUB_TOKEN
RUN mvn clean package

CMD java -jar ./target/hello-world-1.0.0-shaded.jar
```

### addon-examples-main/hello-world-java/configure-maven.sh

- Size: 903 bytes
- MIME: text/x-shellscript; charset=us-ascii

```sh
#!/bin/bash
if [ $# != 2 ]; then
  echo "You need to pass the Github username and access tokens as parameters."
  exit 1
fi

m2="<settings>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/clockify/addon-java-sdk</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

    <servers>
      <server>
        <id>github</id>
        <username>$1</username>
        <password>$2</password>
      </server>
    </servers>
</settings>"

mkdir /root/.m2
echo "$m2" > /root/.m2/settings.xml
```

### addon-examples-main/hello-world-java/docker-compose.yml

- Size: 345 bytes
- MIME: text/plain; charset=us-ascii

```yaml
version: '3.8'
services:
  addon:
    container_name: hello-world-addon
    image: hello-world-addon
    build:
      context: ./
    ports:
      - "8080:8080"
    environment:
      PUBLIC_URL:
      ADDON_KEY: helloworld
      ADDON_NAME: Hello World
      ADDON_DESCRIPTION: A sample addon that renders a UI component
      LOCAL_PORT: 8080

```

### addon-examples-main/hello-world-java/pom.xml

- Size: 2848 bytes
- MIME: text/xml; charset=us-ascii

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>group-id</groupId>
    <artifactId>hello-world</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
            <version>4.7.1</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId> <!-- or jjwt-gson if Gson is preferred -->
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.thymeleaf</groupId>
            <artifactId>thymeleaf</artifactId>
            <version>3.1.1.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>com.cake.clockify</groupId>
            <artifactId>addon-sdk</artifactId>
            <version>1.1.1</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <shadedArtifactAttached>true</shadedArtifactAttached>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.cake.clockify.helloworld.Server</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### addon-examples-main/hello-world-java/readme.md

- Size: 2467 bytes
- MIME: text/html; charset=us-ascii

```markdown
## Hello World

This is a simple addon which will render a widget component that when clicked will display a 'Hello World!' message (or a custom one).

### How it works
The embedded webserver is started, and handlers are registered for the lifecycle and the UI component.

### Getting started
#### Requirements
- A Github account and an access token associated with it
- Docker

#### Running the addon with docker
The addon can be run using the provided docker compose file.

You should update the PUBLIC_URL environment variable from the docker-compose.yml to reflect the actual value.

First, we build the image by passing in a Github username and it's access token.
These are only used in order to pull the Addon SDK dependency from Github packages.

Then, we run the container and pass in the addon public URL.
The container will expose the following port for the addon: 8080.

Use the following commands to run the addon app:
```shell
docker-compose build --build-arg GITHUB_USERNAME="{username}" --build-arg GITHUB_TOKEN="{token}"
docker-compose up
```

This addon example serves the manifest under the following path:
```
{baseUrl}/manifest
```

### Required environment variables
The Server class is the entrypoint to the addon application.

The addon makes use of the following environment variables:

```
ADDON_KEY=helloworld
ADDON_NAME=Hello World
ADDON_DESCRIPTION=A sample addon that renders a UI component

PUBLIC_URL=
LOCAL_PORT=8080
```

### Retrieving a public URL
The addon must be accessible through a public URL in order for Clockify to be able to communicate with it.

For this example we made use of a free service called <a href="https://ngrok.com">ngrok</a>.

After downloading the binary, we can execute the following command which will expose our server running on our local port through a public URL.
```shell
ngrok http 8080
```

We then pass the public URL that ngrok provides as an env variable:
```
PUBLIC_URL={ngrok public url}
```

### Settings
```displayed-text```
The custom text to be displayed.

### Structure
#### Handlers
The addon makes use of several HTTP handlers:
- Lifecycle handlers (installed, uninstalled, settings updated)
- UI Component handler

The first group is intended to handle lifecycle events and store related information linked to the lifecycle of the addon.
The latter is intended to render the requested UI component.

#### Addon
The HelloWorldAddon class contains all the information related to the addon.
```

### addon-examples-main/hello-world-java/src/main/java/com/cake/clockify/helloworld/HelloWorldAddon.java

- Size: 4448 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.helloworld;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyComponent;
import com.cake.clockify.addonsdk.clockify.model.ClockifyLifecycleEvent;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.ClockifySetting;
import com.cake.clockify.addonsdk.clockify.model.ClockifySettings;
import com.cake.clockify.addonsdk.clockify.model.ClockifySettingsTab;
import com.cake.clockify.helloworld.model.Installation;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;


public final class HelloWorldAddon extends ClockifyAddon {
    public static final ClockifySettings SETTINGS = ClockifySettings.builder()
            .settingsTabs(List.of(ClockifySettingsTab.builder()
                    .id("settings")
                    .name("Settings")
                    .settings(
                            List.of(ClockifySetting.builder()
                                    .id("displayed-text")
                                    .name("Displayed text")
                                    .allowEveryone()
                                    .asTxt()
                                    .value("Hello World!")
                                    .build())
                    ).build()))
            .build();
    private final Gson gson = new Gson();

    public HelloWorldAddon(String publicUrl) {
        super(ClockifyManifest.builder()
                .key(System.getenv("ADDON_KEY"))
                .name(System.getenv("ADDON_NAME"))
                .baseUrl(publicUrl)
                .requireStandardPlan()
                .scopes(List.of())
                .settings(SETTINGS)
                .description(System.getenv("ADDON_DESCRIPTION"))
                .build()
        );
        // lifecycles, components and webhooks can either be added to the manifest builder above
        // if their respective routes are already handled, or they can be registered below in order
        // for them to be served through the addon's servlet
        registerLifecycleEvents();
        registerUiComponents();
    }

    private void registerLifecycleEvents() {
        // this callback is called when the addon is installed in a workspace
        // notice that the auth token that this callback is provided with
        // has full access to the Clockify workspace and should not be leaked to the user
        // or to the frontend
        registerLifecycleEvent(ClockifyLifecycleEvent.builder()
                .path("/lifecycle/installed")
                .onInstalled()
                .build(), (request, response) -> {

            Installation payload = gson.fromJson(request.getReader(), Installation.class);
            // handle the payload here
            response.setStatus(HttpStatus.OK_200);
        });

        // this callback is called when the addon is uninstalled from the workspace
        // from now on, the addon will not be able to communicate with that workspace anymore
        registerLifecycleEvent(ClockifyLifecycleEvent.builder()
                .path("/lifecycle/uninstalled")
                .onDeleted()
                .build(), (request, response) -> {

            Installation payload = gson.fromJson(request.getReader(), Installation.class);
            // handle the payload here
            response.setStatus(HttpStatus.OK_200);
        });
    }

    private void registerUiComponents() {
        registerComponent(ClockifyComponent.builder()
                .widget()
                .allowEveryone()
                .path("/ui")
                .label("widget")
                .build(), (request, response) -> {

            String html;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("ui.html")) {
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                html = CharStreams.toString(reader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            response.getWriter().write(html);
            response.setHeader("Content-Type", "text/html");
            response.setStatus(HttpStatus.OK_200);
        });
    }
}

```

### addon-examples-main/hello-world-java/src/main/java/com/cake/clockify/helloworld/Server.java

- Size: 753 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.helloworld;

import com.cake.clockify.addonsdk.shared.AddonServlet;
import com.cake.clockify.addonsdk.shared.EmbeddedServer;

public class Server {

    public static void main(String[] args) throws Exception {
        String publicUrl = System.getenv("PUBLIC_URL");
        int port = Integer.parseInt(System.getenv("LOCAL_PORT"));

        HelloWorldAddon addon = new HelloWorldAddon(publicUrl);

        // create a HttpServlet to handle the paths that the addon has defined
        AddonServlet servlet = new AddonServlet(addon);

        // start an embedded webserver serving the servlet instance
        // the servlet can also be served through other frameworks
        new EmbeddedServer(servlet).start(port);
    }
}

```

### addon-examples-main/hello-world-java/src/main/java/com/cake/clockify/helloworld/model/Installation.java

- Size: 200 bytes
- MIME: text/plain; charset=us-ascii

```java
package com.cake.clockify.helloworld.model;

public record Installation(
        String addonId,
        String authToken,
        String workspaceId,
        String asUser,
        String apiUrl
) {}
```

### addon-examples-main/hello-world-java/src/main/resources/ui.html

- Size: 2179 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Index</title>
    <style>
        body {
            background-color: #F2F6F8;
            padding: 20px;
        }
    </style>
  </head>

  <body>
    <h1 id="displayed-text"></h1>


    <script>
        // warning: the signature is not validated here
        // this serves only as an example
        function parseJwtClaims (token) {
            var base64Url = token.split('.')[1];
            var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            var jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));

            return JSON.parse(jsonPayload);
        }

        const url = new URL(location.href);
        // UI components receive a parameter containing the JWT token whenever they are rendered
        // this JWT token is specific to the current user that is viewing the iframe
        // and will only be able to access data for this user
        const token = url.searchParams.get("auth_token");
        // parse claims from the JWT
        const claims = parseJwtClaims(token);
        // get the workspaceId claim
        const workspaceId = claims.workspaceId;

        let endpoint = `https://developer.clockify.me/api/addon/workspaces/${workspaceId}/settings`;
        fetch(endpoint, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Addon-Token': token
            }
        })
            .then((response) => response.json())
            .then((data) => {
                const setting = data['tabs'][0]['settings'].filter(s => s['id'] === 'displayed-text')[0];
                const value = setting['value'];

                document.getElementById('displayed-text').textContent=value;
            })
            .catch((error) => {
                console.log('An error occurred.');
                console.log(error);
            });
    </script>
  </body>
</html>

```

### addon-examples-main/iframe-example/Caddyfile

- Size: 514 bytes
- MIME: text/plain; charset=us-ascii

```conf
:80 {
	@static {
		path_regexp ^.*\.(js|css|png|jpe?g|svg|ico)$
	}
	@other {
		not path_regexp ^.*\.(js|css|png|jpe?g|svg|ico)$
	}
	header X-Robots-Tag "noindex"
	header @static Cache-control max-age=2592000
	header @other Cache-control no-store
	header -server
	encode zstd gzip
	log {
		output stdout
		format json
	}

	root * /app

	handle /manifest.json {
	    header Content-Type application/json
	    respond `$MANIFEST_FILE`
	}

	route {
		try_files {path} {path}.html {path}/ index.html
		file_server
	}
}

```

### addon-examples-main/iframe-example/Dockerfile

- Size: 242 bytes
- MIME: text/plain; charset=us-ascii

```dockerfile
FROM caddy:2-alpine

RUN apk add gettext

WORKDIR /app

COPY static /app
COPY src/manifest-v0.1.json /app

COPY Caddyfile /app
RUN MANIFEST_FILE=$(cat manifest-v0.1.json) envsubst < /app/Caddyfile > /etc/caddy/Caddyfile
RUN rm /app/Caddyfile

```

### addon-examples-main/iframe-example/Dockerfile.dev

- Size: 80 bytes
- MIME: text/plain; charset=us-ascii

```
FROM node:16-alpine

WORKDIR /app
COPY . /app
RUN npm i

ENTRYPOINT npm run dev

```

### addon-examples-main/iframe-example/README.md

- Size: 892 bytes
- MIME: text/plain; charset=us-ascii

```markdown
# iframe example Add-on
This addon is an example of storing a url on the settings page and using that to display an iframe on sidebar.

The manifest can be obtained on `GET {addonUrl}/manifest-v0.1.json`.

Developed using NodeJS, Docker. This addon doesn't not use any sdks, just plain HTML to display the iframe and a static manifest (static/manifest-v0.1.json) file to define settings page and the sidebar entrypoint.

## How to run this addon locally

You need docker installed and a ngrok auth token that can be found on [ngrok dashboard](https://dashboard.ngrok.com/get-started/your-authtoken).

```
cp .env.example .env
```
Edit .env and include your token on `NGROK_AUTH_TOKEN=(your token here)`
```
docker compose up
```

After that the container is up, use the url provided on the console to register the addon. This addon comes up with ngrok and generates the public url by itself.

```

### addon-examples-main/iframe-example/docker-compose.yml

- Size: 264 bytes
- MIME: text/plain; charset=us-ascii

```yaml
version: '3'
services:
  addon:
    restart: always
    build:
      context: .
      dockerfile: Dockerfile.dev
    volumes:
      - ./src:/app/src
      - ./static:/app/static
    env_file:
      - .env
    ports:
      - "8080:8080"
    entrypoint: npm run dev

```

### addon-examples-main/iframe-example/package-lock.json

- Size: 106242 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "name": "node-example",
  "version": "0.0.1",
  "lockfileVersion": 2,
  "requires": true,
  "packages": {
    "": {
      "name": "node-example",
      "version": "0.0.1",
      "license": "MIT",
      "dependencies": {
        "async-exit-hook": "^2.0.1",
        "body-parser": "^1.20.2",
        "cli-color": "^2.0.3",
        "express": "^4.18.2",
        "ngrok": "^4.3.3",
        "nodemon": "^2.0.20"
      },
      "devDependencies": {
        "prettier": "3.0.3"
      }
    },
    "node_modules/@sindresorhus/is": {
      "version": "4.6.0",
      "resolved": "https://registry.npmjs.org/@sindresorhus/is/-/is-4.6.0.tgz",
      "integrity": "sha512-t09vSN3MdfsyCHoFcTRCH/iUtG7OJ0CsjzB8cjAmKc/va/kIgeDI/TxsigdncE/4be734m0cvIYwNaV4i2XqAw==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sindresorhus/is?sponsor=1"
      }
    },
    "node_modules/@szmarczak/http-timer": {
      "version": "4.0.6",
      "resolved": "https://registry.npmjs.org/@szmarczak/http-timer/-/http-timer-4.0.6.tgz",
      "integrity": "sha512-4BAffykYOgO+5nzBWYwE3W90sBgLJoUPRWWcL8wlyiM8IB8ipJz3UMJ9KXQd1RKQXpKp8Tutn80HZtWsu2u76w==",
      "dependencies": {
        "defer-to-connect": "^2.0.0"
      },
      "engines": {
        "node": ">=10"
      }
    },
    "node_modules/@types/cacheable-request": {
      "version": "6.0.3",
      "resolved": "https://registry.npmjs.org/@types/cacheable-request/-/cacheable-request-6.0.3.tgz",
      "integrity": "sha512-IQ3EbTzGxIigb1I3qPZc1rWJnH0BmSKv5QYTalEwweFvyBDLSAe24zP0le/hyi7ecGfZVlIVAg4BZqb8WBwKqw==",
      "dependencies": {
        "@types/http-cache-semantics": "*",
        "@types/keyv": "^3.1.4",
        "@types/node": "*",
        "@types/responselike": "^1.0.0"
      }
    },
    "node_modules/@types/http-cache-semantics": {
      "version": "4.0.1",
      "resolved": "https://registry.npmjs.org/@types/http-cache-semantics/-/http-cache-semantics-4.0.1.tgz",
      "integrity": "sha512-SZs7ekbP8CN0txVG2xVRH6EgKmEm31BOxA07vkFaETzZz1xh+cbt8BcI0slpymvwhx5dlFnQG2rTlPVQn+iRPQ=="
    },
    "node_modules/@types/keyv": {
      "version": "3.1.4",
      "resolved": "https://registry.npmjs.org/@types/keyv/-/keyv-3.1.4.tgz",
      "integrity": "sha512-BQ5aZNSCpj7D6K2ksrRCTmKRLEpnPvWDiLPfoGyhZ++8YtiK9d/3DBKPJgry359X/P1PfruyYwvnvwFjuEiEIg==",
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/@types/node": {
      "version": "8.10.66",
      "resolved": "https://registry.npmjs.org/@types/node/-/node-8.10.66.tgz",
      "integrity": "sha512-tktOkFUA4kXx2hhhrB8bIFb5TbwzS4uOhKEmwiD+NoiL0qtP2OQ9mFldbgD4dV1djrlBYP6eBuQZiWjuHUpqFw=="
    },
    "node_modules/@types/responselike": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/@types/responselike/-/responselike-1.0.0.tgz",
      "integrity": "sha512-85Y2BjiufFzaMIlvJDvTTB8Fxl2xfLo4HgmHzVBz08w4wDePCTjYw66PdrolO0kzli3yam/YCgRufyo1DdQVTA==",
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/@types/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/@types/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-Cn6WYCm0tXv8p6k+A8PvbDG763EDpBoTzHdA+Q/MF6H3sapGjCm9NzoaJncJS9tUKSuCoDs9XHxYYsQDgxR6kw==",
      "optional": true,
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/abbrev": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/abbrev/-/abbrev-1.1.1.tgz",
      "integrity": "sha512-nne9/IiQ/hzIhY6pdDnbBtz7DjPTKrY00P/zvPSm5pOFkl6xuGrGnXn/VtTNNfNtAfZ9/1RtehkszU9qcTii0Q=="
    },
    "node_modules/accepts": {
      "version": "1.3.8",
      "resolved": "https://registry.npmjs.org/accepts/-/accepts-1.3.8.tgz",
      "integrity": "sha512-PYAthTa2m2VKxuvSD3DPC/Gy+U+sOA1LAuT8mkmRuvw+NACSaeXEQ+NHcVF7rONl6qcaxV3Uuemwawk+7+SJLw==",
      "dependencies": {
        "mime-types": "~2.1.34",
        "negotiator": "0.6.3"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/anymatch": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/anymatch/-/anymatch-3.1.3.tgz",
      "integrity": "sha512-KMReFUr0B4t+D+OBkjR3KYqvocp2XaSzO55UcB6mgQMd3KbcE+mWTyvVV7D/zsdEbNnV6acZUutkiHQXvTr1Rw==",
      "dependencies": {
        "normalize-path": "^3.0.0",
        "picomatch": "^2.0.4"
      },
      "engines": {
        "node": ">= 8"
      }
    },
    "node_modules/array-flatten": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/array-flatten/-/array-flatten-1.1.1.tgz",
      "integrity": "sha512-PCVAQswWemu6UdxsDFFX/+gVeYqKAod3D3UVm91jHwynguOwAvYPhx8nNlM++NqRcK6CxxpUafjmhIdKiHibqg=="
    },
    "node_modules/async-exit-hook": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/async-exit-hook/-/async-exit-hook-2.0.1.tgz",
      "integrity": "sha512-NW2cX8m1Q7KPA7a5M2ULQeZ2wR5qI5PAbw5L0UOMxdioVk9PMZ0h1TmyZEkPYrCvYjDlFICusOu1dlEKAAeXBw==",
      "engines": {
        "node": ">=0.12.0"
      }
    },
    "node_modules/balanced-match": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/balanced-match/-/balanced-match-1.0.2.tgz",
      "integrity": "sha512-3oSeUO0TMV67hN1AmbXsK4yaqU7tjiHlbxRDZOpH0KW9+CeX4bRAaX0Anxt0tx2MrpRpWwQaPwIlISEJhYU5Pw=="
    },
    "node_modules/binary-extensions": {
      "version": "2.2.0",
      "resolved": "https://registry.npmjs.org/binary-extensions/-/binary-extensions-2.2.0.tgz",
      "integrity": "sha512-jDctJ/IVQbZoJykoeHbhXpOlNBqGNcwXJKJog42E5HDPUwQTSdjCHdihjj0DlnheQ7blbT6dHOafNAiS8ooQKA==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/body-parser": {
      "version": "1.20.2",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.2.tgz",
      "integrity": "sha512-ml9pReCu3M61kGlqoTm2umSXTlRTuGTx0bfYj+uIUKKYycG5NtSbeetV3faSU6R7ajOPw0g/J1PvK4qNy7s5bA==",
      "dependencies": {
        "bytes": "3.1.2",
        "content-type": "~1.0.5",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.2",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/brace-expansion": {
      "version": "1.1.11",
      "resolved": "https://registry.npmjs.org/brace-expansion/-/brace-expansion-1.1.11.tgz",
      "integrity": "sha512-iCuPHDFgrHX7H2vEI/5xpz07zSHB00TpugqhmYtVmMO6518mCuRMoOYFldEBl0g187ufozdaHgWKcYFb61qGiA==",
      "dependencies": {
        "balanced-match": "^1.0.0",
        "concat-map": "0.0.1"
      }
    },
    "node_modules/braces": {
      "version": "3.0.2",
      "resolved": "https://registry.npmjs.org/braces/-/braces-3.0.2.tgz",
      "integrity": "sha512-b8um+L1RzM3WDSzvhm6gIz1yfTbBt6YTlcEKAvsmqCZZFw46z626lVj9j1yEPW33H5H+lBQpZMP1k8l+78Ha0A==",
      "dependencies": {
        "fill-range": "^7.0.1"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/buffer-crc32": {
      "version": "0.2.13",
      "resolved": "https://registry.npmjs.org/buffer-crc32/-/buffer-crc32-0.2.13.tgz",
      "integrity": "sha512-VO9Ht/+p3SN7SKWqcrgEzjGbRSJYTx+Q1pTQC0wrWqHx0vpJraQ6GtHx8tvcg1rlK1byhU5gccxgOgj7B0TDkQ==",
      "engines": {
        "node": "*"
      }
    },
    "node_modules/bytes": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/bytes/-/bytes-3.1.2.tgz",
      "integrity": "sha512-/Nf7TyzTx6S3yRJObOAV7956r8cr2+Oj8AC5dt8wSP3BQAoeX58NoHyCU8P8zGkNXStjTSi6fzO6F0pBdcYbEg==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/cacheable-lookup": {
      "version": "5.0.4",
      "resolved": "https://registry.npmjs.org/cacheable-lookup/-/cacheable-lookup-5.0.4.tgz",
      "integrity": "sha512-2/kNscPhpcxrOigMZzbiWF7dz8ilhb/nIHU3EyZiXWXpeq/au8qJ8VhdftMkty3n7Gj6HIGalQG8oiBNB3AJgA==",
      "engines": {
        "node": ">=10.6.0"
      }
    },
    "node_modules/cacheable-request": {
      "version": "7.0.2",
      "resolved": "https://registry.npmjs.org/cacheable-request/-/cacheable-request-7.0.2.tgz",
      "integrity": "sha512-pouW8/FmiPQbuGpkXQ9BAPv/Mo5xDGANgSNXzTzJ8DrKGuXOssM4wIQRjfanNRh3Yu5cfYPvcorqbhg2KIJtew==",
      "dependencies": {
        "clone-response": "^1.0.2",
        "get-stream": "^5.1.0",
        "http-cache-semantics": "^4.0.0",
        "keyv": "^4.0.0",
        "lowercase-keys": "^2.0.0",
        "normalize-url": "^6.0.1",
        "responselike": "^2.0.0"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/call-bind": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/call-bind/-/call-bind-1.0.2.tgz",
      "integrity": "sha512-7O+FbCihrB5WGbFYesctwmTKae6rOiIzmz1icreWJ+0aA7LJfuqhEso2T9ncpcFtzMQtzXf2QGGueWJGTYsqrA==",
      "dependencies": {
        "function-bind": "^1.1.1",
        "get-intrinsic": "^1.0.2"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/chokidar": {
      "version": "3.5.3",
      "resolved": "https://registry.npmjs.org/chokidar/-/chokidar-3.5.3.tgz",
      "integrity": "sha512-Dr3sfKRP6oTcjf2JmUmFJfeVMvXBdegxB0iVQ5eb2V10uFJUCAS8OByZdVAyVb8xXNz3GjjTgj9kLWsZTqE6kw==",
      "funding": [
        {
          "type": "individual",
          "url": "https://paulmillr.com/funding/"
        }
      ],
      "dependencies": {
        "anymatch": "~3.1.2",
        "braces": "~3.0.2",
        "glob-parent": "~5.1.2",
        "is-binary-path": "~2.1.0",
        "is-glob": "~4.0.1",
        "normalize-path": "~3.0.0",
        "readdirp": "~3.6.0"
      },
      "engines": {
        "node": ">= 8.10.0"
      },
      "optionalDependencies": {
        "fsevents": "~2.3.2"
      }
    },
    "node_modules/cli-color": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/cli-color/-/cli-color-2.0.3.tgz",
      "integrity": "sha512-OkoZnxyC4ERN3zLzZaY9Emb7f/MhBOIpePv0Ycok0fJYT+Ouo00UBEIwsVsr0yoow++n5YWlSUgST9GKhNHiRQ==",
      "dependencies": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.61",
        "es6-iterator": "^2.0.3",
        "memoizee": "^0.4.15",
        "timers-ext": "^0.1.7"
      },
      "engines": {
        "node": ">=0.10"
      }
    },
    "node_modules/clone-response": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/clone-response/-/clone-response-1.0.3.tgz",
      "integrity": "sha512-ROoL94jJH2dUVML2Y/5PEDNaSHgeOdSDicUyS7izcF63G6sTc/FTjLub4b8Il9S8S0beOfYt0TaA5qvFK+w0wA==",
      "dependencies": {
        "mimic-response": "^1.0.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/concat-map": {
      "version": "0.0.1",
      "resolved": "https://registry.npmjs.org/concat-map/-/concat-map-0.0.1.tgz",
      "integrity": "sha512-/Srv4dswyQNBfohGpz9o6Yb3Gz3SrUDqBH5rTuhGR7ahtlbYKnVxw2bCFMRljaA7EXHaXZ8wsHdodFvbkhKmqg=="
    },
    "node_modules/content-disposition": {
      "version": "0.5.4",
      "resolved": "https://registry.npmjs.org/content-disposition/-/content-disposition-0.5.4.tgz",
      "integrity": "sha512-FveZTNuGw04cxlAiWbzi6zTAL/lhehaWbTtgluJh4/E95DqMwTmha3KZN1aAWA8cFIhHzMZUvLevkw5Rqk+tSQ==",
      "dependencies": {
        "safe-buffer": "5.2.1"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/content-type": {
      "version": "1.0.5",
      "resolved": "https://registry.npmjs.org/content-type/-/content-type-1.0.5.tgz",
      "integrity": "sha512-nTjqfcBFEipKdXCv4YDQWCfmcLZKm81ldF0pAopTvyrFGVbcR6P/VAAd5G7N+0tTr8QqiU0tFadD6FK4NtJwOA==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/cookie": {
      "version": "0.5.0",
      "resolved": "https://registry.npmjs.org/cookie/-/cookie-0.5.0.tgz",
      "integrity": "sha512-YZ3GUyn/o8gfKJlnlX7g7xq4gyO6OSuhGPKaaGssGB2qgDUS0gPgtTvoyZLTt9Ab6dC4hfc9dV5arkvc/OCmrw==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/cookie-signature": {
      "version": "1.0.6",
      "resolved": "https://registry.npmjs.org/cookie-signature/-/cookie-signature-1.0.6.tgz",
      "integrity": "sha512-QADzlaHc8icV8I7vbaJXJwod9HWYp8uCqf1xa4OfNu1T7JVxQIrUgOWtHdNDtPiywmFbiS12VjotIXLrKM3orQ=="
    },
    "node_modules/d": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/d/-/d-1.0.1.tgz",
      "integrity": "sha512-m62ShEObQ39CfralilEQRjH6oAMtNCV1xJyEx5LpRYUVN+EviphDgUc/F3hnYbADmkiNs67Y+3ylmlG7Lnu+FA==",
      "dependencies": {
        "es5-ext": "^0.10.50",
        "type": "^1.0.1"
      }
    },
    "node_modules/debug": {
      "version": "2.6.9",
      "resolved": "https://registry.npmjs.org/debug/-/debug-2.6.9.tgz",
      "integrity": "sha512-bC7ElrdJaJnPbAP+1EotYvqZsb3ecl5wi6Bfi6BJTUcNowp6cvspg0jXznRTKDjm/E7AdgFBVeAPVMNcKGsHMA==",
      "dependencies": {
        "ms": "2.0.0"
      }
    },
    "node_modules/decompress-response": {
      "version": "6.0.0",
      "resolved": "https://registry.npmjs.org/decompress-response/-/decompress-response-6.0.0.tgz",
      "integrity": "sha512-aW35yZM6Bb/4oJlZncMH2LCoZtJXTRxES17vE3hoRiowU2kWHaJKFkSBDnDR+cm9J+9QhXmREyIfv0pji9ejCQ==",
      "dependencies": {
        "mimic-response": "^3.1.0"
      },
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/decompress-response/node_modules/mimic-response": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-3.1.0.tgz",
      "integrity": "sha512-z0yWI+4FDrrweS8Zmt4Ej5HdJmky15+L2e6Wgn3+iK5fWzb6T3fhNFq2+MeTRb064c6Wr4N/wv0DzQTjNzHNGQ==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/defer-to-connect": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/defer-to-connect/-/defer-to-connect-2.0.1.tgz",
      "integrity": "sha512-4tvttepXG1VaYGrRibk5EwJd1t4udunSOVMdLSAL6mId1ix438oPwPZMALY41FCijukO1L0twNcGsdzS7dHgDg==",
      "engines": {
        "node": ">=10"
      }
    },
    "node_modules/depd": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/depd/-/depd-2.0.0.tgz",
      "integrity": "sha512-g7nH6P6dyDioJogAAGprGpCtVImJhpPk/roCzdb3fIh61/s/nPsfR6onyMwkCAR/OlC3yBC0lESvUoQEAssIrw==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/destroy": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/destroy/-/destroy-1.2.0.tgz",
      "integrity": "sha512-2sJGJTaXIIaR1w4iJSNoN0hnMY7Gpc/n8D4qSCJw8QqFWXf7cuAgnEHxBpweaVcPevC2l3KpjYCx3NypQQgaJg==",
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/ee-first": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/ee-first/-/ee-first-1.1.1.tgz",
      "integrity": "sha512-WMwm9LhRUo+WUaRN+vRuETqG89IgZphVSNkdFgeb6sS/E4OrDIN7t48CAewSHXc6C8lefD8KKfr5vY61brQlow=="
    },
    "node_modules/encodeurl": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/encodeurl/-/encodeurl-1.0.2.tgz",
      "integrity": "sha512-TPJXq8JqFaVYm2CWmPvnP2Iyo4ZSM7/QKcSmuMLDObfpH5fi7RUGmd/rTDf+rut/saiDiQEeVTNgAmJEdAOx0w==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/end-of-stream": {
      "version": "1.4.4",
      "resolved": "https://registry.npmjs.org/end-of-stream/-/end-of-stream-1.4.4.tgz",
      "integrity": "sha512-+uw1inIHVPQoaVuHzRyXd21icM+cnt4CzD5rW+NC1wjOUSTOs+Te7FOv7AhN7vS9x/oIyhLP5PR1H+phQAHu5Q==",
      "dependencies": {
        "once": "^1.4.0"
      }
    },
    "node_modules/es5-ext": {
      "version": "0.10.62",
      "resolved": "https://registry.npmjs.org/es5-ext/-/es5-ext-0.10.62.tgz",
      "integrity": "sha512-BHLqn0klhEpnOKSrzn/Xsz2UIW8j+cGmo9JLzr8BiUapV8hPL9+FliFqjwr9ngW7jWdnxv6eO+/LqyhJVqgrjA==",
      "hasInstallScript": true,
      "dependencies": {
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.3",
        "next-tick": "^1.1.0"
      },
      "engines": {
        "node": ">=0.10"
      }
    },
    "node_modules/es6-iterator": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-iterator/-/es6-iterator-2.0.3.tgz",
      "integrity": "sha512-zw4SRzoUkd+cl+ZoE15A9o1oQd920Bb0iOJMQkQhl3jNc03YqVjAhG7scf9C5KWRU/R13Orf588uCC6525o02g==",
      "dependencies": {
        "d": "1",
        "es5-ext": "^0.10.35",
        "es6-symbol": "^3.1.1"
      }
    },
    "node_modules/es6-symbol": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/es6-symbol/-/es6-symbol-3.1.3.tgz",
      "integrity": "sha512-NJ6Yn3FuDinBaBRWl/q5X/s4koRHBrgKAu+yGI6JCBeiu3qrcbJhwT2GeR/EXVfylRk8dpQVJoLEFhK+Mu31NA==",
      "dependencies": {
        "d": "^1.0.1",
        "ext": "^1.1.2"
      }
    },
    "node_modules/es6-weak-map": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-weak-map/-/es6-weak-map-2.0.3.tgz",
      "integrity": "sha512-p5um32HOTO1kP+w7PRnB+5lQ43Z6muuMuIMffvDN8ZB4GcnjLBV6zGStpbASIMk4DCAvEaamhe2zhyCb/QXXsA==",
      "dependencies": {
        "d": "1",
        "es5-ext": "^0.10.46",
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.1"
      }
    },
    "node_modules/escape-html": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/escape-html/-/escape-html-1.0.3.tgz",
      "integrity": "sha512-NiSupZ4OeuGwr68lGIeym/ksIZMJodUGOSCZ/FSnTxcrekbvqrgdUxlJOMpijaKZVjAJrWrGs/6Jy8OMuyj9ow=="
    },
    "node_modules/etag": {
      "version": "1.8.1",
      "resolved": "https://registry.npmjs.org/etag/-/etag-1.8.1.tgz",
      "integrity": "sha512-aIL5Fx7mawVa300al2BnEE4iNvo1qETxLrPI/o05L7z6go7fCw1J6EQmbK4FmJ2AS7kgVF/KEZWufBfdClMcPg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/event-emitter": {
      "version": "0.3.5",
      "resolved": "https://registry.npmjs.org/event-emitter/-/event-emitter-0.3.5.tgz",
      "integrity": "sha512-D9rRn9y7kLPnJ+hMq7S/nhvoKwwvVJahBi2BPmx3bvbsEdK3W9ii8cBSGjP+72/LnM4n6fo3+dkCX5FeTQruXA==",
      "dependencies": {
        "d": "1",
        "es5-ext": "~0.10.14"
      }
    },
    "node_modules/express": {
      "version": "4.18.2",
      "resolved": "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
      "integrity": "sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==",
      "dependencies": {
        "accepts": "~1.3.8",
        "array-flatten": "1.1.1",
        "body-parser": "1.20.1",
        "content-disposition": "0.5.4",
        "content-type": "~1.0.4",
        "cookie": "0.5.0",
        "cookie-signature": "1.0.6",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "finalhandler": "1.2.0",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "merge-descriptors": "1.0.1",
        "methods": "~1.1.2",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "path-to-regexp": "0.1.7",
        "proxy-addr": "~2.0.7",
        "qs": "6.11.0",
        "range-parser": "~1.2.1",
        "safe-buffer": "5.2.1",
        "send": "0.18.0",
        "serve-static": "1.15.0",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "type-is": "~1.6.18",
        "utils-merge": "1.0.1",
        "vary": "~1.1.2"
      },
      "engines": {
        "node": ">= 0.10.0"
      }
    },
    "node_modules/express/node_modules/body-parser": {
      "version": "1.20.1",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.1.tgz",
      "integrity": "sha512-jWi7abTbYwajOytWCQc37VulmWiRae5RyTpaCyDcS5/lMdtwSz5lOpDE67srw/HYe35f1z3fDQw+3txg7gNtWw==",
      "dependencies": {
        "bytes": "3.1.2",
        "content-type": "~1.0.4",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.1",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/express/node_modules/raw-body": {
      "version": "2.5.1",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.1.tgz",
      "integrity": "sha512-qqJBtEyVgS0ZmPGdCFPWJ3FreoqvG4MVQln/kCgF7Olq95IbOp0/BWyMwbdtn4VTvkM8Y7khCQ2Xgk/tcrCXig==",
      "dependencies": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/ext": {
      "version": "1.7.0",
      "resolved": "https://registry.npmjs.org/ext/-/ext-1.7.0.tgz",
      "integrity": "sha512-6hxeJYaL110a9b5TEJSj0gojyHQAmA2ch5Os+ySCiA1QGdS697XWY1pzsrSjqA9LDEEgdB/KypIlR59RcLuHYw==",
      "dependencies": {
        "type": "^2.7.2"
      }
    },
    "node_modules/ext/node_modules/type": {
      "version": "2.7.2",
      "resolved": "https://registry.npmjs.org/type/-/type-2.7.2.tgz",
      "integrity": "sha512-dzlvlNlt6AXU7EBSfpAscydQ7gXB+pPGsPnfJnZpiNJBDj7IaJzQlBZYGdEi4R9HmPdBv2XmWJ6YUtoTa7lmCw=="
    },
    "node_modules/extract-zip": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/extract-zip/-/extract-zip-2.0.1.tgz",
      "integrity": "sha512-GDhU9ntwuKyGXdZBUgTIe+vXnWj0fppUEtMDL0+idd5Sta8TGpHssn/eusA9mrPr9qNDym6SxAYZjNvCn/9RBg==",
      "dependencies": {
        "debug": "^4.1.1",
        "get-stream": "^5.1.0",
        "yauzl": "^2.10.0"
      },
      "bin": {
        "extract-zip": "cli.js"
      },
      "engines": {
        "node": ">= 10.17.0"
      },
      "optionalDependencies": {
        "@types/yauzl": "^2.9.1"
      }
    },
    "node_modules/extract-zip/node_modules/debug": {
      "version": "4.3.4",
      "resolved": "https://registry.npmjs.org/debug/-/debug-4.3.4.tgz",
      "integrity": "sha512-PRWFHuSU3eDtQJPvnNY7Jcket1j0t5OuOsFzPPzsekD52Zl8qUfFIPEiswXqIvHWGVHOgX+7G/vCNNhehwxfkQ==",
      "dependencies": {
        "ms": "2.1.2"
      },
      "engines": {
        "node": ">=6.0"
      },
      "peerDependenciesMeta": {
        "supports-color": {
          "optional": true
        }
      }
    },
    "node_modules/extract-zip/node_modules/ms": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.2.tgz",
      "integrity": "sha512-sGkPx+VjMtmA6MX27oA4FBFELFCZZ4S4XqeGOXCv68tT+jb3vk/RyaKWP0PTKyWtmLSM0b+adUTEvbs1PEaH2w=="
    },
    "node_modules/fd-slicer": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/fd-slicer/-/fd-slicer-1.1.0.tgz",
      "integrity": "sha512-cE1qsB/VwyQozZ+q1dGxR8LBYNZeofhEdUNGSMbQD3Gw2lAzX9Zb3uIU6Ebc/Fmyjo9AWWfnn0AUCHqtevs/8g==",
      "dependencies": {
        "pend": "~1.2.0"
      }
    },
    "node_modules/fill-range": {
      "version": "7.0.1",
      "resolved": "https://registry.npmjs.org/fill-range/-/fill-range-7.0.1.tgz",
      "integrity": "sha512-qOo9F+dMUmC2Lcb4BbVvnKJxTPjCm+RRpe4gDuGrzkL7mEVl/djYSu2OdQ2Pa302N4oqkSg9ir6jaLWJ2USVpQ==",
      "dependencies": {
        "to-regex-range": "^5.0.1"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/finalhandler": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/finalhandler/-/finalhandler-1.2.0.tgz",
      "integrity": "sha512-5uXcUVftlQMFnWC9qu/svkWv3GTd2PfUhK/3PLkYNAe7FbqJMt3515HaxE6eRL74GdsriiwujiawdaB1BpEISg==",
      "dependencies": {
        "debug": "2.6.9",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "statuses": "2.0.1",
        "unpipe": "~1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/forwarded": {
      "version": "0.2.0",
      "resolved": "https://registry.npmjs.org/forwarded/-/forwarded-0.2.0.tgz",
      "integrity": "sha512-buRG0fpBtRHSTCOASe6hD258tEubFoRLb4ZNA6NxMVHNw2gOcwHo9wyablzMzOA5z9xA9L1KNjk/Nt6MT9aYow==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/fresh": {
      "version": "0.5.2",
      "resolved": "https://registry.npmjs.org/fresh/-/fresh-0.5.2.tgz",
      "integrity": "sha512-zJ2mQYM18rEFOudeV4GShTGIQ7RbzA7ozbU9I/XBpm7kqgMywgmylMwXHxZJmkVoYkna9d2pVXVXPdYTP9ej8Q==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/fsevents": {
      "version": "2.3.2",
      "resolved": "https://registry.npmjs.org/fsevents/-/fsevents-2.3.2.tgz",
      "integrity": "sha512-xiqMQR4xAeHTuB9uWm+fFRcIOgKBMiOBP+eXiyT7jsgVCq1bkVygt00oASowB7EdtpOHaaPgKt812P9ab+DDKA==",
      "hasInstallScript": true,
      "optional": true,
      "os": [
        "darwin"
      ],
      "engines": {
        "node": "^8.16.0 || ^10.6.0 || >=11.0.0"
      }
    },
    "node_modules/function-bind": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/function-bind/-/function-bind-1.1.1.tgz",
      "integrity": "sha512-yIovAzMX49sF8Yl58fSCWJ5svSLuaibPxXQJFLmBObTuCr0Mf1KiPopGM9NiFjiYBCbfaa2Fh6breQ6ANVTI0A=="
    },
    "node_modules/get-intrinsic": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/get-intrinsic/-/get-intrinsic-1.2.0.tgz",
      "integrity": "sha512-L049y6nFOuom5wGyRc3/gdTLO94dySVKRACj1RmJZBQXlbTMhtNIgkWkUHq+jYmZvKf14EW1EoJnnjbmoHij0Q==",
      "dependencies": {
        "function-bind": "^1.1.1",
        "has": "^1.0.3",
        "has-symbols": "^1.0.3"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/get-stream": {
      "version": "5.2.0",
      "resolved": "https://registry.npmjs.org/get-stream/-/get-stream-5.2.0.tgz",
      "integrity": "sha512-nBF+F1rAZVCu/p7rjzgA+Yb4lfYXrpl7a6VmJrU8wF9I1CKvP/QwPNZHnOlwbTkY6dvtFIzFMSyQXbLoTQPRpA==",
      "dependencies": {
        "pump": "^3.0.0"
      },
      "engines": {
        "node": ">=8"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/glob-parent": {
      "version": "5.1.2",
      "resolved": "https://registry.npmjs.org/glob-parent/-/glob-parent-5.1.2.tgz",
      "integrity": "sha512-AOIgSQCepiJYwP3ARnGx+5VnTu2HBYdzbGP45eLw1vr3zB3vZLeyed1sC9hnbcOc9/SrMyM5RPQrkGz4aS9Zow==",
      "dependencies": {
        "is-glob": "^4.0.1"
      },
      "engines": {
        "node": ">= 6"
      }
    },
    "node_modules/got": {
      "version": "11.8.6",
      "resolved": "https://registry.npmjs.org/got/-/got-11.8.6.tgz",
      "integrity": "sha512-6tfZ91bOr7bOXnK7PRDCGBLa1H4U080YHNaAQ2KsMGlLEzRbk44nsZF2E1IeRc3vtJHPVbKCYgdFbaGO2ljd8g==",
      "dependencies": {
        "@sindresorhus/is": "^4.0.0",
        "@szmarczak/http-timer": "^4.0.5",
        "@types/cacheable-request": "^6.0.1",
        "@types/responselike": "^1.0.0",
        "cacheable-lookup": "^5.0.3",
        "cacheable-request": "^7.0.2",
        "decompress-response": "^6.0.0",
        "http2-wrapper": "^1.0.0-beta.5.2",
        "lowercase-keys": "^2.0.0",
        "p-cancelable": "^2.0.0",
        "responselike": "^2.0.0"
      },
      "engines": {
        "node": ">=10.19.0"
      },
      "funding": {
        "url": "https://github.com/sindresorhus/got?sponsor=1"
      }
    },
    "node_modules/has": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has/-/has-1.0.3.tgz",
      "integrity": "sha512-f2dvO0VU6Oej7RkWJGrehjbzMAjFp5/VKPp5tTpWIV4JHHZK1/BxbFRtf/siA2SWTe09caDmVtYYzWEIbBS4zw==",
      "dependencies": {
        "function-bind": "^1.1.1"
      },
      "engines": {
        "node": ">= 0.4.0"
      }
    },
    "node_modules/has-flag": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/has-flag/-/has-flag-3.0.0.tgz",
      "integrity": "sha512-sKJf1+ceQBr4SMkvQnBDNDtf4TXpVhVGateu0t918bl30FnbE2m4vNLX+VWe/dpjlb+HugGYzW7uQXH98HPEYw==",
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/has-symbols": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has-symbols/-/has-symbols-1.0.3.tgz",
      "integrity": "sha512-l3LCuF6MgDNwTDKkdYGEihYjt5pRPbEg46rtlmnSPlUbgmB8LOIrKJbYYFBSbnPaJexMKtiPO8hmeRjRz2Td+A==",
      "engines": {
        "node": ">= 0.4"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/hpagent": {
      "version": "0.1.2",
      "resolved": "https://registry.npmjs.org/hpagent/-/hpagent-0.1.2.tgz",
      "integrity": "sha512-ePqFXHtSQWAFXYmj+JtOTHr84iNrII4/QRlAAPPE+zqnKy4xJo7Ie1Y4kC7AdB+LxLxSTTzBMASsEcy0q8YyvQ==",
      "optional": true
    },
    "node_modules/http-cache-semantics": {
      "version": "4.1.1",
      "resolved": "https://registry.npmjs.org/http-cache-semantics/-/http-cache-semantics-4.1.1.tgz",
      "integrity": "sha512-er295DKPVsV82j5kw1Gjt+ADA/XYHsajl82cGNQG2eyoPkvgUhX+nDIyelzhIWbbsXP39EHcI6l5tYs2FYqYXQ=="
    },
    "node_modules/http-errors": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/http-errors/-/http-errors-2.0.0.tgz",
      "integrity": "sha512-FtwrG/euBzaEjYeRqOgly7G0qviiXoJWnvEH2Z1plBdXgbyjv34pHTSb9zoeHMyDy33+DWy5Wt9Wo+TURtOYSQ==",
      "dependencies": {
        "depd": "2.0.0",
        "inherits": "2.0.4",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "toidentifier": "1.0.1"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/http2-wrapper": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/http2-wrapper/-/http2-wrapper-1.0.3.tgz",
      "integrity": "sha512-V+23sDMr12Wnz7iTcDeJr3O6AIxlnvT/bmaAAAP/Xda35C90p9599p0F1eHR/N1KILWSoWVAiOMFjBBXaXSMxg==",
      "dependencies": {
        "quick-lru": "^5.1.1",
        "resolve-alpn": "^1.0.0"
      },
      "engines": {
        "node": ">=10.19.0"
      }
    },
    "node_modules/iconv-lite": {
      "version": "0.4.24",
      "resolved": "https://registry.npmjs.org/iconv-lite/-/iconv-lite-0.4.24.tgz",
      "integrity": "sha512-v3MXnZAcvnywkTUEZomIActle7RXXeedOR31wwl7VlyoXO4Qi9arvSenNQWne1TcRwhCL1HwLI21bEqdpj8/rA==",
      "dependencies": {
        "safer-buffer": ">= 2.1.2 < 3"
      },
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/ignore-by-default": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/ignore-by-default/-/ignore-by-default-1.0.1.tgz",
      "integrity": "sha512-Ius2VYcGNk7T90CppJqcIkS5ooHUZyIQK+ClZfMfMNFEF9VSE73Fq+906u/CWu92x4gzZMWOwfFYckPObzdEbA=="
    },
    "node_modules/inherits": {
      "version": "2.0.4",
      "resolved": "https://registry.npmjs.org/inherits/-/inherits-2.0.4.tgz",
      "integrity": "sha512-k/vGaX4/Yla3WzyMCvTQOXYeIHvqOKtnqBduzTHpzpQZzAskKMhZ2K+EnBiSM9zGSoIFeMpXKxa4dYeZIQqewQ=="
    },
    "node_modules/ipaddr.js": {
      "version": "1.9.1",
      "resolved": "https://registry.npmjs.org/ipaddr.js/-/ipaddr.js-1.9.1.tgz",
      "integrity": "sha512-0KI/607xoxSToH7GjN1FfSbLoU0+btTicjsQSWQlh/hZykN8KpmMf7uYwPW3R+akZ6R/w18ZlXSHBYXiYUPO3g==",
      "engines": {
        "node": ">= 0.10"
      }
    },
    "node_modules/is-binary-path": {
      "version": "2.1.0",
      "resolved": "https://registry.npmjs.org/is-binary-path/-/is-binary-path-2.1.0.tgz",
      "integrity": "sha512-ZMERYes6pDydyuGidse7OsHxtbI7WVeUEozgR/g7rd0xUimYNlvZRE/K2MgZTjWy725IfelLeVcEM97mmtRGXw==",
      "dependencies": {
        "binary-extensions": "^2.0.0"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/is-extglob": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/is-extglob/-/is-extglob-2.1.1.tgz",
      "integrity": "sha512-SbKbANkN603Vi4jEZv49LeVJMn4yGwsbzZworEoyEiutsN3nJYdbO36zfhGJ6QEDpOZIFkDtnq5JRxmvl3jsoQ==",
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/is-glob": {
      "version": "4.0.3",
      "resolved": "https://registry.npmjs.org/is-glob/-/is-glob-4.0.3.tgz",
      "integrity": "sha512-xelSayHH36ZgE7ZWhli7pW34hNbNl8Ojv5KVmkJD4hBdD3th8Tfk9vYasLM+mXWOZhFkgZfxhLSnrwRr4elSSg==",
      "dependencies": {
        "is-extglob": "^2.1.1"
      },
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/is-number": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
      "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==",
      "engines": {
        "node": ">=0.12.0"
      }
    },
    "node_modules/is-promise": {
      "version": "2.2.2",
      "resolved": "https://registry.npmjs.org/is-promise/-/is-promise-2.2.2.tgz",
      "integrity": "sha512-+lP4/6lKUBfQjZ2pdxThZvLUAafmZb8OAxFb8XXtiQmS35INgr85hdOGoEs124ez1FCnZJt6jau/T+alh58QFQ=="
    },
    "node_modules/json-buffer": {
      "version": "3.0.1",
      "resolved": "https://registry.npmjs.org/json-buffer/-/json-buffer-3.0.1.tgz",
      "integrity": "sha512-4bV5BfR2mqfQTJm+V5tPPdf+ZpuhiIvTuAB5g8kcrXOZpTT/QwwVRWBywX1ozr6lEuPdbHxwaJlm9G6mI2sfSQ=="
    },
    "node_modules/keyv": {
      "version": "4.5.2",
      "resolved": "https://registry.npmjs.org/keyv/-/keyv-4.5.2.tgz",
      "integrity": "sha512-5MHbFaKn8cNSmVW7BYnijeAVlE4cYA/SVkifVgrh7yotnfhKmjuXpDKjrABLnT0SfHWV21P8ow07OGfRrNDg8g==",
      "dependencies": {
        "json-buffer": "3.0.1"
      }
    },
    "node_modules/lodash.clonedeep": {
      "version": "4.5.0",
      "resolved": "https://registry.npmjs.org/lodash.clonedeep/-/lodash.clonedeep-4.5.0.tgz",
      "integrity": "sha512-H5ZhCF25riFd9uB5UCkVKo61m3S/xZk1x4wA6yp/L3RFP6Z/eHH1ymQcGLo7J3GMPfm0V/7m1tryHuGVxpqEBQ=="
    },
    "node_modules/lowercase-keys": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/lowercase-keys/-/lowercase-keys-2.0.0.tgz",
      "integrity": "sha512-tqNXrS78oMOE73NMxK4EMLQsQowWf8jKooH9g7xPavRT706R6bkQJ6DY2Te7QukaZsulxa30wQ7bk0pm4XiHmA==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/lru-queue": {
      "version": "0.1.0",
      "resolved": "https://registry.npmjs.org/lru-queue/-/lru-queue-0.1.0.tgz",
      "integrity": "sha512-BpdYkt9EvGl8OfWHDQPISVpcl5xZthb+XPsbELj5AQXxIC8IriDZIQYjBJPEm5rS420sjZ0TLEzRcq5KdBhYrQ==",
      "dependencies": {
        "es5-ext": "~0.10.2"
      }
    },
    "node_modules/media-typer": {
      "version": "0.3.0",
      "resolved": "https://registry.npmjs.org/media-typer/-/media-typer-0.3.0.tgz",
      "integrity": "sha512-dq+qelQ9akHpcOl/gUVRTxVIOkAJ1wR3QAvb4RsVjS8oVoFjDGTc679wJYmUmknUF5HwMLOgb5O+a3KxfWapPQ==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/memoizee": {
      "version": "0.4.15",
      "resolved": "https://registry.npmjs.org/memoizee/-/memoizee-0.4.15.tgz",
      "integrity": "sha512-UBWmJpLZd5STPm7PMUlOw/TSy972M+z8gcyQ5veOnSDRREz/0bmpyTfKt3/51DhEBqCZQn1udM/5flcSPYhkdQ==",
      "dependencies": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.53",
        "es6-weak-map": "^2.0.3",
        "event-emitter": "^0.3.5",
        "is-promise": "^2.2.2",
        "lru-queue": "^0.1.0",
        "next-tick": "^1.1.0",
        "timers-ext": "^0.1.7"
      }
    },
    "node_modules/merge-descriptors": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/merge-descriptors/-/merge-descriptors-1.0.1.tgz",
      "integrity": "sha512-cCi6g3/Zr1iqQi6ySbseM1Xvooa98N0w31jzUYrXPX2xqObmFGHJ0tQ5u74H3mVh7wLouTseZyYIq39g8cNp1w=="
    },
    "node_modules/methods": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/methods/-/methods-1.1.2.tgz",
      "integrity": "sha512-iclAHeNqNm68zFtnZ0e+1L2yUIdvzNoauKU4WBA3VvH/vPFieF7qfRlwUZU+DA9P9bPXIS90ulxoUoCH23sV2w==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mime": {
      "version": "1.6.0",
      "resolved": "https://registry.npmjs.org/mime/-/mime-1.6.0.tgz",
      "integrity": "sha512-x0Vn8spI+wuJ1O6S7gnbaQg8Pxh4NNHb7KSINmEWKiPE4RKOplvijn+NkmYmmRgP68mc70j2EbeTFRsrswaQeg==",
      "bin": {
        "mime": "cli.js"
      },
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/mime-db": {
      "version": "1.52.0",
      "resolved": "https://registry.npmjs.org/mime-db/-/mime-db-1.52.0.tgz",
      "integrity": "sha512-sPU4uV7dYlvtWJxwwxHD0PuihVNiE7TyAbQ5SWxDCB9mUYvOgroQOwYQQOKPJ8CIbE+1ETVlOoK1UC2nU3gYvg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mime-types": {
      "version": "2.1.35",
      "resolved": "https://registry.npmjs.org/mime-types/-/mime-types-2.1.35.tgz",
      "integrity": "sha512-ZDY+bPm5zTTF+YpCrAU9nK0UgICYPT0QtT1NZWFv4s++TNkcgVaT0g6+4R2uI4MjQjzysHB1zxuWL50hzaeXiw==",
      "dependencies": {
        "mime-db": "1.52.0"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mimic-response": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-1.0.1.tgz",
      "integrity": "sha512-j5EctnkH7amfV/q5Hgmoal1g2QHFJRraOtmx0JpIqkxhBhI/lJSl1nMpQ45hVarwNETOoWEimndZ4QK0RHxuxQ==",
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/minimatch": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/minimatch/-/minimatch-3.1.2.tgz",
      "integrity": "sha512-J7p63hRiAjw1NDEww1W7i37+ByIrOWO5XQQAzZ3VOcL0PNybwpfmV/N05zFAzwQ9USyEcX6t3UO+K5aqBQOIHw==",
      "dependencies": {
        "brace-expansion": "^1.1.7"
      },
      "engines": {
        "node": "*"
      }
    },
    "node_modules/ms": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.0.0.tgz",
      "integrity": "sha512-Tpp60P6IUJDTuOq/5Z8cdskzJujfwqfOTkrwIwj7IRISpnkJnT6SyJ4PCPnGMoFjC9ddhal5KVIYtAt97ix05A=="
    },
    "node_modules/negotiator": {
      "version": "0.6.3",
      "resolved": "https://registry.npmjs.org/negotiator/-/negotiator-0.6.3.tgz",
      "integrity": "sha512-+EUsqGPLsM+j/zdChZjsnX51g4XrHFOIXwfnCVPGlQk/k5giakcKsuxCObBRu6DSm9opw/O6slWbJdghQM4bBg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/next-tick": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/next-tick/-/next-tick-1.1.0.tgz",
      "integrity": "sha512-CXdUiJembsNjuToQvxayPZF9Vqht7hewsvy2sOWafLvi2awflj9mOC6bHIg50orX8IJvWKY9wYQ/zB2kogPslQ=="
    },
    "node_modules/ngrok": {
      "version": "4.3.3",
      "resolved": "https://registry.npmjs.org/ngrok/-/ngrok-4.3.3.tgz",
      "integrity": "sha512-a2KApnkiG5urRxBPdDf76nNBQTnNNWXU0nXw0SsqsPI+Kmt2lGf9TdVYpYrHMnC+T9KhcNSWjCpWqBgC6QcFvw==",
      "hasInstallScript": true,
      "dependencies": {
        "@types/node": "^8.10.50",
        "extract-zip": "^2.0.1",
        "got": "^11.8.5",
        "lodash.clonedeep": "^4.5.0",
        "uuid": "^7.0.0 || ^8.0.0",
        "yaml": "^1.10.0"
      },
      "bin": {
        "ngrok": "bin/ngrok"
      },
      "engines": {
        "node": ">=10.19.0 <14 || >=14.2"
      },
      "optionalDependencies": {
        "hpagent": "^0.1.2"
      }
    },
    "node_modules/nodemon": {
      "version": "2.0.20",
      "resolved": "https://registry.npmjs.org/nodemon/-/nodemon-2.0.20.tgz",
      "integrity": "sha512-Km2mWHKKY5GzRg6i1j5OxOHQtuvVsgskLfigG25yTtbyfRGn/GNvIbRyOf1PSCKJ2aT/58TiuUsuOU5UToVViw==",
      "dependencies": {
        "chokidar": "^3.5.2",
        "debug": "^3.2.7",
        "ignore-by-default": "^1.0.1",
        "minimatch": "^3.1.2",
        "pstree.remy": "^1.1.8",
        "semver": "^5.7.1",
        "simple-update-notifier": "^1.0.7",
        "supports-color": "^5.5.0",
        "touch": "^3.1.0",
        "undefsafe": "^2.0.5"
      },
      "bin": {
        "nodemon": "bin/nodemon.js"
      },
      "engines": {
        "node": ">=8.10.0"
      },
      "funding": {
        "type": "opencollective",
        "url": "https://opencollective.com/nodemon"
      }
    },
    "node_modules/nodemon/node_modules/debug": {
      "version": "3.2.7",
      "resolved": "https://registry.npmjs.org/debug/-/debug-3.2.7.tgz",
      "integrity": "sha512-CFjzYYAi4ThfiQvizrFQevTTXHtnCqWfe7x1AhgEscTz6ZbLbfoLRLPugTQyBth6f8ZERVUSyWHFD/7Wu4t1XQ==",
      "dependencies": {
        "ms": "^2.1.1"
      }
    },
    "node_modules/nodemon/node_modules/ms": {
      "version": "2.1.3",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
      "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
    },
    "node_modules/nopt": {
      "version": "1.0.10",
      "resolved": "https://registry.npmjs.org/nopt/-/nopt-1.0.10.tgz",
      "integrity": "sha512-NWmpvLSqUrgrAC9HCuxEvb+PSloHpqVu+FqcO4eeF2h5qYRhA7ev6KvelyQAKtegUbC6RypJnlEOhd8vloNKYg==",
      "dependencies": {
        "abbrev": "1"
      },
      "bin": {
        "nopt": "bin/nopt.js"
      },
      "engines": {
        "node": "*"
      }
    },
    "node_modules/normalize-path": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/normalize-path/-/normalize-path-3.0.0.tgz",
      "integrity": "sha512-6eZs5Ls3WtCisHWp9S2GUy8dqkpGi4BVSz3GaqiE6ezub0512ESztXUwUB6C6IKbQkY2Pnb/mD4WYojCRwcwLA==",
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/normalize-url": {
      "version": "6.1.0",
      "resolved": "https://registry.npmjs.org/normalize-url/-/normalize-url-6.1.0.tgz",
      "integrity": "sha512-DlL+XwOy3NxAQ8xuC0okPgK46iuVNAK01YN7RueYBqqFeGsBjV9XmCAzAdgt+667bCl5kPh9EqKKDwnaPG1I7A==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/object-inspect": {
      "version": "1.12.3",
      "resolved": "https://registry.npmjs.org/object-inspect/-/object-inspect-1.12.3.tgz",
      "integrity": "sha512-geUvdk7c+eizMNUDkRpW1wJwgfOiOeHbxBR/hLXK1aT6zmVSO0jsQcs7fj6MGw89jC/cjGfLcNOrtMYtGqm81g==",
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/on-finished": {
      "version": "2.4.1",
      "resolved": "https://registry.npmjs.org/on-finished/-/on-finished-2.4.1.tgz",
      "integrity": "sha512-oVlzkg3ENAhCk2zdv7IJwd/QUD4z2RxRwpkcGY8psCVcCYZNq4wYnVWALHM+brtuJjePWiYF/ClmuDr8Ch5+kg==",
      "dependencies": {
        "ee-first": "1.1.1"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/once": {
      "version": "1.4.0",
      "resolved": "https://registry.npmjs.org/once/-/once-1.4.0.tgz",
      "integrity": "sha512-lNaJgI+2Q5URQBkccEKHTQOPaXdUxnZZElQTZY0MFUAuaEqe1E+Nyvgdz/aIyNi6Z9MzO5dv1H8n58/GELp3+w==",
      "dependencies": {
        "wrappy": "1"
      }
    },
    "node_modules/p-cancelable": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/p-cancelable/-/p-cancelable-2.1.1.tgz",
      "integrity": "sha512-BZOr3nRQHOntUjTrH8+Lh54smKHoHyur8We1V8DSMVrl5A2malOOwuJRnKRDjSnkoeBh4at6BwEnb5I7Jl31wg==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/parseurl": {
      "version": "1.3.3",
      "resolved": "https://registry.npmjs.org/parseurl/-/parseurl-1.3.3.tgz",
      "integrity": "sha512-CiyeOxFT/JZyN5m0z9PfXw4SCBJ6Sygz1Dpl0wqjlhDEGGBP1GnsUVEL0p63hoG1fcj3fHynXi9NYO4nWOL+qQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/path-to-regexp": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/path-to-regexp/-/path-to-regexp-0.1.7.tgz",
      "integrity": "sha512-5DFkuoqlv1uYQKxy8omFBeJPQcdoE07Kv2sferDCrAq1ohOU+MSDswDIbnx3YAM60qIOnYa53wBhXW0EbMonrQ=="
    },
    "node_modules/pend": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/pend/-/pend-1.2.0.tgz",
      "integrity": "sha512-F3asv42UuXchdzt+xXqfW1OGlVBe+mxa2mqI0pg5yAHZPvFmY3Y6drSf/GQ1A86WgWEN9Kzh/WrgKa6iGcHXLg=="
    },
    "node_modules/picomatch": {
      "version": "2.3.1",
      "resolved": "https://registry.npmjs.org/picomatch/-/picomatch-2.3.1.tgz",
      "integrity": "sha512-JU3teHTNjmE2VCGFzuY8EXzCDVwEqB2a8fsIvwaStHhAWJEeVd1o1QD80CU6+ZdEXXSLbSsuLwJjkCBWqRQUVA==",
      "engines": {
        "node": ">=8.6"
      },
      "funding": {
        "url": "https://github.com/sponsors/jonschlinkert"
      }
    },
    "node_modules/prettier": {
      "version": "3.0.3",
      "resolved": "https://registry.npmjs.org/prettier/-/prettier-3.0.3.tgz",
      "integrity": "sha512-L/4pUDMxcNa8R/EthV08Zt42WBO4h1rarVtK0K+QJG0X187OLo7l699jWw0GKuwzkPQ//jMFA/8Xm6Fh3J/DAg==",
      "dev": true,
      "bin": {
        "prettier": "bin/prettier.cjs"
      },
      "engines": {
        "node": ">=14"
      },
      "funding": {
        "url": "https://github.com/prettier/prettier?sponsor=1"
      }
    },
    "node_modules/proxy-addr": {
      "version": "2.0.7",
      "resolved": "https://registry.npmjs.org/proxy-addr/-/proxy-addr-2.0.7.tgz",
      "integrity": "sha512-llQsMLSUDUPT44jdrU/O37qlnifitDP+ZwrmmZcoSKyLKvtZxpyV0n2/bD/N4tBAAZ/gJEdZU7KMraoK1+XYAg==",
      "dependencies": {
        "forwarded": "0.2.0",
        "ipaddr.js": "1.9.1"
      },
      "engines": {
        "node": ">= 0.10"
      }
    },
    "node_modules/pstree.remy": {
      "version": "1.1.8",
      "resolved": "https://registry.npmjs.org/pstree.remy/-/pstree.remy-1.1.8.tgz",
      "integrity": "sha512-77DZwxQmxKnu3aR542U+X8FypNzbfJ+C5XQDk3uWjWxn6151aIMGthWYRXTqT1E5oJvg+ljaa2OJi+VfvCOQ8w=="
    },
    "node_modules/pump": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/pump/-/pump-3.0.0.tgz",
      "integrity": "sha512-LwZy+p3SFs1Pytd/jYct4wpv49HiYCqd9Rlc5ZVdk0V+8Yzv6jR5Blk3TRmPL1ft69TxP0IMZGJ+WPFU2BFhww==",
      "dependencies": {
        "end-of-stream": "^1.1.0",
        "once": "^1.3.1"
      }
    },
    "node_modules/qs": {
      "version": "6.11.0",
      "resolved": "https://registry.npmjs.org/qs/-/qs-6.11.0.tgz",
      "integrity": "sha512-MvjoMCJwEarSbUYk5O+nmoSzSutSsTwF85zcHPQ9OrlFoZOYIjaqBAJIqIXjptyD5vThxGq52Xu/MaJzRkIk4Q==",
      "dependencies": {
        "side-channel": "^1.0.4"
      },
      "engines": {
        "node": ">=0.6"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/quick-lru": {
      "version": "5.1.1",
      "resolved": "https://registry.npmjs.org/quick-lru/-/quick-lru-5.1.1.tgz",
      "integrity": "sha512-WuyALRjWPDGtt/wzJiadO5AXY+8hZ80hVpe6MyivgraREW751X3SbhRvG3eLKOYN+8VEvqLcf3wdnt44Z4S4SA==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/range-parser": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/range-parser/-/range-parser-1.2.1.tgz",
      "integrity": "sha512-Hrgsx+orqoygnmhFbKaHE6c296J+HTAQXoxEF6gNupROmmGJRoyzfG3ccAveqCBrwr/2yxQ5BVd/GTl5agOwSg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/raw-body": {
      "version": "2.5.2",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.2.tgz",
      "integrity": "sha512-8zGqypfENjCIqGhgXToC8aB2r7YrBX+AQAfIPs/Mlk+BtPTztOvTS01NRW/3Eh60J+a48lt8qsCzirQ6loCVfA==",
      "dependencies": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/readdirp": {
      "version": "3.6.0",
      "resolved": "https://registry.npmjs.org/readdirp/-/readdirp-3.6.0.tgz",
      "integrity": "sha512-hOS089on8RduqdbhvQ5Z37A0ESjsqz6qnRcffsMU3495FuTdqSm+7bhJ29JvIOsBDEEnan5DPu9t3To9VRlMzA==",
      "dependencies": {
        "picomatch": "^2.2.1"
      },
      "engines": {
        "node": ">=8.10.0"
      }
    },
    "node_modules/resolve-alpn": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/resolve-alpn/-/resolve-alpn-1.2.1.tgz",
      "integrity": "sha512-0a1F4l73/ZFZOakJnQ3FvkJ2+gSTQWz/r2KE5OdDY0TxPm5h4GkqkWWfM47T7HsbnOtcJVEF4epCVy6u7Q3K+g=="
    },
    "node_modules/responselike": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/responselike/-/responselike-2.0.1.tgz",
      "integrity": "sha512-4gl03wn3hj1HP3yzgdI7d3lCkF95F21Pz4BPGvKHinyQzALR5CapwC8yIi0Rh58DEMQ/SguC03wFj2k0M/mHhw==",
      "dependencies": {
        "lowercase-keys": "^2.0.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/safe-buffer": {
      "version": "5.2.1",
      "resolved": "https://registry.npmjs.org/safe-buffer/-/safe-buffer-5.2.1.tgz",
      "integrity": "sha512-rp3So07KcdmmKbGvgaNxQSJr7bGVSVk5S9Eq1F+ppbRo70+YeaDxkw5Dd8NPN+GD6bjnYm2VuPuCXmpuYvmCXQ==",
      "funding": [
        {
          "type": "github",
          "url": "https://github.com/sponsors/feross"
        },
        {
          "type": "patreon",
          "url": "https://www.patreon.com/feross"
        },
        {
          "type": "consulting",
          "url": "https://feross.org/support"
        }
      ]
    },
    "node_modules/safer-buffer": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/safer-buffer/-/safer-buffer-2.1.2.tgz",
      "integrity": "sha512-YZo3K82SD7Riyi0E1EQPojLz7kpepnSQI9IyPbHHg1XXXevb5dJI7tpyN2ADxGcQbHG7vcyRHk0cbwqcQriUtg=="
    },
    "node_modules/semver": {
      "version": "5.7.1",
      "resolved": "https://registry.npmjs.org/semver/-/semver-5.7.1.tgz",
      "integrity": "sha512-sauaDf/PZdVgrLTNYHRtpXa1iRiKcaebiKQ1BJdpQlWH2lCvexQdX55snPFyK7QzpudqbCI0qXFfOasHdyNDGQ==",
      "bin": {
        "semver": "bin/semver"
      }
    },
    "node_modules/send": {
      "version": "0.18.0",
      "resolved": "https://registry.npmjs.org/send/-/send-0.18.0.tgz",
      "integrity": "sha512-qqWzuOjSFOuqPjFe4NOsMLafToQQwBSOEpS+FwEt3A2V3vKubTquT3vmLTQpFgMXp8AlFWFuP1qKaJZOtPpVXg==",
      "dependencies": {
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "mime": "1.6.0",
        "ms": "2.1.3",
        "on-finished": "2.4.1",
        "range-parser": "~1.2.1",
        "statuses": "2.0.1"
      },
      "engines": {
        "node": ">= 0.8.0"
      }
    },
    "node_modules/send/node_modules/ms": {
      "version": "2.1.3",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
      "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
    },
    "node_modules/serve-static": {
      "version": "1.15.0",
      "resolved": "https://registry.npmjs.org/serve-static/-/serve-static-1.15.0.tgz",
      "integrity": "sha512-XGuRDNjXUijsUL0vl6nSD7cwURuzEgglbOaFuZM9g3kwDXOWVTck0jLzjPzGD+TazWbboZYu52/9/XPdUgne9g==",
      "dependencies": {
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "parseurl": "~1.3.3",
        "send": "0.18.0"
      },
      "engines": {
        "node": ">= 0.8.0"
      }
    },
    "node_modules/setprototypeof": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/setprototypeof/-/setprototypeof-1.2.0.tgz",
      "integrity": "sha512-E5LDX7Wrp85Kil5bhZv46j8jOeboKq5JMmYM3gVGdGH8xFpPWXUMsNrlODCrkoxMEeNi/XZIwuRvY4XNwYMJpw=="
    },
    "node_modules/side-channel": {
      "version": "1.0.4",
      "resolved": "https://registry.npmjs.org/side-channel/-/side-channel-1.0.4.tgz",
      "integrity": "sha512-q5XPytqFEIKHkGdiMIrY10mvLRvnQh42/+GoBlFW3b2LXLE2xxJpZFdm94we0BaoV3RwJyGqg5wS7epxTv0Zvw==",
      "dependencies": {
        "call-bind": "^1.0.0",
        "get-intrinsic": "^1.0.2",
        "object-inspect": "^1.9.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/simple-update-notifier": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/simple-update-notifier/-/simple-update-notifier-1.1.0.tgz",
      "integrity": "sha512-VpsrsJSUcJEseSbMHkrsrAVSdvVS5I96Qo1QAQ4FxQ9wXFcB+pjj7FB7/us9+GcgfW4ziHtYMc1J0PLczb55mg==",
      "dependencies": {
        "semver": "~7.0.0"
      },
      "engines": {
        "node": ">=8.10.0"
      }
    },
    "node_modules/simple-update-notifier/node_modules/semver": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/semver/-/semver-7.0.0.tgz",
      "integrity": "sha512-+GB6zVA9LWh6zovYQLALHwv5rb2PHGlJi3lfiqIHxR0uuwCgefcOJc59v9fv1w8GbStwxuuqqAjI9NMAOOgq1A==",
      "bin": {
        "semver": "bin/semver.js"
      }
    },
    "node_modules/statuses": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/statuses/-/statuses-2.0.1.tgz",
      "integrity": "sha512-RwNA9Z/7PrK06rYLIzFMlaF+l73iwpzsqRIFgbMLbTcLD6cOao82TaWefPXQvB2fOC4AjuYSEndS7N/mTCbkdQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/supports-color": {
      "version": "5.5.0",
      "resolved": "https://registry.npmjs.org/supports-color/-/supports-color-5.5.0.tgz",
      "integrity": "sha512-QjVjwdXIt408MIiAqCX4oUKsgU2EqAGzs2Ppkm4aQYbjm+ZEWEcW4SfFNTr4uMNZma0ey4f5lgLrkB0aX0QMow==",
      "dependencies": {
        "has-flag": "^3.0.0"
      },
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/timers-ext": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/timers-ext/-/timers-ext-0.1.7.tgz",
      "integrity": "sha512-b85NUNzTSdodShTIbky6ZF02e8STtVVfD+fu4aXXShEELpozH+bCpJLYMPZbsABN2wDH7fJpqIoXxJpzbf0NqQ==",
      "dependencies": {
        "es5-ext": "~0.10.46",
        "next-tick": "1"
      }
    },
    "node_modules/to-regex-range": {
      "version": "5.0.1",
      "resolved": "https://registry.npmjs.org/to-regex-range/-/to-regex-range-5.0.1.tgz",
      "integrity": "sha512-65P7iz6X5yEr1cwcgvQxbbIw7Uk3gOy5dIdtZ4rDveLqhrdJP+Li/Hx6tyK0NEb+2GCyneCMJiGqrADCSNk8sQ==",
      "dependencies": {
        "is-number": "^7.0.0"
      },
      "engines": {
        "node": ">=8.0"
      }
    },
    "node_modules/toidentifier": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/toidentifier/-/toidentifier-1.0.1.tgz",
      "integrity": "sha512-o5sSPKEkg/DIQNmH43V0/uerLrpzVedkUh8tGNvaeXpfpuwjKenlSox/2O/BTlZUtEe+JG7s5YhEz608PlAHRA==",
      "engines": {
        "node": ">=0.6"
      }
    },
    "node_modules/touch": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/touch/-/touch-3.1.0.tgz",
      "integrity": "sha512-WBx8Uy5TLtOSRtIq+M03/sKDrXCLHxwDcquSP2c43Le03/9serjQBIztjRz6FkJez9D/hleyAXTBGLwwZUw9lA==",
      "dependencies": {
        "nopt": "~1.0.10"
      },
      "bin": {
        "nodetouch": "bin/nodetouch.js"
      }
    },
    "node_modules/type": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/type/-/type-1.2.0.tgz",
      "integrity": "sha512-+5nt5AAniqsCnu2cEQQdpzCAh33kVx8n0VoFidKpB1dVVLAN/F+bgVOqOJqOnEnrhp222clB5p3vUlD+1QAnfg=="
    },
    "node_modules/type-is": {
      "version": "1.6.18",
      "resolved": "https://registry.npmjs.org/type-is/-/type-is-1.6.18.tgz",
      "integrity": "sha512-TkRKr9sUTxEH8MdfuCSP7VizJyzRNMjj2J2do2Jr3Kym598JVdEksuzPQCnlFPW4ky9Q+iA+ma9BGm06XQBy8g==",
      "dependencies": {
        "media-typer": "0.3.0",
        "mime-types": "~2.1.24"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/undefsafe": {
      "version": "2.0.5",
      "resolved": "https://registry.npmjs.org/undefsafe/-/undefsafe-2.0.5.tgz",
      "integrity": "sha512-WxONCrssBM8TSPRqN5EmsjVrsv4A8X12J4ArBiiayv3DyyG3ZlIg6yysuuSYdZsVz3TKcTg2fd//Ujd4CHV1iA=="
    },
    "node_modules/unpipe": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/unpipe/-/unpipe-1.0.0.tgz",
      "integrity": "sha512-pjy2bYhSsufwWlKwPc+l3cN7+wuJlK6uz0YdJEOlQDbl6jo/YlPi4mb8agUkVC8BF7V8NuzeyPNqRksA3hztKQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/utils-merge": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/utils-merge/-/utils-merge-1.0.1.tgz",
      "integrity": "sha512-pMZTvIkT1d+TFGvDOqodOclx0QWkkgi6Tdoa8gC8ffGAAqz9pzPTZWAybbsHHoED/ztMtkv/VoYTYyShUn81hA==",
      "engines": {
        "node": ">= 0.4.0"
      }
    },
    "node_modules/uuid": {
      "version": "8.3.2",
      "resolved": "https://registry.npmjs.org/uuid/-/uuid-8.3.2.tgz",
      "integrity": "sha512-+NYs2QeMWy+GWFOEm9xnn6HCDp0l7QBD7ml8zLUmJ+93Q5NF0NocErnwkTkXVFNiX3/fpC6afS8Dhb/gz7R7eg==",
      "bin": {
        "uuid": "dist/bin/uuid"
      }
    },
    "node_modules/vary": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/vary/-/vary-1.1.2.tgz",
      "integrity": "sha512-BNGbWLfd0eUPabhkXUVm0j8uuvREyTh5ovRa/dyow/BqAbZJyC+5fU+IzQOzmAKzYqYRAISoRhdQr3eIZ/PXqg==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/wrappy": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/wrappy/-/wrappy-1.0.2.tgz",
      "integrity": "sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ=="
    },
    "node_modules/yaml": {
      "version": "1.10.2",
      "resolved": "https://registry.npmjs.org/yaml/-/yaml-1.10.2.tgz",
      "integrity": "sha512-r3vXyErRCYJ7wg28yvBY5VSoAF8ZvlcW9/BwUzEtUsjvX/DKs24dIkuwjtuprwJJHsbyUbLApepYTR1BN4uHrg==",
      "engines": {
        "node": ">= 6"
      }
    },
    "node_modules/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-p4a9I6X6nu6IhoGmBqAcbJy1mlC4j27vEPZX9F4L4/vZT3Lyq1VkFHw/V/PUcB9Buo+DG3iHkT0x3Qya58zc3g==",
      "dependencies": {
        "buffer-crc32": "~0.2.3",
        "fd-slicer": "~1.1.0"
      }
    }
  },
  "dependencies": {
    "@sindresorhus/is": {
      "version": "4.6.0",
      "resolved": "https://registry.npmjs.org/@sindresorhus/is/-/is-4.6.0.tgz",
      "integrity": "sha512-t09vSN3MdfsyCHoFcTRCH/iUtG7OJ0CsjzB8cjAmKc/va/kIgeDI/TxsigdncE/4be734m0cvIYwNaV4i2XqAw=="
    },
    "@szmarczak/http-timer": {
      "version": "4.0.6",
      "resolved": "https://registry.npmjs.org/@szmarczak/http-timer/-/http-timer-4.0.6.tgz",
      "integrity": "sha512-4BAffykYOgO+5nzBWYwE3W90sBgLJoUPRWWcL8wlyiM8IB8ipJz3UMJ9KXQd1RKQXpKp8Tutn80HZtWsu2u76w==",
      "requires": {
        "defer-to-connect": "^2.0.0"
      }
    },
    "@types/cacheable-request": {
      "version": "6.0.3",
      "resolved": "https://registry.npmjs.org/@types/cacheable-request/-/cacheable-request-6.0.3.tgz",
      "integrity": "sha512-IQ3EbTzGxIigb1I3qPZc1rWJnH0BmSKv5QYTalEwweFvyBDLSAe24zP0le/hyi7ecGfZVlIVAg4BZqb8WBwKqw==",
      "requires": {
        "@types/http-cache-semantics": "*",
        "@types/keyv": "^3.1.4",
        "@types/node": "*",
        "@types/responselike": "^1.0.0"
      }
    },
    "@types/http-cache-semantics": {
      "version": "4.0.1",
      "resolved": "https://registry.npmjs.org/@types/http-cache-semantics/-/http-cache-semantics-4.0.1.tgz",
      "integrity": "sha512-SZs7ekbP8CN0txVG2xVRH6EgKmEm31BOxA07vkFaETzZz1xh+cbt8BcI0slpymvwhx5dlFnQG2rTlPVQn+iRPQ=="
    },
    "@types/keyv": {
      "version": "3.1.4",
      "resolved": "https://registry.npmjs.org/@types/keyv/-/keyv-3.1.4.tgz",
      "integrity": "sha512-BQ5aZNSCpj7D6K2ksrRCTmKRLEpnPvWDiLPfoGyhZ++8YtiK9d/3DBKPJgry359X/P1PfruyYwvnvwFjuEiEIg==",
      "requires": {
        "@types/node": "*"
      }
    },
    "@types/node": {
      "version": "8.10.66",
      "resolved": "https://registry.npmjs.org/@types/node/-/node-8.10.66.tgz",
      "integrity": "sha512-tktOkFUA4kXx2hhhrB8bIFb5TbwzS4uOhKEmwiD+NoiL0qtP2OQ9mFldbgD4dV1djrlBYP6eBuQZiWjuHUpqFw=="
    },
    "@types/responselike": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/@types/responselike/-/responselike-1.0.0.tgz",
      "integrity": "sha512-85Y2BjiufFzaMIlvJDvTTB8Fxl2xfLo4HgmHzVBz08w4wDePCTjYw66PdrolO0kzli3yam/YCgRufyo1DdQVTA==",
      "requires": {
        "@types/node": "*"
      }
    },
    "@types/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/@types/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-Cn6WYCm0tXv8p6k+A8PvbDG763EDpBoTzHdA+Q/MF6H3sapGjCm9NzoaJncJS9tUKSuCoDs9XHxYYsQDgxR6kw==",
      "optional": true,
      "requires": {
        "@types/node": "*"
      }
    },
    "abbrev": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/abbrev/-/abbrev-1.1.1.tgz",
      "integrity": "sha512-nne9/IiQ/hzIhY6pdDnbBtz7DjPTKrY00P/zvPSm5pOFkl6xuGrGnXn/VtTNNfNtAfZ9/1RtehkszU9qcTii0Q=="
    },
    "accepts": {
      "version": "1.3.8",
      "resolved": "https://registry.npmjs.org/accepts/-/accepts-1.3.8.tgz",
      "integrity": "sha512-PYAthTa2m2VKxuvSD3DPC/Gy+U+sOA1LAuT8mkmRuvw+NACSaeXEQ+NHcVF7rONl6qcaxV3Uuemwawk+7+SJLw==",
      "requires": {
        "mime-types": "~2.1.34",
        "negotiator": "0.6.3"
      }
    },
    "anymatch": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/anymatch/-/anymatch-3.1.3.tgz",
      "integrity": "sha512-KMReFUr0B4t+D+OBkjR3KYqvocp2XaSzO55UcB6mgQMd3KbcE+mWTyvVV7D/zsdEbNnV6acZUutkiHQXvTr1Rw==",
      "requires": {
        "normalize-path": "^3.0.0",
        "picomatch": "^2.0.4"
      }
    },
    "array-flatten": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/array-flatten/-/array-flatten-1.1.1.tgz",
      "integrity": "sha512-PCVAQswWemu6UdxsDFFX/+gVeYqKAod3D3UVm91jHwynguOwAvYPhx8nNlM++NqRcK6CxxpUafjmhIdKiHibqg=="
    },
    "async-exit-hook": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/async-exit-hook/-/async-exit-hook-2.0.1.tgz",
      "integrity": "sha512-NW2cX8m1Q7KPA7a5M2ULQeZ2wR5qI5PAbw5L0UOMxdioVk9PMZ0h1TmyZEkPYrCvYjDlFICusOu1dlEKAAeXBw=="
    },
    "balanced-match": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/balanced-match/-/balanced-match-1.0.2.tgz",
      "integrity": "sha512-3oSeUO0TMV67hN1AmbXsK4yaqU7tjiHlbxRDZOpH0KW9+CeX4bRAaX0Anxt0tx2MrpRpWwQaPwIlISEJhYU5Pw=="
    },
    "binary-extensions": {
      "version": "2.2.0",
      "resolved": "https://registry.npmjs.org/binary-extensions/-/binary-extensions-2.2.0.tgz",
      "integrity": "sha512-jDctJ/IVQbZoJykoeHbhXpOlNBqGNcwXJKJog42E5HDPUwQTSdjCHdihjj0DlnheQ7blbT6dHOafNAiS8ooQKA=="
    },
    "body-parser": {
      "version": "1.20.2",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.2.tgz",
      "integrity": "sha512-ml9pReCu3M61kGlqoTm2umSXTlRTuGTx0bfYj+uIUKKYycG5NtSbeetV3faSU6R7ajOPw0g/J1PvK4qNy7s5bA==",
      "requires": {
        "bytes": "3.1.2",
        "content-type": "~1.0.5",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.2",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      }
    },
    "brace-expansion": {
      "version": "1.1.11",
      "resolved": "https://registry.npmjs.org/brace-expansion/-/brace-expansion-1.1.11.tgz",
      "integrity": "sha512-iCuPHDFgrHX7H2vEI/5xpz07zSHB00TpugqhmYtVmMO6518mCuRMoOYFldEBl0g187ufozdaHgWKcYFb61qGiA==",
      "requires": {
        "balanced-match": "^1.0.0",
        "concat-map": "0.0.1"
      }
    },
    "braces": {
      "version": "3.0.2",
      "resolved": "https://registry.npmjs.org/braces/-/braces-3.0.2.tgz",
      "integrity": "sha512-b8um+L1RzM3WDSzvhm6gIz1yfTbBt6YTlcEKAvsmqCZZFw46z626lVj9j1yEPW33H5H+lBQpZMP1k8l+78Ha0A==",
      "requires": {
        "fill-range": "^7.0.1"
      }
    },
    "buffer-crc32": {
      "version": "0.2.13",
      "resolved": "https://registry.npmjs.org/buffer-crc32/-/buffer-crc32-0.2.13.tgz",
      "integrity": "sha512-VO9Ht/+p3SN7SKWqcrgEzjGbRSJYTx+Q1pTQC0wrWqHx0vpJraQ6GtHx8tvcg1rlK1byhU5gccxgOgj7B0TDkQ=="
    },
    "bytes": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/bytes/-/bytes-3.1.2.tgz",
      "integrity": "sha512-/Nf7TyzTx6S3yRJObOAV7956r8cr2+Oj8AC5dt8wSP3BQAoeX58NoHyCU8P8zGkNXStjTSi6fzO6F0pBdcYbEg=="
    },
    "cacheable-lookup": {
      "version": "5.0.4",
      "resolved": "https://registry.npmjs.org/cacheable-lookup/-/cacheable-lookup-5.0.4.tgz",
      "integrity": "sha512-2/kNscPhpcxrOigMZzbiWF7dz8ilhb/nIHU3EyZiXWXpeq/au8qJ8VhdftMkty3n7Gj6HIGalQG8oiBNB3AJgA=="
    },
    "cacheable-request": {
      "version": "7.0.2",
      "resolved": "https://registry.npmjs.org/cacheable-request/-/cacheable-request-7.0.2.tgz",
      "integrity": "sha512-pouW8/FmiPQbuGpkXQ9BAPv/Mo5xDGANgSNXzTzJ8DrKGuXOssM4wIQRjfanNRh3Yu5cfYPvcorqbhg2KIJtew==",
      "requires": {
        "clone-response": "^1.0.2",
        "get-stream": "^5.1.0",
        "http-cache-semantics": "^4.0.0",
        "keyv": "^4.0.0",
        "lowercase-keys": "^2.0.0",
        "normalize-url": "^6.0.1",
        "responselike": "^2.0.0"
      }
    },
    "call-bind": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/call-bind/-/call-bind-1.0.2.tgz",
      "integrity": "sha512-7O+FbCihrB5WGbFYesctwmTKae6rOiIzmz1icreWJ+0aA7LJfuqhEso2T9ncpcFtzMQtzXf2QGGueWJGTYsqrA==",
      "requires": {
        "function-bind": "^1.1.1",
        "get-intrinsic": "^1.0.2"
      }
    },
    "chokidar": {
      "version": "3.5.3",
      "resolved": "https://registry.npmjs.org/chokidar/-/chokidar-3.5.3.tgz",
      "integrity": "sha512-Dr3sfKRP6oTcjf2JmUmFJfeVMvXBdegxB0iVQ5eb2V10uFJUCAS8OByZdVAyVb8xXNz3GjjTgj9kLWsZTqE6kw==",
      "requires": {
        "anymatch": "~3.1.2",
        "braces": "~3.0.2",
        "fsevents": "~2.3.2",
        "glob-parent": "~5.1.2",
        "is-binary-path": "~2.1.0",
        "is-glob": "~4.0.1",
        "normalize-path": "~3.0.0",
        "readdirp": "~3.6.0"
      }
    },
    "cli-color": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/cli-color/-/cli-color-2.0.3.tgz",
      "integrity": "sha512-OkoZnxyC4ERN3zLzZaY9Emb7f/MhBOIpePv0Ycok0fJYT+Ouo00UBEIwsVsr0yoow++n5YWlSUgST9GKhNHiRQ==",
      "requires": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.61",
        "es6-iterator": "^2.0.3",
        "memoizee": "^0.4.15",
        "timers-ext": "^0.1.7"
      }
    },
    "clone-response": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/clone-response/-/clone-response-1.0.3.tgz",
      "integrity": "sha512-ROoL94jJH2dUVML2Y/5PEDNaSHgeOdSDicUyS7izcF63G6sTc/FTjLub4b8Il9S8S0beOfYt0TaA5qvFK+w0wA==",
      "requires": {
        "mimic-response": "^1.0.0"
      }
    },
    "concat-map": {
      "version": "0.0.1",
      "resolved": "https://registry.npmjs.org/concat-map/-/concat-map-0.0.1.tgz",
      "integrity": "sha512-/Srv4dswyQNBfohGpz9o6Yb3Gz3SrUDqBH5rTuhGR7ahtlbYKnVxw2bCFMRljaA7EXHaXZ8wsHdodFvbkhKmqg=="
    },
    "content-disposition": {
      "version": "0.5.4",
      "resolved": "https://registry.npmjs.org/content-disposition/-/content-disposition-0.5.4.tgz",
      "integrity": "sha512-FveZTNuGw04cxlAiWbzi6zTAL/lhehaWbTtgluJh4/E95DqMwTmha3KZN1aAWA8cFIhHzMZUvLevkw5Rqk+tSQ==",
      "requires": {
        "safe-buffer": "5.2.1"
      }
    },
    "content-type": {
      "version": "1.0.5",
      "resolved": "https://registry.npmjs.org/content-type/-/content-type-1.0.5.tgz",
      "integrity": "sha512-nTjqfcBFEipKdXCv4YDQWCfmcLZKm81ldF0pAopTvyrFGVbcR6P/VAAd5G7N+0tTr8QqiU0tFadD6FK4NtJwOA=="
    },
    "cookie": {
      "version": "0.5.0",
      "resolved": "https://registry.npmjs.org/cookie/-/cookie-0.5.0.tgz",
      "integrity": "sha512-YZ3GUyn/o8gfKJlnlX7g7xq4gyO6OSuhGPKaaGssGB2qgDUS0gPgtTvoyZLTt9Ab6dC4hfc9dV5arkvc/OCmrw=="
    },
    "cookie-signature": {
      "version": "1.0.6",
      "resolved": "https://registry.npmjs.org/cookie-signature/-/cookie-signature-1.0.6.tgz",
      "integrity": "sha512-QADzlaHc8icV8I7vbaJXJwod9HWYp8uCqf1xa4OfNu1T7JVxQIrUgOWtHdNDtPiywmFbiS12VjotIXLrKM3orQ=="
    },
    "d": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/d/-/d-1.0.1.tgz",
      "integrity": "sha512-m62ShEObQ39CfralilEQRjH6oAMtNCV1xJyEx5LpRYUVN+EviphDgUc/F3hnYbADmkiNs67Y+3ylmlG7Lnu+FA==",
      "requires": {
        "es5-ext": "^0.10.50",
        "type": "^1.0.1"
      }
    },
    "debug": {
      "version": "2.6.9",
      "resolved": "https://registry.npmjs.org/debug/-/debug-2.6.9.tgz",
      "integrity": "sha512-bC7ElrdJaJnPbAP+1EotYvqZsb3ecl5wi6Bfi6BJTUcNowp6cvspg0jXznRTKDjm/E7AdgFBVeAPVMNcKGsHMA==",
      "requires": {
        "ms": "2.0.0"
      }
    },
    "decompress-response": {
      "version": "6.0.0",
      "resolved": "https://registry.npmjs.org/decompress-response/-/decompress-response-6.0.0.tgz",
      "integrity": "sha512-aW35yZM6Bb/4oJlZncMH2LCoZtJXTRxES17vE3hoRiowU2kWHaJKFkSBDnDR+cm9J+9QhXmREyIfv0pji9ejCQ==",
      "requires": {
        "mimic-response": "^3.1.0"
      },
      "dependencies": {
        "mimic-response": {
          "version": "3.1.0",
          "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-3.1.0.tgz",
          "integrity": "sha512-z0yWI+4FDrrweS8Zmt4Ej5HdJmky15+L2e6Wgn3+iK5fWzb6T3fhNFq2+MeTRb064c6Wr4N/wv0DzQTjNzHNGQ=="
        }
      }
    },
    "defer-to-connect": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/defer-to-connect/-/defer-to-connect-2.0.1.tgz",
      "integrity": "sha512-4tvttepXG1VaYGrRibk5EwJd1t4udunSOVMdLSAL6mId1ix438oPwPZMALY41FCijukO1L0twNcGsdzS7dHgDg=="
    },
    "depd": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/depd/-/depd-2.0.0.tgz",
      "integrity": "sha512-g7nH6P6dyDioJogAAGprGpCtVImJhpPk/roCzdb3fIh61/s/nPsfR6onyMwkCAR/OlC3yBC0lESvUoQEAssIrw=="
    },
    "destroy": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/destroy/-/destroy-1.2.0.tgz",
      "integrity": "sha512-2sJGJTaXIIaR1w4iJSNoN0hnMY7Gpc/n8D4qSCJw8QqFWXf7cuAgnEHxBpweaVcPevC2l3KpjYCx3NypQQgaJg=="
    },
    "ee-first": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/ee-first/-/ee-first-1.1.1.tgz",
      "integrity": "sha512-WMwm9LhRUo+WUaRN+vRuETqG89IgZphVSNkdFgeb6sS/E4OrDIN7t48CAewSHXc6C8lefD8KKfr5vY61brQlow=="
    },
    "encodeurl": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/encodeurl/-/encodeurl-1.0.2.tgz",
      "integrity": "sha512-TPJXq8JqFaVYm2CWmPvnP2Iyo4ZSM7/QKcSmuMLDObfpH5fi7RUGmd/rTDf+rut/saiDiQEeVTNgAmJEdAOx0w=="
    },
    "end-of-stream": {
      "version": "1.4.4",
      "resolved": "https://registry.npmjs.org/end-of-stream/-/end-of-stream-1.4.4.tgz",
      "integrity": "sha512-+uw1inIHVPQoaVuHzRyXd21icM+cnt4CzD5rW+NC1wjOUSTOs+Te7FOv7AhN7vS9x/oIyhLP5PR1H+phQAHu5Q==",
      "requires": {
        "once": "^1.4.0"
      }
    },
    "es5-ext": {
      "version": "0.10.62",
      "resolved": "https://registry.npmjs.org/es5-ext/-/es5-ext-0.10.62.tgz",
      "integrity": "sha512-BHLqn0klhEpnOKSrzn/Xsz2UIW8j+cGmo9JLzr8BiUapV8hPL9+FliFqjwr9ngW7jWdnxv6eO+/LqyhJVqgrjA==",
      "requires": {
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.3",
        "next-tick": "^1.1.0"
      }
    },
    "es6-iterator": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-iterator/-/es6-iterator-2.0.3.tgz",
      "integrity": "sha512-zw4SRzoUkd+cl+ZoE15A9o1oQd920Bb0iOJMQkQhl3jNc03YqVjAhG7scf9C5KWRU/R13Orf588uCC6525o02g==",
      "requires": {
        "d": "1",
        "es5-ext": "^0.10.35",
        "es6-symbol": "^3.1.1"
      }
    },
    "es6-symbol": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/es6-symbol/-/es6-symbol-3.1.3.tgz",
      "integrity": "sha512-NJ6Yn3FuDinBaBRWl/q5X/s4koRHBrgKAu+yGI6JCBeiu3qrcbJhwT2GeR/EXVfylRk8dpQVJoLEFhK+Mu31NA==",
      "requires": {
        "d": "^1.0.1",
        "ext": "^1.1.2"
      }
    },
    "es6-weak-map": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-weak-map/-/es6-weak-map-2.0.3.tgz",
      "integrity": "sha512-p5um32HOTO1kP+w7PRnB+5lQ43Z6muuMuIMffvDN8ZB4GcnjLBV6zGStpbASIMk4DCAvEaamhe2zhyCb/QXXsA==",
      "requires": {
        "d": "1",
        "es5-ext": "^0.10.46",
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.1"
      }
    },
    "escape-html": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/escape-html/-/escape-html-1.0.3.tgz",
      "integrity": "sha512-NiSupZ4OeuGwr68lGIeym/ksIZMJodUGOSCZ/FSnTxcrekbvqrgdUxlJOMpijaKZVjAJrWrGs/6Jy8OMuyj9ow=="
    },
    "etag": {
      "version": "1.8.1",
      "resolved": "https://registry.npmjs.org/etag/-/etag-1.8.1.tgz",
      "integrity": "sha512-aIL5Fx7mawVa300al2BnEE4iNvo1qETxLrPI/o05L7z6go7fCw1J6EQmbK4FmJ2AS7kgVF/KEZWufBfdClMcPg=="
    },
    "event-emitter": {
      "version": "0.3.5",
      "resolved": "https://registry.npmjs.org/event-emitter/-/event-emitter-0.3.5.tgz",
      "integrity": "sha512-D9rRn9y7kLPnJ+hMq7S/nhvoKwwvVJahBi2BPmx3bvbsEdK3W9ii8cBSGjP+72/LnM4n6fo3+dkCX5FeTQruXA==",
      "requires": {
        "d": "1",
        "es5-ext": "~0.10.14"
      }
    },
    "express": {
      "version": "4.18.2",
      "resolved": "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
      "integrity": "sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==",
      "requires": {
        "accepts": "~1.3.8",
        "array-flatten": "1.1.1",
        "body-parser": "1.20.1",
        "content-disposition": "0.5.4",
        "content-type": "~1.0.4",
        "cookie": "0.5.0",
        "cookie-signature": "1.0.6",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "finalhandler": "1.2.0",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "merge-descriptors": "1.0.1",
        "methods": "~1.1.2",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "path-to-regexp": "0.1.7",
        "proxy-addr": "~2.0.7",
        "qs": "6.11.0",
        "range-parser": "~1.2.1",
        "safe-buffer": "5.2.1",
        "send": "0.18.0",
        "serve-static": "1.15.0",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "type-is": "~1.6.18",
        "utils-merge": "1.0.1",
        "vary": "~1.1.2"
      },
      "dependencies": {
        "body-parser": {
          "version": "1.20.1",
          "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.1.tgz",
          "integrity": "sha512-jWi7abTbYwajOytWCQc37VulmWiRae5RyTpaCyDcS5/lMdtwSz5lOpDE67srw/HYe35f1z3fDQw+3txg7gNtWw==",
          "requires": {
            "bytes": "3.1.2",
            "content-type": "~1.0.4",
            "debug": "2.6.9",
            "depd": "2.0.0",
            "destroy": "1.2.0",
            "http-errors": "2.0.0",
            "iconv-lite": "0.4.24",
            "on-finished": "2.4.1",
            "qs": "6.11.0",
            "raw-body": "2.5.1",
            "type-is": "~1.6.18",
            "unpipe": "1.0.0"
          }
        },
        "raw-body": {
          "version": "2.5.1",
          "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.1.tgz",
          "integrity": "sha512-qqJBtEyVgS0ZmPGdCFPWJ3FreoqvG4MVQln/kCgF7Olq95IbOp0/BWyMwbdtn4VTvkM8Y7khCQ2Xgk/tcrCXig==",
          "requires": {
            "bytes": "3.1.2",
            "http-errors": "2.0.0",
            "iconv-lite": "0.4.24",
            "unpipe": "1.0.0"
          }
        }
      }
    },
    "ext": {
      "version": "1.7.0",
      "resolved": "https://registry.npmjs.org/ext/-/ext-1.7.0.tgz",
      "integrity": "sha512-6hxeJYaL110a9b5TEJSj0gojyHQAmA2ch5Os+ySCiA1QGdS697XWY1pzsrSjqA9LDEEgdB/KypIlR59RcLuHYw==",
      "requires": {
        "type": "^2.7.2"
      },
      "dependencies": {
        "type": {
          "version": "2.7.2",
          "resolved": "https://registry.npmjs.org/type/-/type-2.7.2.tgz",
          "integrity": "sha512-dzlvlNlt6AXU7EBSfpAscydQ7gXB+pPGsPnfJnZpiNJBDj7IaJzQlBZYGdEi4R9HmPdBv2XmWJ6YUtoTa7lmCw=="
        }
      }
    },
    "extract-zip": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/extract-zip/-/extract-zip-2.0.1.tgz",
      "integrity": "sha512-GDhU9ntwuKyGXdZBUgTIe+vXnWj0fppUEtMDL0+idd5Sta8TGpHssn/eusA9mrPr9qNDym6SxAYZjNvCn/9RBg==",
      "requires": {
        "@types/yauzl": "^2.9.1",
        "debug": "^4.1.1",
        "get-stream": "^5.1.0",
        "yauzl": "^2.10.0"
      },
      "dependencies": {
        "debug": {
          "version": "4.3.4",
          "resolved": "https://registry.npmjs.org/debug/-/debug-4.3.4.tgz",
          "integrity": "sha512-PRWFHuSU3eDtQJPvnNY7Jcket1j0t5OuOsFzPPzsekD52Zl8qUfFIPEiswXqIvHWGVHOgX+7G/vCNNhehwxfkQ==",
          "requires": {
            "ms": "2.1.2"
          }
        },
        "ms": {
          "version": "2.1.2",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.2.tgz",
          "integrity": "sha512-sGkPx+VjMtmA6MX27oA4FBFELFCZZ4S4XqeGOXCv68tT+jb3vk/RyaKWP0PTKyWtmLSM0b+adUTEvbs1PEaH2w=="
        }
      }
    },
    "fd-slicer": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/fd-slicer/-/fd-slicer-1.1.0.tgz",
      "integrity": "sha512-cE1qsB/VwyQozZ+q1dGxR8LBYNZeofhEdUNGSMbQD3Gw2lAzX9Zb3uIU6Ebc/Fmyjo9AWWfnn0AUCHqtevs/8g==",
      "requires": {
        "pend": "~1.2.0"
      }
    },
    "fill-range": {
      "version": "7.0.1",
      "resolved": "https://registry.npmjs.org/fill-range/-/fill-range-7.0.1.tgz",
      "integrity": "sha512-qOo9F+dMUmC2Lcb4BbVvnKJxTPjCm+RRpe4gDuGrzkL7mEVl/djYSu2OdQ2Pa302N4oqkSg9ir6jaLWJ2USVpQ==",
      "requires": {
        "to-regex-range": "^5.0.1"
      }
    },
    "finalhandler": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/finalhandler/-/finalhandler-1.2.0.tgz",
      "integrity": "sha512-5uXcUVftlQMFnWC9qu/svkWv3GTd2PfUhK/3PLkYNAe7FbqJMt3515HaxE6eRL74GdsriiwujiawdaB1BpEISg==",
      "requires": {
        "debug": "2.6.9",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "statuses": "2.0.1",
        "unpipe": "~1.0.0"
      }
    },
    "forwarded": {
      "version": "0.2.0",
      "resolved": "https://registry.npmjs.org/forwarded/-/forwarded-0.2.0.tgz",
      "integrity": "sha512-buRG0fpBtRHSTCOASe6hD258tEubFoRLb4ZNA6NxMVHNw2gOcwHo9wyablzMzOA5z9xA9L1KNjk/Nt6MT9aYow=="
    },
    "fresh": {
      "version": "0.5.2",
      "resolved": "https://registry.npmjs.org/fresh/-/fresh-0.5.2.tgz",
      "integrity": "sha512-zJ2mQYM18rEFOudeV4GShTGIQ7RbzA7ozbU9I/XBpm7kqgMywgmylMwXHxZJmkVoYkna9d2pVXVXPdYTP9ej8Q=="
    },
    "fsevents": {
      "version": "2.3.2",
      "resolved": "https://registry.npmjs.org/fsevents/-/fsevents-2.3.2.tgz",
      "integrity": "sha512-xiqMQR4xAeHTuB9uWm+fFRcIOgKBMiOBP+eXiyT7jsgVCq1bkVygt00oASowB7EdtpOHaaPgKt812P9ab+DDKA==",
      "optional": true
    },
    "function-bind": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/function-bind/-/function-bind-1.1.1.tgz",
      "integrity": "sha512-yIovAzMX49sF8Yl58fSCWJ5svSLuaibPxXQJFLmBObTuCr0Mf1KiPopGM9NiFjiYBCbfaa2Fh6breQ6ANVTI0A=="
    },
    "get-intrinsic": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/get-intrinsic/-/get-intrinsic-1.2.0.tgz",
      "integrity": "sha512-L049y6nFOuom5wGyRc3/gdTLO94dySVKRACj1RmJZBQXlbTMhtNIgkWkUHq+jYmZvKf14EW1EoJnnjbmoHij0Q==",
      "requires": {
        "function-bind": "^1.1.1",
        "has": "^1.0.3",
        "has-symbols": "^1.0.3"
      }
    },
    "get-stream": {
      "version": "5.2.0",
      "resolved": "https://registry.npmjs.org/get-stream/-/get-stream-5.2.0.tgz",
      "integrity": "sha512-nBF+F1rAZVCu/p7rjzgA+Yb4lfYXrpl7a6VmJrU8wF9I1CKvP/QwPNZHnOlwbTkY6dvtFIzFMSyQXbLoTQPRpA==",
      "requires": {
        "pump": "^3.0.0"
      }
    },
    "glob-parent": {
      "version": "5.1.2",
      "resolved": "https://registry.npmjs.org/glob-parent/-/glob-parent-5.1.2.tgz",
      "integrity": "sha512-AOIgSQCepiJYwP3ARnGx+5VnTu2HBYdzbGP45eLw1vr3zB3vZLeyed1sC9hnbcOc9/SrMyM5RPQrkGz4aS9Zow==",
      "requires": {
        "is-glob": "^4.0.1"
      }
    },
    "got": {
      "version": "11.8.6",
      "resolved": "https://registry.npmjs.org/got/-/got-11.8.6.tgz",
      "integrity": "sha512-6tfZ91bOr7bOXnK7PRDCGBLa1H4U080YHNaAQ2KsMGlLEzRbk44nsZF2E1IeRc3vtJHPVbKCYgdFbaGO2ljd8g==",
      "requires": {
        "@sindresorhus/is": "^4.0.0",
        "@szmarczak/http-timer": "^4.0.5",
        "@types/cacheable-request": "^6.0.1",
        "@types/responselike": "^1.0.0",
        "cacheable-lookup": "^5.0.3",
        "cacheable-request": "^7.0.2",
        "decompress-response": "^6.0.0",
        "http2-wrapper": "^1.0.0-beta.5.2",
        "lowercase-keys": "^2.0.0",
        "p-cancelable": "^2.0.0",
        "responselike": "^2.0.0"
      }
    },
    "has": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has/-/has-1.0.3.tgz",
      "integrity": "sha512-f2dvO0VU6Oej7RkWJGrehjbzMAjFp5/VKPp5tTpWIV4JHHZK1/BxbFRtf/siA2SWTe09caDmVtYYzWEIbBS4zw==",
      "requires": {
        "function-bind": "^1.1.1"
      }
    },
    "has-flag": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/has-flag/-/has-flag-3.0.0.tgz",
      "integrity": "sha512-sKJf1+ceQBr4SMkvQnBDNDtf4TXpVhVGateu0t918bl30FnbE2m4vNLX+VWe/dpjlb+HugGYzW7uQXH98HPEYw=="
    },
    "has-symbols": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has-symbols/-/has-symbols-1.0.3.tgz",
      "integrity": "sha512-l3LCuF6MgDNwTDKkdYGEihYjt5pRPbEg46rtlmnSPlUbgmB8LOIrKJbYYFBSbnPaJexMKtiPO8hmeRjRz2Td+A=="
    },
    "hpagent": {
      "version": "0.1.2",
      "resolved": "https://registry.npmjs.org/hpagent/-/hpagent-0.1.2.tgz",
      "integrity": "sha512-ePqFXHtSQWAFXYmj+JtOTHr84iNrII4/QRlAAPPE+zqnKy4xJo7Ie1Y4kC7AdB+LxLxSTTzBMASsEcy0q8YyvQ==",
      "optional": true
    },
    "http-cache-semantics": {
      "version": "4.1.1",
      "resolved": "https://registry.npmjs.org/http-cache-semantics/-/http-cache-semantics-4.1.1.tgz",
      "integrity": "sha512-er295DKPVsV82j5kw1Gjt+ADA/XYHsajl82cGNQG2eyoPkvgUhX+nDIyelzhIWbbsXP39EHcI6l5tYs2FYqYXQ=="
    },
    "http-errors": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/http-errors/-/http-errors-2.0.0.tgz",
      "integrity": "sha512-FtwrG/euBzaEjYeRqOgly7G0qviiXoJWnvEH2Z1plBdXgbyjv34pHTSb9zoeHMyDy33+DWy5Wt9Wo+TURtOYSQ==",
      "requires": {
        "depd": "2.0.0",
        "inherits": "2.0.4",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "toidentifier": "1.0.1"
      }
    },
    "http2-wrapper": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/http2-wrapper/-/http2-wrapper-1.0.3.tgz",
      "integrity": "sha512-V+23sDMr12Wnz7iTcDeJr3O6AIxlnvT/bmaAAAP/Xda35C90p9599p0F1eHR/N1KILWSoWVAiOMFjBBXaXSMxg==",
      "requires": {
        "quick-lru": "^5.1.1",
        "resolve-alpn": "^1.0.0"
      }
    },
    "iconv-lite": {
      "version": "0.4.24",
      "resolved": "https://registry.npmjs.org/iconv-lite/-/iconv-lite-0.4.24.tgz",
      "integrity": "sha512-v3MXnZAcvnywkTUEZomIActle7RXXeedOR31wwl7VlyoXO4Qi9arvSenNQWne1TcRwhCL1HwLI21bEqdpj8/rA==",
      "requires": {
        "safer-buffer": ">= 2.1.2 < 3"
      }
    },
    "ignore-by-default": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/ignore-by-default/-/ignore-by-default-1.0.1.tgz",
      "integrity": "sha512-Ius2VYcGNk7T90CppJqcIkS5ooHUZyIQK+ClZfMfMNFEF9VSE73Fq+906u/CWu92x4gzZMWOwfFYckPObzdEbA=="
    },
    "inherits": {
      "version": "2.0.4",
      "resolved": "https://registry.npmjs.org/inherits/-/inherits-2.0.4.tgz",
      "integrity": "sha512-k/vGaX4/Yla3WzyMCvTQOXYeIHvqOKtnqBduzTHpzpQZzAskKMhZ2K+EnBiSM9zGSoIFeMpXKxa4dYeZIQqewQ=="
    },
    "ipaddr.js": {
      "version": "1.9.1",
      "resolved": "https://registry.npmjs.org/ipaddr.js/-/ipaddr.js-1.9.1.tgz",
      "integrity": "sha512-0KI/607xoxSToH7GjN1FfSbLoU0+btTicjsQSWQlh/hZykN8KpmMf7uYwPW3R+akZ6R/w18ZlXSHBYXiYUPO3g=="
    },
    "is-binary-path": {
      "version": "2.1.0",
      "resolved": "https://registry.npmjs.org/is-binary-path/-/is-binary-path-2.1.0.tgz",
      "integrity": "sha512-ZMERYes6pDydyuGidse7OsHxtbI7WVeUEozgR/g7rd0xUimYNlvZRE/K2MgZTjWy725IfelLeVcEM97mmtRGXw==",
      "requires": {
        "binary-extensions": "^2.0.0"
      }
    },
    "is-extglob": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/is-extglob/-/is-extglob-2.1.1.tgz",
      "integrity": "sha512-SbKbANkN603Vi4jEZv49LeVJMn4yGwsbzZworEoyEiutsN3nJYdbO36zfhGJ6QEDpOZIFkDtnq5JRxmvl3jsoQ=="
    },
    "is-glob": {
      "version": "4.0.3",
      "resolved": "https://registry.npmjs.org/is-glob/-/is-glob-4.0.3.tgz",
      "integrity": "sha512-xelSayHH36ZgE7ZWhli7pW34hNbNl8Ojv5KVmkJD4hBdD3th8Tfk9vYasLM+mXWOZhFkgZfxhLSnrwRr4elSSg==",
      "requires": {
        "is-extglob": "^2.1.1"
      }
    },
    "is-number": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
      "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng=="
    },
    "is-promise": {
      "version": "2.2.2",
      "resolved": "https://registry.npmjs.org/is-promise/-/is-promise-2.2.2.tgz",
      "integrity": "sha512-+lP4/6lKUBfQjZ2pdxThZvLUAafmZb8OAxFb8XXtiQmS35INgr85hdOGoEs124ez1FCnZJt6jau/T+alh58QFQ=="
    },
    "json-buffer": {
      "version": "3.0.1",
      "resolved": "https://registry.npmjs.org/json-buffer/-/json-buffer-3.0.1.tgz",
      "integrity": "sha512-4bV5BfR2mqfQTJm+V5tPPdf+ZpuhiIvTuAB5g8kcrXOZpTT/QwwVRWBywX1ozr6lEuPdbHxwaJlm9G6mI2sfSQ=="
    },
    "keyv": {
      "version": "4.5.2",
      "resolved": "https://registry.npmjs.org/keyv/-/keyv-4.5.2.tgz",
      "integrity": "sha512-5MHbFaKn8cNSmVW7BYnijeAVlE4cYA/SVkifVgrh7yotnfhKmjuXpDKjrABLnT0SfHWV21P8ow07OGfRrNDg8g==",
      "requires": {
        "json-buffer": "3.0.1"
      }
    },
    "lodash.clonedeep": {
      "version": "4.5.0",
      "resolved": "https://registry.npmjs.org/lodash.clonedeep/-/lodash.clonedeep-4.5.0.tgz",
      "integrity": "sha512-H5ZhCF25riFd9uB5UCkVKo61m3S/xZk1x4wA6yp/L3RFP6Z/eHH1ymQcGLo7J3GMPfm0V/7m1tryHuGVxpqEBQ=="
    },
    "lowercase-keys": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/lowercase-keys/-/lowercase-keys-2.0.0.tgz",
      "integrity": "sha512-tqNXrS78oMOE73NMxK4EMLQsQowWf8jKooH9g7xPavRT706R6bkQJ6DY2Te7QukaZsulxa30wQ7bk0pm4XiHmA=="
    },
    "lru-queue": {
      "version": "0.1.0",
      "resolved": "https://registry.npmjs.org/lru-queue/-/lru-queue-0.1.0.tgz",
      "integrity": "sha512-BpdYkt9EvGl8OfWHDQPISVpcl5xZthb+XPsbELj5AQXxIC8IriDZIQYjBJPEm5rS420sjZ0TLEzRcq5KdBhYrQ==",
      "requires": {
        "es5-ext": "~0.10.2"
      }
    },
    "media-typer": {
      "version": "0.3.0",
      "resolved": "https://registry.npmjs.org/media-typer/-/media-typer-0.3.0.tgz",
      "integrity": "sha512-dq+qelQ9akHpcOl/gUVRTxVIOkAJ1wR3QAvb4RsVjS8oVoFjDGTc679wJYmUmknUF5HwMLOgb5O+a3KxfWapPQ=="
    },
    "memoizee": {
      "version": "0.4.15",
      "resolved": "https://registry.npmjs.org/memoizee/-/memoizee-0.4.15.tgz",
      "integrity": "sha512-UBWmJpLZd5STPm7PMUlOw/TSy972M+z8gcyQ5veOnSDRREz/0bmpyTfKt3/51DhEBqCZQn1udM/5flcSPYhkdQ==",
      "requires": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.53",
        "es6-weak-map": "^2.0.3",
        "event-emitter": "^0.3.5",
        "is-promise": "^2.2.2",
        "lru-queue": "^0.1.0",
        "next-tick": "^1.1.0",
        "timers-ext": "^0.1.7"
      }
    },
    "merge-descriptors": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/merge-descriptors/-/merge-descriptors-1.0.1.tgz",
      "integrity": "sha512-cCi6g3/Zr1iqQi6ySbseM1Xvooa98N0w31jzUYrXPX2xqObmFGHJ0tQ5u74H3mVh7wLouTseZyYIq39g8cNp1w=="
    },
    "methods": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/methods/-/methods-1.1.2.tgz",
      "integrity": "sha512-iclAHeNqNm68zFtnZ0e+1L2yUIdvzNoauKU4WBA3VvH/vPFieF7qfRlwUZU+DA9P9bPXIS90ulxoUoCH23sV2w=="
    },
    "mime": {
      "version": "1.6.0",
      "resolved": "https://registry.npmjs.org/mime/-/mime-1.6.0.tgz",
      "integrity": "sha512-x0Vn8spI+wuJ1O6S7gnbaQg8Pxh4NNHb7KSINmEWKiPE4RKOplvijn+NkmYmmRgP68mc70j2EbeTFRsrswaQeg=="
    },
    "mime-db": {
      "version": "1.52.0",
      "resolved": "https://registry.npmjs.org/mime-db/-/mime-db-1.52.0.tgz",
      "integrity": "sha512-sPU4uV7dYlvtWJxwwxHD0PuihVNiE7TyAbQ5SWxDCB9mUYvOgroQOwYQQOKPJ8CIbE+1ETVlOoK1UC2nU3gYvg=="
    },
    "mime-types": {
      "version": "2.1.35",
      "resolved": "https://registry.npmjs.org/mime-types/-/mime-types-2.1.35.tgz",
      "integrity": "sha512-ZDY+bPm5zTTF+YpCrAU9nK0UgICYPT0QtT1NZWFv4s++TNkcgVaT0g6+4R2uI4MjQjzysHB1zxuWL50hzaeXiw==",
      "requires": {
        "mime-db": "1.52.0"
      }
    },
    "mimic-response": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-1.0.1.tgz",
      "integrity": "sha512-j5EctnkH7amfV/q5Hgmoal1g2QHFJRraOtmx0JpIqkxhBhI/lJSl1nMpQ45hVarwNETOoWEimndZ4QK0RHxuxQ=="
    },
    "minimatch": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/minimatch/-/minimatch-3.1.2.tgz",
      "integrity": "sha512-J7p63hRiAjw1NDEww1W7i37+ByIrOWO5XQQAzZ3VOcL0PNybwpfmV/N05zFAzwQ9USyEcX6t3UO+K5aqBQOIHw==",
      "requires": {
        "brace-expansion": "^1.1.7"
      }
    },
    "ms": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.0.0.tgz",
      "integrity": "sha512-Tpp60P6IUJDTuOq/5Z8cdskzJujfwqfOTkrwIwj7IRISpnkJnT6SyJ4PCPnGMoFjC9ddhal5KVIYtAt97ix05A=="
    },
    "negotiator": {
      "version": "0.6.3",
      "resolved": "https://registry.npmjs.org/negotiator/-/negotiator-0.6.3.tgz",
      "integrity": "sha512-+EUsqGPLsM+j/zdChZjsnX51g4XrHFOIXwfnCVPGlQk/k5giakcKsuxCObBRu6DSm9opw/O6slWbJdghQM4bBg=="
    },
    "next-tick": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/next-tick/-/next-tick-1.1.0.tgz",
      "integrity": "sha512-CXdUiJembsNjuToQvxayPZF9Vqht7hewsvy2sOWafLvi2awflj9mOC6bHIg50orX8IJvWKY9wYQ/zB2kogPslQ=="
    },
    "ngrok": {
      "version": "4.3.3",
      "resolved": "https://registry.npmjs.org/ngrok/-/ngrok-4.3.3.tgz",
      "integrity": "sha512-a2KApnkiG5urRxBPdDf76nNBQTnNNWXU0nXw0SsqsPI+Kmt2lGf9TdVYpYrHMnC+T9KhcNSWjCpWqBgC6QcFvw==",
      "requires": {
        "@types/node": "^8.10.50",
        "extract-zip": "^2.0.1",
        "got": "^11.8.5",
        "hpagent": "^0.1.2",
        "lodash.clonedeep": "^4.5.0",
        "uuid": "^7.0.0 || ^8.0.0",
        "yaml": "^1.10.0"
      }
    },
    "nodemon": {
      "version": "2.0.20",
      "resolved": "https://registry.npmjs.org/nodemon/-/nodemon-2.0.20.tgz",
      "integrity": "sha512-Km2mWHKKY5GzRg6i1j5OxOHQtuvVsgskLfigG25yTtbyfRGn/GNvIbRyOf1PSCKJ2aT/58TiuUsuOU5UToVViw==",
      "requires": {
        "chokidar": "^3.5.2",
        "debug": "^3.2.7",
        "ignore-by-default": "^1.0.1",
        "minimatch": "^3.1.2",
        "pstree.remy": "^1.1.8",
        "semver": "^5.7.1",
        "simple-update-notifier": "^1.0.7",
        "supports-color": "^5.5.0",
        "touch": "^3.1.0",
        "undefsafe": "^2.0.5"
      },
      "dependencies": {
        "debug": {
          "version": "3.2.7",
          "resolved": "https://registry.npmjs.org/debug/-/debug-3.2.7.tgz",
          "integrity": "sha512-CFjzYYAi4ThfiQvizrFQevTTXHtnCqWfe7x1AhgEscTz6ZbLbfoLRLPugTQyBth6f8ZERVUSyWHFD/7Wu4t1XQ==",
          "requires": {
            "ms": "^2.1.1"
          }
        },
        "ms": {
          "version": "2.1.3",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
          "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
        }
      }
    },
    "nopt": {
      "version": "1.0.10",
      "resolved": "https://registry.npmjs.org/nopt/-/nopt-1.0.10.tgz",
      "integrity": "sha512-NWmpvLSqUrgrAC9HCuxEvb+PSloHpqVu+FqcO4eeF2h5qYRhA7ev6KvelyQAKtegUbC6RypJnlEOhd8vloNKYg==",
      "requires": {
        "abbrev": "1"
      }
    },
    "normalize-path": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/normalize-path/-/normalize-path-3.0.0.tgz",
      "integrity": "sha512-6eZs5Ls3WtCisHWp9S2GUy8dqkpGi4BVSz3GaqiE6ezub0512ESztXUwUB6C6IKbQkY2Pnb/mD4WYojCRwcwLA=="
    },
    "normalize-url": {
      "version": "6.1.0",
      "resolved": "https://registry.npmjs.org/normalize-url/-/normalize-url-6.1.0.tgz",
      "integrity": "sha512-DlL+XwOy3NxAQ8xuC0okPgK46iuVNAK01YN7RueYBqqFeGsBjV9XmCAzAdgt+667bCl5kPh9EqKKDwnaPG1I7A=="
    },
    "object-inspect": {
      "version": "1.12.3",
      "resolved": "https://registry.npmjs.org/object-inspect/-/object-inspect-1.12.3.tgz",
      "integrity": "sha512-geUvdk7c+eizMNUDkRpW1wJwgfOiOeHbxBR/hLXK1aT6zmVSO0jsQcs7fj6MGw89jC/cjGfLcNOrtMYtGqm81g=="
    },
    "on-finished": {
      "version": "2.4.1",
      "resolved": "https://registry.npmjs.org/on-finished/-/on-finished-2.4.1.tgz",
      "integrity": "sha512-oVlzkg3ENAhCk2zdv7IJwd/QUD4z2RxRwpkcGY8psCVcCYZNq4wYnVWALHM+brtuJjePWiYF/ClmuDr8Ch5+kg==",
      "requires": {
        "ee-first": "1.1.1"
      }
    },
    "once": {
      "version": "1.4.0",
      "resolved": "https://registry.npmjs.org/once/-/once-1.4.0.tgz",
      "integrity": "sha512-lNaJgI+2Q5URQBkccEKHTQOPaXdUxnZZElQTZY0MFUAuaEqe1E+Nyvgdz/aIyNi6Z9MzO5dv1H8n58/GELp3+w==",
      "requires": {
        "wrappy": "1"
      }
    },
    "p-cancelable": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/p-cancelable/-/p-cancelable-2.1.1.tgz",
      "integrity": "sha512-BZOr3nRQHOntUjTrH8+Lh54smKHoHyur8We1V8DSMVrl5A2malOOwuJRnKRDjSnkoeBh4at6BwEnb5I7Jl31wg=="
    },
    "parseurl": {
      "version": "1.3.3",
      "resolved": "https://registry.npmjs.org/parseurl/-/parseurl-1.3.3.tgz",
      "integrity": "sha512-CiyeOxFT/JZyN5m0z9PfXw4SCBJ6Sygz1Dpl0wqjlhDEGGBP1GnsUVEL0p63hoG1fcj3fHynXi9NYO4nWOL+qQ=="
    },
    "path-to-regexp": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/path-to-regexp/-/path-to-regexp-0.1.7.tgz",
      "integrity": "sha512-5DFkuoqlv1uYQKxy8omFBeJPQcdoE07Kv2sferDCrAq1ohOU+MSDswDIbnx3YAM60qIOnYa53wBhXW0EbMonrQ=="
    },
    "pend": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/pend/-/pend-1.2.0.tgz",
      "integrity": "sha512-F3asv42UuXchdzt+xXqfW1OGlVBe+mxa2mqI0pg5yAHZPvFmY3Y6drSf/GQ1A86WgWEN9Kzh/WrgKa6iGcHXLg=="
    },
    "picomatch": {
      "version": "2.3.1",
      "resolved": "https://registry.npmjs.org/picomatch/-/picomatch-2.3.1.tgz",
      "integrity": "sha512-JU3teHTNjmE2VCGFzuY8EXzCDVwEqB2a8fsIvwaStHhAWJEeVd1o1QD80CU6+ZdEXXSLbSsuLwJjkCBWqRQUVA=="
    },
    "prettier": {
      "version": "3.0.3",
      "resolved": "https://registry.npmjs.org/prettier/-/prettier-3.0.3.tgz",
      "integrity": "sha512-L/4pUDMxcNa8R/EthV08Zt42WBO4h1rarVtK0K+QJG0X187OLo7l699jWw0GKuwzkPQ//jMFA/8Xm6Fh3J/DAg==",
      "dev": true
    },
    "proxy-addr": {
      "version": "2.0.7",
      "resolved": "https://registry.npmjs.org/proxy-addr/-/proxy-addr-2.0.7.tgz",
      "integrity": "sha512-llQsMLSUDUPT44jdrU/O37qlnifitDP+ZwrmmZcoSKyLKvtZxpyV0n2/bD/N4tBAAZ/gJEdZU7KMraoK1+XYAg==",
      "requires": {
        "forwarded": "0.2.0",
        "ipaddr.js": "1.9.1"
      }
    },
    "pstree.remy": {
      "version": "1.1.8",
      "resolved": "https://registry.npmjs.org/pstree.remy/-/pstree.remy-1.1.8.tgz",
      "integrity": "sha512-77DZwxQmxKnu3aR542U+X8FypNzbfJ+C5XQDk3uWjWxn6151aIMGthWYRXTqT1E5oJvg+ljaa2OJi+VfvCOQ8w=="
    },
    "pump": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/pump/-/pump-3.0.0.tgz",
      "integrity": "sha512-LwZy+p3SFs1Pytd/jYct4wpv49HiYCqd9Rlc5ZVdk0V+8Yzv6jR5Blk3TRmPL1ft69TxP0IMZGJ+WPFU2BFhww==",
      "requires": {
        "end-of-stream": "^1.1.0",
        "once": "^1.3.1"
      }
    },
    "qs": {
      "version": "6.11.0",
      "resolved": "https://registry.npmjs.org/qs/-/qs-6.11.0.tgz",
      "integrity": "sha512-MvjoMCJwEarSbUYk5O+nmoSzSutSsTwF85zcHPQ9OrlFoZOYIjaqBAJIqIXjptyD5vThxGq52Xu/MaJzRkIk4Q==",
      "requires": {
        "side-channel": "^1.0.4"
      }
    },
    "quick-lru": {
      "version": "5.1.1",
      "resolved": "https://registry.npmjs.org/quick-lru/-/quick-lru-5.1.1.tgz",
      "integrity": "sha512-WuyALRjWPDGtt/wzJiadO5AXY+8hZ80hVpe6MyivgraREW751X3SbhRvG3eLKOYN+8VEvqLcf3wdnt44Z4S4SA=="
    },
    "range-parser": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/range-parser/-/range-parser-1.2.1.tgz",
      "integrity": "sha512-Hrgsx+orqoygnmhFbKaHE6c296J+HTAQXoxEF6gNupROmmGJRoyzfG3ccAveqCBrwr/2yxQ5BVd/GTl5agOwSg=="
    },
    "raw-body": {
      "version": "2.5.2",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.2.tgz",
      "integrity": "sha512-8zGqypfENjCIqGhgXToC8aB2r7YrBX+AQAfIPs/Mlk+BtPTztOvTS01NRW/3Eh60J+a48lt8qsCzirQ6loCVfA==",
      "requires": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      }
    },
    "readdirp": {
      "version": "3.6.0",
      "resolved": "https://registry.npmjs.org/readdirp/-/readdirp-3.6.0.tgz",
      "integrity": "sha512-hOS089on8RduqdbhvQ5Z37A0ESjsqz6qnRcffsMU3495FuTdqSm+7bhJ29JvIOsBDEEnan5DPu9t3To9VRlMzA==",
      "requires": {
        "picomatch": "^2.2.1"
      }
    },
    "resolve-alpn": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/resolve-alpn/-/resolve-alpn-1.2.1.tgz",
      "integrity": "sha512-0a1F4l73/ZFZOakJnQ3FvkJ2+gSTQWz/r2KE5OdDY0TxPm5h4GkqkWWfM47T7HsbnOtcJVEF4epCVy6u7Q3K+g=="
    },
    "responselike": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/responselike/-/responselike-2.0.1.tgz",
      "integrity": "sha512-4gl03wn3hj1HP3yzgdI7d3lCkF95F21Pz4BPGvKHinyQzALR5CapwC8yIi0Rh58DEMQ/SguC03wFj2k0M/mHhw==",
      "requires": {
        "lowercase-keys": "^2.0.0"
      }
    },
    "safe-buffer": {
      "version": "5.2.1",
      "resolved": "https://registry.npmjs.org/safe-buffer/-/safe-buffer-5.2.1.tgz",
      "integrity": "sha512-rp3So07KcdmmKbGvgaNxQSJr7bGVSVk5S9Eq1F+ppbRo70+YeaDxkw5Dd8NPN+GD6bjnYm2VuPuCXmpuYvmCXQ=="
    },
    "safer-buffer": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/safer-buffer/-/safer-buffer-2.1.2.tgz",
      "integrity": "sha512-YZo3K82SD7Riyi0E1EQPojLz7kpepnSQI9IyPbHHg1XXXevb5dJI7tpyN2ADxGcQbHG7vcyRHk0cbwqcQriUtg=="
    },
    "semver": {
      "version": "5.7.1",
      "resolved": "https://registry.npmjs.org/semver/-/semver-5.7.1.tgz",
      "integrity": "sha512-sauaDf/PZdVgrLTNYHRtpXa1iRiKcaebiKQ1BJdpQlWH2lCvexQdX55snPFyK7QzpudqbCI0qXFfOasHdyNDGQ=="
    },
    "send": {
      "version": "0.18.0",
      "resolved": "https://registry.npmjs.org/send/-/send-0.18.0.tgz",
      "integrity": "sha512-qqWzuOjSFOuqPjFe4NOsMLafToQQwBSOEpS+FwEt3A2V3vKubTquT3vmLTQpFgMXp8AlFWFuP1qKaJZOtPpVXg==",
      "requires": {
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "mime": "1.6.0",
        "ms": "2.1.3",
        "on-finished": "2.4.1",
        "range-parser": "~1.2.1",
        "statuses": "2.0.1"
      },
      "dependencies": {
        "ms": {
          "version": "2.1.3",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
          "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
        }
      }
    },
    "serve-static": {
      "version": "1.15.0",
      "resolved": "https://registry.npmjs.org/serve-static/-/serve-static-1.15.0.tgz",
      "integrity": "sha512-XGuRDNjXUijsUL0vl6nSD7cwURuzEgglbOaFuZM9g3kwDXOWVTck0jLzjPzGD+TazWbboZYu52/9/XPdUgne9g==",
      "requires": {
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "parseurl": "~1.3.3",
        "send": "0.18.0"
      }
    },
    "setprototypeof": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/setprototypeof/-/setprototypeof-1.2.0.tgz",
      "integrity": "sha512-E5LDX7Wrp85Kil5bhZv46j8jOeboKq5JMmYM3gVGdGH8xFpPWXUMsNrlODCrkoxMEeNi/XZIwuRvY4XNwYMJpw=="
    },
    "side-channel": {
      "version": "1.0.4",
      "resolved": "https://registry.npmjs.org/side-channel/-/side-channel-1.0.4.tgz",
      "integrity": "sha512-q5XPytqFEIKHkGdiMIrY10mvLRvnQh42/+GoBlFW3b2LXLE2xxJpZFdm94we0BaoV3RwJyGqg5wS7epxTv0Zvw==",
      "requires": {
        "call-bind": "^1.0.0",
        "get-intrinsic": "^1.0.2",
        "object-inspect": "^1.9.0"
      }
    },
    "simple-update-notifier": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/simple-update-notifier/-/simple-update-notifier-1.1.0.tgz",
      "integrity": "sha512-VpsrsJSUcJEseSbMHkrsrAVSdvVS5I96Qo1QAQ4FxQ9wXFcB+pjj7FB7/us9+GcgfW4ziHtYMc1J0PLczb55mg==",
      "requires": {
        "semver": "~7.0.0"
      },
      "dependencies": {
        "semver": {
          "version": "7.0.0",
          "resolved": "https://registry.npmjs.org/semver/-/semver-7.0.0.tgz",
          "integrity": "sha512-+GB6zVA9LWh6zovYQLALHwv5rb2PHGlJi3lfiqIHxR0uuwCgefcOJc59v9fv1w8GbStwxuuqqAjI9NMAOOgq1A=="
        }
      }
    },
    "statuses": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/statuses/-/statuses-2.0.1.tgz",
      "integrity": "sha512-RwNA9Z/7PrK06rYLIzFMlaF+l73iwpzsqRIFgbMLbTcLD6cOao82TaWefPXQvB2fOC4AjuYSEndS7N/mTCbkdQ=="
    },
    "supports-color": {
      "version": "5.5.0",
      "resolved": "https://registry.npmjs.org/supports-color/-/supports-color-5.5.0.tgz",
      "integrity": "sha512-QjVjwdXIt408MIiAqCX4oUKsgU2EqAGzs2Ppkm4aQYbjm+ZEWEcW4SfFNTr4uMNZma0ey4f5lgLrkB0aX0QMow==",
      "requires": {
        "has-flag": "^3.0.0"
      }
    },
    "timers-ext": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/timers-ext/-/timers-ext-0.1.7.tgz",
      "integrity": "sha512-b85NUNzTSdodShTIbky6ZF02e8STtVVfD+fu4aXXShEELpozH+bCpJLYMPZbsABN2wDH7fJpqIoXxJpzbf0NqQ==",
      "requires": {
        "es5-ext": "~0.10.46",
        "next-tick": "1"
      }
    },
    "to-regex-range": {
      "version": "5.0.1",
      "resolved": "https://registry.npmjs.org/to-regex-range/-/to-regex-range-5.0.1.tgz",
      "integrity": "sha512-65P7iz6X5yEr1cwcgvQxbbIw7Uk3gOy5dIdtZ4rDveLqhrdJP+Li/Hx6tyK0NEb+2GCyneCMJiGqrADCSNk8sQ==",
      "requires": {
        "is-number": "^7.0.0"
      }
    },
    "toidentifier": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/toidentifier/-/toidentifier-1.0.1.tgz",
      "integrity": "sha512-o5sSPKEkg/DIQNmH43V0/uerLrpzVedkUh8tGNvaeXpfpuwjKenlSox/2O/BTlZUtEe+JG7s5YhEz608PlAHRA=="
    },
    "touch": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/touch/-/touch-3.1.0.tgz",
      "integrity": "sha512-WBx8Uy5TLtOSRtIq+M03/sKDrXCLHxwDcquSP2c43Le03/9serjQBIztjRz6FkJez9D/hleyAXTBGLwwZUw9lA==",
      "requires": {
        "nopt": "~1.0.10"
      }
    },
    "type": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/type/-/type-1.2.0.tgz",
      "integrity": "sha512-+5nt5AAniqsCnu2cEQQdpzCAh33kVx8n0VoFidKpB1dVVLAN/F+bgVOqOJqOnEnrhp222clB5p3vUlD+1QAnfg=="
    },
    "type-is": {
      "version": "1.6.18",
      "resolved": "https://registry.npmjs.org/type-is/-/type-is-1.6.18.tgz",
      "integrity": "sha512-TkRKr9sUTxEH8MdfuCSP7VizJyzRNMjj2J2do2Jr3Kym598JVdEksuzPQCnlFPW4ky9Q+iA+ma9BGm06XQBy8g==",
      "requires": {
        "media-typer": "0.3.0",
        "mime-types": "~2.1.24"
      }
    },
    "undefsafe": {
      "version": "2.0.5",
      "resolved": "https://registry.npmjs.org/undefsafe/-/undefsafe-2.0.5.tgz",
      "integrity": "sha512-WxONCrssBM8TSPRqN5EmsjVrsv4A8X12J4ArBiiayv3DyyG3ZlIg6yysuuSYdZsVz3TKcTg2fd//Ujd4CHV1iA=="
    },
    "unpipe": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/unpipe/-/unpipe-1.0.0.tgz",
      "integrity": "sha512-pjy2bYhSsufwWlKwPc+l3cN7+wuJlK6uz0YdJEOlQDbl6jo/YlPi4mb8agUkVC8BF7V8NuzeyPNqRksA3hztKQ=="
    },
    "utils-merge": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/utils-merge/-/utils-merge-1.0.1.tgz",
      "integrity": "sha512-pMZTvIkT1d+TFGvDOqodOclx0QWkkgi6Tdoa8gC8ffGAAqz9pzPTZWAybbsHHoED/ztMtkv/VoYTYyShUn81hA=="
    },
    "uuid": {
      "version": "8.3.2",
      "resolved": "https://registry.npmjs.org/uuid/-/uuid-8.3.2.tgz",
      "integrity": "sha512-+NYs2QeMWy+GWFOEm9xnn6HCDp0l7QBD7ml8zLUmJ+93Q5NF0NocErnwkTkXVFNiX3/fpC6afS8Dhb/gz7R7eg=="
    },
    "vary": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/vary/-/vary-1.1.2.tgz",
      "integrity": "sha512-BNGbWLfd0eUPabhkXUVm0j8uuvREyTh5ovRa/dyow/BqAbZJyC+5fU+IzQOzmAKzYqYRAISoRhdQr3eIZ/PXqg=="
    },
    "wrappy": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/wrappy/-/wrappy-1.0.2.tgz",
      "integrity": "sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ=="
    },
    "yaml": {
      "version": "1.10.2",
      "resolved": "https://registry.npmjs.org/yaml/-/yaml-1.10.2.tgz",
      "integrity": "sha512-r3vXyErRCYJ7wg28yvBY5VSoAF8ZvlcW9/BwUzEtUsjvX/DKs24dIkuwjtuprwJJHsbyUbLApepYTR1BN4uHrg=="
    },
    "yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-p4a9I6X6nu6IhoGmBqAcbJy1mlC4j27vEPZX9F4L4/vZT3Lyq1VkFHw/V/PUcB9Buo+DG3iHkT0x3Qya58zc3g==",
      "requires": {
        "buffer-crc32": "~0.2.3",
        "fd-slicer": "~1.1.0"
      }
    }
  }
}

```

### addon-examples-main/iframe-example/package.json

- Size: 643 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "name": "node-example",
  "version": "0.0.1",
  "description": "Example of a NodeJS clockify addon.",
  "main": "index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "./src/dev",
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "keywords": [
    "Clockify",
    "Addon",
    "Manifest",
    "Example"
  ],
  "author": "Aleksander Koko",
  "license": "MIT",
  "dependencies": {
    "async-exit-hook": "^2.0.1",
    "body-parser": "^1.20.2",
    "cli-color": "^2.0.3",
    "express": "^4.18.2",
    "ngrok": "5.0.0-beta.2",
    "nodemon": "^2.0.20"
  },
  "devDependencies": {
    "prettier": "3.0.3"
  }
}

```

### addon-examples-main/iframe-example/src/config.js

- Size: 199 bytes
- MIME: text/plain; charset=us-ascii

```javascript
module.exports.config = {
  url: process.env.URL || "localhost",
  port: process.env.NODE_PORT || 8080,
  ngrok_auth_token: process.env.NGROK_AUTH_TOKEN || "",
  manifestName: 'manifest-v0.1.json'
}

```

### addon-examples-main/iframe-example/src/createWebserver.js

- Size: 193 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const express = require("express");
const bodyParser = require("body-parser");

const app = express();
app.use(bodyParser.json());
app.use(express.static("static"));

module.exports.app = app;

```

### addon-examples-main/iframe-example/src/dev

- Size: 1015 bytes
- MIME: text/plain; charset=us-ascii

```
#!/usr/bin/env node

if (process.env.NODE_ENV === "production") {
  console.error(
    "Do not use nodemon in production, run bin/www directly instead.",
  );
  process.exitCode = 1;
  return;
}

const ngrok = require("ngrok");
const nodemon = require("nodemon");

ngrok
  .connect({
    proto: "http",
    addr: "8080",
    authtoken: process.env.NGROK_AUTH_TOKEN,
  })
  .then((url) => {
    nodemon({
      script: "./src/index.js",
      exec: `URL=${url} node`,
    })
      .on("start", () => {
        console.log("The application has started");
      })
      .on("restart", (files) => {
        console.group("Application restarted due to:");
        files.forEach((file) => console.log(file));
        console.groupEnd();
      })
      .on("quit", () => {
        console.log("The application has quit, closing ngrok tunnel");
        ngrok.kill().then(() => process.exit(0));
      });
  })
  .catch((error) => {
    console.error("Error opening ngrok tunnel: ", error);
    process.exitCode = 1;
  });

```

### addon-examples-main/iframe-example/src/endpoints/index.js

- Size: 23 bytes
- MIME: text/plain; charset=us-ascii

```javascript
require("./manifest");

```

### addon-examples-main/iframe-example/src/endpoints/manifest.js

- Size: 252 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const { app } = require("../createWebserver");
const { config } = require("../config");
const manifest = require("../manifest-v0.1.json");

manifest["baseUrl"] = config.url;

app.get(`/${config.manifestName}`, (req, res) => {
  res.send(manifest);
});

```

### addon-examples-main/iframe-example/src/index.js

- Size: 316 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const { app } = require("./createWebserver");
const { config } = require("./config");
const { printServerInfo } = require("./printServerInfo");

(async () => {
  // Include endpoints
  require("./endpoints");

  // Start server
  app.listen(config.port, () => {});

  // Print server info
  printServerInfo();
})();

```

### addon-examples-main/iframe-example/src/manifest-v0.1.json

- Size: 895 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "schemaVersion": "1.2",
  "key": "iframe-examples",
  "name": "Iframe examples",
  "description": "Example of embedding an iframe on sidebar",
  "baseUrl": "{$BASE_URL}",
  "minimalSubscriptionPlan": "FREE",
  "scopes": [],
  "lifecycle": [
    {
      "type": "SETTINGS_UPDATED",
      "path": "/lifecycle/settings-updated"
    }
  ],
  "webhooks": [],
  "components": [
    {
      "type": "sidebar",
      "accessLevel": "EVERYONE",
      "path": "/sidebar.html",
      "label": "IFRAME",
      "iconPath": "/tab_icon.svg"
    }
  ],
  "settings": {
    "tabs": [
      {
        "id": "settings",
        "name": "settings",
        "settings": [
          {
            "id": "iframe-link-setting",
            "name": "Iframe link",
            "accessLevel": "EVERYONE",
            "type": "LINK",
            "value": "https://clockify.me"
          }
        ]
      }
    ]
  }
}

```

### addon-examples-main/iframe-example/src/printServerInfo.js

- Size: 758 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const clc = require("cli-color");
const {config} = require("./config");

module.exports.printServerInfo = () => {
    const manifestPublicUrl =  `${config.url}/${config.manifestName}`

    console.log('\n\n')
    console.log(clc.magenta('~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'))
    console.log('\n')
    console.log(clc.blue('Manifest is running on:'), clc.green(manifestPublicUrl), '\n')
    console.log(clc.blue("You can add it to your Clockify test instance, available from the \nDeveloper Portal at:"), clc.green('https://developer.marketplace.cake.com/'))
    console.log('\n')
    console.log(clc.magenta('~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'))
    console.log('\n')
}

```

### addon-examples-main/iframe-example/static/sidebar.html

- Size: 1777 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Iframe example</title>
    <style>
        html, body, iframe {
            height: 100%;
        }
        body {
            margin: 0;
        }
        iframe {
            border: 0;
            width: 100%;
        }
    </style>
</head>

<body>
   <iframe></iframe>
    <script>
        try {
            let url = document.documentURI
            let parsedURL = new URL(url);

            // Get auth token from url
            let token = parsedURL.searchParams.get('auth_token');

            // Decode JWT token and get workspaceId
            let jwtContent = JSON.parse(atob(token.split('.')[1]))
            let workspaceId = jwtContent.workspaceId
            let backendUrl = jwtContent.backendUrl

            console.log({ jwtContent })

            // Get settings values from the extension
            let endpoint = `${backendUrl}/addon/workspaces/${workspaceId}/settings`;
            fetch(endpoint, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Addon-Token': token
                }
            }).then(response => {
                response.json().then(rs => {

                    // Get the URL setting
                    const urlValue = rs.tabs[0].settings[0].value

                    // Update the iframe with the url from the settings page
                    const iframeDocument = document.querySelector('iframe')
                    iframeDocument.src = urlValue;
                })
            });
        } catch(e) {
            console.error(e)
        }
    </script>
</body>
</html>

```

### addon-examples-main/iframe-example/static/tab_icon.svg

- Size: 250 bytes
- MIME: image/svg+xml; charset=us-ascii

```
<svg xmlns="http://www.w3.org/2000/svg" height="48" width="48"><path d="M7 40q-1.25 0-2.125-.875T4 37V11q0-1.25.875-2.125T7 8h34q1.25 0 2.125.875T44 11v26q0 1.25-.875 2.125T41 40Zm0-3h3.5V11H7v26Zm6.5 0h21V11h-21Zm24 0H41V11h-3.5Zm-24-26v26Z"/></svg>
```

### addon-examples-main/pumble-notifications-java/Dockerfile

- Size: 274 bytes
- MIME: text/plain; charset=us-ascii

```dockerfile
FROM maven:3.8-eclipse-temurin-18 AS build
ADD pom.xml .
ADD configure-maven.sh .
ADD src ./src

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

RUN ./configure-maven.sh $GITHUB_USERNAME $GITHUB_TOKEN
RUN mvn clean package

CMD java -jar ./target/pumble-notifications-1.0.0-shaded.jar
```

### addon-examples-main/pumble-notifications-java/configure-maven.sh

- Size: 903 bytes
- MIME: text/x-shellscript; charset=us-ascii

```sh
#!/bin/bash
if [ $# != 2 ]; then
  echo "You need to pass the Github username and access tokens as parameters."
  exit 1
fi

m2="<settings>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/clockify/addon-java-sdk</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

    <servers>
      <server>
        <id>github</id>
        <username>$1</username>
        <password>$2</password>
      </server>
    </servers>
</settings>"

mkdir /root/.m2
echo "$m2" > /root/.m2/settings.xml
```

### addon-examples-main/pumble-notifications-java/docker-compose.yml

- Size: 743 bytes
- MIME: text/plain; charset=us-ascii

```yaml
version: '3.8'
services:
  addon:
    container_name: pumble-notifications-addon
    image: pumble-notifications-addon
    build:
      context: ./
    ports:
      - "8080:8080"
    depends_on:
      - mongo
    environment:
      PUBLIC_URL:
      ADDON_KEY: pumblenotifications
      ADDON_NAME: Pumble Notifications
      ADDON_DESCRIPTION: A sample addon that registers Clockify webhooks and then posts a message to the configured Pumble channel whenever the events are triggered.
      LOCAL_PORT: 8080
      MONGO_URI: mongodb://root:123456@mongo:27017/?authSource=admin
      MONGO_DATABASE: pumble-notifications
  mongo:
    image: mongo
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: 123456
```

### addon-examples-main/pumble-notifications-java/pom.xml

- Size: 2863 bytes
- MIME: text/xml; charset=us-ascii

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>group-id</groupId>
    <artifactId>pumble-notifications</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>18</maven.compiler.source>
        <maven.compiler.target>18</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
            <version>4.7.1</version>
        </dependency>

        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.10.0</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId> <!-- or jjwt-gson if Gson is preferred -->
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>com.cake.clockify</groupId>
            <artifactId>addon-sdk</artifactId>
            <version>1.1.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <shadedArtifactAttached>true</shadedArtifactAttached>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.cake.clockify.pumblenotifications.Server</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>

```

### addon-examples-main/pumble-notifications-java/readme.md

- Size: 2920 bytes
- MIME: text/html; charset=us-ascii

```markdown
# Pumble Notifications

This is a simple addon which listens to events from Clockify webhooks, and forwards these events to a Pumble channel.

### How it works
The embedded webserver is started, and handlers are registered to listen for incoming events.

A MongoDB database is used to store the data for an addon installation (workspace info, settings etc).

Once an event is received, it is processed and then POSTed to the Pumble webhook endpoint that the user has configured.

### Getting started
#### Requirements
- A Github account and an access token associated with it
- Docker

#### Running the addon with docker
The addon can be run using the provided docker compose file.

You should update the PUBLIC_URL environment variable from the docker-compose.yml to reflect the actual value.

First, we build the image by passing in a Github username and it's access token.
These are only used in order to pull the Addon SDK dependency from Github packages.

Then, we run the container and pass in the addon public URL.
The container will expose the following port for the addon: 8080.

Use the following commands to run the addon app:
```shell
docker-compose build --build-arg GITHUB_USERNAME="{username}" --build-arg GITHUB_TOKEN="{token}"
docker-compose up
```

This addon example serves the manifest under the following path:
```
{baseUrl}/manifest
```

### Required environment variables
The Server class is the entrypoint to the addon application.

The addon makes use of the following environment variables:

```
ADDON_KEY=pumblenotifications
ADDON_NAME=Pumble Notifications
ADDON_DESCRIPTION=A sample addon that registers Clockify webhooks and then posts a message to the configured Pumble channel whenever the events are triggered.

PUBLIC_URL=
MONGO_URI=
MONGO_DATABASE=
LOCAL_PORT=8080
```
### Retrieving a public URL
The addon must be accessible through a public URL in order for Clockify to be able to communicate with it.

For this example we made use of a free service called <a href="https://ngrok.com">ngrok</a>.

After downloading the binary, we can execute the following command which will expose the server running on our local port through a public URL.
```shell
ngrok http 8080
```

We then pass the public URL that ngrok provides as an env variable:
```
PUBLIC_URL={ngrok public url}
```

### Settings
```pumble-webhook```
the webhook endpoint where the events will be forwarded

### Structure
#### Handlers
The addon makes use of several HTTP handlers:
- Lifecycle handlers (installed, uninstalled, settings updated)
- Webhook handler

The first group is intended to handle lifecycle events and store related information linked to the lifecycle of the addon.
The latter is intended to handle and forward the received events.

#### Repository
The addon installation data are stored in a MongoDB instance.

#### Addon
The NotificationAddon class contains all the information related to the addon.
```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/AddonRequest.java

- Size: 1858 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications;

import com.cake.clockify.addonsdk.shared.RequestHandler;
import com.cake.clockify.addonsdk.shared.utils.JwtUtils;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public abstract class AddonRequest implements RequestHandler {
    private static final String HEADER_SIGNATURE = "Clockify-Signature";

    private String addonId;
    private String workspaceId;
    private String bodyJson;
    private HttpServletRequest request;
    HttpServletResponse response;

    public <T> T getBody(Class<T> clazz) {
        return new Gson().fromJson(bodyJson, clazz);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        String signature = request.getHeader(HEADER_SIGNATURE);

        if (signature != null) {
            // NOTE: we are not verifying the JWT for this example
            // it is always a good practice to ensure that every received request is authentic
            // your addon should not accept unsigned requests
            Claims claims = JwtUtils.parseJwtClaimsWithoutVerifying(signature).getBody();
            workspaceId = claims.get("workspaceId", String.class);
            addonId = claims.get("addonId", String.class);
        } else {
            workspaceId = null;
            addonId = null;
        }
        this.request = request;
        this.response = response;
        bodyJson = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        additionalHandling(this);
    }

    public abstract void additionalHandling(AddonRequest request);
}

```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/NotificationsAddon.java

- Size: 4032 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyLifecycleEvent;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.ClockifySetting;
import com.cake.clockify.addonsdk.clockify.model.ClockifySettings;
import com.cake.clockify.addonsdk.clockify.model.ClockifySettingsTab;
import com.cake.clockify.addonsdk.clockify.model.ClockifyWebhook;
import com.cake.clockify.pumblenotifications.handler.WebhookHandler;
import com.cake.clockify.pumblenotifications.model.Installation;
import java.util.List;
import org.eclipse.jetty.http.HttpStatus;

public final class NotificationsAddon extends ClockifyAddon {
    private final Repository repository = new Repository();

    public NotificationsAddon(String publicUrl) {

        super(ClockifyManifest.builder()
            .key(System.getenv("ADDON_KEY"))
            .name(System.getenv("ADDON_NAME"))
            .baseUrl(publicUrl)
            .requireFreePlan()
            .scopes(List.of())
            .description(System.getenv("ADDON_DESCRIPTION"))
            .settings(ClockifySettings.builder()
                .settingsTabs(List.of(ClockifySettingsTab.builder()
                    .id("settings")
                    .name("Settings")
                    .settings(
                        List.of(ClockifySetting.builder()
                            .id("pumble-webhook")
                            .name("Pumble Webhook URL")
                            .allowEveryone()
                            .asTxt()
                            .value("{webhook url}")
                            .build())
                    ).build()))
                .build()
            )
            .build()
        );
        registerLifecycleEvents();
        registerSupportedEvents();
    }

    private void registerSupportedEvents() {
        WebhookHandler webhookHandler = new WebhookHandler(repository);

        // register the webhooks we are interested in
        registerWebhook(ClockifyWebhook.builder()
                .onNewTimeEntry()
                .path("/events/time-entry-created")
                .build(), webhookHandler);

        registerWebhook(ClockifyWebhook.builder()
                .onTimeEntryUpdated()
                .path("/events/time-entry-updated")
                .build(), webhookHandler);

        registerWebhook(ClockifyWebhook.builder()
                .onTimeEntryDeleted()
                .path("/events/time-entry-deleted")
                .build(), webhookHandler);
    }

    private void registerLifecycleEvents() {
        // this callback is called when the addon is installed in a workspace
        // notice that the auth token that this callback is provided with
        // has full access to the Clockify workspace and should not be leaked to the user
        // or to the frontend
        registerLifecycleEvent(ClockifyLifecycleEvent.builder()
            .path("/lifecycle/installed")
            .onInstalled()
            .build(), new AddonRequest() {
            @Override
            public void additionalHandling(AddonRequest request) {
                repository.persistInstallation(request.getBody(Installation.class));
                request.getResponse().setStatus(HttpStatus.OK_200);
            }
        });

        // this callback is called when the addon is uninstalled from the workspace
        // from now on, the addon will not be able to communicate with that workspace anymore
        registerLifecycleEvent(ClockifyLifecycleEvent.builder()
            .path("/lifecycle/uninstalled")
            .onDeleted()
            .build(), new AddonRequest() {
            @Override
            public void additionalHandling(AddonRequest request) {
                repository.removeInstallation(request.getBody(Installation.class));
                request.getResponse().setStatus(HttpStatus.OK_200);
            }
        });
    }
}

```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/Repository.java

- Size: 1677 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications;

import com.cake.clockify.pumblenotifications.model.Installation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

public class Repository {
    private static final String COLLECTION_INSTALLATIONS = "installations";
    private final String mongoDatabase = System.getenv("MONGO_DATABASE");
    private final MongoClient client = MongoClients.create(System.getenv("MONGO_URI"));

    private final Gson gson = new Gson();

    public void persistInstallation(Installation installation) {
        Document document = Document.parse(gson.toJson(installation));

        client.getDatabase(mongoDatabase)
                .getCollection(COLLECTION_INSTALLATIONS)
                .insertOne(document);
    }

    public void removeInstallation(Installation installation) {
        Document document = new Document("addonId", installation.addonId());

        client.getDatabase(mongoDatabase)
                .getCollection(COLLECTION_INSTALLATIONS)
                .deleteOne(document);
    }

    public Installation getInstallation(String addonId) {
        Document filter = new Document().append("addonId", addonId);

        Document result = client.getDatabase(mongoDatabase)
                .getCollection(COLLECTION_INSTALLATIONS)
                .find(filter)
                .first();

        if (result == null) {
            return null;
        }

        return gson.fromJson(result.toJson(), Installation.class);
    }
}

```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/Server.java

- Size: 768 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications;

import com.cake.clockify.addonsdk.shared.AddonServlet;
import com.cake.clockify.addonsdk.shared.EmbeddedServer;

public class Server {

    public static void main(String[] args) throws Exception {
        String publicUrl = System.getenv("PUBLIC_URL");
        int port = Integer.parseInt(System.getenv("LOCAL_PORT"));

        NotificationsAddon addon = new NotificationsAddon(publicUrl);

        // create a HttpServlet to handle the paths that the addon has defined
        AddonServlet servlet = new AddonServlet(addon);

        // start an embedded webserver serving the servlet instance
        // the servlet can also be served through other frameworks
        new EmbeddedServer(servlet).start(port);
    }
}

```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/handler/WebhookHandler.java

- Size: 4088 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications.handler;

import com.cake.clockify.addonsdk.clockify.model.ClockifySetting;
import com.cake.clockify.addonsdk.clockify.model.ClockifySettings;
import com.cake.clockify.pumblenotifications.AddonRequest;
import com.cake.clockify.pumblenotifications.Repository;
import com.cake.clockify.pumblenotifications.model.Installation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.jetty.http.HttpStatus;

@RequiredArgsConstructor
public class WebhookHandler extends AddonRequest {
    private static final String HEADER_CLOCKIFY_WEBHOOK_EVENT_TYPE = "Clockify-Webhook-Event-Type";
    private final MediaType mediaType = MediaType.parse("application/json");

    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Repository repository;


    @Override
    public void additionalHandling(AddonRequest request) {

        String eventType = request.getRequest().getHeader(HEADER_CLOCKIFY_WEBHOOK_EVENT_TYPE);
        String webhookUrl = retrieveWebhookUrlFromSettings(request.getAddonId());

        String textPayload = prettyGson.toJson(Map.of(
            "event", eventType,
            "payload", request.getBody(Map.class))
        );

        Request okhttpRequest = new Request.Builder()
            .post(RequestBody.create(prettyGson.toJson(Map.of("text", textPayload)), mediaType))
            .url(webhookUrl)
            .build();

        try (Response response = new OkHttpClient().newCall(okhttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Message could not be delivered");
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        request.getResponse().setStatus(HttpStatus.OK_200);
    }

    @SneakyThrows
    private String retrieveWebhookUrlFromSettings(String addonId) {
        // retrieve installation details from the repository
        Installation installation = repository.getInstallation(addonId);
        Objects.requireNonNull(installation, "No installation found for addonId: " + addonId);

        // use the API url and the auth token to retrieve the settings from Clockify
        // the supplied auth token will only work when calling endpoints on the provided API URL
        String pathTemplate = "/addon/workspaces/%1$s/settings";
        String endpoint =
                installation.apiUrl() + String.format(pathTemplate, installation.workspaceId());

        Request request = new Request.Builder()
                .header("X-Addon-Token", installation.authToken())
                .url(endpoint)
                .get()
                .build();

        // execute the call through the OkHttp client
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                Objects.requireNonNull(body, "Could not retrieve settings.");

                // deserialize the response into the appropriate model class
                ClockifySettings settings =
                        prettyGson.fromJson(body.charStream(), ClockifySettings.class);

                ClockifySetting setting = settings.getTabs().get(0).getSettings()
                        .stream()
                        .filter(s -> "pumble-webhook".equals(s.getId()))
                        .findFirst()
                        .orElseThrow();

                // return the stored value
                return String.valueOf(setting.getValue());
            } else {
                throw new RuntimeException("Could not retrieve settings.");
            }
        }
    }
}

```

### addon-examples-main/pumble-notifications-java/src/main/java/com/cake/clockify/pumblenotifications/model/Installation.java

- Size: 209 bytes
- MIME: text/plain; charset=us-ascii

```java
package com.cake.clockify.pumblenotifications.model;

public record Installation(
        String addonId,
        String authToken,
        String workspaceId,
        String asUser,
        String apiUrl
) {}
```

### addon-examples-main/ui-example/.dockerignore

- Size: 13 bytes
- MIME: text/plain; charset=us-ascii

```
node_modules

```

### addon-examples-main/ui-example/.env.example

- Size: 18 bytes
- MIME: text/plain; charset=us-ascii

```
NGROK_AUTH_TOKEN=

```

### addon-examples-main/ui-example/.gitignore

- Size: 24 bytes
- MIME: text/plain; charset=us-ascii

```
node_modules
.idea
.env

```

### addon-examples-main/ui-example/Dockerfile

- Size: 82 bytes
- MIME: text/plain; charset=us-ascii

```dockerfile
FROM node:16-alpine

WORKDIR /app
COPY . /app
RUN npm i

ENTRYPOINT npm run start

```

### addon-examples-main/ui-example/README.md

- Size: 1519 bytes
- MIME: text/plain; charset=us-ascii

```markdown
# UI example Add-on
This addon is an example of every entrypoint for an addon to add custom UI.

The manifest can be obtained on `GET {addonUrl}/manifest-v0.1.json`.

Developed using NodeJS, Docker. This addon doesn't not use any sdks, just plain Node to handle lifecycle events and a static manifest (static/manifest-v0.1.json) file to define UI entrypoints and lifecycle configuration.

## UI entrypoints

This add-on is displayed in all currently supported locations.

1. Sidebar
2. Time off tab
3. Schedule tab
4. Approvals tab
5. Activity tab
6. Team tab
7. Projects tab
8. Widget
9. Reports tab
10. Settings tab

On tab locations you can add more than one tab, in this example we've only used one tab per location. The time off tab contains an example with charts and widget is a chat example. Settings shows every UI element that we support via the manifest configuration. Every other entrypoint shows custom UI elements that can be used via the official [UI library](https://resources.developer.clockify.me/ui/latest/css/main.min.css) .

## How to run this addon locally

You need docker installed and a ngrok auth token that can be found on [ngrok dashboard](https://dashboard.ngrok.com/get-started/your-authtoken).


```
cp .env.example .env
```
Edit .env and include your token on `NGROK_AUTH_TOKEN=(your token here)`
```
docker compose up
```

After that the container is up, use the url provided on the console to register the addon. This addon comes up with ngrok and generates the public url by itself.


```

### addon-examples-main/ui-example/docker-compose.yml

- Size: 187 bytes
- MIME: text/plain; charset=us-ascii

```yaml
version: '3'
services:
  addon:
    restart: always
    build: .
    volumes:
      - ./src:/app/src
    env_file:
      - .env
    ports:
      - "8080:8080"
    entrypoint: npm run dev

```

### addon-examples-main/ui-example/package-lock.json

- Size: 105429 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "name": "node-example",
  "version": "0.0.1",
  "lockfileVersion": 2,
  "requires": true,
  "packages": {
    "": {
      "name": "node-example",
      "version": "0.0.1",
      "license": "MIT",
      "dependencies": {
        "async-exit-hook": "^2.0.1",
        "body-parser": "^1.20.2",
        "cli-color": "^2.0.3",
        "express": "^4.18.2",
        "ngrok": "^4.3.3",
        "nodemon": "^2.0.20"
      }
    },
    "node_modules/@sindresorhus/is": {
      "version": "4.6.0",
      "resolved": "https://registry.npmjs.org/@sindresorhus/is/-/is-4.6.0.tgz",
      "integrity": "sha512-t09vSN3MdfsyCHoFcTRCH/iUtG7OJ0CsjzB8cjAmKc/va/kIgeDI/TxsigdncE/4be734m0cvIYwNaV4i2XqAw==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sindresorhus/is?sponsor=1"
      }
    },
    "node_modules/@szmarczak/http-timer": {
      "version": "4.0.6",
      "resolved": "https://registry.npmjs.org/@szmarczak/http-timer/-/http-timer-4.0.6.tgz",
      "integrity": "sha512-4BAffykYOgO+5nzBWYwE3W90sBgLJoUPRWWcL8wlyiM8IB8ipJz3UMJ9KXQd1RKQXpKp8Tutn80HZtWsu2u76w==",
      "dependencies": {
        "defer-to-connect": "^2.0.0"
      },
      "engines": {
        "node": ">=10"
      }
    },
    "node_modules/@types/cacheable-request": {
      "version": "6.0.3",
      "resolved": "https://registry.npmjs.org/@types/cacheable-request/-/cacheable-request-6.0.3.tgz",
      "integrity": "sha512-IQ3EbTzGxIigb1I3qPZc1rWJnH0BmSKv5QYTalEwweFvyBDLSAe24zP0le/hyi7ecGfZVlIVAg4BZqb8WBwKqw==",
      "dependencies": {
        "@types/http-cache-semantics": "*",
        "@types/keyv": "^3.1.4",
        "@types/node": "*",
        "@types/responselike": "^1.0.0"
      }
    },
    "node_modules/@types/http-cache-semantics": {
      "version": "4.0.1",
      "resolved": "https://registry.npmjs.org/@types/http-cache-semantics/-/http-cache-semantics-4.0.1.tgz",
      "integrity": "sha512-SZs7ekbP8CN0txVG2xVRH6EgKmEm31BOxA07vkFaETzZz1xh+cbt8BcI0slpymvwhx5dlFnQG2rTlPVQn+iRPQ=="
    },
    "node_modules/@types/keyv": {
      "version": "3.1.4",
      "resolved": "https://registry.npmjs.org/@types/keyv/-/keyv-3.1.4.tgz",
      "integrity": "sha512-BQ5aZNSCpj7D6K2ksrRCTmKRLEpnPvWDiLPfoGyhZ++8YtiK9d/3DBKPJgry359X/P1PfruyYwvnvwFjuEiEIg==",
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/@types/node": {
      "version": "8.10.66",
      "resolved": "https://registry.npmjs.org/@types/node/-/node-8.10.66.tgz",
      "integrity": "sha512-tktOkFUA4kXx2hhhrB8bIFb5TbwzS4uOhKEmwiD+NoiL0qtP2OQ9mFldbgD4dV1djrlBYP6eBuQZiWjuHUpqFw=="
    },
    "node_modules/@types/responselike": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/@types/responselike/-/responselike-1.0.0.tgz",
      "integrity": "sha512-85Y2BjiufFzaMIlvJDvTTB8Fxl2xfLo4HgmHzVBz08w4wDePCTjYw66PdrolO0kzli3yam/YCgRufyo1DdQVTA==",
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/@types/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/@types/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-Cn6WYCm0tXv8p6k+A8PvbDG763EDpBoTzHdA+Q/MF6H3sapGjCm9NzoaJncJS9tUKSuCoDs9XHxYYsQDgxR6kw==",
      "optional": true,
      "dependencies": {
        "@types/node": "*"
      }
    },
    "node_modules/abbrev": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/abbrev/-/abbrev-1.1.1.tgz",
      "integrity": "sha512-nne9/IiQ/hzIhY6pdDnbBtz7DjPTKrY00P/zvPSm5pOFkl6xuGrGnXn/VtTNNfNtAfZ9/1RtehkszU9qcTii0Q=="
    },
    "node_modules/accepts": {
      "version": "1.3.8",
      "resolved": "https://registry.npmjs.org/accepts/-/accepts-1.3.8.tgz",
      "integrity": "sha512-PYAthTa2m2VKxuvSD3DPC/Gy+U+sOA1LAuT8mkmRuvw+NACSaeXEQ+NHcVF7rONl6qcaxV3Uuemwawk+7+SJLw==",
      "dependencies": {
        "mime-types": "~2.1.34",
        "negotiator": "0.6.3"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/anymatch": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/anymatch/-/anymatch-3.1.3.tgz",
      "integrity": "sha512-KMReFUr0B4t+D+OBkjR3KYqvocp2XaSzO55UcB6mgQMd3KbcE+mWTyvVV7D/zsdEbNnV6acZUutkiHQXvTr1Rw==",
      "dependencies": {
        "normalize-path": "^3.0.0",
        "picomatch": "^2.0.4"
      },
      "engines": {
        "node": ">= 8"
      }
    },
    "node_modules/array-flatten": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/array-flatten/-/array-flatten-1.1.1.tgz",
      "integrity": "sha512-PCVAQswWemu6UdxsDFFX/+gVeYqKAod3D3UVm91jHwynguOwAvYPhx8nNlM++NqRcK6CxxpUafjmhIdKiHibqg=="
    },
    "node_modules/async-exit-hook": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/async-exit-hook/-/async-exit-hook-2.0.1.tgz",
      "integrity": "sha512-NW2cX8m1Q7KPA7a5M2ULQeZ2wR5qI5PAbw5L0UOMxdioVk9PMZ0h1TmyZEkPYrCvYjDlFICusOu1dlEKAAeXBw==",
      "engines": {
        "node": ">=0.12.0"
      }
    },
    "node_modules/balanced-match": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/balanced-match/-/balanced-match-1.0.2.tgz",
      "integrity": "sha512-3oSeUO0TMV67hN1AmbXsK4yaqU7tjiHlbxRDZOpH0KW9+CeX4bRAaX0Anxt0tx2MrpRpWwQaPwIlISEJhYU5Pw=="
    },
    "node_modules/binary-extensions": {
      "version": "2.2.0",
      "resolved": "https://registry.npmjs.org/binary-extensions/-/binary-extensions-2.2.0.tgz",
      "integrity": "sha512-jDctJ/IVQbZoJykoeHbhXpOlNBqGNcwXJKJog42E5HDPUwQTSdjCHdihjj0DlnheQ7blbT6dHOafNAiS8ooQKA==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/body-parser": {
      "version": "1.20.2",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.2.tgz",
      "integrity": "sha512-ml9pReCu3M61kGlqoTm2umSXTlRTuGTx0bfYj+uIUKKYycG5NtSbeetV3faSU6R7ajOPw0g/J1PvK4qNy7s5bA==",
      "dependencies": {
        "bytes": "3.1.2",
        "content-type": "~1.0.5",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.2",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/brace-expansion": {
      "version": "1.1.11",
      "resolved": "https://registry.npmjs.org/brace-expansion/-/brace-expansion-1.1.11.tgz",
      "integrity": "sha512-iCuPHDFgrHX7H2vEI/5xpz07zSHB00TpugqhmYtVmMO6518mCuRMoOYFldEBl0g187ufozdaHgWKcYFb61qGiA==",
      "dependencies": {
        "balanced-match": "^1.0.0",
        "concat-map": "0.0.1"
      }
    },
    "node_modules/braces": {
      "version": "3.0.2",
      "resolved": "https://registry.npmjs.org/braces/-/braces-3.0.2.tgz",
      "integrity": "sha512-b8um+L1RzM3WDSzvhm6gIz1yfTbBt6YTlcEKAvsmqCZZFw46z626lVj9j1yEPW33H5H+lBQpZMP1k8l+78Ha0A==",
      "dependencies": {
        "fill-range": "^7.0.1"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/buffer-crc32": {
      "version": "0.2.13",
      "resolved": "https://registry.npmjs.org/buffer-crc32/-/buffer-crc32-0.2.13.tgz",
      "integrity": "sha512-VO9Ht/+p3SN7SKWqcrgEzjGbRSJYTx+Q1pTQC0wrWqHx0vpJraQ6GtHx8tvcg1rlK1byhU5gccxgOgj7B0TDkQ==",
      "engines": {
        "node": "*"
      }
    },
    "node_modules/bytes": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/bytes/-/bytes-3.1.2.tgz",
      "integrity": "sha512-/Nf7TyzTx6S3yRJObOAV7956r8cr2+Oj8AC5dt8wSP3BQAoeX58NoHyCU8P8zGkNXStjTSi6fzO6F0pBdcYbEg==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/cacheable-lookup": {
      "version": "5.0.4",
      "resolved": "https://registry.npmjs.org/cacheable-lookup/-/cacheable-lookup-5.0.4.tgz",
      "integrity": "sha512-2/kNscPhpcxrOigMZzbiWF7dz8ilhb/nIHU3EyZiXWXpeq/au8qJ8VhdftMkty3n7Gj6HIGalQG8oiBNB3AJgA==",
      "engines": {
        "node": ">=10.6.0"
      }
    },
    "node_modules/cacheable-request": {
      "version": "7.0.2",
      "resolved": "https://registry.npmjs.org/cacheable-request/-/cacheable-request-7.0.2.tgz",
      "integrity": "sha512-pouW8/FmiPQbuGpkXQ9BAPv/Mo5xDGANgSNXzTzJ8DrKGuXOssM4wIQRjfanNRh3Yu5cfYPvcorqbhg2KIJtew==",
      "dependencies": {
        "clone-response": "^1.0.2",
        "get-stream": "^5.1.0",
        "http-cache-semantics": "^4.0.0",
        "keyv": "^4.0.0",
        "lowercase-keys": "^2.0.0",
        "normalize-url": "^6.0.1",
        "responselike": "^2.0.0"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/call-bind": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/call-bind/-/call-bind-1.0.2.tgz",
      "integrity": "sha512-7O+FbCihrB5WGbFYesctwmTKae6rOiIzmz1icreWJ+0aA7LJfuqhEso2T9ncpcFtzMQtzXf2QGGueWJGTYsqrA==",
      "dependencies": {
        "function-bind": "^1.1.1",
        "get-intrinsic": "^1.0.2"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/chokidar": {
      "version": "3.5.3",
      "resolved": "https://registry.npmjs.org/chokidar/-/chokidar-3.5.3.tgz",
      "integrity": "sha512-Dr3sfKRP6oTcjf2JmUmFJfeVMvXBdegxB0iVQ5eb2V10uFJUCAS8OByZdVAyVb8xXNz3GjjTgj9kLWsZTqE6kw==",
      "funding": [
        {
          "type": "individual",
          "url": "https://paulmillr.com/funding/"
        }
      ],
      "dependencies": {
        "anymatch": "~3.1.2",
        "braces": "~3.0.2",
        "glob-parent": "~5.1.2",
        "is-binary-path": "~2.1.0",
        "is-glob": "~4.0.1",
        "normalize-path": "~3.0.0",
        "readdirp": "~3.6.0"
      },
      "engines": {
        "node": ">= 8.10.0"
      },
      "optionalDependencies": {
        "fsevents": "~2.3.2"
      }
    },
    "node_modules/cli-color": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/cli-color/-/cli-color-2.0.3.tgz",
      "integrity": "sha512-OkoZnxyC4ERN3zLzZaY9Emb7f/MhBOIpePv0Ycok0fJYT+Ouo00UBEIwsVsr0yoow++n5YWlSUgST9GKhNHiRQ==",
      "dependencies": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.61",
        "es6-iterator": "^2.0.3",
        "memoizee": "^0.4.15",
        "timers-ext": "^0.1.7"
      },
      "engines": {
        "node": ">=0.10"
      }
    },
    "node_modules/clone-response": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/clone-response/-/clone-response-1.0.3.tgz",
      "integrity": "sha512-ROoL94jJH2dUVML2Y/5PEDNaSHgeOdSDicUyS7izcF63G6sTc/FTjLub4b8Il9S8S0beOfYt0TaA5qvFK+w0wA==",
      "dependencies": {
        "mimic-response": "^1.0.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/concat-map": {
      "version": "0.0.1",
      "resolved": "https://registry.npmjs.org/concat-map/-/concat-map-0.0.1.tgz",
      "integrity": "sha512-/Srv4dswyQNBfohGpz9o6Yb3Gz3SrUDqBH5rTuhGR7ahtlbYKnVxw2bCFMRljaA7EXHaXZ8wsHdodFvbkhKmqg=="
    },
    "node_modules/content-disposition": {
      "version": "0.5.4",
      "resolved": "https://registry.npmjs.org/content-disposition/-/content-disposition-0.5.4.tgz",
      "integrity": "sha512-FveZTNuGw04cxlAiWbzi6zTAL/lhehaWbTtgluJh4/E95DqMwTmha3KZN1aAWA8cFIhHzMZUvLevkw5Rqk+tSQ==",
      "dependencies": {
        "safe-buffer": "5.2.1"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/content-type": {
      "version": "1.0.5",
      "resolved": "https://registry.npmjs.org/content-type/-/content-type-1.0.5.tgz",
      "integrity": "sha512-nTjqfcBFEipKdXCv4YDQWCfmcLZKm81ldF0pAopTvyrFGVbcR6P/VAAd5G7N+0tTr8QqiU0tFadD6FK4NtJwOA==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/cookie": {
      "version": "0.5.0",
      "resolved": "https://registry.npmjs.org/cookie/-/cookie-0.5.0.tgz",
      "integrity": "sha512-YZ3GUyn/o8gfKJlnlX7g7xq4gyO6OSuhGPKaaGssGB2qgDUS0gPgtTvoyZLTt9Ab6dC4hfc9dV5arkvc/OCmrw==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/cookie-signature": {
      "version": "1.0.6",
      "resolved": "https://registry.npmjs.org/cookie-signature/-/cookie-signature-1.0.6.tgz",
      "integrity": "sha512-QADzlaHc8icV8I7vbaJXJwod9HWYp8uCqf1xa4OfNu1T7JVxQIrUgOWtHdNDtPiywmFbiS12VjotIXLrKM3orQ=="
    },
    "node_modules/d": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/d/-/d-1.0.1.tgz",
      "integrity": "sha512-m62ShEObQ39CfralilEQRjH6oAMtNCV1xJyEx5LpRYUVN+EviphDgUc/F3hnYbADmkiNs67Y+3ylmlG7Lnu+FA==",
      "dependencies": {
        "es5-ext": "^0.10.50",
        "type": "^1.0.1"
      }
    },
    "node_modules/debug": {
      "version": "2.6.9",
      "resolved": "https://registry.npmjs.org/debug/-/debug-2.6.9.tgz",
      "integrity": "sha512-bC7ElrdJaJnPbAP+1EotYvqZsb3ecl5wi6Bfi6BJTUcNowp6cvspg0jXznRTKDjm/E7AdgFBVeAPVMNcKGsHMA==",
      "dependencies": {
        "ms": "2.0.0"
      }
    },
    "node_modules/decompress-response": {
      "version": "6.0.0",
      "resolved": "https://registry.npmjs.org/decompress-response/-/decompress-response-6.0.0.tgz",
      "integrity": "sha512-aW35yZM6Bb/4oJlZncMH2LCoZtJXTRxES17vE3hoRiowU2kWHaJKFkSBDnDR+cm9J+9QhXmREyIfv0pji9ejCQ==",
      "dependencies": {
        "mimic-response": "^3.1.0"
      },
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/decompress-response/node_modules/mimic-response": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-3.1.0.tgz",
      "integrity": "sha512-z0yWI+4FDrrweS8Zmt4Ej5HdJmky15+L2e6Wgn3+iK5fWzb6T3fhNFq2+MeTRb064c6Wr4N/wv0DzQTjNzHNGQ==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/defer-to-connect": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/defer-to-connect/-/defer-to-connect-2.0.1.tgz",
      "integrity": "sha512-4tvttepXG1VaYGrRibk5EwJd1t4udunSOVMdLSAL6mId1ix438oPwPZMALY41FCijukO1L0twNcGsdzS7dHgDg==",
      "engines": {
        "node": ">=10"
      }
    },
    "node_modules/depd": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/depd/-/depd-2.0.0.tgz",
      "integrity": "sha512-g7nH6P6dyDioJogAAGprGpCtVImJhpPk/roCzdb3fIh61/s/nPsfR6onyMwkCAR/OlC3yBC0lESvUoQEAssIrw==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/destroy": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/destroy/-/destroy-1.2.0.tgz",
      "integrity": "sha512-2sJGJTaXIIaR1w4iJSNoN0hnMY7Gpc/n8D4qSCJw8QqFWXf7cuAgnEHxBpweaVcPevC2l3KpjYCx3NypQQgaJg==",
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/ee-first": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/ee-first/-/ee-first-1.1.1.tgz",
      "integrity": "sha512-WMwm9LhRUo+WUaRN+vRuETqG89IgZphVSNkdFgeb6sS/E4OrDIN7t48CAewSHXc6C8lefD8KKfr5vY61brQlow=="
    },
    "node_modules/encodeurl": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/encodeurl/-/encodeurl-1.0.2.tgz",
      "integrity": "sha512-TPJXq8JqFaVYm2CWmPvnP2Iyo4ZSM7/QKcSmuMLDObfpH5fi7RUGmd/rTDf+rut/saiDiQEeVTNgAmJEdAOx0w==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/end-of-stream": {
      "version": "1.4.4",
      "resolved": "https://registry.npmjs.org/end-of-stream/-/end-of-stream-1.4.4.tgz",
      "integrity": "sha512-+uw1inIHVPQoaVuHzRyXd21icM+cnt4CzD5rW+NC1wjOUSTOs+Te7FOv7AhN7vS9x/oIyhLP5PR1H+phQAHu5Q==",
      "dependencies": {
        "once": "^1.4.0"
      }
    },
    "node_modules/es5-ext": {
      "version": "0.10.62",
      "resolved": "https://registry.npmjs.org/es5-ext/-/es5-ext-0.10.62.tgz",
      "integrity": "sha512-BHLqn0klhEpnOKSrzn/Xsz2UIW8j+cGmo9JLzr8BiUapV8hPL9+FliFqjwr9ngW7jWdnxv6eO+/LqyhJVqgrjA==",
      "hasInstallScript": true,
      "dependencies": {
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.3",
        "next-tick": "^1.1.0"
      },
      "engines": {
        "node": ">=0.10"
      }
    },
    "node_modules/es6-iterator": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-iterator/-/es6-iterator-2.0.3.tgz",
      "integrity": "sha512-zw4SRzoUkd+cl+ZoE15A9o1oQd920Bb0iOJMQkQhl3jNc03YqVjAhG7scf9C5KWRU/R13Orf588uCC6525o02g==",
      "dependencies": {
        "d": "1",
        "es5-ext": "^0.10.35",
        "es6-symbol": "^3.1.1"
      }
    },
    "node_modules/es6-symbol": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/es6-symbol/-/es6-symbol-3.1.3.tgz",
      "integrity": "sha512-NJ6Yn3FuDinBaBRWl/q5X/s4koRHBrgKAu+yGI6JCBeiu3qrcbJhwT2GeR/EXVfylRk8dpQVJoLEFhK+Mu31NA==",
      "dependencies": {
        "d": "^1.0.1",
        "ext": "^1.1.2"
      }
    },
    "node_modules/es6-weak-map": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-weak-map/-/es6-weak-map-2.0.3.tgz",
      "integrity": "sha512-p5um32HOTO1kP+w7PRnB+5lQ43Z6muuMuIMffvDN8ZB4GcnjLBV6zGStpbASIMk4DCAvEaamhe2zhyCb/QXXsA==",
      "dependencies": {
        "d": "1",
        "es5-ext": "^0.10.46",
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.1"
      }
    },
    "node_modules/escape-html": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/escape-html/-/escape-html-1.0.3.tgz",
      "integrity": "sha512-NiSupZ4OeuGwr68lGIeym/ksIZMJodUGOSCZ/FSnTxcrekbvqrgdUxlJOMpijaKZVjAJrWrGs/6Jy8OMuyj9ow=="
    },
    "node_modules/etag": {
      "version": "1.8.1",
      "resolved": "https://registry.npmjs.org/etag/-/etag-1.8.1.tgz",
      "integrity": "sha512-aIL5Fx7mawVa300al2BnEE4iNvo1qETxLrPI/o05L7z6go7fCw1J6EQmbK4FmJ2AS7kgVF/KEZWufBfdClMcPg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/event-emitter": {
      "version": "0.3.5",
      "resolved": "https://registry.npmjs.org/event-emitter/-/event-emitter-0.3.5.tgz",
      "integrity": "sha512-D9rRn9y7kLPnJ+hMq7S/nhvoKwwvVJahBi2BPmx3bvbsEdK3W9ii8cBSGjP+72/LnM4n6fo3+dkCX5FeTQruXA==",
      "dependencies": {
        "d": "1",
        "es5-ext": "~0.10.14"
      }
    },
    "node_modules/express": {
      "version": "4.18.2",
      "resolved": "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
      "integrity": "sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==",
      "dependencies": {
        "accepts": "~1.3.8",
        "array-flatten": "1.1.1",
        "body-parser": "1.20.1",
        "content-disposition": "0.5.4",
        "content-type": "~1.0.4",
        "cookie": "0.5.0",
        "cookie-signature": "1.0.6",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "finalhandler": "1.2.0",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "merge-descriptors": "1.0.1",
        "methods": "~1.1.2",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "path-to-regexp": "0.1.7",
        "proxy-addr": "~2.0.7",
        "qs": "6.11.0",
        "range-parser": "~1.2.1",
        "safe-buffer": "5.2.1",
        "send": "0.18.0",
        "serve-static": "1.15.0",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "type-is": "~1.6.18",
        "utils-merge": "1.0.1",
        "vary": "~1.1.2"
      },
      "engines": {
        "node": ">= 0.10.0"
      }
    },
    "node_modules/express/node_modules/body-parser": {
      "version": "1.20.1",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.1.tgz",
      "integrity": "sha512-jWi7abTbYwajOytWCQc37VulmWiRae5RyTpaCyDcS5/lMdtwSz5lOpDE67srw/HYe35f1z3fDQw+3txg7gNtWw==",
      "dependencies": {
        "bytes": "3.1.2",
        "content-type": "~1.0.4",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.1",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8",
        "npm": "1.2.8000 || >= 1.4.16"
      }
    },
    "node_modules/express/node_modules/raw-body": {
      "version": "2.5.1",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.1.tgz",
      "integrity": "sha512-qqJBtEyVgS0ZmPGdCFPWJ3FreoqvG4MVQln/kCgF7Olq95IbOp0/BWyMwbdtn4VTvkM8Y7khCQ2Xgk/tcrCXig==",
      "dependencies": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/ext": {
      "version": "1.7.0",
      "resolved": "https://registry.npmjs.org/ext/-/ext-1.7.0.tgz",
      "integrity": "sha512-6hxeJYaL110a9b5TEJSj0gojyHQAmA2ch5Os+ySCiA1QGdS697XWY1pzsrSjqA9LDEEgdB/KypIlR59RcLuHYw==",
      "dependencies": {
        "type": "^2.7.2"
      }
    },
    "node_modules/ext/node_modules/type": {
      "version": "2.7.2",
      "resolved": "https://registry.npmjs.org/type/-/type-2.7.2.tgz",
      "integrity": "sha512-dzlvlNlt6AXU7EBSfpAscydQ7gXB+pPGsPnfJnZpiNJBDj7IaJzQlBZYGdEi4R9HmPdBv2XmWJ6YUtoTa7lmCw=="
    },
    "node_modules/extract-zip": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/extract-zip/-/extract-zip-2.0.1.tgz",
      "integrity": "sha512-GDhU9ntwuKyGXdZBUgTIe+vXnWj0fppUEtMDL0+idd5Sta8TGpHssn/eusA9mrPr9qNDym6SxAYZjNvCn/9RBg==",
      "dependencies": {
        "debug": "^4.1.1",
        "get-stream": "^5.1.0",
        "yauzl": "^2.10.0"
      },
      "bin": {
        "extract-zip": "cli.js"
      },
      "engines": {
        "node": ">= 10.17.0"
      },
      "optionalDependencies": {
        "@types/yauzl": "^2.9.1"
      }
    },
    "node_modules/extract-zip/node_modules/debug": {
      "version": "4.3.4",
      "resolved": "https://registry.npmjs.org/debug/-/debug-4.3.4.tgz",
      "integrity": "sha512-PRWFHuSU3eDtQJPvnNY7Jcket1j0t5OuOsFzPPzsekD52Zl8qUfFIPEiswXqIvHWGVHOgX+7G/vCNNhehwxfkQ==",
      "dependencies": {
        "ms": "2.1.2"
      },
      "engines": {
        "node": ">=6.0"
      },
      "peerDependenciesMeta": {
        "supports-color": {
          "optional": true
        }
      }
    },
    "node_modules/extract-zip/node_modules/ms": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.2.tgz",
      "integrity": "sha512-sGkPx+VjMtmA6MX27oA4FBFELFCZZ4S4XqeGOXCv68tT+jb3vk/RyaKWP0PTKyWtmLSM0b+adUTEvbs1PEaH2w=="
    },
    "node_modules/fd-slicer": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/fd-slicer/-/fd-slicer-1.1.0.tgz",
      "integrity": "sha512-cE1qsB/VwyQozZ+q1dGxR8LBYNZeofhEdUNGSMbQD3Gw2lAzX9Zb3uIU6Ebc/Fmyjo9AWWfnn0AUCHqtevs/8g==",
      "dependencies": {
        "pend": "~1.2.0"
      }
    },
    "node_modules/fill-range": {
      "version": "7.0.1",
      "resolved": "https://registry.npmjs.org/fill-range/-/fill-range-7.0.1.tgz",
      "integrity": "sha512-qOo9F+dMUmC2Lcb4BbVvnKJxTPjCm+RRpe4gDuGrzkL7mEVl/djYSu2OdQ2Pa302N4oqkSg9ir6jaLWJ2USVpQ==",
      "dependencies": {
        "to-regex-range": "^5.0.1"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/finalhandler": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/finalhandler/-/finalhandler-1.2.0.tgz",
      "integrity": "sha512-5uXcUVftlQMFnWC9qu/svkWv3GTd2PfUhK/3PLkYNAe7FbqJMt3515HaxE6eRL74GdsriiwujiawdaB1BpEISg==",
      "dependencies": {
        "debug": "2.6.9",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "statuses": "2.0.1",
        "unpipe": "~1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/forwarded": {
      "version": "0.2.0",
      "resolved": "https://registry.npmjs.org/forwarded/-/forwarded-0.2.0.tgz",
      "integrity": "sha512-buRG0fpBtRHSTCOASe6hD258tEubFoRLb4ZNA6NxMVHNw2gOcwHo9wyablzMzOA5z9xA9L1KNjk/Nt6MT9aYow==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/fresh": {
      "version": "0.5.2",
      "resolved": "https://registry.npmjs.org/fresh/-/fresh-0.5.2.tgz",
      "integrity": "sha512-zJ2mQYM18rEFOudeV4GShTGIQ7RbzA7ozbU9I/XBpm7kqgMywgmylMwXHxZJmkVoYkna9d2pVXVXPdYTP9ej8Q==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/fsevents": {
      "version": "2.3.2",
      "resolved": "https://registry.npmjs.org/fsevents/-/fsevents-2.3.2.tgz",
      "integrity": "sha512-xiqMQR4xAeHTuB9uWm+fFRcIOgKBMiOBP+eXiyT7jsgVCq1bkVygt00oASowB7EdtpOHaaPgKt812P9ab+DDKA==",
      "hasInstallScript": true,
      "optional": true,
      "os": [
        "darwin"
      ],
      "engines": {
        "node": "^8.16.0 || ^10.6.0 || >=11.0.0"
      }
    },
    "node_modules/function-bind": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/function-bind/-/function-bind-1.1.1.tgz",
      "integrity": "sha512-yIovAzMX49sF8Yl58fSCWJ5svSLuaibPxXQJFLmBObTuCr0Mf1KiPopGM9NiFjiYBCbfaa2Fh6breQ6ANVTI0A=="
    },
    "node_modules/get-intrinsic": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/get-intrinsic/-/get-intrinsic-1.2.0.tgz",
      "integrity": "sha512-L049y6nFOuom5wGyRc3/gdTLO94dySVKRACj1RmJZBQXlbTMhtNIgkWkUHq+jYmZvKf14EW1EoJnnjbmoHij0Q==",
      "dependencies": {
        "function-bind": "^1.1.1",
        "has": "^1.0.3",
        "has-symbols": "^1.0.3"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/get-stream": {
      "version": "5.2.0",
      "resolved": "https://registry.npmjs.org/get-stream/-/get-stream-5.2.0.tgz",
      "integrity": "sha512-nBF+F1rAZVCu/p7rjzgA+Yb4lfYXrpl7a6VmJrU8wF9I1CKvP/QwPNZHnOlwbTkY6dvtFIzFMSyQXbLoTQPRpA==",
      "dependencies": {
        "pump": "^3.0.0"
      },
      "engines": {
        "node": ">=8"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/glob-parent": {
      "version": "5.1.2",
      "resolved": "https://registry.npmjs.org/glob-parent/-/glob-parent-5.1.2.tgz",
      "integrity": "sha512-AOIgSQCepiJYwP3ARnGx+5VnTu2HBYdzbGP45eLw1vr3zB3vZLeyed1sC9hnbcOc9/SrMyM5RPQrkGz4aS9Zow==",
      "dependencies": {
        "is-glob": "^4.0.1"
      },
      "engines": {
        "node": ">= 6"
      }
    },
    "node_modules/got": {
      "version": "11.8.6",
      "resolved": "https://registry.npmjs.org/got/-/got-11.8.6.tgz",
      "integrity": "sha512-6tfZ91bOr7bOXnK7PRDCGBLa1H4U080YHNaAQ2KsMGlLEzRbk44nsZF2E1IeRc3vtJHPVbKCYgdFbaGO2ljd8g==",
      "dependencies": {
        "@sindresorhus/is": "^4.0.0",
        "@szmarczak/http-timer": "^4.0.5",
        "@types/cacheable-request": "^6.0.1",
        "@types/responselike": "^1.0.0",
        "cacheable-lookup": "^5.0.3",
        "cacheable-request": "^7.0.2",
        "decompress-response": "^6.0.0",
        "http2-wrapper": "^1.0.0-beta.5.2",
        "lowercase-keys": "^2.0.0",
        "p-cancelable": "^2.0.0",
        "responselike": "^2.0.0"
      },
      "engines": {
        "node": ">=10.19.0"
      },
      "funding": {
        "url": "https://github.com/sindresorhus/got?sponsor=1"
      }
    },
    "node_modules/has": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has/-/has-1.0.3.tgz",
      "integrity": "sha512-f2dvO0VU6Oej7RkWJGrehjbzMAjFp5/VKPp5tTpWIV4JHHZK1/BxbFRtf/siA2SWTe09caDmVtYYzWEIbBS4zw==",
      "dependencies": {
        "function-bind": "^1.1.1"
      },
      "engines": {
        "node": ">= 0.4.0"
      }
    },
    "node_modules/has-flag": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/has-flag/-/has-flag-3.0.0.tgz",
      "integrity": "sha512-sKJf1+ceQBr4SMkvQnBDNDtf4TXpVhVGateu0t918bl30FnbE2m4vNLX+VWe/dpjlb+HugGYzW7uQXH98HPEYw==",
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/has-symbols": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has-symbols/-/has-symbols-1.0.3.tgz",
      "integrity": "sha512-l3LCuF6MgDNwTDKkdYGEihYjt5pRPbEg46rtlmnSPlUbgmB8LOIrKJbYYFBSbnPaJexMKtiPO8hmeRjRz2Td+A==",
      "engines": {
        "node": ">= 0.4"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/hpagent": {
      "version": "0.1.2",
      "resolved": "https://registry.npmjs.org/hpagent/-/hpagent-0.1.2.tgz",
      "integrity": "sha512-ePqFXHtSQWAFXYmj+JtOTHr84iNrII4/QRlAAPPE+zqnKy4xJo7Ie1Y4kC7AdB+LxLxSTTzBMASsEcy0q8YyvQ==",
      "optional": true
    },
    "node_modules/http-cache-semantics": {
      "version": "4.1.1",
      "resolved": "https://registry.npmjs.org/http-cache-semantics/-/http-cache-semantics-4.1.1.tgz",
      "integrity": "sha512-er295DKPVsV82j5kw1Gjt+ADA/XYHsajl82cGNQG2eyoPkvgUhX+nDIyelzhIWbbsXP39EHcI6l5tYs2FYqYXQ=="
    },
    "node_modules/http-errors": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/http-errors/-/http-errors-2.0.0.tgz",
      "integrity": "sha512-FtwrG/euBzaEjYeRqOgly7G0qviiXoJWnvEH2Z1plBdXgbyjv34pHTSb9zoeHMyDy33+DWy5Wt9Wo+TURtOYSQ==",
      "dependencies": {
        "depd": "2.0.0",
        "inherits": "2.0.4",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "toidentifier": "1.0.1"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/http2-wrapper": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/http2-wrapper/-/http2-wrapper-1.0.3.tgz",
      "integrity": "sha512-V+23sDMr12Wnz7iTcDeJr3O6AIxlnvT/bmaAAAP/Xda35C90p9599p0F1eHR/N1KILWSoWVAiOMFjBBXaXSMxg==",
      "dependencies": {
        "quick-lru": "^5.1.1",
        "resolve-alpn": "^1.0.0"
      },
      "engines": {
        "node": ">=10.19.0"
      }
    },
    "node_modules/iconv-lite": {
      "version": "0.4.24",
      "resolved": "https://registry.npmjs.org/iconv-lite/-/iconv-lite-0.4.24.tgz",
      "integrity": "sha512-v3MXnZAcvnywkTUEZomIActle7RXXeedOR31wwl7VlyoXO4Qi9arvSenNQWne1TcRwhCL1HwLI21bEqdpj8/rA==",
      "dependencies": {
        "safer-buffer": ">= 2.1.2 < 3"
      },
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/ignore-by-default": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/ignore-by-default/-/ignore-by-default-1.0.1.tgz",
      "integrity": "sha512-Ius2VYcGNk7T90CppJqcIkS5ooHUZyIQK+ClZfMfMNFEF9VSE73Fq+906u/CWu92x4gzZMWOwfFYckPObzdEbA=="
    },
    "node_modules/inherits": {
      "version": "2.0.4",
      "resolved": "https://registry.npmjs.org/inherits/-/inherits-2.0.4.tgz",
      "integrity": "sha512-k/vGaX4/Yla3WzyMCvTQOXYeIHvqOKtnqBduzTHpzpQZzAskKMhZ2K+EnBiSM9zGSoIFeMpXKxa4dYeZIQqewQ=="
    },
    "node_modules/ipaddr.js": {
      "version": "1.9.1",
      "resolved": "https://registry.npmjs.org/ipaddr.js/-/ipaddr.js-1.9.1.tgz",
      "integrity": "sha512-0KI/607xoxSToH7GjN1FfSbLoU0+btTicjsQSWQlh/hZykN8KpmMf7uYwPW3R+akZ6R/w18ZlXSHBYXiYUPO3g==",
      "engines": {
        "node": ">= 0.10"
      }
    },
    "node_modules/is-binary-path": {
      "version": "2.1.0",
      "resolved": "https://registry.npmjs.org/is-binary-path/-/is-binary-path-2.1.0.tgz",
      "integrity": "sha512-ZMERYes6pDydyuGidse7OsHxtbI7WVeUEozgR/g7rd0xUimYNlvZRE/K2MgZTjWy725IfelLeVcEM97mmtRGXw==",
      "dependencies": {
        "binary-extensions": "^2.0.0"
      },
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/is-extglob": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/is-extglob/-/is-extglob-2.1.1.tgz",
      "integrity": "sha512-SbKbANkN603Vi4jEZv49LeVJMn4yGwsbzZworEoyEiutsN3nJYdbO36zfhGJ6QEDpOZIFkDtnq5JRxmvl3jsoQ==",
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/is-glob": {
      "version": "4.0.3",
      "resolved": "https://registry.npmjs.org/is-glob/-/is-glob-4.0.3.tgz",
      "integrity": "sha512-xelSayHH36ZgE7ZWhli7pW34hNbNl8Ojv5KVmkJD4hBdD3th8Tfk9vYasLM+mXWOZhFkgZfxhLSnrwRr4elSSg==",
      "dependencies": {
        "is-extglob": "^2.1.1"
      },
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/is-number": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
      "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==",
      "engines": {
        "node": ">=0.12.0"
      }
    },
    "node_modules/is-promise": {
      "version": "2.2.2",
      "resolved": "https://registry.npmjs.org/is-promise/-/is-promise-2.2.2.tgz",
      "integrity": "sha512-+lP4/6lKUBfQjZ2pdxThZvLUAafmZb8OAxFb8XXtiQmS35INgr85hdOGoEs124ez1FCnZJt6jau/T+alh58QFQ=="
    },
    "node_modules/json-buffer": {
      "version": "3.0.1",
      "resolved": "https://registry.npmjs.org/json-buffer/-/json-buffer-3.0.1.tgz",
      "integrity": "sha512-4bV5BfR2mqfQTJm+V5tPPdf+ZpuhiIvTuAB5g8kcrXOZpTT/QwwVRWBywX1ozr6lEuPdbHxwaJlm9G6mI2sfSQ=="
    },
    "node_modules/keyv": {
      "version": "4.5.2",
      "resolved": "https://registry.npmjs.org/keyv/-/keyv-4.5.2.tgz",
      "integrity": "sha512-5MHbFaKn8cNSmVW7BYnijeAVlE4cYA/SVkifVgrh7yotnfhKmjuXpDKjrABLnT0SfHWV21P8ow07OGfRrNDg8g==",
      "dependencies": {
        "json-buffer": "3.0.1"
      }
    },
    "node_modules/lodash.clonedeep": {
      "version": "4.5.0",
      "resolved": "https://registry.npmjs.org/lodash.clonedeep/-/lodash.clonedeep-4.5.0.tgz",
      "integrity": "sha512-H5ZhCF25riFd9uB5UCkVKo61m3S/xZk1x4wA6yp/L3RFP6Z/eHH1ymQcGLo7J3GMPfm0V/7m1tryHuGVxpqEBQ=="
    },
    "node_modules/lowercase-keys": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/lowercase-keys/-/lowercase-keys-2.0.0.tgz",
      "integrity": "sha512-tqNXrS78oMOE73NMxK4EMLQsQowWf8jKooH9g7xPavRT706R6bkQJ6DY2Te7QukaZsulxa30wQ7bk0pm4XiHmA==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/lru-queue": {
      "version": "0.1.0",
      "resolved": "https://registry.npmjs.org/lru-queue/-/lru-queue-0.1.0.tgz",
      "integrity": "sha512-BpdYkt9EvGl8OfWHDQPISVpcl5xZthb+XPsbELj5AQXxIC8IriDZIQYjBJPEm5rS420sjZ0TLEzRcq5KdBhYrQ==",
      "dependencies": {
        "es5-ext": "~0.10.2"
      }
    },
    "node_modules/media-typer": {
      "version": "0.3.0",
      "resolved": "https://registry.npmjs.org/media-typer/-/media-typer-0.3.0.tgz",
      "integrity": "sha512-dq+qelQ9akHpcOl/gUVRTxVIOkAJ1wR3QAvb4RsVjS8oVoFjDGTc679wJYmUmknUF5HwMLOgb5O+a3KxfWapPQ==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/memoizee": {
      "version": "0.4.15",
      "resolved": "https://registry.npmjs.org/memoizee/-/memoizee-0.4.15.tgz",
      "integrity": "sha512-UBWmJpLZd5STPm7PMUlOw/TSy972M+z8gcyQ5veOnSDRREz/0bmpyTfKt3/51DhEBqCZQn1udM/5flcSPYhkdQ==",
      "dependencies": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.53",
        "es6-weak-map": "^2.0.3",
        "event-emitter": "^0.3.5",
        "is-promise": "^2.2.2",
        "lru-queue": "^0.1.0",
        "next-tick": "^1.1.0",
        "timers-ext": "^0.1.7"
      }
    },
    "node_modules/merge-descriptors": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/merge-descriptors/-/merge-descriptors-1.0.1.tgz",
      "integrity": "sha512-cCi6g3/Zr1iqQi6ySbseM1Xvooa98N0w31jzUYrXPX2xqObmFGHJ0tQ5u74H3mVh7wLouTseZyYIq39g8cNp1w=="
    },
    "node_modules/methods": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/methods/-/methods-1.1.2.tgz",
      "integrity": "sha512-iclAHeNqNm68zFtnZ0e+1L2yUIdvzNoauKU4WBA3VvH/vPFieF7qfRlwUZU+DA9P9bPXIS90ulxoUoCH23sV2w==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mime": {
      "version": "1.6.0",
      "resolved": "https://registry.npmjs.org/mime/-/mime-1.6.0.tgz",
      "integrity": "sha512-x0Vn8spI+wuJ1O6S7gnbaQg8Pxh4NNHb7KSINmEWKiPE4RKOplvijn+NkmYmmRgP68mc70j2EbeTFRsrswaQeg==",
      "bin": {
        "mime": "cli.js"
      },
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/mime-db": {
      "version": "1.52.0",
      "resolved": "https://registry.npmjs.org/mime-db/-/mime-db-1.52.0.tgz",
      "integrity": "sha512-sPU4uV7dYlvtWJxwwxHD0PuihVNiE7TyAbQ5SWxDCB9mUYvOgroQOwYQQOKPJ8CIbE+1ETVlOoK1UC2nU3gYvg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mime-types": {
      "version": "2.1.35",
      "resolved": "https://registry.npmjs.org/mime-types/-/mime-types-2.1.35.tgz",
      "integrity": "sha512-ZDY+bPm5zTTF+YpCrAU9nK0UgICYPT0QtT1NZWFv4s++TNkcgVaT0g6+4R2uI4MjQjzysHB1zxuWL50hzaeXiw==",
      "dependencies": {
        "mime-db": "1.52.0"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/mimic-response": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-1.0.1.tgz",
      "integrity": "sha512-j5EctnkH7amfV/q5Hgmoal1g2QHFJRraOtmx0JpIqkxhBhI/lJSl1nMpQ45hVarwNETOoWEimndZ4QK0RHxuxQ==",
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/minimatch": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/minimatch/-/minimatch-3.1.2.tgz",
      "integrity": "sha512-J7p63hRiAjw1NDEww1W7i37+ByIrOWO5XQQAzZ3VOcL0PNybwpfmV/N05zFAzwQ9USyEcX6t3UO+K5aqBQOIHw==",
      "dependencies": {
        "brace-expansion": "^1.1.7"
      },
      "engines": {
        "node": "*"
      }
    },
    "node_modules/ms": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.0.0.tgz",
      "integrity": "sha512-Tpp60P6IUJDTuOq/5Z8cdskzJujfwqfOTkrwIwj7IRISpnkJnT6SyJ4PCPnGMoFjC9ddhal5KVIYtAt97ix05A=="
    },
    "node_modules/negotiator": {
      "version": "0.6.3",
      "resolved": "https://registry.npmjs.org/negotiator/-/negotiator-0.6.3.tgz",
      "integrity": "sha512-+EUsqGPLsM+j/zdChZjsnX51g4XrHFOIXwfnCVPGlQk/k5giakcKsuxCObBRu6DSm9opw/O6slWbJdghQM4bBg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/next-tick": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/next-tick/-/next-tick-1.1.0.tgz",
      "integrity": "sha512-CXdUiJembsNjuToQvxayPZF9Vqht7hewsvy2sOWafLvi2awflj9mOC6bHIg50orX8IJvWKY9wYQ/zB2kogPslQ=="
    },
    "node_modules/ngrok": {
      "version": "4.3.3",
      "resolved": "https://registry.npmjs.org/ngrok/-/ngrok-4.3.3.tgz",
      "integrity": "sha512-a2KApnkiG5urRxBPdDf76nNBQTnNNWXU0nXw0SsqsPI+Kmt2lGf9TdVYpYrHMnC+T9KhcNSWjCpWqBgC6QcFvw==",
      "hasInstallScript": true,
      "dependencies": {
        "@types/node": "^8.10.50",
        "extract-zip": "^2.0.1",
        "got": "^11.8.5",
        "lodash.clonedeep": "^4.5.0",
        "uuid": "^7.0.0 || ^8.0.0",
        "yaml": "^1.10.0"
      },
      "bin": {
        "ngrok": "bin/ngrok"
      },
      "engines": {
        "node": ">=10.19.0 <14 || >=14.2"
      },
      "optionalDependencies": {
        "hpagent": "^0.1.2"
      }
    },
    "node_modules/nodemon": {
      "version": "2.0.20",
      "resolved": "https://registry.npmjs.org/nodemon/-/nodemon-2.0.20.tgz",
      "integrity": "sha512-Km2mWHKKY5GzRg6i1j5OxOHQtuvVsgskLfigG25yTtbyfRGn/GNvIbRyOf1PSCKJ2aT/58TiuUsuOU5UToVViw==",
      "dependencies": {
        "chokidar": "^3.5.2",
        "debug": "^3.2.7",
        "ignore-by-default": "^1.0.1",
        "minimatch": "^3.1.2",
        "pstree.remy": "^1.1.8",
        "semver": "^5.7.1",
        "simple-update-notifier": "^1.0.7",
        "supports-color": "^5.5.0",
        "touch": "^3.1.0",
        "undefsafe": "^2.0.5"
      },
      "bin": {
        "nodemon": "bin/nodemon.js"
      },
      "engines": {
        "node": ">=8.10.0"
      },
      "funding": {
        "type": "opencollective",
        "url": "https://opencollective.com/nodemon"
      }
    },
    "node_modules/nodemon/node_modules/debug": {
      "version": "3.2.7",
      "resolved": "https://registry.npmjs.org/debug/-/debug-3.2.7.tgz",
      "integrity": "sha512-CFjzYYAi4ThfiQvizrFQevTTXHtnCqWfe7x1AhgEscTz6ZbLbfoLRLPugTQyBth6f8ZERVUSyWHFD/7Wu4t1XQ==",
      "dependencies": {
        "ms": "^2.1.1"
      }
    },
    "node_modules/nodemon/node_modules/ms": {
      "version": "2.1.3",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
      "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
    },
    "node_modules/nopt": {
      "version": "1.0.10",
      "resolved": "https://registry.npmjs.org/nopt/-/nopt-1.0.10.tgz",
      "integrity": "sha512-NWmpvLSqUrgrAC9HCuxEvb+PSloHpqVu+FqcO4eeF2h5qYRhA7ev6KvelyQAKtegUbC6RypJnlEOhd8vloNKYg==",
      "dependencies": {
        "abbrev": "1"
      },
      "bin": {
        "nopt": "bin/nopt.js"
      },
      "engines": {
        "node": "*"
      }
    },
    "node_modules/normalize-path": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/normalize-path/-/normalize-path-3.0.0.tgz",
      "integrity": "sha512-6eZs5Ls3WtCisHWp9S2GUy8dqkpGi4BVSz3GaqiE6ezub0512ESztXUwUB6C6IKbQkY2Pnb/mD4WYojCRwcwLA==",
      "engines": {
        "node": ">=0.10.0"
      }
    },
    "node_modules/normalize-url": {
      "version": "6.1.0",
      "resolved": "https://registry.npmjs.org/normalize-url/-/normalize-url-6.1.0.tgz",
      "integrity": "sha512-DlL+XwOy3NxAQ8xuC0okPgK46iuVNAK01YN7RueYBqqFeGsBjV9XmCAzAdgt+667bCl5kPh9EqKKDwnaPG1I7A==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/object-inspect": {
      "version": "1.12.3",
      "resolved": "https://registry.npmjs.org/object-inspect/-/object-inspect-1.12.3.tgz",
      "integrity": "sha512-geUvdk7c+eizMNUDkRpW1wJwgfOiOeHbxBR/hLXK1aT6zmVSO0jsQcs7fj6MGw89jC/cjGfLcNOrtMYtGqm81g==",
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/on-finished": {
      "version": "2.4.1",
      "resolved": "https://registry.npmjs.org/on-finished/-/on-finished-2.4.1.tgz",
      "integrity": "sha512-oVlzkg3ENAhCk2zdv7IJwd/QUD4z2RxRwpkcGY8psCVcCYZNq4wYnVWALHM+brtuJjePWiYF/ClmuDr8Ch5+kg==",
      "dependencies": {
        "ee-first": "1.1.1"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/once": {
      "version": "1.4.0",
      "resolved": "https://registry.npmjs.org/once/-/once-1.4.0.tgz",
      "integrity": "sha512-lNaJgI+2Q5URQBkccEKHTQOPaXdUxnZZElQTZY0MFUAuaEqe1E+Nyvgdz/aIyNi6Z9MzO5dv1H8n58/GELp3+w==",
      "dependencies": {
        "wrappy": "1"
      }
    },
    "node_modules/p-cancelable": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/p-cancelable/-/p-cancelable-2.1.1.tgz",
      "integrity": "sha512-BZOr3nRQHOntUjTrH8+Lh54smKHoHyur8We1V8DSMVrl5A2malOOwuJRnKRDjSnkoeBh4at6BwEnb5I7Jl31wg==",
      "engines": {
        "node": ">=8"
      }
    },
    "node_modules/parseurl": {
      "version": "1.3.3",
      "resolved": "https://registry.npmjs.org/parseurl/-/parseurl-1.3.3.tgz",
      "integrity": "sha512-CiyeOxFT/JZyN5m0z9PfXw4SCBJ6Sygz1Dpl0wqjlhDEGGBP1GnsUVEL0p63hoG1fcj3fHynXi9NYO4nWOL+qQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/path-to-regexp": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/path-to-regexp/-/path-to-regexp-0.1.7.tgz",
      "integrity": "sha512-5DFkuoqlv1uYQKxy8omFBeJPQcdoE07Kv2sferDCrAq1ohOU+MSDswDIbnx3YAM60qIOnYa53wBhXW0EbMonrQ=="
    },
    "node_modules/pend": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/pend/-/pend-1.2.0.tgz",
      "integrity": "sha512-F3asv42UuXchdzt+xXqfW1OGlVBe+mxa2mqI0pg5yAHZPvFmY3Y6drSf/GQ1A86WgWEN9Kzh/WrgKa6iGcHXLg=="
    },
    "node_modules/picomatch": {
      "version": "2.3.1",
      "resolved": "https://registry.npmjs.org/picomatch/-/picomatch-2.3.1.tgz",
      "integrity": "sha512-JU3teHTNjmE2VCGFzuY8EXzCDVwEqB2a8fsIvwaStHhAWJEeVd1o1QD80CU6+ZdEXXSLbSsuLwJjkCBWqRQUVA==",
      "engines": {
        "node": ">=8.6"
      },
      "funding": {
        "url": "https://github.com/sponsors/jonschlinkert"
      }
    },
    "node_modules/proxy-addr": {
      "version": "2.0.7",
      "resolved": "https://registry.npmjs.org/proxy-addr/-/proxy-addr-2.0.7.tgz",
      "integrity": "sha512-llQsMLSUDUPT44jdrU/O37qlnifitDP+ZwrmmZcoSKyLKvtZxpyV0n2/bD/N4tBAAZ/gJEdZU7KMraoK1+XYAg==",
      "dependencies": {
        "forwarded": "0.2.0",
        "ipaddr.js": "1.9.1"
      },
      "engines": {
        "node": ">= 0.10"
      }
    },
    "node_modules/pstree.remy": {
      "version": "1.1.8",
      "resolved": "https://registry.npmjs.org/pstree.remy/-/pstree.remy-1.1.8.tgz",
      "integrity": "sha512-77DZwxQmxKnu3aR542U+X8FypNzbfJ+C5XQDk3uWjWxn6151aIMGthWYRXTqT1E5oJvg+ljaa2OJi+VfvCOQ8w=="
    },
    "node_modules/pump": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/pump/-/pump-3.0.0.tgz",
      "integrity": "sha512-LwZy+p3SFs1Pytd/jYct4wpv49HiYCqd9Rlc5ZVdk0V+8Yzv6jR5Blk3TRmPL1ft69TxP0IMZGJ+WPFU2BFhww==",
      "dependencies": {
        "end-of-stream": "^1.1.0",
        "once": "^1.3.1"
      }
    },
    "node_modules/qs": {
      "version": "6.11.0",
      "resolved": "https://registry.npmjs.org/qs/-/qs-6.11.0.tgz",
      "integrity": "sha512-MvjoMCJwEarSbUYk5O+nmoSzSutSsTwF85zcHPQ9OrlFoZOYIjaqBAJIqIXjptyD5vThxGq52Xu/MaJzRkIk4Q==",
      "dependencies": {
        "side-channel": "^1.0.4"
      },
      "engines": {
        "node": ">=0.6"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/quick-lru": {
      "version": "5.1.1",
      "resolved": "https://registry.npmjs.org/quick-lru/-/quick-lru-5.1.1.tgz",
      "integrity": "sha512-WuyALRjWPDGtt/wzJiadO5AXY+8hZ80hVpe6MyivgraREW751X3SbhRvG3eLKOYN+8VEvqLcf3wdnt44Z4S4SA==",
      "engines": {
        "node": ">=10"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/range-parser": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/range-parser/-/range-parser-1.2.1.tgz",
      "integrity": "sha512-Hrgsx+orqoygnmhFbKaHE6c296J+HTAQXoxEF6gNupROmmGJRoyzfG3ccAveqCBrwr/2yxQ5BVd/GTl5agOwSg==",
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/raw-body": {
      "version": "2.5.2",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.2.tgz",
      "integrity": "sha512-8zGqypfENjCIqGhgXToC8aB2r7YrBX+AQAfIPs/Mlk+BtPTztOvTS01NRW/3Eh60J+a48lt8qsCzirQ6loCVfA==",
      "dependencies": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      },
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/readdirp": {
      "version": "3.6.0",
      "resolved": "https://registry.npmjs.org/readdirp/-/readdirp-3.6.0.tgz",
      "integrity": "sha512-hOS089on8RduqdbhvQ5Z37A0ESjsqz6qnRcffsMU3495FuTdqSm+7bhJ29JvIOsBDEEnan5DPu9t3To9VRlMzA==",
      "dependencies": {
        "picomatch": "^2.2.1"
      },
      "engines": {
        "node": ">=8.10.0"
      }
    },
    "node_modules/resolve-alpn": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/resolve-alpn/-/resolve-alpn-1.2.1.tgz",
      "integrity": "sha512-0a1F4l73/ZFZOakJnQ3FvkJ2+gSTQWz/r2KE5OdDY0TxPm5h4GkqkWWfM47T7HsbnOtcJVEF4epCVy6u7Q3K+g=="
    },
    "node_modules/responselike": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/responselike/-/responselike-2.0.1.tgz",
      "integrity": "sha512-4gl03wn3hj1HP3yzgdI7d3lCkF95F21Pz4BPGvKHinyQzALR5CapwC8yIi0Rh58DEMQ/SguC03wFj2k0M/mHhw==",
      "dependencies": {
        "lowercase-keys": "^2.0.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/sindresorhus"
      }
    },
    "node_modules/safe-buffer": {
      "version": "5.2.1",
      "resolved": "https://registry.npmjs.org/safe-buffer/-/safe-buffer-5.2.1.tgz",
      "integrity": "sha512-rp3So07KcdmmKbGvgaNxQSJr7bGVSVk5S9Eq1F+ppbRo70+YeaDxkw5Dd8NPN+GD6bjnYm2VuPuCXmpuYvmCXQ==",
      "funding": [
        {
          "type": "github",
          "url": "https://github.com/sponsors/feross"
        },
        {
          "type": "patreon",
          "url": "https://www.patreon.com/feross"
        },
        {
          "type": "consulting",
          "url": "https://feross.org/support"
        }
      ]
    },
    "node_modules/safer-buffer": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/safer-buffer/-/safer-buffer-2.1.2.tgz",
      "integrity": "sha512-YZo3K82SD7Riyi0E1EQPojLz7kpepnSQI9IyPbHHg1XXXevb5dJI7tpyN2ADxGcQbHG7vcyRHk0cbwqcQriUtg=="
    },
    "node_modules/semver": {
      "version": "5.7.1",
      "resolved": "https://registry.npmjs.org/semver/-/semver-5.7.1.tgz",
      "integrity": "sha512-sauaDf/PZdVgrLTNYHRtpXa1iRiKcaebiKQ1BJdpQlWH2lCvexQdX55snPFyK7QzpudqbCI0qXFfOasHdyNDGQ==",
      "bin": {
        "semver": "bin/semver"
      }
    },
    "node_modules/send": {
      "version": "0.18.0",
      "resolved": "https://registry.npmjs.org/send/-/send-0.18.0.tgz",
      "integrity": "sha512-qqWzuOjSFOuqPjFe4NOsMLafToQQwBSOEpS+FwEt3A2V3vKubTquT3vmLTQpFgMXp8AlFWFuP1qKaJZOtPpVXg==",
      "dependencies": {
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "mime": "1.6.0",
        "ms": "2.1.3",
        "on-finished": "2.4.1",
        "range-parser": "~1.2.1",
        "statuses": "2.0.1"
      },
      "engines": {
        "node": ">= 0.8.0"
      }
    },
    "node_modules/send/node_modules/ms": {
      "version": "2.1.3",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
      "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
    },
    "node_modules/serve-static": {
      "version": "1.15.0",
      "resolved": "https://registry.npmjs.org/serve-static/-/serve-static-1.15.0.tgz",
      "integrity": "sha512-XGuRDNjXUijsUL0vl6nSD7cwURuzEgglbOaFuZM9g3kwDXOWVTck0jLzjPzGD+TazWbboZYu52/9/XPdUgne9g==",
      "dependencies": {
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "parseurl": "~1.3.3",
        "send": "0.18.0"
      },
      "engines": {
        "node": ">= 0.8.0"
      }
    },
    "node_modules/setprototypeof": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/setprototypeof/-/setprototypeof-1.2.0.tgz",
      "integrity": "sha512-E5LDX7Wrp85Kil5bhZv46j8jOeboKq5JMmYM3gVGdGH8xFpPWXUMsNrlODCrkoxMEeNi/XZIwuRvY4XNwYMJpw=="
    },
    "node_modules/side-channel": {
      "version": "1.0.4",
      "resolved": "https://registry.npmjs.org/side-channel/-/side-channel-1.0.4.tgz",
      "integrity": "sha512-q5XPytqFEIKHkGdiMIrY10mvLRvnQh42/+GoBlFW3b2LXLE2xxJpZFdm94we0BaoV3RwJyGqg5wS7epxTv0Zvw==",
      "dependencies": {
        "call-bind": "^1.0.0",
        "get-intrinsic": "^1.0.2",
        "object-inspect": "^1.9.0"
      },
      "funding": {
        "url": "https://github.com/sponsors/ljharb"
      }
    },
    "node_modules/simple-update-notifier": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/simple-update-notifier/-/simple-update-notifier-1.1.0.tgz",
      "integrity": "sha512-VpsrsJSUcJEseSbMHkrsrAVSdvVS5I96Qo1QAQ4FxQ9wXFcB+pjj7FB7/us9+GcgfW4ziHtYMc1J0PLczb55mg==",
      "dependencies": {
        "semver": "~7.0.0"
      },
      "engines": {
        "node": ">=8.10.0"
      }
    },
    "node_modules/simple-update-notifier/node_modules/semver": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/semver/-/semver-7.0.0.tgz",
      "integrity": "sha512-+GB6zVA9LWh6zovYQLALHwv5rb2PHGlJi3lfiqIHxR0uuwCgefcOJc59v9fv1w8GbStwxuuqqAjI9NMAOOgq1A==",
      "bin": {
        "semver": "bin/semver.js"
      }
    },
    "node_modules/statuses": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/statuses/-/statuses-2.0.1.tgz",
      "integrity": "sha512-RwNA9Z/7PrK06rYLIzFMlaF+l73iwpzsqRIFgbMLbTcLD6cOao82TaWefPXQvB2fOC4AjuYSEndS7N/mTCbkdQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/supports-color": {
      "version": "5.5.0",
      "resolved": "https://registry.npmjs.org/supports-color/-/supports-color-5.5.0.tgz",
      "integrity": "sha512-QjVjwdXIt408MIiAqCX4oUKsgU2EqAGzs2Ppkm4aQYbjm+ZEWEcW4SfFNTr4uMNZma0ey4f5lgLrkB0aX0QMow==",
      "dependencies": {
        "has-flag": "^3.0.0"
      },
      "engines": {
        "node": ">=4"
      }
    },
    "node_modules/timers-ext": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/timers-ext/-/timers-ext-0.1.7.tgz",
      "integrity": "sha512-b85NUNzTSdodShTIbky6ZF02e8STtVVfD+fu4aXXShEELpozH+bCpJLYMPZbsABN2wDH7fJpqIoXxJpzbf0NqQ==",
      "dependencies": {
        "es5-ext": "~0.10.46",
        "next-tick": "1"
      }
    },
    "node_modules/to-regex-range": {
      "version": "5.0.1",
      "resolved": "https://registry.npmjs.org/to-regex-range/-/to-regex-range-5.0.1.tgz",
      "integrity": "sha512-65P7iz6X5yEr1cwcgvQxbbIw7Uk3gOy5dIdtZ4rDveLqhrdJP+Li/Hx6tyK0NEb+2GCyneCMJiGqrADCSNk8sQ==",
      "dependencies": {
        "is-number": "^7.0.0"
      },
      "engines": {
        "node": ">=8.0"
      }
    },
    "node_modules/toidentifier": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/toidentifier/-/toidentifier-1.0.1.tgz",
      "integrity": "sha512-o5sSPKEkg/DIQNmH43V0/uerLrpzVedkUh8tGNvaeXpfpuwjKenlSox/2O/BTlZUtEe+JG7s5YhEz608PlAHRA==",
      "engines": {
        "node": ">=0.6"
      }
    },
    "node_modules/touch": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/touch/-/touch-3.1.0.tgz",
      "integrity": "sha512-WBx8Uy5TLtOSRtIq+M03/sKDrXCLHxwDcquSP2c43Le03/9serjQBIztjRz6FkJez9D/hleyAXTBGLwwZUw9lA==",
      "dependencies": {
        "nopt": "~1.0.10"
      },
      "bin": {
        "nodetouch": "bin/nodetouch.js"
      }
    },
    "node_modules/type": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/type/-/type-1.2.0.tgz",
      "integrity": "sha512-+5nt5AAniqsCnu2cEQQdpzCAh33kVx8n0VoFidKpB1dVVLAN/F+bgVOqOJqOnEnrhp222clB5p3vUlD+1QAnfg=="
    },
    "node_modules/type-is": {
      "version": "1.6.18",
      "resolved": "https://registry.npmjs.org/type-is/-/type-is-1.6.18.tgz",
      "integrity": "sha512-TkRKr9sUTxEH8MdfuCSP7VizJyzRNMjj2J2do2Jr3Kym598JVdEksuzPQCnlFPW4ky9Q+iA+ma9BGm06XQBy8g==",
      "dependencies": {
        "media-typer": "0.3.0",
        "mime-types": "~2.1.24"
      },
      "engines": {
        "node": ">= 0.6"
      }
    },
    "node_modules/undefsafe": {
      "version": "2.0.5",
      "resolved": "https://registry.npmjs.org/undefsafe/-/undefsafe-2.0.5.tgz",
      "integrity": "sha512-WxONCrssBM8TSPRqN5EmsjVrsv4A8X12J4ArBiiayv3DyyG3ZlIg6yysuuSYdZsVz3TKcTg2fd//Ujd4CHV1iA=="
    },
    "node_modules/unpipe": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/unpipe/-/unpipe-1.0.0.tgz",
      "integrity": "sha512-pjy2bYhSsufwWlKwPc+l3cN7+wuJlK6uz0YdJEOlQDbl6jo/YlPi4mb8agUkVC8BF7V8NuzeyPNqRksA3hztKQ==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/utils-merge": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/utils-merge/-/utils-merge-1.0.1.tgz",
      "integrity": "sha512-pMZTvIkT1d+TFGvDOqodOclx0QWkkgi6Tdoa8gC8ffGAAqz9pzPTZWAybbsHHoED/ztMtkv/VoYTYyShUn81hA==",
      "engines": {
        "node": ">= 0.4.0"
      }
    },
    "node_modules/uuid": {
      "version": "8.3.2",
      "resolved": "https://registry.npmjs.org/uuid/-/uuid-8.3.2.tgz",
      "integrity": "sha512-+NYs2QeMWy+GWFOEm9xnn6HCDp0l7QBD7ml8zLUmJ+93Q5NF0NocErnwkTkXVFNiX3/fpC6afS8Dhb/gz7R7eg==",
      "bin": {
        "uuid": "dist/bin/uuid"
      }
    },
    "node_modules/vary": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/vary/-/vary-1.1.2.tgz",
      "integrity": "sha512-BNGbWLfd0eUPabhkXUVm0j8uuvREyTh5ovRa/dyow/BqAbZJyC+5fU+IzQOzmAKzYqYRAISoRhdQr3eIZ/PXqg==",
      "engines": {
        "node": ">= 0.8"
      }
    },
    "node_modules/wrappy": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/wrappy/-/wrappy-1.0.2.tgz",
      "integrity": "sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ=="
    },
    "node_modules/yaml": {
      "version": "1.10.2",
      "resolved": "https://registry.npmjs.org/yaml/-/yaml-1.10.2.tgz",
      "integrity": "sha512-r3vXyErRCYJ7wg28yvBY5VSoAF8ZvlcW9/BwUzEtUsjvX/DKs24dIkuwjtuprwJJHsbyUbLApepYTR1BN4uHrg==",
      "engines": {
        "node": ">= 6"
      }
    },
    "node_modules/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-p4a9I6X6nu6IhoGmBqAcbJy1mlC4j27vEPZX9F4L4/vZT3Lyq1VkFHw/V/PUcB9Buo+DG3iHkT0x3Qya58zc3g==",
      "dependencies": {
        "buffer-crc32": "~0.2.3",
        "fd-slicer": "~1.1.0"
      }
    }
  },
  "dependencies": {
    "@sindresorhus/is": {
      "version": "4.6.0",
      "resolved": "https://registry.npmjs.org/@sindresorhus/is/-/is-4.6.0.tgz",
      "integrity": "sha512-t09vSN3MdfsyCHoFcTRCH/iUtG7OJ0CsjzB8cjAmKc/va/kIgeDI/TxsigdncE/4be734m0cvIYwNaV4i2XqAw=="
    },
    "@szmarczak/http-timer": {
      "version": "4.0.6",
      "resolved": "https://registry.npmjs.org/@szmarczak/http-timer/-/http-timer-4.0.6.tgz",
      "integrity": "sha512-4BAffykYOgO+5nzBWYwE3W90sBgLJoUPRWWcL8wlyiM8IB8ipJz3UMJ9KXQd1RKQXpKp8Tutn80HZtWsu2u76w==",
      "requires": {
        "defer-to-connect": "^2.0.0"
      }
    },
    "@types/cacheable-request": {
      "version": "6.0.3",
      "resolved": "https://registry.npmjs.org/@types/cacheable-request/-/cacheable-request-6.0.3.tgz",
      "integrity": "sha512-IQ3EbTzGxIigb1I3qPZc1rWJnH0BmSKv5QYTalEwweFvyBDLSAe24zP0le/hyi7ecGfZVlIVAg4BZqb8WBwKqw==",
      "requires": {
        "@types/http-cache-semantics": "*",
        "@types/keyv": "^3.1.4",
        "@types/node": "*",
        "@types/responselike": "^1.0.0"
      }
    },
    "@types/http-cache-semantics": {
      "version": "4.0.1",
      "resolved": "https://registry.npmjs.org/@types/http-cache-semantics/-/http-cache-semantics-4.0.1.tgz",
      "integrity": "sha512-SZs7ekbP8CN0txVG2xVRH6EgKmEm31BOxA07vkFaETzZz1xh+cbt8BcI0slpymvwhx5dlFnQG2rTlPVQn+iRPQ=="
    },
    "@types/keyv": {
      "version": "3.1.4",
      "resolved": "https://registry.npmjs.org/@types/keyv/-/keyv-3.1.4.tgz",
      "integrity": "sha512-BQ5aZNSCpj7D6K2ksrRCTmKRLEpnPvWDiLPfoGyhZ++8YtiK9d/3DBKPJgry359X/P1PfruyYwvnvwFjuEiEIg==",
      "requires": {
        "@types/node": "*"
      }
    },
    "@types/node": {
      "version": "8.10.66",
      "resolved": "https://registry.npmjs.org/@types/node/-/node-8.10.66.tgz",
      "integrity": "sha512-tktOkFUA4kXx2hhhrB8bIFb5TbwzS4uOhKEmwiD+NoiL0qtP2OQ9mFldbgD4dV1djrlBYP6eBuQZiWjuHUpqFw=="
    },
    "@types/responselike": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/@types/responselike/-/responselike-1.0.0.tgz",
      "integrity": "sha512-85Y2BjiufFzaMIlvJDvTTB8Fxl2xfLo4HgmHzVBz08w4wDePCTjYw66PdrolO0kzli3yam/YCgRufyo1DdQVTA==",
      "requires": {
        "@types/node": "*"
      }
    },
    "@types/yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/@types/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-Cn6WYCm0tXv8p6k+A8PvbDG763EDpBoTzHdA+Q/MF6H3sapGjCm9NzoaJncJS9tUKSuCoDs9XHxYYsQDgxR6kw==",
      "optional": true,
      "requires": {
        "@types/node": "*"
      }
    },
    "abbrev": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/abbrev/-/abbrev-1.1.1.tgz",
      "integrity": "sha512-nne9/IiQ/hzIhY6pdDnbBtz7DjPTKrY00P/zvPSm5pOFkl6xuGrGnXn/VtTNNfNtAfZ9/1RtehkszU9qcTii0Q=="
    },
    "accepts": {
      "version": "1.3.8",
      "resolved": "https://registry.npmjs.org/accepts/-/accepts-1.3.8.tgz",
      "integrity": "sha512-PYAthTa2m2VKxuvSD3DPC/Gy+U+sOA1LAuT8mkmRuvw+NACSaeXEQ+NHcVF7rONl6qcaxV3Uuemwawk+7+SJLw==",
      "requires": {
        "mime-types": "~2.1.34",
        "negotiator": "0.6.3"
      }
    },
    "anymatch": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/anymatch/-/anymatch-3.1.3.tgz",
      "integrity": "sha512-KMReFUr0B4t+D+OBkjR3KYqvocp2XaSzO55UcB6mgQMd3KbcE+mWTyvVV7D/zsdEbNnV6acZUutkiHQXvTr1Rw==",
      "requires": {
        "normalize-path": "^3.0.0",
        "picomatch": "^2.0.4"
      }
    },
    "array-flatten": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/array-flatten/-/array-flatten-1.1.1.tgz",
      "integrity": "sha512-PCVAQswWemu6UdxsDFFX/+gVeYqKAod3D3UVm91jHwynguOwAvYPhx8nNlM++NqRcK6CxxpUafjmhIdKiHibqg=="
    },
    "async-exit-hook": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/async-exit-hook/-/async-exit-hook-2.0.1.tgz",
      "integrity": "sha512-NW2cX8m1Q7KPA7a5M2ULQeZ2wR5qI5PAbw5L0UOMxdioVk9PMZ0h1TmyZEkPYrCvYjDlFICusOu1dlEKAAeXBw=="
    },
    "balanced-match": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/balanced-match/-/balanced-match-1.0.2.tgz",
      "integrity": "sha512-3oSeUO0TMV67hN1AmbXsK4yaqU7tjiHlbxRDZOpH0KW9+CeX4bRAaX0Anxt0tx2MrpRpWwQaPwIlISEJhYU5Pw=="
    },
    "binary-extensions": {
      "version": "2.2.0",
      "resolved": "https://registry.npmjs.org/binary-extensions/-/binary-extensions-2.2.0.tgz",
      "integrity": "sha512-jDctJ/IVQbZoJykoeHbhXpOlNBqGNcwXJKJog42E5HDPUwQTSdjCHdihjj0DlnheQ7blbT6dHOafNAiS8ooQKA=="
    },
    "body-parser": {
      "version": "1.20.2",
      "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.2.tgz",
      "integrity": "sha512-ml9pReCu3M61kGlqoTm2umSXTlRTuGTx0bfYj+uIUKKYycG5NtSbeetV3faSU6R7ajOPw0g/J1PvK4qNy7s5bA==",
      "requires": {
        "bytes": "3.1.2",
        "content-type": "~1.0.5",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "on-finished": "2.4.1",
        "qs": "6.11.0",
        "raw-body": "2.5.2",
        "type-is": "~1.6.18",
        "unpipe": "1.0.0"
      }
    },
    "brace-expansion": {
      "version": "1.1.11",
      "resolved": "https://registry.npmjs.org/brace-expansion/-/brace-expansion-1.1.11.tgz",
      "integrity": "sha512-iCuPHDFgrHX7H2vEI/5xpz07zSHB00TpugqhmYtVmMO6518mCuRMoOYFldEBl0g187ufozdaHgWKcYFb61qGiA==",
      "requires": {
        "balanced-match": "^1.0.0",
        "concat-map": "0.0.1"
      }
    },
    "braces": {
      "version": "3.0.2",
      "resolved": "https://registry.npmjs.org/braces/-/braces-3.0.2.tgz",
      "integrity": "sha512-b8um+L1RzM3WDSzvhm6gIz1yfTbBt6YTlcEKAvsmqCZZFw46z626lVj9j1yEPW33H5H+lBQpZMP1k8l+78Ha0A==",
      "requires": {
        "fill-range": "^7.0.1"
      }
    },
    "buffer-crc32": {
      "version": "0.2.13",
      "resolved": "https://registry.npmjs.org/buffer-crc32/-/buffer-crc32-0.2.13.tgz",
      "integrity": "sha512-VO9Ht/+p3SN7SKWqcrgEzjGbRSJYTx+Q1pTQC0wrWqHx0vpJraQ6GtHx8tvcg1rlK1byhU5gccxgOgj7B0TDkQ=="
    },
    "bytes": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/bytes/-/bytes-3.1.2.tgz",
      "integrity": "sha512-/Nf7TyzTx6S3yRJObOAV7956r8cr2+Oj8AC5dt8wSP3BQAoeX58NoHyCU8P8zGkNXStjTSi6fzO6F0pBdcYbEg=="
    },
    "cacheable-lookup": {
      "version": "5.0.4",
      "resolved": "https://registry.npmjs.org/cacheable-lookup/-/cacheable-lookup-5.0.4.tgz",
      "integrity": "sha512-2/kNscPhpcxrOigMZzbiWF7dz8ilhb/nIHU3EyZiXWXpeq/au8qJ8VhdftMkty3n7Gj6HIGalQG8oiBNB3AJgA=="
    },
    "cacheable-request": {
      "version": "7.0.2",
      "resolved": "https://registry.npmjs.org/cacheable-request/-/cacheable-request-7.0.2.tgz",
      "integrity": "sha512-pouW8/FmiPQbuGpkXQ9BAPv/Mo5xDGANgSNXzTzJ8DrKGuXOssM4wIQRjfanNRh3Yu5cfYPvcorqbhg2KIJtew==",
      "requires": {
        "clone-response": "^1.0.2",
        "get-stream": "^5.1.0",
        "http-cache-semantics": "^4.0.0",
        "keyv": "^4.0.0",
        "lowercase-keys": "^2.0.0",
        "normalize-url": "^6.0.1",
        "responselike": "^2.0.0"
      }
    },
    "call-bind": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/call-bind/-/call-bind-1.0.2.tgz",
      "integrity": "sha512-7O+FbCihrB5WGbFYesctwmTKae6rOiIzmz1icreWJ+0aA7LJfuqhEso2T9ncpcFtzMQtzXf2QGGueWJGTYsqrA==",
      "requires": {
        "function-bind": "^1.1.1",
        "get-intrinsic": "^1.0.2"
      }
    },
    "chokidar": {
      "version": "3.5.3",
      "resolved": "https://registry.npmjs.org/chokidar/-/chokidar-3.5.3.tgz",
      "integrity": "sha512-Dr3sfKRP6oTcjf2JmUmFJfeVMvXBdegxB0iVQ5eb2V10uFJUCAS8OByZdVAyVb8xXNz3GjjTgj9kLWsZTqE6kw==",
      "requires": {
        "anymatch": "~3.1.2",
        "braces": "~3.0.2",
        "fsevents": "~2.3.2",
        "glob-parent": "~5.1.2",
        "is-binary-path": "~2.1.0",
        "is-glob": "~4.0.1",
        "normalize-path": "~3.0.0",
        "readdirp": "~3.6.0"
      }
    },
    "cli-color": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/cli-color/-/cli-color-2.0.3.tgz",
      "integrity": "sha512-OkoZnxyC4ERN3zLzZaY9Emb7f/MhBOIpePv0Ycok0fJYT+Ouo00UBEIwsVsr0yoow++n5YWlSUgST9GKhNHiRQ==",
      "requires": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.61",
        "es6-iterator": "^2.0.3",
        "memoizee": "^0.4.15",
        "timers-ext": "^0.1.7"
      }
    },
    "clone-response": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/clone-response/-/clone-response-1.0.3.tgz",
      "integrity": "sha512-ROoL94jJH2dUVML2Y/5PEDNaSHgeOdSDicUyS7izcF63G6sTc/FTjLub4b8Il9S8S0beOfYt0TaA5qvFK+w0wA==",
      "requires": {
        "mimic-response": "^1.0.0"
      }
    },
    "concat-map": {
      "version": "0.0.1",
      "resolved": "https://registry.npmjs.org/concat-map/-/concat-map-0.0.1.tgz",
      "integrity": "sha512-/Srv4dswyQNBfohGpz9o6Yb3Gz3SrUDqBH5rTuhGR7ahtlbYKnVxw2bCFMRljaA7EXHaXZ8wsHdodFvbkhKmqg=="
    },
    "content-disposition": {
      "version": "0.5.4",
      "resolved": "https://registry.npmjs.org/content-disposition/-/content-disposition-0.5.4.tgz",
      "integrity": "sha512-FveZTNuGw04cxlAiWbzi6zTAL/lhehaWbTtgluJh4/E95DqMwTmha3KZN1aAWA8cFIhHzMZUvLevkw5Rqk+tSQ==",
      "requires": {
        "safe-buffer": "5.2.1"
      }
    },
    "content-type": {
      "version": "1.0.5",
      "resolved": "https://registry.npmjs.org/content-type/-/content-type-1.0.5.tgz",
      "integrity": "sha512-nTjqfcBFEipKdXCv4YDQWCfmcLZKm81ldF0pAopTvyrFGVbcR6P/VAAd5G7N+0tTr8QqiU0tFadD6FK4NtJwOA=="
    },
    "cookie": {
      "version": "0.5.0",
      "resolved": "https://registry.npmjs.org/cookie/-/cookie-0.5.0.tgz",
      "integrity": "sha512-YZ3GUyn/o8gfKJlnlX7g7xq4gyO6OSuhGPKaaGssGB2qgDUS0gPgtTvoyZLTt9Ab6dC4hfc9dV5arkvc/OCmrw=="
    },
    "cookie-signature": {
      "version": "1.0.6",
      "resolved": "https://registry.npmjs.org/cookie-signature/-/cookie-signature-1.0.6.tgz",
      "integrity": "sha512-QADzlaHc8icV8I7vbaJXJwod9HWYp8uCqf1xa4OfNu1T7JVxQIrUgOWtHdNDtPiywmFbiS12VjotIXLrKM3orQ=="
    },
    "d": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/d/-/d-1.0.1.tgz",
      "integrity": "sha512-m62ShEObQ39CfralilEQRjH6oAMtNCV1xJyEx5LpRYUVN+EviphDgUc/F3hnYbADmkiNs67Y+3ylmlG7Lnu+FA==",
      "requires": {
        "es5-ext": "^0.10.50",
        "type": "^1.0.1"
      }
    },
    "debug": {
      "version": "2.6.9",
      "resolved": "https://registry.npmjs.org/debug/-/debug-2.6.9.tgz",
      "integrity": "sha512-bC7ElrdJaJnPbAP+1EotYvqZsb3ecl5wi6Bfi6BJTUcNowp6cvspg0jXznRTKDjm/E7AdgFBVeAPVMNcKGsHMA==",
      "requires": {
        "ms": "2.0.0"
      }
    },
    "decompress-response": {
      "version": "6.0.0",
      "resolved": "https://registry.npmjs.org/decompress-response/-/decompress-response-6.0.0.tgz",
      "integrity": "sha512-aW35yZM6Bb/4oJlZncMH2LCoZtJXTRxES17vE3hoRiowU2kWHaJKFkSBDnDR+cm9J+9QhXmREyIfv0pji9ejCQ==",
      "requires": {
        "mimic-response": "^3.1.0"
      },
      "dependencies": {
        "mimic-response": {
          "version": "3.1.0",
          "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-3.1.0.tgz",
          "integrity": "sha512-z0yWI+4FDrrweS8Zmt4Ej5HdJmky15+L2e6Wgn3+iK5fWzb6T3fhNFq2+MeTRb064c6Wr4N/wv0DzQTjNzHNGQ=="
        }
      }
    },
    "defer-to-connect": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/defer-to-connect/-/defer-to-connect-2.0.1.tgz",
      "integrity": "sha512-4tvttepXG1VaYGrRibk5EwJd1t4udunSOVMdLSAL6mId1ix438oPwPZMALY41FCijukO1L0twNcGsdzS7dHgDg=="
    },
    "depd": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/depd/-/depd-2.0.0.tgz",
      "integrity": "sha512-g7nH6P6dyDioJogAAGprGpCtVImJhpPk/roCzdb3fIh61/s/nPsfR6onyMwkCAR/OlC3yBC0lESvUoQEAssIrw=="
    },
    "destroy": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/destroy/-/destroy-1.2.0.tgz",
      "integrity": "sha512-2sJGJTaXIIaR1w4iJSNoN0hnMY7Gpc/n8D4qSCJw8QqFWXf7cuAgnEHxBpweaVcPevC2l3KpjYCx3NypQQgaJg=="
    },
    "ee-first": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/ee-first/-/ee-first-1.1.1.tgz",
      "integrity": "sha512-WMwm9LhRUo+WUaRN+vRuETqG89IgZphVSNkdFgeb6sS/E4OrDIN7t48CAewSHXc6C8lefD8KKfr5vY61brQlow=="
    },
    "encodeurl": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/encodeurl/-/encodeurl-1.0.2.tgz",
      "integrity": "sha512-TPJXq8JqFaVYm2CWmPvnP2Iyo4ZSM7/QKcSmuMLDObfpH5fi7RUGmd/rTDf+rut/saiDiQEeVTNgAmJEdAOx0w=="
    },
    "end-of-stream": {
      "version": "1.4.4",
      "resolved": "https://registry.npmjs.org/end-of-stream/-/end-of-stream-1.4.4.tgz",
      "integrity": "sha512-+uw1inIHVPQoaVuHzRyXd21icM+cnt4CzD5rW+NC1wjOUSTOs+Te7FOv7AhN7vS9x/oIyhLP5PR1H+phQAHu5Q==",
      "requires": {
        "once": "^1.4.0"
      }
    },
    "es5-ext": {
      "version": "0.10.62",
      "resolved": "https://registry.npmjs.org/es5-ext/-/es5-ext-0.10.62.tgz",
      "integrity": "sha512-BHLqn0klhEpnOKSrzn/Xsz2UIW8j+cGmo9JLzr8BiUapV8hPL9+FliFqjwr9ngW7jWdnxv6eO+/LqyhJVqgrjA==",
      "requires": {
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.3",
        "next-tick": "^1.1.0"
      }
    },
    "es6-iterator": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-iterator/-/es6-iterator-2.0.3.tgz",
      "integrity": "sha512-zw4SRzoUkd+cl+ZoE15A9o1oQd920Bb0iOJMQkQhl3jNc03YqVjAhG7scf9C5KWRU/R13Orf588uCC6525o02g==",
      "requires": {
        "d": "1",
        "es5-ext": "^0.10.35",
        "es6-symbol": "^3.1.1"
      }
    },
    "es6-symbol": {
      "version": "3.1.3",
      "resolved": "https://registry.npmjs.org/es6-symbol/-/es6-symbol-3.1.3.tgz",
      "integrity": "sha512-NJ6Yn3FuDinBaBRWl/q5X/s4koRHBrgKAu+yGI6JCBeiu3qrcbJhwT2GeR/EXVfylRk8dpQVJoLEFhK+Mu31NA==",
      "requires": {
        "d": "^1.0.1",
        "ext": "^1.1.2"
      }
    },
    "es6-weak-map": {
      "version": "2.0.3",
      "resolved": "https://registry.npmjs.org/es6-weak-map/-/es6-weak-map-2.0.3.tgz",
      "integrity": "sha512-p5um32HOTO1kP+w7PRnB+5lQ43Z6muuMuIMffvDN8ZB4GcnjLBV6zGStpbASIMk4DCAvEaamhe2zhyCb/QXXsA==",
      "requires": {
        "d": "1",
        "es5-ext": "^0.10.46",
        "es6-iterator": "^2.0.3",
        "es6-symbol": "^3.1.1"
      }
    },
    "escape-html": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/escape-html/-/escape-html-1.0.3.tgz",
      "integrity": "sha512-NiSupZ4OeuGwr68lGIeym/ksIZMJodUGOSCZ/FSnTxcrekbvqrgdUxlJOMpijaKZVjAJrWrGs/6Jy8OMuyj9ow=="
    },
    "etag": {
      "version": "1.8.1",
      "resolved": "https://registry.npmjs.org/etag/-/etag-1.8.1.tgz",
      "integrity": "sha512-aIL5Fx7mawVa300al2BnEE4iNvo1qETxLrPI/o05L7z6go7fCw1J6EQmbK4FmJ2AS7kgVF/KEZWufBfdClMcPg=="
    },
    "event-emitter": {
      "version": "0.3.5",
      "resolved": "https://registry.npmjs.org/event-emitter/-/event-emitter-0.3.5.tgz",
      "integrity": "sha512-D9rRn9y7kLPnJ+hMq7S/nhvoKwwvVJahBi2BPmx3bvbsEdK3W9ii8cBSGjP+72/LnM4n6fo3+dkCX5FeTQruXA==",
      "requires": {
        "d": "1",
        "es5-ext": "~0.10.14"
      }
    },
    "express": {
      "version": "4.18.2",
      "resolved": "https://registry.npmjs.org/express/-/express-4.18.2.tgz",
      "integrity": "sha512-5/PsL6iGPdfQ/lKM1UuielYgv3BUoJfz1aUwU9vHZ+J7gyvwdQXFEBIEIaxeGf0GIcreATNyBExtalisDbuMqQ==",
      "requires": {
        "accepts": "~1.3.8",
        "array-flatten": "1.1.1",
        "body-parser": "1.20.1",
        "content-disposition": "0.5.4",
        "content-type": "~1.0.4",
        "cookie": "0.5.0",
        "cookie-signature": "1.0.6",
        "debug": "2.6.9",
        "depd": "2.0.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "finalhandler": "1.2.0",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "merge-descriptors": "1.0.1",
        "methods": "~1.1.2",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "path-to-regexp": "0.1.7",
        "proxy-addr": "~2.0.7",
        "qs": "6.11.0",
        "range-parser": "~1.2.1",
        "safe-buffer": "5.2.1",
        "send": "0.18.0",
        "serve-static": "1.15.0",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "type-is": "~1.6.18",
        "utils-merge": "1.0.1",
        "vary": "~1.1.2"
      },
      "dependencies": {
        "body-parser": {
          "version": "1.20.1",
          "resolved": "https://registry.npmjs.org/body-parser/-/body-parser-1.20.1.tgz",
          "integrity": "sha512-jWi7abTbYwajOytWCQc37VulmWiRae5RyTpaCyDcS5/lMdtwSz5lOpDE67srw/HYe35f1z3fDQw+3txg7gNtWw==",
          "requires": {
            "bytes": "3.1.2",
            "content-type": "~1.0.4",
            "debug": "2.6.9",
            "depd": "2.0.0",
            "destroy": "1.2.0",
            "http-errors": "2.0.0",
            "iconv-lite": "0.4.24",
            "on-finished": "2.4.1",
            "qs": "6.11.0",
            "raw-body": "2.5.1",
            "type-is": "~1.6.18",
            "unpipe": "1.0.0"
          }
        },
        "raw-body": {
          "version": "2.5.1",
          "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.1.tgz",
          "integrity": "sha512-qqJBtEyVgS0ZmPGdCFPWJ3FreoqvG4MVQln/kCgF7Olq95IbOp0/BWyMwbdtn4VTvkM8Y7khCQ2Xgk/tcrCXig==",
          "requires": {
            "bytes": "3.1.2",
            "http-errors": "2.0.0",
            "iconv-lite": "0.4.24",
            "unpipe": "1.0.0"
          }
        }
      }
    },
    "ext": {
      "version": "1.7.0",
      "resolved": "https://registry.npmjs.org/ext/-/ext-1.7.0.tgz",
      "integrity": "sha512-6hxeJYaL110a9b5TEJSj0gojyHQAmA2ch5Os+ySCiA1QGdS697XWY1pzsrSjqA9LDEEgdB/KypIlR59RcLuHYw==",
      "requires": {
        "type": "^2.7.2"
      },
      "dependencies": {
        "type": {
          "version": "2.7.2",
          "resolved": "https://registry.npmjs.org/type/-/type-2.7.2.tgz",
          "integrity": "sha512-dzlvlNlt6AXU7EBSfpAscydQ7gXB+pPGsPnfJnZpiNJBDj7IaJzQlBZYGdEi4R9HmPdBv2XmWJ6YUtoTa7lmCw=="
        }
      }
    },
    "extract-zip": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/extract-zip/-/extract-zip-2.0.1.tgz",
      "integrity": "sha512-GDhU9ntwuKyGXdZBUgTIe+vXnWj0fppUEtMDL0+idd5Sta8TGpHssn/eusA9mrPr9qNDym6SxAYZjNvCn/9RBg==",
      "requires": {
        "@types/yauzl": "^2.9.1",
        "debug": "^4.1.1",
        "get-stream": "^5.1.0",
        "yauzl": "^2.10.0"
      },
      "dependencies": {
        "debug": {
          "version": "4.3.4",
          "resolved": "https://registry.npmjs.org/debug/-/debug-4.3.4.tgz",
          "integrity": "sha512-PRWFHuSU3eDtQJPvnNY7Jcket1j0t5OuOsFzPPzsekD52Zl8qUfFIPEiswXqIvHWGVHOgX+7G/vCNNhehwxfkQ==",
          "requires": {
            "ms": "2.1.2"
          }
        },
        "ms": {
          "version": "2.1.2",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.2.tgz",
          "integrity": "sha512-sGkPx+VjMtmA6MX27oA4FBFELFCZZ4S4XqeGOXCv68tT+jb3vk/RyaKWP0PTKyWtmLSM0b+adUTEvbs1PEaH2w=="
        }
      }
    },
    "fd-slicer": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/fd-slicer/-/fd-slicer-1.1.0.tgz",
      "integrity": "sha512-cE1qsB/VwyQozZ+q1dGxR8LBYNZeofhEdUNGSMbQD3Gw2lAzX9Zb3uIU6Ebc/Fmyjo9AWWfnn0AUCHqtevs/8g==",
      "requires": {
        "pend": "~1.2.0"
      }
    },
    "fill-range": {
      "version": "7.0.1",
      "resolved": "https://registry.npmjs.org/fill-range/-/fill-range-7.0.1.tgz",
      "integrity": "sha512-qOo9F+dMUmC2Lcb4BbVvnKJxTPjCm+RRpe4gDuGrzkL7mEVl/djYSu2OdQ2Pa302N4oqkSg9ir6jaLWJ2USVpQ==",
      "requires": {
        "to-regex-range": "^5.0.1"
      }
    },
    "finalhandler": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/finalhandler/-/finalhandler-1.2.0.tgz",
      "integrity": "sha512-5uXcUVftlQMFnWC9qu/svkWv3GTd2PfUhK/3PLkYNAe7FbqJMt3515HaxE6eRL74GdsriiwujiawdaB1BpEISg==",
      "requires": {
        "debug": "2.6.9",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "on-finished": "2.4.1",
        "parseurl": "~1.3.3",
        "statuses": "2.0.1",
        "unpipe": "~1.0.0"
      }
    },
    "forwarded": {
      "version": "0.2.0",
      "resolved": "https://registry.npmjs.org/forwarded/-/forwarded-0.2.0.tgz",
      "integrity": "sha512-buRG0fpBtRHSTCOASe6hD258tEubFoRLb4ZNA6NxMVHNw2gOcwHo9wyablzMzOA5z9xA9L1KNjk/Nt6MT9aYow=="
    },
    "fresh": {
      "version": "0.5.2",
      "resolved": "https://registry.npmjs.org/fresh/-/fresh-0.5.2.tgz",
      "integrity": "sha512-zJ2mQYM18rEFOudeV4GShTGIQ7RbzA7ozbU9I/XBpm7kqgMywgmylMwXHxZJmkVoYkna9d2pVXVXPdYTP9ej8Q=="
    },
    "fsevents": {
      "version": "2.3.2",
      "resolved": "https://registry.npmjs.org/fsevents/-/fsevents-2.3.2.tgz",
      "integrity": "sha512-xiqMQR4xAeHTuB9uWm+fFRcIOgKBMiOBP+eXiyT7jsgVCq1bkVygt00oASowB7EdtpOHaaPgKt812P9ab+DDKA==",
      "optional": true
    },
    "function-bind": {
      "version": "1.1.1",
      "resolved": "https://registry.npmjs.org/function-bind/-/function-bind-1.1.1.tgz",
      "integrity": "sha512-yIovAzMX49sF8Yl58fSCWJ5svSLuaibPxXQJFLmBObTuCr0Mf1KiPopGM9NiFjiYBCbfaa2Fh6breQ6ANVTI0A=="
    },
    "get-intrinsic": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/get-intrinsic/-/get-intrinsic-1.2.0.tgz",
      "integrity": "sha512-L049y6nFOuom5wGyRc3/gdTLO94dySVKRACj1RmJZBQXlbTMhtNIgkWkUHq+jYmZvKf14EW1EoJnnjbmoHij0Q==",
      "requires": {
        "function-bind": "^1.1.1",
        "has": "^1.0.3",
        "has-symbols": "^1.0.3"
      }
    },
    "get-stream": {
      "version": "5.2.0",
      "resolved": "https://registry.npmjs.org/get-stream/-/get-stream-5.2.0.tgz",
      "integrity": "sha512-nBF+F1rAZVCu/p7rjzgA+Yb4lfYXrpl7a6VmJrU8wF9I1CKvP/QwPNZHnOlwbTkY6dvtFIzFMSyQXbLoTQPRpA==",
      "requires": {
        "pump": "^3.0.0"
      }
    },
    "glob-parent": {
      "version": "5.1.2",
      "resolved": "https://registry.npmjs.org/glob-parent/-/glob-parent-5.1.2.tgz",
      "integrity": "sha512-AOIgSQCepiJYwP3ARnGx+5VnTu2HBYdzbGP45eLw1vr3zB3vZLeyed1sC9hnbcOc9/SrMyM5RPQrkGz4aS9Zow==",
      "requires": {
        "is-glob": "^4.0.1"
      }
    },
    "got": {
      "version": "11.8.6",
      "resolved": "https://registry.npmjs.org/got/-/got-11.8.6.tgz",
      "integrity": "sha512-6tfZ91bOr7bOXnK7PRDCGBLa1H4U080YHNaAQ2KsMGlLEzRbk44nsZF2E1IeRc3vtJHPVbKCYgdFbaGO2ljd8g==",
      "requires": {
        "@sindresorhus/is": "^4.0.0",
        "@szmarczak/http-timer": "^4.0.5",
        "@types/cacheable-request": "^6.0.1",
        "@types/responselike": "^1.0.0",
        "cacheable-lookup": "^5.0.3",
        "cacheable-request": "^7.0.2",
        "decompress-response": "^6.0.0",
        "http2-wrapper": "^1.0.0-beta.5.2",
        "lowercase-keys": "^2.0.0",
        "p-cancelable": "^2.0.0",
        "responselike": "^2.0.0"
      }
    },
    "has": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has/-/has-1.0.3.tgz",
      "integrity": "sha512-f2dvO0VU6Oej7RkWJGrehjbzMAjFp5/VKPp5tTpWIV4JHHZK1/BxbFRtf/siA2SWTe09caDmVtYYzWEIbBS4zw==",
      "requires": {
        "function-bind": "^1.1.1"
      }
    },
    "has-flag": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/has-flag/-/has-flag-3.0.0.tgz",
      "integrity": "sha512-sKJf1+ceQBr4SMkvQnBDNDtf4TXpVhVGateu0t918bl30FnbE2m4vNLX+VWe/dpjlb+HugGYzW7uQXH98HPEYw=="
    },
    "has-symbols": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/has-symbols/-/has-symbols-1.0.3.tgz",
      "integrity": "sha512-l3LCuF6MgDNwTDKkdYGEihYjt5pRPbEg46rtlmnSPlUbgmB8LOIrKJbYYFBSbnPaJexMKtiPO8hmeRjRz2Td+A=="
    },
    "hpagent": {
      "version": "0.1.2",
      "resolved": "https://registry.npmjs.org/hpagent/-/hpagent-0.1.2.tgz",
      "integrity": "sha512-ePqFXHtSQWAFXYmj+JtOTHr84iNrII4/QRlAAPPE+zqnKy4xJo7Ie1Y4kC7AdB+LxLxSTTzBMASsEcy0q8YyvQ==",
      "optional": true
    },
    "http-cache-semantics": {
      "version": "4.1.1",
      "resolved": "https://registry.npmjs.org/http-cache-semantics/-/http-cache-semantics-4.1.1.tgz",
      "integrity": "sha512-er295DKPVsV82j5kw1Gjt+ADA/XYHsajl82cGNQG2eyoPkvgUhX+nDIyelzhIWbbsXP39EHcI6l5tYs2FYqYXQ=="
    },
    "http-errors": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/http-errors/-/http-errors-2.0.0.tgz",
      "integrity": "sha512-FtwrG/euBzaEjYeRqOgly7G0qviiXoJWnvEH2Z1plBdXgbyjv34pHTSb9zoeHMyDy33+DWy5Wt9Wo+TURtOYSQ==",
      "requires": {
        "depd": "2.0.0",
        "inherits": "2.0.4",
        "setprototypeof": "1.2.0",
        "statuses": "2.0.1",
        "toidentifier": "1.0.1"
      }
    },
    "http2-wrapper": {
      "version": "1.0.3",
      "resolved": "https://registry.npmjs.org/http2-wrapper/-/http2-wrapper-1.0.3.tgz",
      "integrity": "sha512-V+23sDMr12Wnz7iTcDeJr3O6AIxlnvT/bmaAAAP/Xda35C90p9599p0F1eHR/N1KILWSoWVAiOMFjBBXaXSMxg==",
      "requires": {
        "quick-lru": "^5.1.1",
        "resolve-alpn": "^1.0.0"
      }
    },
    "iconv-lite": {
      "version": "0.4.24",
      "resolved": "https://registry.npmjs.org/iconv-lite/-/iconv-lite-0.4.24.tgz",
      "integrity": "sha512-v3MXnZAcvnywkTUEZomIActle7RXXeedOR31wwl7VlyoXO4Qi9arvSenNQWne1TcRwhCL1HwLI21bEqdpj8/rA==",
      "requires": {
        "safer-buffer": ">= 2.1.2 < 3"
      }
    },
    "ignore-by-default": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/ignore-by-default/-/ignore-by-default-1.0.1.tgz",
      "integrity": "sha512-Ius2VYcGNk7T90CppJqcIkS5ooHUZyIQK+ClZfMfMNFEF9VSE73Fq+906u/CWu92x4gzZMWOwfFYckPObzdEbA=="
    },
    "inherits": {
      "version": "2.0.4",
      "resolved": "https://registry.npmjs.org/inherits/-/inherits-2.0.4.tgz",
      "integrity": "sha512-k/vGaX4/Yla3WzyMCvTQOXYeIHvqOKtnqBduzTHpzpQZzAskKMhZ2K+EnBiSM9zGSoIFeMpXKxa4dYeZIQqewQ=="
    },
    "ipaddr.js": {
      "version": "1.9.1",
      "resolved": "https://registry.npmjs.org/ipaddr.js/-/ipaddr.js-1.9.1.tgz",
      "integrity": "sha512-0KI/607xoxSToH7GjN1FfSbLoU0+btTicjsQSWQlh/hZykN8KpmMf7uYwPW3R+akZ6R/w18ZlXSHBYXiYUPO3g=="
    },
    "is-binary-path": {
      "version": "2.1.0",
      "resolved": "https://registry.npmjs.org/is-binary-path/-/is-binary-path-2.1.0.tgz",
      "integrity": "sha512-ZMERYes6pDydyuGidse7OsHxtbI7WVeUEozgR/g7rd0xUimYNlvZRE/K2MgZTjWy725IfelLeVcEM97mmtRGXw==",
      "requires": {
        "binary-extensions": "^2.0.0"
      }
    },
    "is-extglob": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/is-extglob/-/is-extglob-2.1.1.tgz",
      "integrity": "sha512-SbKbANkN603Vi4jEZv49LeVJMn4yGwsbzZworEoyEiutsN3nJYdbO36zfhGJ6QEDpOZIFkDtnq5JRxmvl3jsoQ=="
    },
    "is-glob": {
      "version": "4.0.3",
      "resolved": "https://registry.npmjs.org/is-glob/-/is-glob-4.0.3.tgz",
      "integrity": "sha512-xelSayHH36ZgE7ZWhli7pW34hNbNl8Ojv5KVmkJD4hBdD3th8Tfk9vYasLM+mXWOZhFkgZfxhLSnrwRr4elSSg==",
      "requires": {
        "is-extglob": "^2.1.1"
      }
    },
    "is-number": {
      "version": "7.0.0",
      "resolved": "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
      "integrity": "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng=="
    },
    "is-promise": {
      "version": "2.2.2",
      "resolved": "https://registry.npmjs.org/is-promise/-/is-promise-2.2.2.tgz",
      "integrity": "sha512-+lP4/6lKUBfQjZ2pdxThZvLUAafmZb8OAxFb8XXtiQmS35INgr85hdOGoEs124ez1FCnZJt6jau/T+alh58QFQ=="
    },
    "json-buffer": {
      "version": "3.0.1",
      "resolved": "https://registry.npmjs.org/json-buffer/-/json-buffer-3.0.1.tgz",
      "integrity": "sha512-4bV5BfR2mqfQTJm+V5tPPdf+ZpuhiIvTuAB5g8kcrXOZpTT/QwwVRWBywX1ozr6lEuPdbHxwaJlm9G6mI2sfSQ=="
    },
    "keyv": {
      "version": "4.5.2",
      "resolved": "https://registry.npmjs.org/keyv/-/keyv-4.5.2.tgz",
      "integrity": "sha512-5MHbFaKn8cNSmVW7BYnijeAVlE4cYA/SVkifVgrh7yotnfhKmjuXpDKjrABLnT0SfHWV21P8ow07OGfRrNDg8g==",
      "requires": {
        "json-buffer": "3.0.1"
      }
    },
    "lodash.clonedeep": {
      "version": "4.5.0",
      "resolved": "https://registry.npmjs.org/lodash.clonedeep/-/lodash.clonedeep-4.5.0.tgz",
      "integrity": "sha512-H5ZhCF25riFd9uB5UCkVKo61m3S/xZk1x4wA6yp/L3RFP6Z/eHH1ymQcGLo7J3GMPfm0V/7m1tryHuGVxpqEBQ=="
    },
    "lowercase-keys": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/lowercase-keys/-/lowercase-keys-2.0.0.tgz",
      "integrity": "sha512-tqNXrS78oMOE73NMxK4EMLQsQowWf8jKooH9g7xPavRT706R6bkQJ6DY2Te7QukaZsulxa30wQ7bk0pm4XiHmA=="
    },
    "lru-queue": {
      "version": "0.1.0",
      "resolved": "https://registry.npmjs.org/lru-queue/-/lru-queue-0.1.0.tgz",
      "integrity": "sha512-BpdYkt9EvGl8OfWHDQPISVpcl5xZthb+XPsbELj5AQXxIC8IriDZIQYjBJPEm5rS420sjZ0TLEzRcq5KdBhYrQ==",
      "requires": {
        "es5-ext": "~0.10.2"
      }
    },
    "media-typer": {
      "version": "0.3.0",
      "resolved": "https://registry.npmjs.org/media-typer/-/media-typer-0.3.0.tgz",
      "integrity": "sha512-dq+qelQ9akHpcOl/gUVRTxVIOkAJ1wR3QAvb4RsVjS8oVoFjDGTc679wJYmUmknUF5HwMLOgb5O+a3KxfWapPQ=="
    },
    "memoizee": {
      "version": "0.4.15",
      "resolved": "https://registry.npmjs.org/memoizee/-/memoizee-0.4.15.tgz",
      "integrity": "sha512-UBWmJpLZd5STPm7PMUlOw/TSy972M+z8gcyQ5veOnSDRREz/0bmpyTfKt3/51DhEBqCZQn1udM/5flcSPYhkdQ==",
      "requires": {
        "d": "^1.0.1",
        "es5-ext": "^0.10.53",
        "es6-weak-map": "^2.0.3",
        "event-emitter": "^0.3.5",
        "is-promise": "^2.2.2",
        "lru-queue": "^0.1.0",
        "next-tick": "^1.1.0",
        "timers-ext": "^0.1.7"
      }
    },
    "merge-descriptors": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/merge-descriptors/-/merge-descriptors-1.0.1.tgz",
      "integrity": "sha512-cCi6g3/Zr1iqQi6ySbseM1Xvooa98N0w31jzUYrXPX2xqObmFGHJ0tQ5u74H3mVh7wLouTseZyYIq39g8cNp1w=="
    },
    "methods": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/methods/-/methods-1.1.2.tgz",
      "integrity": "sha512-iclAHeNqNm68zFtnZ0e+1L2yUIdvzNoauKU4WBA3VvH/vPFieF7qfRlwUZU+DA9P9bPXIS90ulxoUoCH23sV2w=="
    },
    "mime": {
      "version": "1.6.0",
      "resolved": "https://registry.npmjs.org/mime/-/mime-1.6.0.tgz",
      "integrity": "sha512-x0Vn8spI+wuJ1O6S7gnbaQg8Pxh4NNHb7KSINmEWKiPE4RKOplvijn+NkmYmmRgP68mc70j2EbeTFRsrswaQeg=="
    },
    "mime-db": {
      "version": "1.52.0",
      "resolved": "https://registry.npmjs.org/mime-db/-/mime-db-1.52.0.tgz",
      "integrity": "sha512-sPU4uV7dYlvtWJxwwxHD0PuihVNiE7TyAbQ5SWxDCB9mUYvOgroQOwYQQOKPJ8CIbE+1ETVlOoK1UC2nU3gYvg=="
    },
    "mime-types": {
      "version": "2.1.35",
      "resolved": "https://registry.npmjs.org/mime-types/-/mime-types-2.1.35.tgz",
      "integrity": "sha512-ZDY+bPm5zTTF+YpCrAU9nK0UgICYPT0QtT1NZWFv4s++TNkcgVaT0g6+4R2uI4MjQjzysHB1zxuWL50hzaeXiw==",
      "requires": {
        "mime-db": "1.52.0"
      }
    },
    "mimic-response": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/mimic-response/-/mimic-response-1.0.1.tgz",
      "integrity": "sha512-j5EctnkH7amfV/q5Hgmoal1g2QHFJRraOtmx0JpIqkxhBhI/lJSl1nMpQ45hVarwNETOoWEimndZ4QK0RHxuxQ=="
    },
    "minimatch": {
      "version": "3.1.2",
      "resolved": "https://registry.npmjs.org/minimatch/-/minimatch-3.1.2.tgz",
      "integrity": "sha512-J7p63hRiAjw1NDEww1W7i37+ByIrOWO5XQQAzZ3VOcL0PNybwpfmV/N05zFAzwQ9USyEcX6t3UO+K5aqBQOIHw==",
      "requires": {
        "brace-expansion": "^1.1.7"
      }
    },
    "ms": {
      "version": "2.0.0",
      "resolved": "https://registry.npmjs.org/ms/-/ms-2.0.0.tgz",
      "integrity": "sha512-Tpp60P6IUJDTuOq/5Z8cdskzJujfwqfOTkrwIwj7IRISpnkJnT6SyJ4PCPnGMoFjC9ddhal5KVIYtAt97ix05A=="
    },
    "negotiator": {
      "version": "0.6.3",
      "resolved": "https://registry.npmjs.org/negotiator/-/negotiator-0.6.3.tgz",
      "integrity": "sha512-+EUsqGPLsM+j/zdChZjsnX51g4XrHFOIXwfnCVPGlQk/k5giakcKsuxCObBRu6DSm9opw/O6slWbJdghQM4bBg=="
    },
    "next-tick": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/next-tick/-/next-tick-1.1.0.tgz",
      "integrity": "sha512-CXdUiJembsNjuToQvxayPZF9Vqht7hewsvy2sOWafLvi2awflj9mOC6bHIg50orX8IJvWKY9wYQ/zB2kogPslQ=="
    },
    "ngrok": {
      "version": "4.3.3",
      "resolved": "https://registry.npmjs.org/ngrok/-/ngrok-4.3.3.tgz",
      "integrity": "sha512-a2KApnkiG5urRxBPdDf76nNBQTnNNWXU0nXw0SsqsPI+Kmt2lGf9TdVYpYrHMnC+T9KhcNSWjCpWqBgC6QcFvw==",
      "requires": {
        "@types/node": "^8.10.50",
        "extract-zip": "^2.0.1",
        "got": "^11.8.5",
        "hpagent": "^0.1.2",
        "lodash.clonedeep": "^4.5.0",
        "uuid": "^7.0.0 || ^8.0.0",
        "yaml": "^1.10.0"
      }
    },
    "nodemon": {
      "version": "2.0.20",
      "resolved": "https://registry.npmjs.org/nodemon/-/nodemon-2.0.20.tgz",
      "integrity": "sha512-Km2mWHKKY5GzRg6i1j5OxOHQtuvVsgskLfigG25yTtbyfRGn/GNvIbRyOf1PSCKJ2aT/58TiuUsuOU5UToVViw==",
      "requires": {
        "chokidar": "^3.5.2",
        "debug": "^3.2.7",
        "ignore-by-default": "^1.0.1",
        "minimatch": "^3.1.2",
        "pstree.remy": "^1.1.8",
        "semver": "^5.7.1",
        "simple-update-notifier": "^1.0.7",
        "supports-color": "^5.5.0",
        "touch": "^3.1.0",
        "undefsafe": "^2.0.5"
      },
      "dependencies": {
        "debug": {
          "version": "3.2.7",
          "resolved": "https://registry.npmjs.org/debug/-/debug-3.2.7.tgz",
          "integrity": "sha512-CFjzYYAi4ThfiQvizrFQevTTXHtnCqWfe7x1AhgEscTz6ZbLbfoLRLPugTQyBth6f8ZERVUSyWHFD/7Wu4t1XQ==",
          "requires": {
            "ms": "^2.1.1"
          }
        },
        "ms": {
          "version": "2.1.3",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
          "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
        }
      }
    },
    "nopt": {
      "version": "1.0.10",
      "resolved": "https://registry.npmjs.org/nopt/-/nopt-1.0.10.tgz",
      "integrity": "sha512-NWmpvLSqUrgrAC9HCuxEvb+PSloHpqVu+FqcO4eeF2h5qYRhA7ev6KvelyQAKtegUbC6RypJnlEOhd8vloNKYg==",
      "requires": {
        "abbrev": "1"
      }
    },
    "normalize-path": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/normalize-path/-/normalize-path-3.0.0.tgz",
      "integrity": "sha512-6eZs5Ls3WtCisHWp9S2GUy8dqkpGi4BVSz3GaqiE6ezub0512ESztXUwUB6C6IKbQkY2Pnb/mD4WYojCRwcwLA=="
    },
    "normalize-url": {
      "version": "6.1.0",
      "resolved": "https://registry.npmjs.org/normalize-url/-/normalize-url-6.1.0.tgz",
      "integrity": "sha512-DlL+XwOy3NxAQ8xuC0okPgK46iuVNAK01YN7RueYBqqFeGsBjV9XmCAzAdgt+667bCl5kPh9EqKKDwnaPG1I7A=="
    },
    "object-inspect": {
      "version": "1.12.3",
      "resolved": "https://registry.npmjs.org/object-inspect/-/object-inspect-1.12.3.tgz",
      "integrity": "sha512-geUvdk7c+eizMNUDkRpW1wJwgfOiOeHbxBR/hLXK1aT6zmVSO0jsQcs7fj6MGw89jC/cjGfLcNOrtMYtGqm81g=="
    },
    "on-finished": {
      "version": "2.4.1",
      "resolved": "https://registry.npmjs.org/on-finished/-/on-finished-2.4.1.tgz",
      "integrity": "sha512-oVlzkg3ENAhCk2zdv7IJwd/QUD4z2RxRwpkcGY8psCVcCYZNq4wYnVWALHM+brtuJjePWiYF/ClmuDr8Ch5+kg==",
      "requires": {
        "ee-first": "1.1.1"
      }
    },
    "once": {
      "version": "1.4.0",
      "resolved": "https://registry.npmjs.org/once/-/once-1.4.0.tgz",
      "integrity": "sha512-lNaJgI+2Q5URQBkccEKHTQOPaXdUxnZZElQTZY0MFUAuaEqe1E+Nyvgdz/aIyNi6Z9MzO5dv1H8n58/GELp3+w==",
      "requires": {
        "wrappy": "1"
      }
    },
    "p-cancelable": {
      "version": "2.1.1",
      "resolved": "https://registry.npmjs.org/p-cancelable/-/p-cancelable-2.1.1.tgz",
      "integrity": "sha512-BZOr3nRQHOntUjTrH8+Lh54smKHoHyur8We1V8DSMVrl5A2malOOwuJRnKRDjSnkoeBh4at6BwEnb5I7Jl31wg=="
    },
    "parseurl": {
      "version": "1.3.3",
      "resolved": "https://registry.npmjs.org/parseurl/-/parseurl-1.3.3.tgz",
      "integrity": "sha512-CiyeOxFT/JZyN5m0z9PfXw4SCBJ6Sygz1Dpl0wqjlhDEGGBP1GnsUVEL0p63hoG1fcj3fHynXi9NYO4nWOL+qQ=="
    },
    "path-to-regexp": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/path-to-regexp/-/path-to-regexp-0.1.7.tgz",
      "integrity": "sha512-5DFkuoqlv1uYQKxy8omFBeJPQcdoE07Kv2sferDCrAq1ohOU+MSDswDIbnx3YAM60qIOnYa53wBhXW0EbMonrQ=="
    },
    "pend": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/pend/-/pend-1.2.0.tgz",
      "integrity": "sha512-F3asv42UuXchdzt+xXqfW1OGlVBe+mxa2mqI0pg5yAHZPvFmY3Y6drSf/GQ1A86WgWEN9Kzh/WrgKa6iGcHXLg=="
    },
    "picomatch": {
      "version": "2.3.1",
      "resolved": "https://registry.npmjs.org/picomatch/-/picomatch-2.3.1.tgz",
      "integrity": "sha512-JU3teHTNjmE2VCGFzuY8EXzCDVwEqB2a8fsIvwaStHhAWJEeVd1o1QD80CU6+ZdEXXSLbSsuLwJjkCBWqRQUVA=="
    },
    "proxy-addr": {
      "version": "2.0.7",
      "resolved": "https://registry.npmjs.org/proxy-addr/-/proxy-addr-2.0.7.tgz",
      "integrity": "sha512-llQsMLSUDUPT44jdrU/O37qlnifitDP+ZwrmmZcoSKyLKvtZxpyV0n2/bD/N4tBAAZ/gJEdZU7KMraoK1+XYAg==",
      "requires": {
        "forwarded": "0.2.0",
        "ipaddr.js": "1.9.1"
      }
    },
    "pstree.remy": {
      "version": "1.1.8",
      "resolved": "https://registry.npmjs.org/pstree.remy/-/pstree.remy-1.1.8.tgz",
      "integrity": "sha512-77DZwxQmxKnu3aR542U+X8FypNzbfJ+C5XQDk3uWjWxn6151aIMGthWYRXTqT1E5oJvg+ljaa2OJi+VfvCOQ8w=="
    },
    "pump": {
      "version": "3.0.0",
      "resolved": "https://registry.npmjs.org/pump/-/pump-3.0.0.tgz",
      "integrity": "sha512-LwZy+p3SFs1Pytd/jYct4wpv49HiYCqd9Rlc5ZVdk0V+8Yzv6jR5Blk3TRmPL1ft69TxP0IMZGJ+WPFU2BFhww==",
      "requires": {
        "end-of-stream": "^1.1.0",
        "once": "^1.3.1"
      }
    },
    "qs": {
      "version": "6.11.0",
      "resolved": "https://registry.npmjs.org/qs/-/qs-6.11.0.tgz",
      "integrity": "sha512-MvjoMCJwEarSbUYk5O+nmoSzSutSsTwF85zcHPQ9OrlFoZOYIjaqBAJIqIXjptyD5vThxGq52Xu/MaJzRkIk4Q==",
      "requires": {
        "side-channel": "^1.0.4"
      }
    },
    "quick-lru": {
      "version": "5.1.1",
      "resolved": "https://registry.npmjs.org/quick-lru/-/quick-lru-5.1.1.tgz",
      "integrity": "sha512-WuyALRjWPDGtt/wzJiadO5AXY+8hZ80hVpe6MyivgraREW751X3SbhRvG3eLKOYN+8VEvqLcf3wdnt44Z4S4SA=="
    },
    "range-parser": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/range-parser/-/range-parser-1.2.1.tgz",
      "integrity": "sha512-Hrgsx+orqoygnmhFbKaHE6c296J+HTAQXoxEF6gNupROmmGJRoyzfG3ccAveqCBrwr/2yxQ5BVd/GTl5agOwSg=="
    },
    "raw-body": {
      "version": "2.5.2",
      "resolved": "https://registry.npmjs.org/raw-body/-/raw-body-2.5.2.tgz",
      "integrity": "sha512-8zGqypfENjCIqGhgXToC8aB2r7YrBX+AQAfIPs/Mlk+BtPTztOvTS01NRW/3Eh60J+a48lt8qsCzirQ6loCVfA==",
      "requires": {
        "bytes": "3.1.2",
        "http-errors": "2.0.0",
        "iconv-lite": "0.4.24",
        "unpipe": "1.0.0"
      }
    },
    "readdirp": {
      "version": "3.6.0",
      "resolved": "https://registry.npmjs.org/readdirp/-/readdirp-3.6.0.tgz",
      "integrity": "sha512-hOS089on8RduqdbhvQ5Z37A0ESjsqz6qnRcffsMU3495FuTdqSm+7bhJ29JvIOsBDEEnan5DPu9t3To9VRlMzA==",
      "requires": {
        "picomatch": "^2.2.1"
      }
    },
    "resolve-alpn": {
      "version": "1.2.1",
      "resolved": "https://registry.npmjs.org/resolve-alpn/-/resolve-alpn-1.2.1.tgz",
      "integrity": "sha512-0a1F4l73/ZFZOakJnQ3FvkJ2+gSTQWz/r2KE5OdDY0TxPm5h4GkqkWWfM47T7HsbnOtcJVEF4epCVy6u7Q3K+g=="
    },
    "responselike": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/responselike/-/responselike-2.0.1.tgz",
      "integrity": "sha512-4gl03wn3hj1HP3yzgdI7d3lCkF95F21Pz4BPGvKHinyQzALR5CapwC8yIi0Rh58DEMQ/SguC03wFj2k0M/mHhw==",
      "requires": {
        "lowercase-keys": "^2.0.0"
      }
    },
    "safe-buffer": {
      "version": "5.2.1",
      "resolved": "https://registry.npmjs.org/safe-buffer/-/safe-buffer-5.2.1.tgz",
      "integrity": "sha512-rp3So07KcdmmKbGvgaNxQSJr7bGVSVk5S9Eq1F+ppbRo70+YeaDxkw5Dd8NPN+GD6bjnYm2VuPuCXmpuYvmCXQ=="
    },
    "safer-buffer": {
      "version": "2.1.2",
      "resolved": "https://registry.npmjs.org/safer-buffer/-/safer-buffer-2.1.2.tgz",
      "integrity": "sha512-YZo3K82SD7Riyi0E1EQPojLz7kpepnSQI9IyPbHHg1XXXevb5dJI7tpyN2ADxGcQbHG7vcyRHk0cbwqcQriUtg=="
    },
    "semver": {
      "version": "5.7.1",
      "resolved": "https://registry.npmjs.org/semver/-/semver-5.7.1.tgz",
      "integrity": "sha512-sauaDf/PZdVgrLTNYHRtpXa1iRiKcaebiKQ1BJdpQlWH2lCvexQdX55snPFyK7QzpudqbCI0qXFfOasHdyNDGQ=="
    },
    "send": {
      "version": "0.18.0",
      "resolved": "https://registry.npmjs.org/send/-/send-0.18.0.tgz",
      "integrity": "sha512-qqWzuOjSFOuqPjFe4NOsMLafToQQwBSOEpS+FwEt3A2V3vKubTquT3vmLTQpFgMXp8AlFWFuP1qKaJZOtPpVXg==",
      "requires": {
        "debug": "2.6.9",
        "depd": "2.0.0",
        "destroy": "1.2.0",
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "etag": "~1.8.1",
        "fresh": "0.5.2",
        "http-errors": "2.0.0",
        "mime": "1.6.0",
        "ms": "2.1.3",
        "on-finished": "2.4.1",
        "range-parser": "~1.2.1",
        "statuses": "2.0.1"
      },
      "dependencies": {
        "ms": {
          "version": "2.1.3",
          "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
          "integrity": "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA=="
        }
      }
    },
    "serve-static": {
      "version": "1.15.0",
      "resolved": "https://registry.npmjs.org/serve-static/-/serve-static-1.15.0.tgz",
      "integrity": "sha512-XGuRDNjXUijsUL0vl6nSD7cwURuzEgglbOaFuZM9g3kwDXOWVTck0jLzjPzGD+TazWbboZYu52/9/XPdUgne9g==",
      "requires": {
        "encodeurl": "~1.0.2",
        "escape-html": "~1.0.3",
        "parseurl": "~1.3.3",
        "send": "0.18.0"
      }
    },
    "setprototypeof": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/setprototypeof/-/setprototypeof-1.2.0.tgz",
      "integrity": "sha512-E5LDX7Wrp85Kil5bhZv46j8jOeboKq5JMmYM3gVGdGH8xFpPWXUMsNrlODCrkoxMEeNi/XZIwuRvY4XNwYMJpw=="
    },
    "side-channel": {
      "version": "1.0.4",
      "resolved": "https://registry.npmjs.org/side-channel/-/side-channel-1.0.4.tgz",
      "integrity": "sha512-q5XPytqFEIKHkGdiMIrY10mvLRvnQh42/+GoBlFW3b2LXLE2xxJpZFdm94we0BaoV3RwJyGqg5wS7epxTv0Zvw==",
      "requires": {
        "call-bind": "^1.0.0",
        "get-intrinsic": "^1.0.2",
        "object-inspect": "^1.9.0"
      }
    },
    "simple-update-notifier": {
      "version": "1.1.0",
      "resolved": "https://registry.npmjs.org/simple-update-notifier/-/simple-update-notifier-1.1.0.tgz",
      "integrity": "sha512-VpsrsJSUcJEseSbMHkrsrAVSdvVS5I96Qo1QAQ4FxQ9wXFcB+pjj7FB7/us9+GcgfW4ziHtYMc1J0PLczb55mg==",
      "requires": {
        "semver": "~7.0.0"
      },
      "dependencies": {
        "semver": {
          "version": "7.0.0",
          "resolved": "https://registry.npmjs.org/semver/-/semver-7.0.0.tgz",
          "integrity": "sha512-+GB6zVA9LWh6zovYQLALHwv5rb2PHGlJi3lfiqIHxR0uuwCgefcOJc59v9fv1w8GbStwxuuqqAjI9NMAOOgq1A=="
        }
      }
    },
    "statuses": {
      "version": "2.0.1",
      "resolved": "https://registry.npmjs.org/statuses/-/statuses-2.0.1.tgz",
      "integrity": "sha512-RwNA9Z/7PrK06rYLIzFMlaF+l73iwpzsqRIFgbMLbTcLD6cOao82TaWefPXQvB2fOC4AjuYSEndS7N/mTCbkdQ=="
    },
    "supports-color": {
      "version": "5.5.0",
      "resolved": "https://registry.npmjs.org/supports-color/-/supports-color-5.5.0.tgz",
      "integrity": "sha512-QjVjwdXIt408MIiAqCX4oUKsgU2EqAGzs2Ppkm4aQYbjm+ZEWEcW4SfFNTr4uMNZma0ey4f5lgLrkB0aX0QMow==",
      "requires": {
        "has-flag": "^3.0.0"
      }
    },
    "timers-ext": {
      "version": "0.1.7",
      "resolved": "https://registry.npmjs.org/timers-ext/-/timers-ext-0.1.7.tgz",
      "integrity": "sha512-b85NUNzTSdodShTIbky6ZF02e8STtVVfD+fu4aXXShEELpozH+bCpJLYMPZbsABN2wDH7fJpqIoXxJpzbf0NqQ==",
      "requires": {
        "es5-ext": "~0.10.46",
        "next-tick": "1"
      }
    },
    "to-regex-range": {
      "version": "5.0.1",
      "resolved": "https://registry.npmjs.org/to-regex-range/-/to-regex-range-5.0.1.tgz",
      "integrity": "sha512-65P7iz6X5yEr1cwcgvQxbbIw7Uk3gOy5dIdtZ4rDveLqhrdJP+Li/Hx6tyK0NEb+2GCyneCMJiGqrADCSNk8sQ==",
      "requires": {
        "is-number": "^7.0.0"
      }
    },
    "toidentifier": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/toidentifier/-/toidentifier-1.0.1.tgz",
      "integrity": "sha512-o5sSPKEkg/DIQNmH43V0/uerLrpzVedkUh8tGNvaeXpfpuwjKenlSox/2O/BTlZUtEe+JG7s5YhEz608PlAHRA=="
    },
    "touch": {
      "version": "3.1.0",
      "resolved": "https://registry.npmjs.org/touch/-/touch-3.1.0.tgz",
      "integrity": "sha512-WBx8Uy5TLtOSRtIq+M03/sKDrXCLHxwDcquSP2c43Le03/9serjQBIztjRz6FkJez9D/hleyAXTBGLwwZUw9lA==",
      "requires": {
        "nopt": "~1.0.10"
      }
    },
    "type": {
      "version": "1.2.0",
      "resolved": "https://registry.npmjs.org/type/-/type-1.2.0.tgz",
      "integrity": "sha512-+5nt5AAniqsCnu2cEQQdpzCAh33kVx8n0VoFidKpB1dVVLAN/F+bgVOqOJqOnEnrhp222clB5p3vUlD+1QAnfg=="
    },
    "type-is": {
      "version": "1.6.18",
      "resolved": "https://registry.npmjs.org/type-is/-/type-is-1.6.18.tgz",
      "integrity": "sha512-TkRKr9sUTxEH8MdfuCSP7VizJyzRNMjj2J2do2Jr3Kym598JVdEksuzPQCnlFPW4ky9Q+iA+ma9BGm06XQBy8g==",
      "requires": {
        "media-typer": "0.3.0",
        "mime-types": "~2.1.24"
      }
    },
    "undefsafe": {
      "version": "2.0.5",
      "resolved": "https://registry.npmjs.org/undefsafe/-/undefsafe-2.0.5.tgz",
      "integrity": "sha512-WxONCrssBM8TSPRqN5EmsjVrsv4A8X12J4ArBiiayv3DyyG3ZlIg6yysuuSYdZsVz3TKcTg2fd//Ujd4CHV1iA=="
    },
    "unpipe": {
      "version": "1.0.0",
      "resolved": "https://registry.npmjs.org/unpipe/-/unpipe-1.0.0.tgz",
      "integrity": "sha512-pjy2bYhSsufwWlKwPc+l3cN7+wuJlK6uz0YdJEOlQDbl6jo/YlPi4mb8agUkVC8BF7V8NuzeyPNqRksA3hztKQ=="
    },
    "utils-merge": {
      "version": "1.0.1",
      "resolved": "https://registry.npmjs.org/utils-merge/-/utils-merge-1.0.1.tgz",
      "integrity": "sha512-pMZTvIkT1d+TFGvDOqodOclx0QWkkgi6Tdoa8gC8ffGAAqz9pzPTZWAybbsHHoED/ztMtkv/VoYTYyShUn81hA=="
    },
    "uuid": {
      "version": "8.3.2",
      "resolved": "https://registry.npmjs.org/uuid/-/uuid-8.3.2.tgz",
      "integrity": "sha512-+NYs2QeMWy+GWFOEm9xnn6HCDp0l7QBD7ml8zLUmJ+93Q5NF0NocErnwkTkXVFNiX3/fpC6afS8Dhb/gz7R7eg=="
    },
    "vary": {
      "version": "1.1.2",
      "resolved": "https://registry.npmjs.org/vary/-/vary-1.1.2.tgz",
      "integrity": "sha512-BNGbWLfd0eUPabhkXUVm0j8uuvREyTh5ovRa/dyow/BqAbZJyC+5fU+IzQOzmAKzYqYRAISoRhdQr3eIZ/PXqg=="
    },
    "wrappy": {
      "version": "1.0.2",
      "resolved": "https://registry.npmjs.org/wrappy/-/wrappy-1.0.2.tgz",
      "integrity": "sha512-l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRcR3QCvezPcQ=="
    },
    "yaml": {
      "version": "1.10.2",
      "resolved": "https://registry.npmjs.org/yaml/-/yaml-1.10.2.tgz",
      "integrity": "sha512-r3vXyErRCYJ7wg28yvBY5VSoAF8ZvlcW9/BwUzEtUsjvX/DKs24dIkuwjtuprwJJHsbyUbLApepYTR1BN4uHrg=="
    },
    "yauzl": {
      "version": "2.10.0",
      "resolved": "https://registry.npmjs.org/yauzl/-/yauzl-2.10.0.tgz",
      "integrity": "sha512-p4a9I6X6nu6IhoGmBqAcbJy1mlC4j27vEPZX9F4L4/vZT3Lyq1VkFHw/V/PUcB9Buo+DG3iHkT0x3Qya58zc3g==",
      "requires": {
        "buffer-crc32": "~0.2.3",
        "fd-slicer": "~1.1.0"
      }
    }
  }
}

```

### addon-examples-main/ui-example/package.json

- Size: 617 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "name": "node-example",
  "version": "0.0.1",
  "description": "Example of a NodeJS clockify addon.",
  "main": "index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "nodemon src/index.js localhost 8080",
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "keywords": [
    "Clockify",
    "Addon",
    "Manifest",
    "Example"
  ],
  "author": "Aleksander Koko",
  "license": "MIT",
  "dependencies": {
    "async-exit-hook": "^2.0.1",
    "body-parser": "^1.20.2",
    "cli-color": "^2.0.3",
    "express": "^4.18.2",
    "ngrok": "5.0.0-beta.2",
    "nodemon": "^2.0.20"
  }
}

```

### addon-examples-main/ui-example/src/config.js

- Size: 122 bytes
- MIME: text/plain; charset=us-ascii

```javascript
module.exports.config = {
  port: process.env.NODE_PORT || 8080,
  ngrok_auth_token: process.env.NGROK_AUTH_TOKEN || ""
}

```

### addon-examples-main/ui-example/src/getPublicUrlFromNgrok.js

- Size: 521 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const ngrok = require('ngrok');
const { config } = require('./config')

module.exports.getPublicUrlFromNgrok = async function() {
  await ngrok.connect({ addr: config.port, authtoken: config.ngrok_auth_token }).catch(err => console.info("ERR"))
  const ngrokApi = ngrok.getApi();
  const tunnels = await ngrokApi.listTunnels()
  const initialPublicUrl = tunnels.tunnels[tunnels.tunnels.length - 1].public_url
  return !initialPublicUrl.startsWith("https") ? initialPublicUrl.replace("http", "https") : initialPublicUrl
}

```

### addon-examples-main/ui-example/src/index.js

- Size: 1947 bytes
- MIME: text/plain; charset=us-ascii

```javascript
const express = require('express')
const bodyParser = require('body-parser')
const { getPublicUrlFromNgrok } = require('./getPublicUrlFromNgrok')
const ngrok = require('ngrok')
const { config } = require('./config')
const manifest = require('./manifest-v0.1.json');
const clc = require("cli-color");

const manifestName = 'manifest-v0.1.json';

;(async () => {
    const publicUrl = await getPublicUrlFromNgrok()

    const manifestPublicUrl =  `${publicUrl}/${manifestName}`
    manifest["baseUrl"] = publicUrl 

    const app = express()

    app.use(bodyParser.json())
    app.use(express.static('static'))

    app.get('/manifest-v0.1.json', (req, res) => {
        res.send(manifest)
    })

    app.post('/lifecycle/installed', (req, res) => {
        console.log(req.body, "installed");
        res.send('got a post request')
    })

    app.post('/lifecycle/uninstalled', (req, res) => {
        console.log(req.body, "uninstalled");
        res.send('got a post request')
    })

    app.post('/lifecycle/settings-updated', (req, res) => {
        console.log(req.body, "settings-updated");
        res.send('got a post request')
    })

    app.listen(config.port, () => {
        // console.log(`app listening on port ${config.port}`)
    })

    console.log('\n\n')
    console.log(clc.magenta('~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'))
    console.log('\n')
    console.log(clc.blue('Manifest is running on:'), clc.green(manifestPublicUrl), '\n')
    console.log(clc.blue("You can add it to your Clockify test instance, available from the \nDeveloper Portal at:"), clc.green('https://developer.marketplace.cake.com/'))
    console.log('\n')
    console.log(clc.magenta('~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'))
    console.log('\n')
})();

process.once('SIGUSR2', async function() {
    await ngrok.kill()
    process.kill(process.pid, 'SIGUSR2');
});




```

### addon-examples-main/ui-example/src/manifest-v0.1.json

- Size: 3874 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "schemaVersion": "1.2",
  "key": "ui-examples",
  "name": "UI examples",
  "description": "Example of every UI entrypoint for an addon",
  "baseUrl": "https://271a-95-107-169-8.eu.ngrok.io",
  "lifecycle": [
    {
      "type": "INSTALLED",
      "path": "/lifecycle/installed"
    },
    {
      "type": "DELETED",
      "path": "/lifecycle/uninstalled"
    },
    {
      "type": "SETTINGS_UPDATED",
      "path": "/lifecycle/settings-updated"
    }
  ],
  "webhooks": [],
  "components": [
    {
      "type": "widget",
      "accessLevel": "EVERYONE",
      "label": "Chat",
      "path": "/chat.html"
    },
    {
      "type": "sidebar",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Sidebar",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "timeoff.tab",
      "accessLevel": "EVERYONE",
      "path": "/chart.html",
      "label": "Monthly Time Offs",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "schedule.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Schedule Tab",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "approvals.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Approvals Tab",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "reports.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Reports Tab",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "activity.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Activity Tab",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "team.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Team Tab",
      "iconPath": "/tab_icon.svg"
    },
    {
      "type": "projects.tab",
      "accessLevel": "EVERYONE",
      "path": "/",
      "label": "Projects Tab",
      "iconPath": "/tab_icon.svg"
    }
  ],
  "settings": {
    "tabs": [
      {
        "id": "settings",
        "name": "settings",
        "settings": [
          {
            "id": "addon-dropdown-single-setting-1",
            "name": "Dropdown single setting",
            "accessLevel": "EVERYONE",
            "type": "DROPDOWN_SINGLE",
            "value": "option 1",
            "allowedValues": [
              "option 1",
              "option 2",
              "option 3"
            ]
          },
          {
            "id": "addon-dropdown-multiple-setting-1",
            "name": "Dropdown multiple setting",
            "accessLevel": "ADMINS",
            "type": "DROPDOWN_MULTIPLE",
            "value": [
              "option 1",
              "option 2"
            ],
            "allowedValues": [
              "option 1",
              "option 2",
              "option 3"
            ]
          }
        ],
        "groups": [
          {
            "id": "addon-settings-group-1",
            "title": "Addon settings group 1",
            "header": {
              "title": "Addon settings"
            },
            "description": "Addon settings group 1",
            "settings": [
              {
                "id": "addon-txt-setting",
                "name": "Txt setting",
                "accessLevel": "ADMINS",
                "type": "TXT",
                "value": "Some text"
              },
              {
                "id": "addon-link-setting",
                "name": "Link setting",
                "accessLevel": "EVERYONE",
                "type": "LINK",
                "value": "https://clockify.me"
              },
              {
                "id": "addon-number-setting",
                "name": "Number setting",
                "accessLevel": "EVERYONE",
                "type": "NUMBER",
                "value": 5
              }
            ]
          }
        ]
      }
    ]
  },
  "minimalSubscriptionPlan": "FREE",
  "scopes": []
}

```

### addon-examples-main/ui-example/src/routes.js

- Size: 0 bytes
- MIME: inode/x-empty; charset=binary

```text
[binary omitted]
Path: addon-examples-main/ui-example/src/routes.js
MIME: inode/x-empty; charset=binary
Size: 0 bytes
```

### addon-examples-main/ui-example/static/chart.html

- Size: 2397 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Index</title>
    <link rel="stylesheet" href="https://resources.developer.clockify.me/ui/latest/css/main.min.css" />
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .canvas-container {
            height: 400px;
            background-color: #1D272C;
            display: flex;
            justify-content: center;
            padding-top: 60px;
            padding-bottom: 60px;
        }
    </style>
</head>

<body>
    <div class="canvas-container">
        <canvas id="top-time-off-chart"></canvas>
    </div>
    <script>
        window.addEventListener("load", () => {
            const ctx = document.getElementById('top-time-off-chart');
            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: ['Employee 1', 'Employee 2', 'Employee 3', 'Employee 4', 'Employee 5'],
                    datasets: [{
                        label: '# of Day Off',
                        backgroundColor: '#f44336',
                        data: [11, 8, 5, 3, 3, 2],
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true,
                            border: {
                                display: false
                            },
                            grid: {
                                color: '#4A5963'
                            }
                        },
                        x: {
                            border: {
                                display: false
                            },
                            grid: {
                                display: false,
                            },
                            borderDash: [5, 5],
                            ticks: {
                                color: '#C8D1D9',
                                minRotation: 30,
                            }
                        }
                    },
                    plugins: {
                        legend: {
                            display: false
                        }
                    }
                }
            });
        });

    </script>
</body>
</html>

```

### addon-examples-main/ui-example/static/chat.html

- Size: 3563 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Index</title>
    <link rel="stylesheet" href="https://resources.developer.clockify.me/ui/latest/css/main.min.css" />
    <style>
      html, body {
        margin: 0;
        padding: 0;
      }

      .chat-container {
        display: flex;
        flex-direction: column;
        position: relative;
        height: 100%;
      }

      input, div, span {
        color: #2f2f2f;
      }

      .chat--header {
        height: 40px;
        border-bottom: 1px solid #cfcfcf;
        color: #2f2f2f;
        display: flex;
        align-items: center;
        position: fixed;
        top: 0;
        left: 0;
        width: calc(100% - 20px);
        background-color: white;
        padding: 0 10px;
      }

      .chat--header .chat--header-title {
        font-size: 20px;
        font-weight: 600;
      }

      .chat--header .chat--header-subtitle {
        font-size: 16px;
        margin-left: 22px;
        margin-top: 2px;
      }

      .chat--messages {
        margin-top: 40px;
        margin-bottom: 40px;
        display: flex;
        flex-direction: column;
        /* overflow-y: scroll; */
        padding: 10px;
      }

      .chat--messages .message-user, .chat--messages .message {
        margin-top: 10px;
      }

      .chat--messages .message-user {
        display: flex;
        align-items: center;
      }

      .chat--messages .message-user img {
        border-radius: 15px;
        overflow: hidden;
      }

      .chat--messages .message-user span {
        margin-top: 2px;
        margin-left: 12px;
      }

      .chat--messages .message {
        padding: 10px;
        background-color: #e2e2e2;
        border-radius: 10px;
        width: fit-content;
      }

      .chat--messages .message-user-right, .chat--messages .message-right {
        align-self: end;
      }

      .chat--input {
        position: fixed;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 40px;
        display: flex;
        align-items: center;
        border-top: 1px solid #cfcfcf;
        background: white;
      }

      .chat--input input {
        padding: 0 10px;
        height: 30px;
        background-color: #d2d2d2;
        border: none;
        width: calc(100% - 80px);
        margin-left: 10px;
        border-radius: 5px;
        overflow: hidden;
      }

      .chat--input svg {
        margin-left: 10px;
      }

      .chat--input svg:hover {
        cursor: pointer;
        user-select: none;
      }
    </style>
</head>

<body>
  <div class="chat-container">
    <div class="chat--header">
      <span class="chat--header-title">Support chat</span>
      <span class="chat--header-subtitle">Active 5min ago</span>
    </div>
    <div class="chat--messages">
      <div class="message-user"><img src="https://robohash.org/aleksander" widht="30" height="30" /><span>Alex</span></div>
      <div class="message">Hello!</div>
      <div class="message-user message-user-right"><img src="https://robohash.org/ljuba" width="30" height="30" /><span>Support</span></div>
      <div class="message message-right">Hi :)</div>
      <div class="message message-right">How are you?</div>
    </div>
    <div class="chat--input">
      <input type="text" /><svg style="width:24px;height:24px" viewBox="0 0 24 24">
        <path fill="#006ae3" d="M2,21L23,12L2,3V10L17,12L2,14V21Z"></path>
      </svg>
    </div>
  </div>
</body>
</html>
```

### addon-examples-main/ui-example/static/index.html

- Size: 5657 bytes
- MIME: text/html; charset=us-ascii

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Index</title>
    <link rel="stylesheet" href="https://resources.developer.clockify.me/ui/latest/css/main.min.css" />
</head>

<body>
    <div class="tabs">
        <div class="tabs-header">
            <div class="tab-header active" data-tab="tab1">Alert</div>
            <div class="tab-header" data-tab="tab2">Checkbox</div>
            <div class="tab-header" data-tab="tab3">Input</div>
            <div class="tab-header" data-tab="tab4">Radio</div>
            <div class="tab-header" data-tab="tab5">Select</div>
            <div class="tab-header" data-tab="tab6">Switch</div>
        </div>
        <div class="tabs-content">
            <div class="tab-content tab1 active">
                <div class="alert alert-info">Alert Info</div>
                <div class="alert alert-success mt-4">Alert Success</div>
                <div class="alert alert-danger mt-4">Alert Danger</div>
                <div class="alert alert-warning mt-4">Alert Warning</div>
            </div>
            <div class="tab-content tab2">
                <div class="checkbox-container">
                    <input type="checkbox" class="checkbox" name="activate_users" id="activate_users" />
                    <label for="activate_users">Default Checkbox</label>
                </div>
                <div class="mt-4">
                    <div class="checkbox-container mt-4">
                        <input type="checkbox" class="checkbox" disabled name="activate_users_2" id="activate_users_2" />
                        <label for="activate_users_2">Disabled Checkbox</label>
                    </div>
                </div>
                <div class="mt-4">
                    <div class="checkbox-container">
                        <input type="checkbox" class="checkbox" disabled checked name="activate_users_3" id="activate_users_3" />
                        <label for="activate_users_2">Disabled and Checked Checkbox</label>
                    </div>
                </div>
            </div>
            <div class="tab-content tab3">
                <div>
                    <input type="text" placeholder="a placeholder" />
                </div>
                <div class="mt-4">
                    <input type="text" placeholder="a placeholder" value="success input" class="success" />
                </div>
                <div class="mt-4">
                    <input type="text" placeholder="a placeholder" value="warning input" class="warning" />
                </div>
                <div class="mt-4">
                    <input type="text" placeholder="a placeholder" value="error input" class="error" />
                </div>
                <div class="mt-4">
                    <input type="text" placeholder="a placeholder" value="disabled input" disabled />
                </div>
                <div class="mt-4">
                    <input type="text" placeholder="a placeholder" value="readonly input" readonly />
                </div>
            </div>
            <div class="tab-content tab4">
                <div style="display: flex;">
                    <div class="radio-group">
                        <input type="radio" name="checkbox-group" class="radio" value="1" id="radio_1" checked />
                        <label for="radio_1">Radio 1</label>
                    </div>
                    <div class="radio-group ml-4">
                        <input type="radio" name="checkbox-group" class="radio" value="1" id="radio_2"/>
                        <label for="radio_2">Radio 2</label>
                    </div>
                </div>
                <div style="display: flex;" class="mt-4">
                    <div class="radio-group">
                        <input type="radio" name="checkbox-group" class="radio" value="1" id="radio_3" disabled />
                        <label for="radio_3">Disabled Radio</label>
                    </div>
                    <div class="radio-group ml-4">
                        <input type="radio" name="checkbox-group" class="radio" value="1" id="radio_4"/>
                        <label for="radio_4">Radio 4</label>
                    </div>
                </div>
            </div>
            <div class="tab-content tab5">
                <select class="select">
                    <option value="" disabled selected hidden>Select some option</option>
                    <option value="1">Option 1</option>
                    <option value="2">Option 2</option>
                    <option value="3">Option 3</option>
                </select>
                <div class="mt-4">
                    <select class="select" disabled>
                        <option value="" disabled selected hidden>Disabled Select</option>
                        <option value="1">Option 1</option>
                        <option value="2">Option 2</option>
                        <option value="3">Option 3</option>
                    </select>
                </div>
            </div>
            <div class="tab-content tab6">
                <div>
                    <label class="switch">
                        <input type="checkbox" id="switch_1" />
                        <span class="slider round"></span>
                    </label>
                    <span class="switch-label">Some Label</span>
                </div>
            </div>
        </div>
    </div>
    <script src="https://resources.developer.clockify.me/ui/latest/js/main.min.js"></script>
</body>
</html>

```

### addon-examples-main/ui-example/static/tab_icon.svg

- Size: 250 bytes
- MIME: image/svg+xml; charset=us-ascii

```
<svg xmlns="http://www.w3.org/2000/svg" height="48" width="48"><path d="M7 40q-1.25 0-2.125-.875T4 37V11q0-1.25.875-2.125T7 8h34q1.25 0 2.125.875T44 11v26q0 1.25-.875 2.125T41 40Zm0-3h3.5V11H7v26Zm6.5 0h21V11h-21Zm24 0H41V11h-3.5Zm-24-26v26Z"/></svg>
```

### addon-examples-main/weather-example-serverless/README.md

- Size: 1311 bytes
- MIME: text/plain; charset=us-ascii

```markdown
# Weather addon example

This is an example of the simplest way of creating an addon. It has front end code and it does not use any back end code.
The example contains a html file, manifest.json and the icon.
We have used python to serve files and ngrok to forward our inner port to a public domain.

## Installation
Pull the code from git and open the HTML file or serve it with any web server.

## Run addon locally
1. Install python on your local machine
2. Go to the root folder of the project
3. Use one liners [python-http](https://gist.github.com/willurd/5720255)

## Set up Ngrok
1. Create free account at [ngrok](https://ngrok.com/)
2. Follow their guideline for installing & running app [docs](https://ngrok.com/docs/getting-started)
3. After generating the url get the new url domain and add it to our manifest.json file

## Open-meteo API
This app uses data from open-meteo, a free open-source Weather API. \
You can read their API [open-meteo](https://open-meteo.com/en/docs)

## Try it out
1. Create a CAKE.com [developer account](https://developer.marketplace.cake.com/signup)
1. Login to your test Clockify instance, navigate to Workspace settings > Integrations
1. Install the add-on through the following URL: https://resources.developer.clockify.me/integration-examples/weather/manifest.json

```

### addon-examples-main/weather-example-serverless/icon.png

- Size: 2885 bytes
- MIME: image/png; charset=binary

```text
[binary omitted]
Path: addon-examples-main/weather-example-serverless/icon.png
MIME: image/png; charset=binary
Size: 2885 bytes
```

### addon-examples-main/weather-example-serverless/index.html

- Size: 5154 bytes
- MIME: text/html; charset=utf-8

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <title>Weather App</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f1f1f1;
        }

        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }

        h1 {
            text-align: center;
        }

        table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 20px;
        }

        th {
            background-color: #ddd;
            font-weight: bold;
            padding: 10px;
            text-align: center;
        }

        td {
            padding: 10px;
            text-align: center;
            border: 1px solid #ddd;
        }

        .error {
            color: red;
            font-weight: bold;
            text-align: center;
        }
    </style>
</head>
<body>
<div class="container">
    <h1>Weather App</h1>
    <h3 id="user_name"></h3>
    <table>
        <thead>
        <tr>
            <th>Date</th>
            <th>Min Temperature</th>
            <th>Max Temperature</th>
        </tr>
        </thead>
        </thead>
        <tbody id="weather-data"></tbody>
    </table>
    <p id="error-message" class="error"></p>
</div>

<script>
    const FORECAST_API = 'https://api.open-meteo.com/v1/forecast';
    const GEOLOCATION_API = 'https://geocoding-api.open-meteo.com/v1/search'
    const parseJwt = (token) => {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    }

    const getLatLng = async (timeZone) => {
        // New York City coordinates
        const DEFAULT_LAT = 40.73061;
        const DEFAULT_LNG = -73.935242;
        try {
            let cityName = timeZone.split('/')[1];
            if (cityName === 'UTC') cityName = 'Greenwich';
            const response = await fetch(`${GEOLOCATION_API}?name=${cityName}`);
            const data = await response.json();
            if (data.results && data.results.length > 0) {
                return { lat: data.results[0].latitude, lng: data.results[0].longitude }
            }

            throw new Error('No results');
        } catch (e) {
            return { lat: DEFAULT_LAT, lng: DEFAULT_LNG }
        }
    }

    window.addEventListener("load", () => {
        const errorMessageElement = document.getElementById("error-message");
        const params = new Proxy(new URLSearchParams(window.location.search), {
            get: (searchParams, prop) => searchParams.get(prop),
        });
        const userAuthKey = params.auth_token;

        if (!userAuthKey) {
            errorMessageElement.append('User was not found');
        } else {
            const decodedToken = parseJwt(userAuthKey);
            const getUserApiUrl = `${decodedToken.backendUrl}/addon/workspaces/${decodedToken.workspaceId}/user`;
            const headers = new Headers();
            headers.append('x-addon-token', userAuthKey);
            fetch(getUserApiUrl, { headers })
                .then((res) => {
                    res.json()
                        .then(async (data) => {
                            document.getElementById('user_name').append(`Hello ${data.name}`);
                            const coords = await getLatLng(data.timeZone);
                            getWeatherData(coords)
                        })
                        .catch(() => errorMessageElement.append('Could not get user data'))
                })
                .catch(() => errorMessageElement.append('Could not get user data'))
        }
        const getWeatherData = (coords) => {
            fetch(`${FORECAST_API}?latitude=${coords.lat}&longitude=${coords.lng}&daily=temperature_2m_min,temperature_2m_max&timezone=auto`)
                .then(response => response.json())
                .then(data => {
                    const weatherData = data.daily;
                    const tableBody = document.getElementById('weather-data');

                    weatherData.time.forEach((time, index) => {
                        const minTemp = weatherData.temperature_2m_min[index];
                        const maxTemp = weatherData.temperature_2m_max[index];
                        const tableRow = document.createElement('tr');
                        tableRow.innerHTML = `
            <td>${time}</td>
            <td>${minTemp}°C</td>
            <td>${maxTemp} °C</td>
          `;

                        tableBody.appendChild(tableRow);
                    });
                })
                .catch(() => {
                    errorMessageElement.append('Sorry, something went wrong. Please try again later.')
                });
        }
    });
</script>
</body>
</html>
```

### addon-examples-main/weather-example-serverless/manifest.json

- Size: 466 bytes
- MIME: text/plain; charset=us-ascii

```json
{
  "schemaVersion": "1.2",
  "key": "wather-addon",
  "name": "Weather Addon",
  "description": "A sample addon that renders a UI component",
  "baseUrl": "https://271a-95-107-169-8.eu.ngrok.io",
  "lifecycle": [
  ],
  "webhooks": [],
  "components": [
    {
      "type": "widget",
      "accessLevel": "EVERYONE",
      "path": "/",
      "iconPath": "/icon.png",
      "label": "Weather Widget"
    }
  ],
  "minimalSubscriptionPlan" : "FREE",
  "scopes": []
}

```

### addon-java-sdk-main/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/.github/workflows/publish-processor.yml

- Size: 645 bytes
- MIME: text/plain; charset=us-ascii

```yaml
name: Publish annotation processor

on:
  pull_request:
    branches: [ "main" ]
    types: [ closed ]
    paths:
      - annotation-processor/**

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 18
      uses: actions/setup-java@v3
      with:
        java-version: '18'
        distribution: 'temurin'
    - name: Configure maven
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      run: ./configure-maven.sh ${GITHUB_TOKEN} ${GITHUB_REPOSITORY}
    - name: Publish annotation processor
      run: |
        cd $GITHUB_WORKSPACE/annotation-processor
        mvn deploy

```

### addon-java-sdk-main/.github/workflows/publish-sdk.yml

- Size: 739 bytes
- MIME: text/plain; charset=us-ascii

```yaml
name: Publish SDK

on:
  pull_request:
    branches: [ "main" ]
    types: [ closed ]
    paths:
      - addon-sdk/**

jobs:
  build:
    if: github.event.pull_request.merged == true
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 18
      uses: actions/setup-java@v3
      with:
        java-version: '18'
        distribution: 'temurin'
    - name: Configure maven
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      run: ./configure-maven.sh ${GITHUB_TOKEN} ${GITHUB_REPOSITORY}
    - name: Run SDK publishing script
      run: |
        cd $GITHUB_WORKSPACE/addon-sdk
        ./publish.sh ${{ secrets.GITHUB_TOKEN }} "${{ toJson(github.event.pull_request.labels.*.name) }}"

```

### addon-java-sdk-main/addon-sdk/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/pom.xml

- Size: 6395 bytes
- MIME: text/xml; charset=us-ascii

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.cake.clockify</groupId>
    <artifactId>addon-sdk</artifactId>
    <version>1.5.3</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <javax.servlet-api.version>4.0.1</javax.servlet-api.version>
        <jetty.version>11.0.22</jetty.version>
        <gson.version>2.10.1</gson.version>
        <jsonwebtoken.version>0.11.5</jsonwebtoken.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>com.cake.clockify</groupId>
            <artifactId>addon-sdk-annotation-processor</artifactId>
            <version>1.0.10</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.26</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
            <version>${jetty.version}</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
            <version>${jetty.version}</version>
        </dependency>

        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jsonwebtoken.version}</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jsonwebtoken.version}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-gson</artifactId>
            <version>${jsonwebtoken.version}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.9.1</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.5.0</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>build-helper-maven-plugin</artifactId>
                    <version>3.2.0</version>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>versions-maven-plugin</artifactId>
                    <version>2.9.0</version>
                </plugin>
            </plugins>
        </pluginManagement>
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
            <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>versions-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>patch</id>
                        <goals>
                            <goal>set</goal>
                        </goals>
                        <configuration>
                            <generateBackupPoms>false</generateBackupPoms>
                            <newVersion>
                                ${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}
                            </newVersion>
                        </configuration>
                    </execution>
                    <execution>
                        <id>minor</id>
                        <goals>
                            <goal>set</goal>
                        </goals>
                        <configuration>
                            <generateBackupPoms>false</generateBackupPoms>
                            <newVersion>
                                ${parsedVersion.majorVersion}.${parsedVersion.nextMinorVersion}.0
                            </newVersion>
                        </configuration>
                    </execution>
                    <execution>
                        <id>major</id>
                        <goals>
                            <goal>set</goal>
                        </goals>
                        <configuration>
                            <generateBackupPoms>false</generateBackupPoms>
                            <newVersion>
                                ${parsedVersion.nextMajorVersion}.0.0
                            </newVersion>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>default-cli</id>
                        <goals>
                            <goal>parse-version</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <distributionManagement>
        <repository>
            <id>github</id>
            <url>https://maven.pkg.github.com/clockify/addon-java-sdk</url>
        </repository>
    </distributionManagement>
</project>

```

### addon-java-sdk-main/addon-sdk/publish.sh

- Size: 524 bytes
- MIME: text/x-shellscript; charset=us-ascii

```sh
#!/bin/bash

VERSION_TYPE="patch"

if [ $# != 2 ]; then
  echo "Required parameters: githubAccessToken pullRequestLabels"
  exit 1
fi

case "$2" in
*patch*)
  VERSION_TYPE="patch"
  ;;
*minor*)
  VERSION_TYPE="minor"
  ;;
*major*)
  VERSION_TYPE="major"
  ;;
esac

mvn build-helper:parse-version versions:set@$VERSION_TYPE
git add pom.xml
git -c user.name="Clockify Bot" -c user.email="clockify-bot@clockify.me" commit -m "$VERSION_TYPE version bump"
git push https://"$1"@github.com/clockify/addon-java-sdk.git
mvn deploy


```

### addon-java-sdk-main/addon-sdk/src/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/java/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/com/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/java/com/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/java/com/cake/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/.DS_Store

- Size: 6148 bytes
- MIME: application/octet-stream; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/.DS_Store
MIME: application/octet-stream; charset=binary
Size: 6148 bytes
```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/ClockifyAddon.java

- Size: 1275 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.clockify;

import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.ClockifyResource;
import com.cake.clockify.addonsdk.shared.Addon;
import com.cake.clockify.addonsdk.shared.RequestHandler;
import lombok.NonNull;

public class ClockifyAddon extends Addon<ClockifyManifest> {
    public ClockifyAddon(@NonNull ClockifyManifest manifest) {
        super(manifest);
    }

    public void registerWebhook(ClockifyResource webhook, RequestHandler handler) {
        registerHandler(webhook.getPath(), HTTP_POST, handler);
        manifest.getWebhooks().add(webhook);
    }

    public void registerLifecycleEvent(ClockifyResource lifecycleEvent, RequestHandler handler) {
        registerHandler(lifecycleEvent.getPath(), HTTP_POST, handler);
        manifest.getLifecycle().add(lifecycleEvent);
    }

    public void registerComponent(ClockifyResource component, RequestHandler handler) {
        registerHandler(component.getPath(), HTTP_GET, handler);
        manifest.getComponents().add(component);
    }

    public void registerCustomSettings(String path, RequestHandler handler) {
        registerHandler(path, HTTP_GET, handler);
        manifest.setSettings(path);
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/ClockifySignatureParser.java

- Size: 1419 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.clockify;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

public class ClockifySignatureParser {
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_BACKEND_URL = "backendUrl";
    public static final String CLAIM_PTO_URL = "ptoUrl";
    public static final String CLAIM_REPORTS_URL = "reportsUrl";
    public static final String CLAIM_WORKSPACE_ID = "workspaceId";
    public static final String CLAIM_ADDON_ID = "addonId";
    public static final String CLAIM_USER_ID = "user";
    public static final String CLAIM_WORKSPACE_ROLE = "workspaceRole";

    public static final String ISSUER = "clockify";
    public static final String ADDON = "addon";
    private final JwtParser parser;

    /**
     * @param addonKey  the key declared inside the addon manifest
     * @param publicKey the RSA256 public key
     */
    public ClockifySignatureParser(String addonKey, RSAPublicKey publicKey) {
        this.parser = Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .requireSubject(addonKey)
                .require(CLAIM_TYPE, ADDON)
                .setSigningKey(publicKey)
                .build();
    }

    public Map<String, Object> parseClaims(String token) {
        return parser.parseClaimsJws(token).getBody();
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/model/ClockifyManifest.java

- Size: 994 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.clockify.model;

import com.cake.clockify.annotationprocessor.clockify.ExtendClockifyManifest;

import java.util.List;

@ExtendClockifyManifest
public interface ClockifyManifest {

    String getSchemaVersion();
    String getKey();

    List getLifecycle();

    List getWebhooks();

    List getComponents();

    void setSettings(Object settings);

    static com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyManifestBuilderKeyStep v1_2Builder() {
        return com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyManifest.builder();
    }

    static com.cake.clockify.addonsdk.clockify.model.v1_3.ClockifyManifestBuilderKeyStep v1_3Builder() {
        return com.cake.clockify.addonsdk.clockify.model.v1_3.ClockifyManifest.builder();
    }

    static com.cake.clockify.addonsdk.clockify.model.v1_4.ClockifyManifestBuilderKeyStep v1_4Builder() {
        return com.cake.clockify.addonsdk.clockify.model.v1_4.ClockifyManifest.builder();
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/model/ClockifyResource.java

- Size: 112 bytes
- MIME: text/plain; charset=us-ascii

```java
package com.cake.clockify.addonsdk.clockify.model;

public interface ClockifyResource {
    String getPath();
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/Addon.java

- Size: 3866 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared;

import com.cake.clockify.addonsdk.shared.utils.Utils;
import com.cake.clockify.addonsdk.shared.utils.ValidationUtils;
import com.google.gson.Gson;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class Addon<M> implements RequestHandler {
    public static final String PATH_MANIFEST = "/manifest";

    private static final String ERROR_PATH_ALREADY_REGISTERED = "Handler has already been registered.";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    @Getter
    protected final M manifest;

    protected final Gson gson;

    private final Map<Request, RequestHandler> requestHandlers = new HashMap<>();
    private final List<Filter> filters = new LinkedList<>();

    protected Addon(@NonNull M manifest) {
        this(manifest, PATH_MANIFEST);
    }

    protected Addon(@NonNull M manifest, String manifestPath) {
        this.gson = new Gson();
        this.manifest = manifest;

        registerHandler(manifestPath, HTTP_GET, (request, response) -> {
            gson.toJson(manifest, response.getWriter());
            response.setStatus(HttpStatus.OK_200);
        });
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = Utils.trimTrailingSlash(request.getRequestURI());
            String method = request.getMethod();

            RequestHandler handler = requestHandlers.get(new Request(path, method));
            if (handler != null) {
                new RequestExecutor(handler, filters).doFilter(request, response);
                return;
            }

            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    public synchronized void registerHandler(String path, String method, RequestHandler handler) {
        if (!ValidationUtils.isValidManifestPath(path)) {
            throw new ValidationException("Url should be an absolute path and not end with a slash.");
        }

        Request key = new Request(path, method);

        if (requestHandlers.containsKey(key)) {
            throw new IllegalArgumentException(ERROR_PATH_ALREADY_REGISTERED);
        }

        requestHandlers.put(key, handler);
    }

    public List<Request> getRegisteredRequests() {
        return requestHandlers.keySet().stream().toList();
    }

    public synchronized void addFilter(Filter filter) {
        filters.add(filter);
    }

    public record Request(String path, String method) {
    }

    private static class RequestExecutor implements FilterChain {
        private final LinkedList<Filter> filters;
        private final RequestHandler handler;

        public RequestExecutor(RequestHandler handler, List<Filter> filters) {
            this.filters = new LinkedList<>(filters);
            this.handler = handler;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
            if (filters.isEmpty()) {
                handler.handle((HttpServletRequest) request, (HttpServletResponse) response);
            } else {
                filters.pop().doFilter(request, response, this);
            }
        }
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/AddonServlet.java

- Size: 554 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AddonServlet extends HttpServlet {

    private final Addon<?> addon;

    public AddonServlet(Addon<?> addon) {
        this.addon = addon;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addon.handle(req, resp);
        resp.flushBuffer();
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/EmbeddedServer.java

- Size: 1056 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared;

import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@RequiredArgsConstructor
public class EmbeddedServer {
    private final AddonServlet servlet;
    private Server server;
    private boolean started = false;

    public synchronized void start(int port) throws Exception {
        start(port, "/*");
    }

    public synchronized void start(int port, String pathSpec) throws Exception {
        if (started) {
            return;
        }

        server = new Server(port);

        var handler = new ServletContextHandler();
        server.setHandler(handler);
        handler.addServlet(new ServletHolder(servlet), pathSpec);

        server.start();
        server.join();

        started = true;
    }

    public synchronized void stop() throws Exception {
        if (!started) {
            return;
        }

        server.stop();
        started = false;
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/RequestHandler.java

- Size: 322 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@FunctionalInterface
public interface RequestHandler {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/ValidationException.java

- Size: 227 bytes
- MIME: text/plain; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared;

public class ValidationException extends RuntimeException {
    public ValidationException() {
    }

    public ValidationException(String message) {
        super(message);
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/response/HttpResponse.java

- Size: 1219 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class HttpResponse {
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_METHOD_NOT_ALLOWED = 405;
    public static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    @Builder.Default
    private int statusCode = 200;
    @Builder.Default
    private String contentType = DEFAULT_CONTENT_TYPE;
    @Builder.Default
    private Map<String, String> headers = new HashMap<>(0);
    private String body;

    public static class HttpResponseBuilder {
        public HttpResponseBuilder success() {
            return statusCode(STATUS_SUCCESS);
        }

        public HttpResponseBuilder methodNotAllowed() {
            return statusCode(STATUS_METHOD_NOT_ALLOWED);
        }

        public HttpResponseBuilder internalServerError() {
            return statusCode(STATUS_INTERNAL_SERVER_ERROR);
        }
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/utils/Utils.java

- Size: 371 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    public static String trimTrailingSlash(String s) {
        if (s.charAt(s.length() - 1) == '/') {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}

```

### addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/utils/ValidationUtils.java

- Size: 467 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.addonsdk.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URL;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationUtils {
    public static boolean isValidManifestPath(String path) {
        return path != null
                && path.length() > 0
                && path.charAt(0) == '/'
                && path.charAt(path.length() - 1) != '/';
    }
}

```

### addon-java-sdk-main/addon-sdk/src/test/java/com/cake/clockify/AddonTests.java

- Size: 5026 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyComponent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyLifecycleEvent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyWebhook;
import com.google.gson.Gson;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static com.cake.clockify.Utils.getMockedServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddonTests {

    @Test
    @SneakyThrows
    public void testAddonHandlers() {
        ClockifyManifest manifest = Utils.getSampleManifest();
        ClockifyAddon addon = new ClockifyAddon(manifest);

        ClockifyComponent component = Utils.getSampleComponent();
        addon.registerComponent(component, (r, s) -> {
            s.getWriter().write("component");
            s.setStatus(200);
        });

        ClockifyWebhook webhook = Utils.getSampleWebhook();
        addon.registerWebhook(webhook, (r, s) -> {
            s.getWriter().write("webhook");
            s.setStatus(200);
        });

        ClockifyLifecycleEvent lifecycleEventInstalled = Utils.getSampleInstalledEvent();
        addon.registerLifecycleEvent(lifecycleEventInstalled, (r, s) -> {
            s.getWriter().write("lifecycle");
            s.setStatus(200);
        });

        StringWriter writer = new StringWriter();

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doNothing().when(response).setStatus(200);
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer, true));

        addon.handle(getMockedServletRequest("GET", component.getPath()), response);

        assertEquals("component", writer.getBuffer().toString());
        writer.getBuffer().delete(0, writer.getBuffer().length());

        addon.handle(getMockedServletRequest("POST", webhook.getPath()), response);

        assertEquals("webhook", writer.getBuffer().toString());
        writer.getBuffer().delete(0, writer.getBuffer().length());

        addon.handle(getMockedServletRequest("POST", lifecycleEventInstalled.getPath()), response);

        assertEquals("lifecycle", writer.getBuffer().toString());
        writer.getBuffer().delete(0, writer.getBuffer().length());

        addon.handle(getMockedServletRequest("GET", "/manifest"), response);

        assertEquals(manifest.getKey(), new Gson().fromJson(writer.getBuffer().toString(), com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyManifest.class).getKey());
    }

    @Test
    @SneakyThrows
    public void testMiddlewareUsage() {
        ClockifyManifest manifest = Utils.getSampleManifest();
        ClockifyAddon clockifyAddon = new ClockifyAddon(manifest);

        ClockifyComponent component = Utils.getSampleComponent();
        clockifyAddon.registerComponent(component, (r, s) -> {
        });

        SampleMiddleware middleware1 = new SampleMiddleware();
        SampleMiddleware middleware2 = new SampleMiddleware() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                used = true;
                response.getWriter().write("intercepted");
            }
        };
        SampleMiddleware middleware3 = new SampleMiddleware();

        clockifyAddon.addFilter(middleware1);
        clockifyAddon.addFilter(middleware2);
        clockifyAddon.addFilter(middleware3);

        StringWriter writer = new StringWriter();

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doNothing().when(response).setStatus(200);
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        clockifyAddon.handle(getMockedServletRequest("GET", component.getPath()), response);

        assertTrue(middleware1.used);
        assertTrue(middleware2.used);
        assertFalse(middleware3.used);

        assertEquals("intercepted", writer.getBuffer().toString());
    }

    private static class SampleMiddleware implements Filter {
        boolean used = false;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            used = true;
            chain.doFilter(request, response);
        }
    }
}

```

### addon-java-sdk-main/addon-sdk/src/test/java/com/cake/clockify/ManifestTests.java

- Size: 1785 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyComponent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyLifecycleEvent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyWebhook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManifestTests {

    @Test
    public void testManifestBuild() {
        ClockifyManifest manifest = Utils.getSampleManifest();
        ClockifyAddon clockifyAddon = new ClockifyAddon(manifest);

        ClockifyComponent component = Utils.getSampleComponent();
        clockifyAddon.registerComponent(component, (r, s) -> {
        });

        ClockifyWebhook webhook = Utils.getSampleWebhook();
        clockifyAddon.registerWebhook(webhook, (r, s) -> {
        });

        ClockifyLifecycleEvent lifecycleEventInstalled = Utils.getSampleInstalledEvent();
        clockifyAddon.registerLifecycleEvent(lifecycleEventInstalled, (r, s) -> {
        });

        ClockifyLifecycleEvent lifecycleEventDeleted = Utils.getSampleDeletedEvent();
        clockifyAddon.registerLifecycleEvent(lifecycleEventDeleted, (r, s) -> {
        });

        assertEquals(1, manifest.getComponents().size());
        assertEquals(component, manifest.getComponents().get(0));

        assertEquals(1, manifest.getWebhooks().size());
        assertEquals(webhook, manifest.getWebhooks().get(0));

        assertEquals(2, manifest.getLifecycle().size());
        assertEquals(lifecycleEventInstalled, manifest.getLifecycle().get(0));
        assertEquals(lifecycleEventDeleted, manifest.getLifecycle().get(1));
    }
}

```

### addon-java-sdk-main/addon-sdk/src/test/java/com/cake/clockify/ServletTests.java

- Size: 1780 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify;

import com.cake.clockify.addonsdk.clockify.ClockifyAddon;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyComponent;
import com.cake.clockify.addonsdk.shared.AddonServlet;
import com.cake.clockify.addonsdk.shared.EmbeddedServer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServletTests {
    private static final int SAMPLE_SERVER_PORT = 10501;

    @Test
    @SneakyThrows
    public void testServerStartup() {
        ClockifyAddon clockifyAddon = new ClockifyAddon(Utils.getSampleManifest());
        clockifyAddon.addFilter(((request, response, chain) -> {
            response.setContentType("application/json");
            chain.doFilter(request, response);
        }));

        ClockifyComponent component = Utils.getSampleComponent();
        clockifyAddon.registerComponent(component, (r, s) -> {
        });

        AddonServlet servlet = new AddonServlet(clockifyAddon);

        Runnable serverRunnable = () -> {
            try {
                new EmbeddedServer(servlet).start(SAMPLE_SERVER_PORT);
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread serverThread = new Thread(serverRunnable);
        serverThread.start();

        Thread.sleep(3_000);

        URL url = new URL("http://localhost:" + SAMPLE_SERVER_PORT + component.getPath());
        URLConnection connection = url.openConnection();

        assertEquals("application/json", connection.getContentType());

        serverThread.interrupt();
    }
}

```

### addon-java-sdk-main/addon-sdk/src/test/java/com/cake/clockify/Utils.java

- Size: 3358 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify;

import com.cake.clockify.addonsdk.clockify.model.ClockifyManifest;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyComponent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyLifecycleEvent;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifySetting;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifySettings;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifySettingsHeader;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifySettingsTab;
import com.cake.clockify.addonsdk.clockify.model.v1_2.ClockifyWebhook;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class Utils {

    public static HttpServletRequest getMockedServletRequest(String method, String path) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getMethod()).thenReturn(method);
        Mockito.when(request.getRequestURI()).thenReturn(path);
        return request;
    }

    public static ClockifyManifest getSampleManifest() {
        return ClockifyManifest.v1_2Builder()
                .key("key")
                .name("name")
                .baseUrl("http://localhost:8080")
                .requireFreePlan()
                .scopes(List.of())
                .description("description")
                .build();
    }

    public static ClockifyComponent getSampleComponent() {
        return ClockifyComponent
                .builder()
                .widget()
                .allowAdmins()
                .path("/component1")
                .label("label")
                .options(Map.of())
                .build();
    }

    public static ClockifyWebhook getSampleWebhook() {
        return ClockifyWebhook.builder()
                .onBalanceUpdated()
                .path("/webhook1")
                .build();
    }

    public static ClockifyLifecycleEvent getSampleInstalledEvent() {
        return ClockifyLifecycleEvent.builder()
                .path("/installed")
                .onInstalled()
                .build();
    }

    public static ClockifyLifecycleEvent getSampleDeletedEvent() {
        return ClockifyLifecycleEvent.builder()
                .path("/deleted")
                .onDeleted()
                .build();
    }

    public static ClockifySettings getSampleSettings() {
        return ClockifySettings.builder()
                .tabs(List.of(
                        ClockifySettingsTab.builder()
                                .id("id")
                                .name("name")
                                .header(ClockifySettingsHeader.builder().title("title").build())
                                .settings(List.of(
                                        ClockifySetting.builder()
                                                .id("id")
                                                .name("name")
                                                .allowEveryone()
                                                .asNumber()
                                                .value(12)
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }
}

```

### addon-java-sdk-main/addon-sdk/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst

- Size: 0 bytes
- MIME: inode/x-empty; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/addon-sdk/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst
MIME: inode/x-empty; charset=binary
Size: 0 bytes
```

### addon-java-sdk-main/addon-sdk/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst

- Size: 1585 bytes
- MIME: text/plain; charset=us-ascii

```
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/ClockifyAddon.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/ClockifySignatureParser.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/model/ClockifyManifest.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/clockify/model/ClockifyResource.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/Addon.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/AddonServlet.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/EmbeddedServer.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/RequestHandler.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/ValidationException.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/response/HttpResponse.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/utils/Utils.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/addon-sdk/src/main/java/com/cake/clockify/addonsdk/shared/utils/ValidationUtils.java

```

### addon-java-sdk-main/annotation-processor/pom.xml

- Size: 2024 bytes
- MIME: text/xml; charset=us-ascii

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.cake.clockify</groupId>
    <artifactId>addon-sdk-annotation-processor</artifactId>
    <version>1.0.10</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.26</version>
        </dependency>

        <dependency>
            <groupId>com.google.auto.service</groupId>
            <artifactId>auto-service</artifactId>
            <version>1.1.1</version>
        </dependency>

        <dependency>
            <groupId>com.squareup</groupId>
            <artifactId>javapoet</artifactId>
            <version>1.13.0</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.14.2</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <release>17</release>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <distributionManagement>
        <repository>
            <id>github</id>
            <url>https://maven.pkg.github.com/clockify/addon-java-sdk</url>
        </repository>
    </distributionManagement>

</project>

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/Constants.java

- Size: 1034 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public static final String DELIMITER_NAME_PARTS = "_";

    public static final String REGEX_METHOD_NAME_SPLIT = "[.\\-_]";
    public static final String REGEX_UPPER_CASE_SPLIT = "(?=\\p{Upper})";

    public static final String CLOCKIFY_MODEL_PACKAGE = "com.cake.clockify.addonsdk.clockify.model";
    public static final String CLOCKIFY_MANIFESTS_DIR = "clockify-manifests";
    public static final List<String> CLOCKIFY_MANIFESTS = List.of(
            CLOCKIFY_MANIFESTS_DIR + "/1.2.json",
            CLOCKIFY_MANIFESTS_DIR + "/1.3.json",
            CLOCKIFY_MANIFESTS_DIR + "/1.4.json"
    );

    public static final String CLOCKIFY_PREFIX = "Clockify";
    public static final String CLOCKIFY_MANIFEST_INTERFACE = "ClockifyManifest";
    public static final String CLOCKIFY_PATH_INTERFACE = "ClockifyResource";
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/ManifestExtensionProcessor.java

- Size: 2362 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor;

import com.cake.clockify.annotationprocessor.clockify.ClockifyManifestProcessor;
import com.cake.clockify.annotationprocessor.clockify.ExtendClockifyManifest;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_MANIFESTS;

@AutoService(javax.annotation.processing.Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@NoArgsConstructor
public class ManifestExtensionProcessor extends javax.annotation.processing.AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ExtendClockifyManifest.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            processAnnotation(annotation, roundEnv);
        }

        return true;
    }

    @SneakyThrows
    private void processAnnotation(TypeElement annotation, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
        List<JavaFile> files = new LinkedList<>();

        for (Element element : elements) {
            DeclaredType type = (DeclaredType) element.asType();

            for (String manifestPath: CLOCKIFY_MANIFESTS) {
                try {
                    files.addAll(new ClockifyManifestProcessor(type, manifestPath).process());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }

        for (JavaFile file : files) {
            try {
                file.writeTo(processingEnv.getFiler());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/NodeConstants.java

- Size: 804 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeConstants {

    public static final String REF = "$ref";
    public static final String TYPE = "type";
    public static final String ANY_OF = "anyOf";
    public static final String DEFINITIONS = "definitions";
    public static final String PROPERTIES = "properties";
    public static final String REQUIRED = "required";
    public static final String ARRAY = "array";
    public static final String OBJECT = "object";
    public static final String STRING = "string";
    public static final String ENUM = "enum";
    public static final String ITEMS = "items";
    public static final String DESCRIPTION = "description";
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/Utils.java

- Size: 6325 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.lang.model.type.DeclaredType;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_PREFIX;
import static com.cake.clockify.annotationprocessor.Constants.REGEX_METHOD_NAME_SPLIT;
import static com.cake.clockify.annotationprocessor.Constants.REGEX_UPPER_CASE_SPLIT;
import static java.util.Collections.emptyList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {
    private static final Locale LOCALE = Locale.US;

    private static final Map<String, String> DEFINITION_CLASSNAME_MAPPINGS = Map.of(
            "lifecycle", "lifecycleEvent"
    );

    public static JsonNode readManifestDefinition(ObjectMapper mapper, String manifestPath) {
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(manifestPath)) {
            return mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String normalizeUppercaseOnlyValue(String value) {
        if (value.toUpperCase(LOCALE).equals(value)) {
            return value.toLowerCase(LOCALE);
        }
        return value;
    }

    public static String toMethodName(String value) {
        List<String> parts = new LinkedList<>();
        for (String part : value.split(REGEX_METHOD_NAME_SPLIT)) {
            // all uppercase values should not be split
            part = normalizeUppercaseOnlyValue(part);

            // split words on uppercase values
            for (String s : part.split(REGEX_UPPER_CASE_SPLIT)) {
                parts.add(s.toLowerCase(LOCALE));
            }
        }

        if (parts.size() <= 1) {
            return normalizeUppercaseOnlyValue(value);
        }

        StringBuilder sb = new StringBuilder(value.length());

        Iterator<String> iterator = parts.iterator();
        sb.append(iterator.next().toLowerCase(LOCALE));
        iterator.forEachRemaining(v -> sb.append(capitalize(v)));

        return sb.toString();
    }

    public static String toClassName(String value) {
        return capitalize(toMethodName(value));
    }

    public static String capitalize(String value) {
        if (value == null) {
            return null;
        }

        if (value.length() == 1) {
            return value.toUpperCase(Locale.US);
        }

        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    public static List<String> getStringValuesFromNode(JsonNode node) {
        List<String> values = new LinkedList<>();
        node.forEach(v -> values.add(v.asText()));
        return values;
    }

    public static List<String> getFieldNamesFromNode(JsonNode node) {
        List<String> values = new LinkedList<>();
        node.fieldNames().forEachRemaining(values::add);
        return values;
    }

    public static List<String> getEnumValuesFromNode(JsonNode node) {
        if (!node.has(NodeConstants.ENUM)) {
            return emptyList();
        }

        List<String> values = new LinkedList<>();
        node.get(NodeConstants.ENUM).forEach(n -> values.add(n.asText()));
        return values;
    }

    public static boolean hasDefinitionRef(JsonNode node) {
        if (node == null) {
            return false;
        }
        return node.has(NodeConstants.REF);
    }

    public static String getDefinitionRef(JsonNode node) {
        String definition = node.get(NodeConstants.REF).asText();
        return definition.substring(definition.lastIndexOf("/") + 1);
    }

    public static JsonNode getDefinitionNode(JsonNode manifest, JsonNode node) {
        return manifest.get(NodeConstants.DEFINITIONS).get(getDefinitionRef(node));
    }

    public static String getNodeType(JsonNode node, JsonNode definitions) {
        if (node.has(NodeConstants.TYPE)) {
            return node.get(NodeConstants.TYPE).asText();
        }

        if (hasDefinitionRef(node)) {
            String ref = getDefinitionRef(node);
            if ("url".equals(ref)) {
                return NodeConstants.STRING;
            }

            if (definitions.has(ref)) {
                return getNodeType(definitions.get(ref), definitions);
            }
        }

        return NodeConstants.STRING;
    }

    public static String[] getPackageAndClassNames(DeclaredType type) {
        String qualifiedName = type.asElement().toString();
        int lastDot = qualifiedName.lastIndexOf(".");

        String packageName = qualifiedName.substring(0, lastDot);
        String className = qualifiedName.substring(lastDot + 1);
        return new String[] {packageName, className};
    }

    /**
     * @param packageName
     * @param definition
     * @return the typename for the given definition, after applying name mappings
     */
    public static TypeName getDefinitionTypeName(String packageName, String definition) {
        return ClassName.get(packageName, getDefinitionSimpleClassName(definition));
    }

    /**
     * @param definition
     * @return the class name for the definition, after applying name mappings
     */
    public static String getDefinitionSimpleClassName(String definition) {
        String classNameSuffix = DEFINITION_CLASSNAME_MAPPINGS.getOrDefault(definition, definition);
        return Utils.toClassName(CLOCKIFY_PREFIX + Constants.DELIMITER_NAME_PARTS + classNameSuffix);
    }

    public static String getPropertyDescription(JsonNode node) {
        if (node.has(NodeConstants.DESCRIPTION)) {
            return node.get(NodeConstants.DESCRIPTION).asText();
        }
        return "";
    }

    public static String getVersionedPackageName(JsonNode manifest, String packageName) {
        String version = manifest.get("version").asText();
        String versionSubpackage = "v" + version.replace(".", "_");

        return packageName + "." + versionSubpackage;
    }
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/ClockifyManifestProcessor.java

- Size: 2648 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor.clockify;

import com.cake.clockify.annotationprocessor.NodeConstants;
import com.cake.clockify.annotationprocessor.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.squareup.javapoet.JavaFile;

import javax.lang.model.type.DeclaredType;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ClockifyManifestProcessor {
    private final JsonNode manifest;

    private final String packageName;
    private final String className;

    public ClockifyManifestProcessor(DeclaredType type, String manifestPath) {
        this.manifest = Utils.readManifestDefinition(new ObjectMapper(), manifestPath);

        String[] qualifiedNames = Utils.getPackageAndClassNames(type);
        this.packageName = qualifiedNames[0];
        this.className = qualifiedNames[1];
    }

    public List<JavaFile> process() {
        return Stream.concat(getObjectDefinitions().stream(), getManifestDefinition().stream()).toList();
    }

    private List<JavaFile> getObjectDefinitions() {
        List<JavaFile> javaFiles = new LinkedList<>();

        manifest.get(NodeConstants.DEFINITIONS).fields().forEachRemaining(entry -> {
            String definition = entry.getKey();
            JsonNode node = entry.getValue();

            if (isObjectDefinition(node)) {
                DefinitionProcessor processor = new DefinitionProcessor(
                        manifest,
                        Utils.getDefinitionSimpleClassName(definition),
                        definition
                );

                javaFiles.addAll(processor.process());
            } else if (isEnumDefinition(node)) {
                EnumConstantsProcessor processor = new EnumConstantsProcessor(
                        manifest,
                        definition
                );

                javaFiles.addAll(processor.process());
            }
        });

        return javaFiles;
    }

    private List<JavaFile> getManifestDefinition() {
        return new DefinitionProcessor(manifest, className, null).process();
    }

    private boolean isObjectDefinition(JsonNode node) {
        return NodeConstants.OBJECT.equals(Utils.getNodeType(node, manifest.get(NodeConstants.DEFINITIONS)));
    }

    private boolean isEnumDefinition(JsonNode node) {
        String type = Utils.getNodeType(node, manifest.get(NodeConstants.DEFINITIONS));
        return NodeConstants.STRING.equals(type) && node.get(NodeConstants.ENUM) instanceof ArrayNode;
    }
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/DefinitionProcessor.java

- Size: 16115 bytes
- MIME: text/x-c++; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor.clockify;

import com.cake.clockify.annotationprocessor.Constants;
import com.cake.clockify.annotationprocessor.NodeConstants;
import com.cake.clockify.annotationprocessor.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_MANIFEST_INTERFACE;
import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_MODEL_PACKAGE;
import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_PATH_INTERFACE;

class DefinitionProcessor {

    private static final String TEMPLATE_INTERFACE_NAME = "Builder%1$sStep";
    private static final String TEMPLATE_DEFAULT_METHOD = "return %1$s (\"%2$s\")";

    private static final String STEP_BUILDER = "Build";
    private static final String STEP_OPTIONAL = "Optional";
    private static final String PROPERTY_SCHEMA_VERSION = "schemaVersion";

    private final JsonNode manifest;
    private final JsonNode propertiesNode;

    private final String packageName;
    private final String className;
    private final String definition;

    private final List<String> properties;
    private final List<String> requiredProperties;
    private final List<String> optionalProperties;

    public DefinitionProcessor(JsonNode manifest, String className, @Nullable String definition) {
        this.manifest = manifest;

        this.packageName = Utils.getVersionedPackageName(manifest, CLOCKIFY_MODEL_PACKAGE);
        this.className = className;
        this.definition = definition;

        this.propertiesNode = readProperties(NodeConstants.PROPERTIES);

        this.properties = Utils.getFieldNamesFromNode(propertiesNode);
        this.requiredProperties = Utils.getStringValuesFromNode(readProperties(NodeConstants.REQUIRED));
        this.optionalProperties = properties.stream()
                .filter(p -> !requiredProperties.contains(p))
                .toList();
    }

    public JsonNode readProperties(String key) {
        if (definition == null) {
            return manifest.get(key);
        }

        return manifest.get(NodeConstants.DEFINITIONS).get(definition).get(key);
    }

    public List<JavaFile> process() {
        List<TypeSpec> interfaces = getBuilderStepClasses();

        List<TypeSpec> specs = new LinkedList<>();
        specs.add(getModelClass(interfaces));
        specs.addAll(interfaces);
        specs.add(getModelBuilderClass(interfaces));

        return specs.stream().map(t -> JavaFile.builder(packageName, t).build()).toList();
    }

    private List<TypeSpec> getBuilderStepClasses() {
        List<TypeSpec> types = new LinkedList<>();

        boolean optionalStep = requiredProperties.size() != properties.size();
        String lastStep = optionalStep ? STEP_OPTIONAL : STEP_BUILDER;

        ClassName lastStepClass = getInterfaceStepClassName(lastStep);

        // each required property will have its own interface to guide the user through
        // the build process for the object
        for (int i = 0; i < requiredProperties.size(); i++) {
            String property = requiredProperties.get(i);

            ClassName nextInterface = i == requiredProperties.size() - 1
                    ? lastStepClass
                    : getInterfaceStepClassName(requiredProperties.get(i + 1));

            List<MethodSpec> methods = new LinkedList<>();
            // adding primary setter method
            methods.add(getPropertySetterMethod(property, nextInterface));
            // for enum nodes, adding default helper methods
            methods.addAll(getEnumSetterMethods(property, nextInterface));

            types.add(getInterfaceClass(property, methods));
        }

        // if there are optional properties, they will be included in the same optional step
        // otherwise, a build step will be used
        if (optionalStep) {
            types.add(getOptionalStepClass(lastStep));

        } else {
            types.add(getInterfaceClass(lastStep, List.of(getBuildMethod())));
        }

        return types;
    }

    private TypeSpec getModelClass(List<TypeSpec> interfaces) {
        List<FieldSpec> fields = properties.stream()
                .map(p -> PROPERTY_SCHEMA_VERSION.equals(p) ? getSchemaVersionField() : getModelPropertyField(p))
                .toList();

        List<MethodSpec> methods = new LinkedList<>();

        methods.add(getModelConstructor(fields));
        methods.add(getModelBuilderMethod(interfaces.get(0)));

        for (FieldSpec field : fields) {
            methods.add(getModelGetterMethod(field));
            if (isManifestDefinition() && "settings".equals(field.name)) {
                methods.add(getModelSetterMethod(field));
            }
        }

        return TypeSpec.classBuilder(ClassName.get(packageName, className))
                .addSuperinterfaces(getModelSuperInterfaces())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addFields(fields)
                .addMethods(methods)
                .build();
    }

    private List<ClassName> getModelSuperInterfaces() {
        return Stream.of(
                        isManifestDefinition() ? ClassName.get(CLOCKIFY_MODEL_PACKAGE, CLOCKIFY_MANIFEST_INTERFACE) : null,
                        shouldImplementPathInterface() ? ClassName.get(CLOCKIFY_MODEL_PACKAGE, CLOCKIFY_PATH_INTERFACE) : null
                )
                .filter(Objects::nonNull)
                .toList();
    }

    private TypeSpec getModelBuilderClass(List<TypeSpec> interfaces) {
        ClassName modelName = ClassName.get(packageName, className);
        ClassName builderName = ClassName.get(packageName, className + "Builder");

        List<FieldSpec> fields = new LinkedList<>();
        List<MethodSpec> methods = new LinkedList<>();

        for (String property : properties.stream().filter(p -> !PROPERTY_SCHEMA_VERSION.equals(p)).toList()) {
            fields.add(getModelPropertyField(property));
        }

        for (FieldSpec field : fields) {
            methods.add(getModelBuilderSetterMethod(builderName, field));
        }

        methods.add(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(modelName)
                .addStatement("return new $L($L)", modelName.simpleName(), CodeBlock.join(
                        fields.stream().map(f -> CodeBlock.of("$N", f)).toList(),
                        ", "
                ))
                .build()
        );

        return TypeSpec.classBuilder(builderName)
                .addSuperinterfaces(interfaces.stream()
                        .map(spec -> ClassName.get("", spec.name))
                        .toList()
                )
                .addFields(fields)
                .addMethods(methods)
                .build();
    }

    private MethodSpec getModelConstructor(List<FieldSpec> fields) {
        fields = fields.stream()
                .filter(f -> !PROPERTY_SCHEMA_VERSION.equals(f.name))
                .toList();

        var constructorBuilder = MethodSpec.constructorBuilder()
                .addParameters(fields.stream()
                        .map(f -> ParameterSpec.builder(f.type, f.name).build())
                        .toList()
                );

        for (String property : requiredProperties) {
            constructorBuilder.addStatement("$T.requireNonNull($N)", Objects.class, property);
        }

        for (FieldSpec field : fields) {
            constructorBuilder.addStatement("this.$N = $N", field, field);
        }

        return constructorBuilder.build();
    }

    private MethodSpec getModelBuilderMethod(TypeSpec firstStep) {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("", firstStep.name))
                .addStatement("return new $N()", ClassName.get(packageName, className + "Builder").simpleName())
                .build();
    }

    private FieldSpec getSchemaVersionField() {
        return FieldSpec.builder(String.class, PROPERTY_SCHEMA_VERSION)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .initializer("$S", manifest.get("version").asText())
                .build();
    }

    private FieldSpec getModelPropertyField(String property) {
        JsonNode node = propertiesNode.get(property);
        TypeName type = getTypeNameFromPropertyNode(node);

        FieldSpec.Builder builder = FieldSpec.builder(type, property);
        if (type instanceof ParameterizedTypeName parametrizedType
                && parametrizedType.rawType.equals(ClassName.get(List.class))) {
            builder.initializer("new $T()", LinkedList.class);
        }
        return builder
                .addModifiers(Modifier.PRIVATE)
                .addJavadoc(Utils.getPropertyDescription(node))
                .build();
    }

    private MethodSpec getModelGetterMethod(FieldSpec field) {
        String propertyMethodName = Utils.toMethodName(field.name);
        String getterName = "get" + Utils.capitalize(propertyMethodName);

        return MethodSpec.methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(field.type)
                .addStatement("return $N", field.name)
                .build();
    }

    private MethodSpec getModelSetterMethod(FieldSpec field) {
        String propertyMethodName = Utils.toMethodName(field.name);
        String setterName = "set" + Utils.capitalize(propertyMethodName);

        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "settings")
                .returns(TypeName.VOID)
                .addStatement("this.$L = $L", field.name, field.name)
                .build();
    }

    private MethodSpec getModelBuilderSetterMethod(TypeName builder, FieldSpec field) {
        return MethodSpec.methodBuilder(Utils.toMethodName(field.name))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .returns(builder)
                .addParameter(field.type, field.name)
                .addStatement("this.$N = $N", field.name, field.name)
                .addStatement("return this")
                .build();
    }

    private boolean isManifestDefinition() {
        return properties.contains(PROPERTY_SCHEMA_VERSION);
    }

    private boolean shouldImplementPathInterface() {
        return definition != null && List.of("lifecycle", "webhook", "component").contains(definition);
    }

    private boolean shouldSkipProperty(String property) {
        return PROPERTY_SCHEMA_VERSION.equals(property);
    }

    private TypeSpec getOptionalStepClass(String step) {
        ClassName interfaceName = getInterfaceStepClassName(step);

        List<MethodSpec> methods = Stream.concat(
                optionalProperties.stream()
                        .filter(p -> !shouldSkipProperty(p))
                        .map(p -> getPropertySetterMethod(p, interfaceName)),
                Stream.of(getBuildMethod())
        ).toList();

        return getInterfaceClass(step, methods);
    }

    private List<MethodSpec> getEnumSetterMethods(String property, ClassName nextInterfaceName) {
        JsonNode node = propertiesNode.get(property);
        if (Utils.hasDefinitionRef(node)) {
            node = Utils.getDefinitionNode(manifest, node);
        }

        return Utils.getEnumValuesFromNode(node)
                .stream()
                .map(value ->
                        MethodSpec.methodBuilder(getEnumSetterMethodName(property, value))
                                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                .returns(nextInterfaceName)
                                .addStatement(TEMPLATE_DEFAULT_METHOD.formatted(property, value))
                                .build())
                .toList();
    }

    private MethodSpec getPropertySetterMethod(String property, ClassName returnType) {
        JsonNode node = propertiesNode.get(property);
        TypeName type = getTypeNameFromPropertyNode(node);

        return MethodSpec.methodBuilder(Utils.toMethodName(property))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(returnType)
                .addParameter(type, "value")
                .addJavadoc(Utils.getPropertyDescription(node))
                .build();
    }

    private TypeName getTypeNameFromPropertyNode(JsonNode node) {
        if (node.has(NodeConstants.ANY_OF)) {
            return TypeName.get(Object.class);
        }

        return switch (Utils.getNodeType(node, manifest.get(NodeConstants.DEFINITIONS))) {
            case "integer" -> TypeName.get(Integer.class);
            case "boolean" -> TypeName.get(Boolean.class);
            case "string" -> TypeName.get(String.class);

            case "object" -> Utils.hasDefinitionRef(node)
                    ? Utils.getDefinitionTypeName(packageName, Utils.getDefinitionRef(node))
                    : ParameterizedTypeName.get(Map.class, String.class, Object.class);

            case "array" -> Utils.hasDefinitionRef(node.get("items"))
                    ? ParameterizedTypeName.get(ClassName.get(List.class),
                    getTypeNameFromPropertyNode(node.get("items")))
                    : ParameterizedTypeName.get(List.class, String.class);

            default -> TypeName.get(Object.class);
        };
    }

    private MethodSpec getBuildMethod() {
        return MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get(packageName, className))
                .build();
    }

    private TypeSpec getInterfaceClass(String property, List<MethodSpec> methods) {
        return TypeSpec.interfaceBuilder(getInterfaceStepClassName(property))
                .addModifiers(Modifier.PUBLIC)
                .addMethods(methods)
                .build();
    }

    private ClassName getInterfaceStepClassName(String step) {
        String name = className + TEMPLATE_INTERFACE_NAME.formatted(Utils.toClassName(step));
        return ClassName.get(packageName, name);
    }

    private String getEnumSetterMethodName(String property, String value) {
        if ("component".equals(definition)) {
            if ("accessLevel".equals(property)) {
                value = "allow" + Constants.DELIMITER_NAME_PARTS + value;
            }

        } else if ("lifecycle".equals(definition)) {
            if ("type".equals(property)) {
                value = "on" + Constants.DELIMITER_NAME_PARTS + value;
            }

        } else if ("webhook".equals(definition)) {
            if ("event".equals(property)) {
                value = "on" + Constants.DELIMITER_NAME_PARTS + value;
            }

        } else if ("setting".equals(definition)) {
            if ("type".equals(property)) {
                value = "as" + Constants.DELIMITER_NAME_PARTS + value;
            } else if ("accessLevel".equals(property)) {
                value = "allow" + Constants.DELIMITER_NAME_PARTS + value;
            }

        } else if (definition == null) {
            if ("minimalSubscriptionPlan".equals(property)) {
                value = "require" + Constants.DELIMITER_NAME_PARTS + value + Constants.DELIMITER_NAME_PARTS + "plan";
            }
        }

        return Utils.toMethodName(value);
    }
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/EnumConstantsProcessor.java

- Size: 2542 bytes
- MIME: text/x-c++; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor.clockify;

import com.cake.clockify.annotationprocessor.NodeConstants;
import com.cake.clockify.annotationprocessor.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.cake.clockify.annotationprocessor.Constants.CLOCKIFY_MODEL_PACKAGE;

class EnumConstantsProcessor {

    private final String packageName;
    private final String definition;

    private final List<String> values = new LinkedList<>();

    public EnumConstantsProcessor(JsonNode manifest, String definition) {
        this.packageName = Utils.getVersionedPackageName(manifest, CLOCKIFY_MODEL_PACKAGE);
        this.definition = definition;

        JsonNode enumNode = manifest.get(NodeConstants.DEFINITIONS).get(definition).get(NodeConstants.ENUM);

        Objects.requireNonNull(enumNode);
        if (!enumNode.isArray()) {
            throw new IllegalArgumentException("The provided definition is not of enum type.");
        }

        enumNode.forEach(node -> values.add(node.asText()));
    }

    public List<JavaFile> process() {
        return List.of(
                JavaFile.builder(packageName, TypeSpec.interfaceBuilder(getInterfaceClassName(definition))
                                .addModifiers(Modifier.PUBLIC)
                                .addFields(values.stream()
                                        .map(enumValue -> FieldSpec.builder(
                                                        String.class,
                                                        enumValue.replace(' ', '_'),
                                                        Modifier.PUBLIC,
                                                        Modifier.STATIC,
                                                        Modifier.FINAL
                                                )
                                                .initializer("\"" + enumValue + "\"")
                                                .build()).toList()
                                )
                                .build())
                        .build()
        );
    }

    private ClassName getInterfaceClassName(String step) {
        return ClassName.get(packageName, Utils.getDefinitionSimpleClassName(step));
    }
}

```

### addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/ExtendClockifyManifest.java

- Size: 325 bytes
- MIME: text/x-java; charset=us-ascii

```java
package com.cake.clockify.annotationprocessor.clockify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ExtendClockifyManifest {
}

```

### addon-java-sdk-main/annotation-processor/src/main/resources/clockify-manifests/1.2.json

- Size: 16445 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.2",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "PROJECT_UPDATED",
            "PROJECT_DELETED",
            "NEW_TASK",
            "TASK_UPDATED",
            "TASK_DELETED",
            "NEW_CLIENT",
            "CLIENT_UPDATED",
            "CLIENT_DELETED",
            "NEW_TAG",
            "TAG_UPDATED",
            "TAG_DELETED",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "USER_EMAIL_CHANGED",
            "USER_UPDATED",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED",
            "USER_GROUP_CREATED",
            "USER_GROUP_UPDATED",
            "USER_GROUP_DELETED",
            "EXPENSE_CREATED",
            "EXPENSE_UPDATED",
            "EXPENSE_DELETED",
            "ASSIGNMENT_CREATED",
            "ASSIGNMENT_UPDATED",
            "ASSIGNMENT_DELETED",
            "ASSIGNMENT_PUBLISHED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type.",
          "maxLength": 25
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan",
    "scopes"
  ]
}
```

### addon-java-sdk-main/annotation-processor/src/main/resources/clockify-manifests/1.3.json

- Size: 15794 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.3",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "NEW_TASK",
            "NEW_CLIENT",
            "NEW_TAG",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type."
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path",
        "label"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan"
  ]
}
```

### addon-java-sdk-main/annotation-processor/src/main/resources/clockify-manifests/1.4.json

- Size: 15825 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.4",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "NEW_TASK",
            "NEW_CLIENT",
            "NEW_TAG",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab",
            "invoices.action"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type."
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path",
        "label"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan"
  ]
}
```

### addon-java-sdk-main/annotation-processor/target/addon-sdk-annotation-processor-1.0.10.jar

- Size: 34220 bytes
- MIME: application/zip; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/addon-sdk-annotation-processor-1.0.10.jar
MIME: application/zip; charset=binary
Size: 34220 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/clockify-manifests/1.2.json

- Size: 16445 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.2",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "PROJECT_UPDATED",
            "PROJECT_DELETED",
            "NEW_TASK",
            "TASK_UPDATED",
            "TASK_DELETED",
            "NEW_CLIENT",
            "CLIENT_UPDATED",
            "CLIENT_DELETED",
            "NEW_TAG",
            "TAG_UPDATED",
            "TAG_DELETED",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "USER_EMAIL_CHANGED",
            "USER_UPDATED",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED",
            "USER_GROUP_CREATED",
            "USER_GROUP_UPDATED",
            "USER_GROUP_DELETED",
            "EXPENSE_CREATED",
            "EXPENSE_UPDATED",
            "EXPENSE_DELETED",
            "ASSIGNMENT_CREATED",
            "ASSIGNMENT_UPDATED",
            "ASSIGNMENT_DELETED",
            "ASSIGNMENT_PUBLISHED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type.",
          "maxLength": 25
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan",
    "scopes"
  ]
}
```

### addon-java-sdk-main/annotation-processor/target/classes/clockify-manifests/1.3.json

- Size: 15794 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.3",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "NEW_TASK",
            "NEW_CLIENT",
            "NEW_TAG",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type."
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path",
        "label"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan"
  ]
}
```

### addon-java-sdk-main/annotation-processor/target/classes/clockify-manifests/1.4.json

- Size: 15825 bytes
- MIME: application/json; charset=us-ascii

```json
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "version": "1.4",
  "definitions": {
    "url": {
      "format": "uri",
      "pattern": "^https?://"
    },
    "webhook": {
      "type": "object",
      "properties": {
        "event": {
          "type": "string",
          "enum": [
            "NEW_PROJECT",
            "NEW_TASK",
            "NEW_CLIENT",
            "NEW_TAG",
            "NEW_TIMER_STARTED",
            "TIMER_STOPPED",
            "TIME_ENTRY_UPDATED",
            "TIME_ENTRY_DELETED",
            "NEW_TIME_ENTRY",
            "NEW_INVOICE",
            "INVOICE_UPDATED",
            "USER_JOINED_WORKSPACE",
            "USER_DELETED_FROM_WORKSPACE",
            "USER_DEACTIVATED_ON_WORKSPACE",
            "USER_ACTIVATED_ON_WORKSPACE",
            "NEW_APPROVAL_REQUEST",
            "APPROVAL_REQUEST_STATUS_UPDATED",
            "TIME_OFF_REQUESTED",
            "TIME_OFF_REQUEST_APPROVED",
            "TIME_OFF_REQUEST_REJECTED",
            "TIME_OFF_REQUEST_WITHDRAWN",
            "BALANCE_UPDATED"
          ],
          "description": "Clockify event that triggers webhook is sending to specified url. Url is constructed by concatenating addon 'baseUrl' and 'webhook.path'."
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving webhooks from Clockify side. Path is just part of url to which webhook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path.",
          "example": [
            "/clockify/webhooks",
            "/webhooks/time-entries/",
            "/triggers"
          ]
        }
      },
      "required": [
        "event",
        "path"
      ]
    },
    "lifecycle": {
      "description": "Specialized webhook triggered on addon lifecycle event.",
      "type": "object",
      "properties": {
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon endpoint designated for receiving lifecycle hooks from Clockify side. Path is just part of url to which a lifecycle hook will be sent. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "type": {
          "type": "string",
          "description": "Specifies addon lifecycle event you want to be notified by Clockify.",
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED",
            "STATUS_CHANGED"
          ]
        }
      },
      "required": [
        "path",
        "type"
      ]
    },
    "scope": {
      "description": "Api scope used by addon.",
      "type": "string",
      "enum": [
        "CLIENT_READ",
        "CLIENT_WRITE",
        "PROJECT_READ",
        "PROJECT_WRITE",
        "TAG_READ",
        "TAG_WRITE",
        "TASK_READ",
        "TASK_WRITE",
        "TIME_ENTRY_READ",
        "TIME_ENTRY_WRITE",
        "EXPENSE_READ",
        "EXPENSE_WRITE",
        "INVOICE_READ",
        "INVOICE_WRITE",
        "USER_READ",
        "USER_WRITE",
        "GROUP_READ",
        "GROUP_WRITE",
        "WORKSPACE_READ",
        "WORKSPACE_WRITE",
        "CUSTOM_FIELDS_READ",
        "CUSTOM_FIELDS_WRITE",
        "APPROVAL_READ",
        "APPROVAL_WRITE",
        "SCHEDULING_READ",
        "SCHEDULING_WRITE",
        "REPORTS_READ",
        "REPORTS_WRITE",
        "TIME_OFF_READ",
        "TIME_OFF_WRITE"
      ]
    },
    "component": {
      "type": "object",
      "description": "UI element shown in Clockify web app. It serves as a placeholder for addon app i.e. addon app will be rendered inside Clockify component.",
      "properties": {
        "type": {
          "type": "string",
          "description": "Specifies which kind of component will be rendered. If component is 'tab', it also comes with the name of Clockify page where 'tab' component will be rendered.",
          "enum": [
            "sidebar",
            "widget",
            "timeoff.tab",
            "schedule.tab",
            "approvals.tab",
            "reports.tab",
            "activity.tab",
            "team.tab",
            "projects.tab",
            "invoices.action"
          ]
        },
        "options": {
          "type": "object",
          "description": "If you want to define some component-specific options for component that holds your addon app, this is the place to define them."
        },
        "label": {
          "type": "string",
          "description": "Label of the component e.g. if component is 'tab', value of 'label' property will be shown in UI. Label is not required for WIDGET component type."
        },
        "accessLevel": {
          "description": "Specifies who can access addon component. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "path": {
          "type": "string",
          "minLength": 1,
          "description": "Path to addon web app that will be rendered inside Clockify component. Path is part of the url from which component content will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "iconPath": {
          "type": "string",
          "description": "Path to addon hosted image which will serve as an icon for Clockify component. Path is part of the url from which the image will be served. Full url is constructed by concatenating addon 'baseUrl' and path."
        },
        "width": {
          "type": "integer",
          "description": "Defines rendered component width expressed in 'vw'. Applicable only to WIDGET components."
        },
        "height": {
          "type": "integer",
          "description": "Defines rendered component height expressed in 'vw'. Applicable only to WIDGET components."
        }
      },
      "required": [
        "type",
        "accessLevel",
        "path",
        "label"
      ]
    },
    "setting": {
      "description": "This is definition of Clockify addon setting. Each setting must have id, name, type and value. Value and type of setting must be compatible.",
      "type": "object",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Setting unique identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Setting name."
        },
        "description": {
          "type": "string",
          "description": "Brief description of setting. What is it used for, what does it affect, etc."
        },
        "placeholder": {
          "type": "string",
          "description": "Text that is shown in UI form field if setting has no value."
        },
        "accessLevel": {
          "description": "Specifies who can access addon settings. You can either choose to give access only to Clockify workspace admins, or everyone.",
          "type": "string",
          "enum": [
            "ADMINS",
            "EVERYONE"
          ]
        },
        "type": {
          "type": "string",
          "description": "Specifies setting value type. Each type is shown differently in UI and value must be compatible with a specified type e.g. if 'type' is TXT, 'value' must be 'string', if 'type' is DROPDOWN, 'value' must be 'array'.",
          "enum": [
            "TXT",
            "NUMBER",
            "DROPDOWN_SINGLE",
            "DROPDOWN_MULTIPLE",
            "CHECKBOX",
            "LINK",
            "USER_DROPDOWN_SINGLE",
            "USER_DROPDOWN_MULTIPLE"
          ]
        },
        "key": {
          "type": "string",
          "description": "Serves as key for setting that represents key-value pair e.g. if you have documentation addon which shows document corresponding to the Clockify page i.e. you need to match the Clockify page to the url of the document describing how to use that page. In that case, key would be 'Clockify page', and 'value' would be 'url of the document'."
        },
        "value": {
          "type": [
            "string",
            "number",
            "array",
            "boolean"
          ],
          "minLength": 1,
          "minItems": 1,
          "description": "Value of the setting. Value must be of type 'setting.type' e.g. For USER_DROPDOWN, value will be the user ID of the installer upon installation."
        },
        "allowedValues": {
          "type": "array",
          "description": "Required if 'setting.type' is DROPDOWN_SINGLE or DROPDOWN_MULTIPLE. Specifies which options will be shown in dropdown."
        },
        "required": {
          "type": "boolean",
          "description": "Toggles whether setting value is required or not."
        },
        "copyable": {
          "type": "boolean",
          "description": "Toggles whether setting value will be shown with 'Copy' button for easier copying."
        },
        "readOnly": {
          "type": "boolean",
          "description": "Toggles whether setting value is read-only i.e. setting value cannot be updated."
        }
      },
      "required": [
        "id",
        "name",
        "accessLevel",
        "type",
        "value"
      ]
    },
    "settingsHeader": {
      "type": "object",
      "description": "Setting banner that shows info about setting group or tab. It is shown as a blue banner before all settings contained in a given group.",
      "properties": {
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Text shown in banner."
        }
      },
      "required": [
        "title"
      ]
    },
    "settingsGroup": {
      "type": "object",
      "description": "Serves as another level of hierarchy when defining settings. Group can be part of tabs, and one tab can contain multiple groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Group identifier."
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Group title. Shown in UI."
        },
        "description": {
          "type": "string",
          "description": "Brief description of the settings that given group contains."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings that given group contains."
        },
        "settings": {
          "type": "array",
          "description": "List of settings the group contains",
          "minItems": 1,
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "title",
        "settings"
      ]
    },
    "settingsTab": {
      "type": "object",
      "description": "Serves as top level of hierarchy when defining settings. Tabs cannot be nested in other tabs or groups.",
      "properties": {
        "id": {
          "type": "string",
          "minLength": 1,
          "description": "Tab identifier."
        },
        "name": {
          "type": "string",
          "minLength": 1,
          "description": "Tab name shown in UI."
        },
        "header": {
          "$ref": "#/definitions/settingsHeader",
          "description": "Banner with info text shown before all settings and groups contained in tab."
        },
        "groups": {
          "type": "array",
          "description": "List of setting groups contained in tab.",
          "items": {
            "$ref": "#/definitions/settingsGroup"
          }
        },
        "settings": {
          "type": "array",
          "description": "List of settings contained in tab",
          "items": {
            "$ref": "#/definitions/setting"
          }
        }
      },
      "required": [
        "id",
        "name"
      ]
    },
    "settings": {
      "type": "object",
      "description": "Top level settings property. All settings grouped in tabs are defined here.",
      "properties": {
        "tabs": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/settingsTab"
          },
          "minItems": 1
        }
      },
      "required": [
        "tabs"
      ]
    },
    "selfHostedSettings": {
      "type": "string",
      "minLength": 1,
      "description": "Path to addon endpoint designated for serving addon hosted settings. Path is just a part of the url from which addon hosted settings are served. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "minimalSubscriptionPlan": {
      "type": "string",
      "description": "Specifies Clockify's minimal subscription plan required by addon.",
      "enum": [
        "FREE",
        "BASIC",
        "STANDARD",
        "PRO",
        "ENTERPRISE"
      ]
    }
  },
  "properties": {
    "schemaVersion": {
      "description": "All JSON schemes will be versioned and this field specifies which version will be used to validate JSON manifest. If no 'schemaVersion' is defined, latest JSON schema version will be used.",
      "type": [
        "string",
        "integer"
      ],
      "minLength": 1,
      "minimum": 1
    },
    "key": {
      "type": "string",
      "minLength": 2,
      "maxLength": 50,
      "description": "Serves as addon identifier. All addons must have unique key."
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 50,
      "description": "Addon name"
    },
    "baseUrl": {
      "$ref": "#/definitions/url",
      "description": "Base url on which addon app is hosted. This url with 'path' from the following entities is used when constructing urls for webhooks, components, lifecycle hooks, etc.",
      "example": [
        "https://addon-address.example.com/"
      ]
    },
    "minimalSubscriptionPlan": {
      "$ref": "#/definitions/minimalSubscriptionPlan",
      "description": "Minimal Clockify's subscription plan that is required for addon. This plan is used when checking if user's current plan is at least equal to the plan required by addon.",
      "example": [
        "PRO"
      ]
    },
    "scopes": {
      "type": "array",
      "uniqueItems": true,
      "description": "API scopes that addon is using.",
      "items": {
        "$ref": "#/definitions/scope",
        "example": [
          "PROJECT_READ"
        ]
      }
    },
    "description": {
      "type": "string",
      "description": "Brief description of given addon functionalities and purpose."
    },
    "iconPath": {
      "type": "string",
      "description": "Path to addon icon. Path is part of the url where image icon is being hosted. Full url is constructed by concatenating addon 'baseUrl' and path."
    },
    "lifecycle": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined lifecycle hooks for given addon.",
      "items": {
        "$ref": "#/definitions/lifecycle"
      }
    },
    "webhooks": {
      "type": "array",
      "uniqueItems": true,
      "description": "List of defined webhooks for given addon.",
      "items": {
        "$ref": "#/definitions/webhook"
      }
    },
    "components": {
      "type": "array",
      "description": "List of defined components for given addon.",
      "uniqueItems": true,
      "items": {
        "$ref": "#/definitions/component"
      }
    },
    "settings": {
      "anyOf": [
        {
          "$ref": "#/definitions/selfHostedSettings"
        },
        {
          "$ref": "#/definitions/settings"
        }
      ]
    }
  },
  "required": [
    "key",
    "name",
    "baseUrl",
    "minimalSubscriptionPlan"
  ]
}
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/Constants.class

- Size: 1246 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/Constants.class
MIME: application/x-java-applet; charset=binary
Size: 1246 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/ManifestExtensionProcessor.class

- Size: 4079 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/ManifestExtensionProcessor.class
MIME: application/x-java-applet; charset=binary
Size: 4079 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/NodeConstants.class

- Size: 835 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/NodeConstants.class
MIME: application/x-java-applet; charset=binary
Size: 835 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/Utils.class

- Size: 9068 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/Utils.class
MIME: application/x-java-applet; charset=binary
Size: 9068 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/ClockifyManifestProcessor.class

- Size: 4702 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/ClockifyManifestProcessor.class
MIME: application/x-java-applet; charset=binary
Size: 4702 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/DefinitionProcessor.class

- Size: 21001 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/DefinitionProcessor.class
MIME: application/x-java-applet; charset=binary
Size: 21001 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/EnumConstantsProcessor.class

- Size: 5268 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/EnumConstantsProcessor.class
MIME: application/x-java-applet; charset=binary
Size: 5268 bytes
```

### addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/ExtendClockifyManifest.class

- Size: 441 bytes
- MIME: application/x-java-applet; charset=binary

```text
[binary omitted]
Path: addon-java-sdk-main/annotation-processor/target/classes/com/cake/clockify/annotationprocessor/clockify/ExtendClockifyManifest.class
MIME: application/x-java-applet; charset=binary
Size: 441 bytes
```

### addon-java-sdk-main/annotation-processor/target/maven-archiver/pom.properties

- Size: 83 bytes
- MIME: text/plain; charset=us-ascii

```properties
artifactId=addon-sdk-annotation-processor
groupId=com.cake.clockify
version=1.0.10

```

### addon-java-sdk-main/annotation-processor/target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst

- Size: 537 bytes
- MIME: text/plain; charset=us-ascii

```
com/cake/clockify/annotationprocessor/clockify/DefinitionProcessor.class
com/cake/clockify/annotationprocessor/clockify/ExtendClockifyManifest.class
com/cake/clockify/annotationprocessor/Utils.class
com/cake/clockify/annotationprocessor/Constants.class
com/cake/clockify/annotationprocessor/clockify/ClockifyManifestProcessor.class
com/cake/clockify/annotationprocessor/ManifestExtensionProcessor.class
com/cake/clockify/annotationprocessor/NodeConstants.class
com/cake/clockify/annotationprocessor/clockify/EnumConstantsProcessor.class

```

### addon-java-sdk-main/annotation-processor/target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst

- Size: 1217 bytes
- MIME: text/plain; charset=us-ascii

```
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/NodeConstants.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/DefinitionProcessor.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/ExtendClockifyManifest.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/EnumConstantsProcessor.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/clockify/ClockifyManifestProcessor.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/Constants.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/ManifestExtensionProcessor.java
/Users/15x/Downloads/NewBoiler/addon-java-sdk-main/annotation-processor/src/main/java/com/cake/clockify/annotationprocessor/Utils.java

```

### addon-java-sdk-main/configure-maven.sh

- Size: 861 bytes
- MIME: text/x-shellscript; charset=us-ascii

```sh
#!/bin/bash
if [ $# != 2 ]; then
  echo "You need to pass the Github access token and the repository path as parameters."
  exit 1
fi

m2="<settings>
  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/$2</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <servers>
    <server>
      <id>github</id>
      <username>OWNER</username>
      <password>$1</password>
    </server>
  </servers>
</settings>"

echo "$m2" > ~/.m2/settings.xml
```

### addon-java-sdk-main/readme.md

- Size: 6930 bytes
- MIME: text/html; charset=us-ascii

```markdown
Addon SDK is a framework that is written in Java and offers abstractions to get up and running with the development of addons for the CAKE.com marketplace.

The SDK is intended to be easy to use and intuitive.

More features are planned and will be added to the SDK.

## Installation
Maven must be configured as described <a href="https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages">here</a>
before the Addons SDK can be installed.

Once you have configured the required settings, you can import the Addons SDK into your project by adding
the following dependency to your pom.xml file:

```
<dependency>
    <groupId>com.cake.clockify</groupId>
    <artifactId>addon-sdk</artifactId>
    <version>${addonssdk.version}</version>
</dependency>
```

## Project
The project is organized in two modules:
- the annotation processor
- the addon SDK

## Annotation processor
The annotation processor module is only used at build-time. It generates helper interfaces based on the manifest schema.

Clockify manifest makes use of the ```@ExtendClockifyManifest``` annotation in order to let the annotation processor know
that it should generate interfaces for this class's builder according the specified definition.

For instance, suppose we have the following definition inside the json schema:
```json
"definitions": {
    "lifecycle": {
      "properties": {
        "path": {...},
        "type": {
          "enum": [
            "INSTALLED",
            "DELETED",
            "SETTINGS_UPDATED"
          ]
        }
      },
      "required": [...]
    }
    ...
}
```

The annotation processor will generate the following interfaces:
```java
public interface ClockifyLifecycleEventBuilderPathStep {
    ClockifyLifecycleEventBuilderTypeStep path(String value);
}

public interface ClockifyLifecycleEventBuilderTypeStep {
    ClockifyLifecycleEventBuilderBuildStep type(String value);

    default ClockifyLifecycleEventBuilderBuildStep onInstalled() {
        return type ("INSTALLED");
    }

    default ClockifyLifecycleEventBuilderBuildStep onDeleted() {
        return type ("DELETED");
    }

    default ClockifyLifecycleEventBuilderBuildStep onSettingsUpdated() {
        return type ("SETTINGS_UPDATED");
    }
}

public interface ClockifyLifecycleEventBuilderBuildStep {
    ClockifyLifecycleEvent build();
}
```

The above interfaces will result in a straightforward step-builder implementation.

Each required property will have its own step, and each step will only be able to set one property.
```java
ClockifyLifecycleEvent lifecycle = ClockifyLifecycleEvent
                .builder()
                .path("/lifecycle")
                .onInstalled()
                .build();
```


## Addon SDK
The SDK is structured into two parts, the shared codebase and the product-specific codebase:
- shared
- clockify, pumble, plaky

The product-specific layers will allow for more granular configuration and validation as well as provide specific helper classes.

### Features
- Predefined POJO models
- Product specific validation & helpers
- Centralized definition and handling of all the components of the addon
- Easy to get started by either relying on the embedded webserver or serving the provided servlet class through a web framework

### Getting started
First, create an addon instance:
```java
ClockifyAddon clockifyAddon = new ClockifyAddon(
        ClockifyManifest.v1_2Builder()
        .key(key)
        .name(name)
        .baseUrl(baseUrl)
        .requireBasicPlan()
        .scopes(List.of(
            ClockifyScope.PROJECT_READ,
            ClockifyScope.PROJECT_WRITE
        ))
        .build()
        );
```

The baseUrl must be a full URI, with the scheme, host and path.

Then, register any webhook / lifecycle / component / setting or custom endpoints on the addon.

Each object has its own builder class which by using the step builder pattern guides through the instantiation of the object and makes it easy to distinguish between required and optional properties.
```java
 ClockifyComponent component = ClockifyComponent
        .builder()
        .activityTab()
        .allowAdmins()
        .path("/component")
        .options(Map.of("opt1", "val1"))
        .build();

ClockifySetting number = ClockifySetting.builder()
        .id("id")
        .name("name")
        .asNumber()
        .value(12)
        .build();

ClockifySetting multipleSetting = ClockifySetting.builder()
        .id("id")
        .name("name")
        .asDropdownMultiple()
        .value(List.of("1", "2"))
        .allowedValues(List.of("1", "2", "3"))
        .build();


RequestHandler<HttpRequest> handler = new RequestHandler() { ... }
clockifyAddon.registerComponent(component, handler);
...
```

Addon-specific filters can be registered for requests that will be handled through the addon's servlet.
```java
Filter filter = ...; // servlet filter
clockifyAddon.addFilter(filter);
```

### Validating Clockify tokens
[https://dev-docs.marketplace.cake.com/development-toolkit/authentication-and-authorization/](https://dev-docs.marketplace.cake.com/development-toolkit/authentication-and-authorization/)

ClockifySignatureParser can be used to verify that the received tokens have been signed by Clockify:
```java
RSAPublicKey publicKey = ...;
ClockifySignatureParser parser = new ClockifySignatureParser("{manifest-key}", publicKey);

String token = ...;
Map<String, Object> claims = parser.parseClaims(token);
String workspaceId = (String) claims.get(ClockifySignatureParser.CLAIM_WORKSPACE_ID);
```

### Serving the addon
#### Using the embedded jetty server
```java
AddonServlet servlet = new AddonServlet(clockifyAddon);
EmbeddedServer server = new EmbeddedServer(servlet);
server.start(port);
```
#### Serving via spring boot (or any other framework)

```java
import addonsdk.shared.Addon;
import addonsdk.shared.AddonServlet;

@WebServlet(...)
public class AppServlet extends AddonServlet {

    public AppServlet(Addon addon) {
        super(addon);
    }
}
```

## Documentation
### Manifest
The central part of an addon is its manifest. The manifest defines the basic information related to the addon, as well as the components and the routes that it supports.

A handler is automatically registered on the GET '/manifest' path when an addon object is instantiated.

When invoked, this handler will return a JSON containing the serialized manifest.

### Handlers
The following arguments are required when registering a handler:
- the HTTP method
- the relative path
- the handler itself

If the handler is being registered through either a component / lifecycle / webhook, the inferred HTTP method will depend on the type of the object itself.
- component -> GET
- lifecycle -> POST
- webhook -> POST

Each addon implementation will only accept its own component / lifecycle / webhook subclasses.
```


# Maven Setup for Clockify Add-on SDK

- Registry: `https://maven.pkg.github.com/clockify/addon-java-sdk`
- Dependency coordinates: `com.cake.clockify:addon-sdk:<version>`
- Auth docs: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token

Add GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
   <repository>
       <id>github</id>
       <url>https://maven.pkg.github.com/clockify/addon-java-sdk</url>
   </repository>
</repositories>
```

Add the SDK dependency:

```xml
<dependencies>
   <dependency>
       <groupId>com.cake.clockify</groupId>
       <artifactId>addon-sdk</artifactId>
       <version>1.4.0</version>
   </dependency>
</dependencies>
```

Optional helper to create `~/.m2/settings.xml` with GitHub token: `extras/addon-java-sdk/configure-maven.sh`.


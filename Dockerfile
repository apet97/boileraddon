# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-17 AS build
ARG ADDON_DIR=addons/rules
WORKDIR /workspace

# Copy the repository into the build context (respecting .dockerignore) and build only the requested module
COPY . .

RUN test -d "${ADDON_DIR}" \
    && mvn -pl "${ADDON_DIR}" -am package -DskipTests \
    && JAR_PATH=$(find "${ADDON_DIR}/target" -maxdepth 1 -name '*-jar-with-dependencies.jar' -print -quit) \
    && [ -n "$JAR_PATH" ] \
    && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
ARG DEFAULT_BASE_URL=http://localhost:8080/rules
ENV ADDON_PORT=8080 \
    ADDON_BASE_URL=${DEFAULT_BASE_URL} \
    JAVA_OPTS=""
WORKDIR /opt/addon

COPY --from=build /workspace/app.jar ./app.jar

RUN groupadd --system addon && useradd --system --gid addon --uid 10001 addon \
    && chown addon:addon /opt/addon/app.jar

EXPOSE 8080

USER addon

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/addon/app.jar"]

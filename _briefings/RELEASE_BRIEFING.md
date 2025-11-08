DevOps/Release Briefing — Clockify Add-on Boilerplate
Repo commit: 239a31a40da23bfaa7eaf8720120d19723058eb4

Scope for this role:

Manage build pipelines, artifact packaging, and runtime images (Dockerfile & Make targets).

Enforce CI workflow stages, artifact uploads, and release verification steps.

Implement production deployment guidance (env vars, TLS, database, monitoring).

Align release notes with Improvements Summary and CHANGELOG for stakeholder communication.

Primary artifacts in repo:

Dockerfile

Makefile docker-run target

Production Deployment Guide

How to do your job:

Use multi-stage Dockerfile to build selected add-on module and produce lightweight runtime image; set ADDON_DIR/DEFAULT_BASE_URL build args per release target.

Provide make docker-run and ADDON_BASE_URL environment variables during release smoke tests to ensure manifest matches external URL.

Enforce CI workflow (validate + build/test) before tagging releases; capture uploaded artifacts for release packaging.

Follow Production Deployment Guide for env variable provisioning, webhook secret rotation, and TLS configuration in staging/prod.

Provision PostgreSQL/MySQL schemas or managed secrets according to DatabaseTokenStore recommendations before deploying.

Reference Docker Compose / Kubernetes templates when automating infrastructure rollouts; adapt health checks and restart policies accordingly.

Coordinate release notes with Improvements Summary and CHANGELOG to highlight new capabilities and dependencies.

Critical decisions already made:

Release images are built via multi-stage Dockerfile using Maven builder then Temurin JRE runtime.

CI pipeline compiles, tests, and uploads artifacts automatically; release must consume those outputs.

Production deployments must include TLS, database-backed token storage, rate limiting, and monitoring per guide.

Open questions and risks:

Owner	Source	Link
DevOps	Template manifest still points to localhost base URL; ensure release process rewrites baseUrl for production builds.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/_template-addon/manifest.json#L7-L28
DevOps	Auto lifecycle handler logs TODO on missing auth token; document fallback actions in runbooks before production launch.	https://github.com/apet97/boileraddon/blob/239a31a40da23bfaa7eaf8720120d19723058eb4/addons/auto-tag-assistant/src/main/java/com/example/autotagassistant/LifecycleHandlers.java#L47-L63
Commands or APIs you will call (if any):

docker build --build-arg ADDON_DIR=addons/auto-tag-assistant -t clockify-addon-auto-tag .

References:

Dockerfile build stages.

Production deployment guide (env, database, orchestration).

CI workflow for release gates.

# Docker Development

This guide shows how to run the add‑ons and a local PostgreSQL using Docker.

## Postgres via Compose (dev)

A dev compose file is included:

```
docker compose -f docker-compose.dev.yml up -d
```

Defaults:
- DB: `addons`
- USER/PASS: `addons` / `addons`

Export env for the app and run with DB‑backed token store:

```
export DB_URL=jdbc:postgresql://localhost:5432/addons
export DB_USER=addons
export DB_PASSWORD=addons
```

## Run an add‑on in Docker

Make target builds a lightweight image and runs it:

```
ADDON_BASE_URL=https://YOUR.ngrok-free.app/auto-tag-assistant \
make docker-run TEMPLATE=auto-tag-assistant
```

The container exposes `ADDON_PORT` (default 8080). Ensure the base URL includes the context path (`/auto-tag-assistant`, `/rules`, etc.).

## Health and metrics

Check in another terminal:

```
curl http://localhost:8080/<addon>/health
curl http://localhost:8080/<addon>/metrics
```

## Notes

- For production, see docs/PRODUCTION-DEPLOYMENT.md (HTTPS, rate limiting, logging, monitoring).
- Compose service names can be used as DB hosts inside Docker networks (e.g., `jdbc:postgresql://db:5432/addons`).


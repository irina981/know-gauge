# Deployment (single server, Docker Compose)

This folder runs KnowGauge on one server with the `prod` Spring profile. For development on your machine, use
[`local-development/`](../local-development/README.md) instead.

| File | Purpose |
|---|---|
| `docker-compose.yml` | The app, its databases, MinIO, the web UI and the Dozzle log viewer |
| `.env.example` | Every variable `docker-compose.yml` reads, with the required secrets first |
| `.env` | Your copy of `.env.example` with real values (gitignored, never commit it) |
| `dozzle/users.yml` | Dozzle login (gitignored, created in step 3) |

## What runs

| Service | Image | Published | What it is for |
|---|---|---|---|
| `knowgauge-ui` | built from `know-gauge-ui` | `KG_HTTP_PORT` (80) | The web UI. Forwards `/api` to the app |
| `knowgauge-service` | built from the root `Dockerfile` | – | The Spring Boot app, `prod` profile, JSON logs |
| `postgres` | `pgvector/pgvector:pg15` | – | Main database |
| `postgres-vectors` | `pgvector/pgvector:pg15` | – | Vector database |
| `minio` | `cgr.dev/chainguard/minio:latest` | – | Object storage for the uploaded PDFs. The app creates the bucket at startup |
| `dozzle` | `amir20/dozzle:latest` | `127.0.0.1:8888` | Log viewer, behind a login |

Only the UI is reachable from the network. Every container restarts automatically and keeps at most 3 × 20 MB
of Docker logs. Data lives in the named volumes `knowgauge_postgres_data`, `knowgauge_postgres_vectors_data` and
`knowgauge_minio_data`, so it survives `docker compose down` (but not `down -v`).

## First-time setup

The server needs Docker with the Compose plugin and about 4 GB of RAM.

1. Clone both repositories side by side:

   ```bash
   git clone https://github.com/irina981/know-gauge.git
   git clone https://github.com/irina981/know-gauge-ui.git
   cd know-gauge/deploy
   ```

   If the UI is elsewhere, set `KG_UI_SOURCE` in `.env`.

2. Create `.env` and fill in the four secrets:

   ```bash
   cp .env.example .env
   ```

3. Create the Dozzle login. This writes a bcrypt hash, not the password:

   ```bash
   docker run --rm amir20/dozzle generate admin --password 'choose-a-password' --name Admin > dozzle/users.yml
   ```

4. Build and start everything:

   ```bash
   docker compose up -d --build
   docker compose ps
   ```

   The first build downloads the Maven and npm dependencies and takes a few minutes. The UI starts once the app's
   healthcheck reports `UP`. Flyway creates the schema on the first start. The `prod` profile seeds no topics or
   documents.

## Logs

The app logs one JSON object per line in the Elastic Common Schema (ECS) format
(`logging.structured.format.console` in `application-prod.properties`). Lines written during ingestion carry a
`documentId` field and lines written during test generation a `testId` field.

Dozzle shows the logs of all containers, with live tail, search, level filters and JSON lines as fields. It reads
the Docker socket, which gives full control of the server, so it only listens on `127.0.0.1`. To open it from your
computer, use an SSH tunnel:

```bash
ssh -L 8888:localhost:8888 user@your-server
```

Then browse to http://localhost:8888 and sign in with the user from step 3.

Dozzle stores no logs. It shows what Docker keeps: up to 60 MB per container, and nothing from a container that
was removed (`docker compose up` after a rebuild recreates the container). For longer history, dashboards or
alerts, add Grafana Loki with Grafana Alloy; the JSON format stays the same.

## Updating

```bash
cd know-gauge && git pull && cd ../know-gauge-ui && git pull && cd ../know-gauge/deploy
docker compose up -d --build
```

## Not covered yet

- **HTTPS.** The UI is published as plain HTTP. Put a reverse proxy with TLS (for example Caddy or Traefik) in
  front of it, or set `KG_HTTP_PORT` to a local port and route to it from your provider's load balancer.
- **Backups.** Back up the three volumes (or use `pg_dump` for the databases).
- **Authentication.** There is no login yet: anyone who can open the UI uses the single tenant `KG_TENANT_ID`.

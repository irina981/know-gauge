# Deployment (single server, Docker Compose)

This folder runs KnowGauge on one server with the `prod` Spring profile. For development on your machine, use
[`local-development/`](../local-development/README.md) instead.

| File | Purpose |
|---|---|
| `docker-compose.yml` | The app, one Postgres, Keycloak, MinIO, the web UI and the Dozzle log viewer |
| `keycloak/knowgauge-realm.json` | The `knowgauge` realm imported on the first start: UI client and the first admin's sign-in (no roles, no demo users). The app makes `KG_ADMIN_EMAIL` an `ADMIN` at sign-in (ADR012) |
| `.env.example` | Every variable `docker-compose.yml` reads, with the required secrets first |
| `.env` | Your copy of `.env.example` with real values (gitignored, never commit it) |
| `dozzle/users.yml` | Dozzle login (gitignored, created in step 3) |

## What runs

| Service | Image | Published | What it is for |
|---|---|---|---|
| `knowgauge-ui` | built from `know-gauge-ui` | `KG_HTTP_PORT` (80) | The web UI. Forwards `/api` to the app |
| `knowgauge-service` | built from the root `Dockerfile` | – | The Spring Boot app, `prod` profile, JSON logs |
| `postgres` | `pgvector/pgvector:pg15` | – | One database (`knowgauge`) with three schemas (ADR011): `knowgauge` (app data), `vectors` (embeddings) and `keycloak` (Keycloak, its own login). [`../postgres/initdb`](../postgres/initdb/01-schemas.sh) creates them on the first start |
| `keycloak` | `quay.io/keycloak/keycloak:26.8.2` | `127.0.0.1:8180` | Sign-in (ADR010), production mode, for a reverse proxy with HTTPS at `KG_AUTH_URL`. Loads the login theme from the `know-gauge-ui` clone |
| `minio` | `cgr.dev/chainguard/minio:latest` | – | Object storage for the uploaded PDFs. The app creates the bucket at startup |
| `dozzle` | `amir20/dozzle:latest` | `127.0.0.1:8888` | Log viewer, behind a login |

Only the UI is reachable from the network; Keycloak and Dozzle listen on `127.0.0.1`. Every container restarts
automatically and keeps at most 3 × 20 MB of Docker logs. Data lives in the named volumes `knowgauge_db_data`
(all three schemas) and `knowgauge_minio_data`, so it survives `docker compose down` (but not `down -v`).

## First-time setup

The server needs Docker with the Compose plugin and about 4 GB of RAM.

1. Clone both repositories side by side:

   ```bash
   git clone https://github.com/irina981/know-gauge.git
   git clone https://github.com/irina981/know-gauge-ui.git
   cd know-gauge/deploy
   ```

   If the UI is elsewhere, set `KG_UI_SOURCE` in `.env`.

2. Create `.env` and fill in the required values: the secrets, the public UI and Keycloak addresses
   (`KG_PUBLIC_UI_URL`, `KG_AUTH_URL`) and the first admin (`KG_ADMIN_EMAIL`, `KG_ADMIN_PASSWORD`, temporary):

   ```bash
   cp .env.example .env
   ```

   Build the Keycloak login theme in the UI clone (needs Node 20 and Maven):

   ```bash
   cd ../../know-gauge-ui && npm ci && npm run build-keycloak-theme && cd -
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
   healthcheck reports `UP`. Flyway creates the app's tables on the first start, and Keycloak imports the realm.
   The `prod` profile seeds no topics or documents. Sign in at the UI with `KG_ADMIN_EMAIL`; Keycloak asks for a new
   password.

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

- **HTTPS.** The UI and Keycloak speak plain HTTP. Put a reverse proxy with TLS (for example Caddy) in front of both:
  the UI at `KG_PUBLIC_UI_URL` and Keycloak (`127.0.0.1:8180`) at `KG_AUTH_URL`. Sign-in needs HTTPS outside
  localhost.
- **Backups.** One `pg_dump` of the `knowgauge` database covers the app data, the embeddings and Keycloak. Back up
  the MinIO volume too.

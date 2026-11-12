# Local development

This folder holds the Docker Compose setup for running KnowGauge on your machine. It contains:

| File | Purpose |
|---|---|
| `docker-compose.yml` | The backing services and the containerised app |
| `.env.example` | Template for the environment variables that Compose and the app read |
| `.env` | Your local copy of `.env.example` (gitignored, never commit it) |

## Setup

Copy the template and fill in your OpenAI key:

```bash
cp local-development/.env.example local-development/.env
```

Compose reads `.env` from this folder, both for `${...}` substitution in `docker-compose.yml` and through each
service's `env_file`.

## What gets started

All containers join the `knowgauge-network` bridge network. Each service has a healthcheck, and data is kept in
named volumes, so it survives `docker compose down` (use `down -v` to wipe it).

| Service | Container | Image | Host ports | Volume | What it is for |
|---|---|---|---|---|---|
| `postgres` | `knowgauge-postgres` | `pgvector/pgvector:pg15` | 5432 | `postgres_data` | Main database: topics, documents, chunks, tests, questions, attempts, generation runs. Schema created by Flyway on app startup |
| `postgres-vectors` | `knowgauge-postgres-vectors` | `pgvector/pgvector:pg15` | 5433 → 5432 | `postgres_vectors_data` | Vector database: chunk embeddings (`vector(1536)`) used for retrieval |
| `minio` | `knowgauge-minio` | `minio/minio:latest` | 9000 (S3 API), 9001 (web console) | `minio_data` | Object storage for the uploaded PDF files |
| `knowgauge-service` | `knowgauge-service` | built from the root `Dockerfile` | 8080 (HTTP), 5005 (JDWP debugger) | – | The Spring Boot app, run with the `docker` profile. Starts only after the other three are healthy |

The database and MinIO credentials come from `.env` (`POSTGRES_*`, `POSTGRES_VECTORS_*`, `MINIO_ROOT_*`). The
app also needs `OPENAI_API_KEY` for embeddings and test generation.

With the `docker` (and `dev`) profile the app seeds a fixed topic tree, creates the MinIO bucket and uploads four
sample PDFs from `knowgauge-service-rest-api/src/main/resources/devdocs/` at startup.

## Option 1: backing services only (run the app from the IDE)

This is the usual setup while coding. Start the databases and MinIO in Docker, then run the app locally with the
`dev` profile, which points at `localhost:5432`, `localhost:5433` and `localhost:9000`.

From the repository root:

```bash
docker compose -f local-development/docker-compose.yml up -d postgres postgres-vectors minio
```

```bash
mvn -pl knowgauge-service-rest-api spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile has the local database and MinIO credentials built in, but `OPENAI_API_KEY` must be set in your
environment. The VS Code launch configuration loads it from `local-development/.env`.

## Option 2: everything in containers

From the repository root:

```bash
docker compose -f local-development/docker-compose.yml up --build
```

Compose builds the app image with the repository root as the build context. The service is ready once its
healthcheck (`/actuator/health/readiness`) reports `UP`. To debug, attach a remote debugger to port 5005.

## Useful URLs

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| MinIO console | http://localhost:9001 (log in with `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`) |

## Common commands

You can also run these from inside this folder and drop the `-f` flag.

```bash
docker compose -f local-development/docker-compose.yml ps
```

```bash
docker compose -f local-development/docker-compose.yml logs -f knowgauge-service
```

```bash
docker compose -f local-development/docker-compose.yml down
```

```bash
docker compose -f local-development/docker-compose.yml down -v
```

The last command also deletes the volumes, so the databases and stored files start empty next time.

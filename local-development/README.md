# Local development

This folder holds the Docker Compose setup for running KnowGauge on your machine. It contains:

| File | Purpose |
|---|---|
| `docker-compose.yml` | The backing services and the containerised app |
| `.env.example` | Optional overrides for the variables `docker-compose.yml` reads |
| `.env` | Optional local copy of `.env.example` (gitignored, never commit it) |

## Setup

This setup is for local development only, so no `.env` file is required. Every variable in `docker-compose.yml`
has a default that matches `application-dev.properties`:

| Variable | Default |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `knowgauge` / `knowgauge_user` / `knowgauge_pass` |
| `POSTGRES_VECTORS_DB` / `POSTGRES_VECTORS_USER` / `POSTGRES_VECTORS_PASSWORD` | `knowgauge_vectors` / `knowgauge_user` / `knowgauge_pass` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `minio` / `minio12345` |
| `OPENAI_API_KEY` | none (needed for ingestion and test generation) |
| `KG_OLLAMA_MODEL` | `qwen2.5:7b-instruct` (local model for distractor verification) |

The only value you need to supply is `OPENAI_API_KEY`, and only when running the app in a container. Set it in your
shell, or copy `.env.example` to `.env` and put it there. Compose reads `.env` from this folder (if it exists) for
`${...}` substitution, and shell variables take precedence. If you change a default, change
`application-dev.properties` too, otherwise the app run from the IDE can't connect.

## What gets started

All containers join the `knowgauge-network` bridge network. Each service has a healthcheck, and data is kept in
named volumes, so it survives `docker compose down` (use `down -v` to wipe it).

| Service | Container | Image | Host ports | Volume | What it is for |
|---|---|---|---|---|---|
| `postgres` | `knowgauge-postgres` | `pgvector/pgvector:pg15` | 5432 | `postgres_data` | Main database: topics, documents, chunks, tests, questions, attempts, generation runs. Schema created by Flyway on app startup |
| `postgres-vectors` | `knowgauge-postgres-vectors` | `pgvector/pgvector:pg15` | 5433 → 5432 | `postgres_vectors_data` | Vector database: chunk embeddings (`vector(1536)`) used for retrieval |
| `minio` | `knowgauge-minio` | `minio/minio:latest` | 9000 (S3 API), 9001 (web console) | `minio_data` | Object storage for the uploaded PDF files |
| `ollama` | `knowgauge-ollama` | `ollama/ollama:latest` | 11434 | `ollama_data` | Local LLM server. Verifies distractors during test generation, so that pass costs no OpenAI tokens |
| `ollama-pull` | `knowgauge-ollama-pull` | `ollama/ollama:latest` | – | – | One-shot: pulls `KG_OLLAMA_MODEL` into `ollama_data`, then exits. The first pull downloads about 4.7 GB |
| `knowgauge-service` | `knowgauge-service` | built from the root `Dockerfile` | 8080 (HTTP), 5005 (JDWP debugger) | – | The Spring Boot app, run with the `docker` profile. Only in the `full` compose profile (see option 2). Starts only after the other services are healthy |

The database and MinIO credentials come from `POSTGRES_*`, `POSTGRES_VECTORS_*` and `MINIO_ROOT_*` (defaults
above). The app also needs `OPENAI_API_KEY` for embeddings and test generation.

With the `docker` (and `dev`) profile the app seeds a fixed topic tree (root "Knowledge Library" with "Programming"
and "Employee Onboarding & Compliance"), creates the MinIO bucket and uploads the sample PDFs from
`knowgauge-service-rest-api/src/main/resources/devdocs/` at startup. The uploads are not ingested automatically; call
the ingestion endpoint for each document before generating tests. The onboarding policy PDFs are generated from
[`demo-content/`](../demo-content/README.md).

## Option 1: backing services only (run the app from the IDE)

This is the usual setup while coding. Start the databases, MinIO and Ollama in Docker, then run the app locally with
the `dev` profile, which points at `localhost:5432`, `localhost:5433`, `localhost:9000` and `localhost:11434`.

A plain `up` starts every service except the app, which sits in the `full` compose profile. From the repository root:

```bash
docker compose -f local-development/docker-compose.yml up -d
```

If Ollama already runs natively on port 11434, start only `postgres postgres-vectors minio` and run
`ollama pull qwen2.5:7b-instruct` once. To verify with OpenAI instead, set
`kg.testgen.verification.provider=openai`.

```bash
mvn -pl knowgauge-service-rest-api spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile has the local database and MinIO credentials built in, but `OPENAI_API_KEY` must be set in your
environment. The VS Code launch configuration loads it from `local-development/.env`.

## Option 2: everything in containers

From the repository root:

Enable the `full` profile to also build and start the app container:

```bash
docker compose -f local-development/docker-compose.yml --profile full up --build
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
docker compose -f local-development/docker-compose.yml --profile full down
```

```bash
docker compose -f local-development/docker-compose.yml --profile full down -v
```

Pass `--profile full` to `down` so it also removes the app container. The last command also deletes the volumes, so the databases and stored files start empty next time.

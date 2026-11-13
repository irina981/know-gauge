# Local development

This folder holds the Docker Compose setup for running KnowGauge on your machine. It contains:

| File | Purpose |
|---|---|
| `docker-compose.yml` | The backing services and the containerised app |
| `.env.example` | Optional overrides for the variables `docker-compose.yml` reads |
| `.env` | Optional local copy of `.env.example` (gitignored, never commit it) |

## Setup

This setup is for local development only, so no `.env` file is required. Every variable in `docker-compose.yml`
has a default that matches `application-local.properties`:

| Variable | Default |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `knowgauge` / `knowgauge_user` / `knowgauge_pass` |
| `POSTGRES_VECTORS_DB` / `POSTGRES_VECTORS_USER` / `POSTGRES_VECTORS_PASSWORD` | `knowgauge_vectors` / `knowgauge_user` / `knowgauge_pass` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `minio` / `minio12345` |
| `OPENAI_API_KEY` | none (needed for ingestion and test generation) |
| `KG_TESTGEN_VERIFICATION_PROVIDER` | `openai` (set `ollama`, with `--profile ollama`, to verify locally) |
| `KG_OLLAMA_MODEL` | `qwen2.5:7b-instruct` (opt-in local model for distractor verification) |

The only value you need to supply is `OPENAI_API_KEY`, and only when running the app in a container. Set it in your
shell, or copy `.env.example` to `.env` and put it there. Compose reads `.env` from this folder (if it exists) for
`${...}` substitution, and shell variables take precedence. If you change a default, change
`application-local.properties` too, otherwise the app run from the IDE can't connect.

## What gets started

All containers join the `knowgauge-network` bridge network. The databases and the app have healthchecks, and data is kept in
named volumes, so it survives `docker compose down` (use `down -v` to wipe it).

| Service | Container | Image | Host ports | Volume | What it is for |
|---|---|---|---|---|---|
| `postgres` | `knowgauge-postgres` | `pgvector/pgvector:pg15` | 5432 | `postgres_data` | Main database: topics, documents, chunks, tests, questions, attempts, generation runs. Schema created by Flyway on app startup |
| `postgres-vectors` | `knowgauge-postgres-vectors` | `pgvector/pgvector:pg15` | 5433 → 5432 | `postgres_vectors_data` | Vector database: chunk embeddings (`vector(1536)`) used for retrieval |
| `minio` | `knowgauge-minio` | `cgr.dev/chainguard/minio:latest` | 9000 (S3 API), 9001 (web console) | `minio_data` | Object storage for the uploaded PDF files |
| `dozzle` | `knowgauge-dozzle` | `amir20/dozzle:latest` | 8888 → 8080 (localhost only) | – | Web log viewer for all containers on this Docker host (see [Logs](#logs)) |
| `ollama` | `knowgauge-ollama` | `ollama/ollama:latest` | 11434 | `ollama_data` | Opt-in (`--profile ollama`). Local LLM server for the optional local distractor verifier |
| `ollama-pull` | `knowgauge-ollama-pull` | `ollama/ollama:latest` | – | – | Opt-in (`--profile ollama`). One-shot: pulls `KG_OLLAMA_MODEL` into `ollama_data`, then exits. The first pull downloads about 4.7 GB |
| `knowgauge-service` | `knowgauge-service` | built from the root `Dockerfile` | 8080 (HTTP), 5005 (JDWP debugger) | – | The Spring Boot app, run with the `local` profile (hostnames overridden through env vars). Only in the `full` compose profile (see option 2). Starts only after the other services are healthy |

The database and MinIO credentials come from `POSTGRES_*`, `POSTGRES_VECTORS_*` and `MINIO_ROOT_*` (defaults
above). The app also needs `OPENAI_API_KEY` for embeddings and test generation.

With the `local` profile the app seeds a fixed topic tree (root "Knowledge Library" with "Programming"
and "Employee Onboarding & Compliance"), creates the MinIO bucket and uploads the sample PDFs from
`knowgauge-service-rest-api/src/main/resources/devdocs/` at startup. The uploads are not ingested automatically; call
the ingestion endpoint for each document before generating tests. The onboarding policy PDFs are generated from
[`demo-content/`](../demo-content/README.md).

## Option 1: backing services only (run the app from the IDE)

This is the usual setup while coding. Start the databases and MinIO in Docker, then run the app locally with the
`local` profile, which points at `localhost:5432`, `localhost:5433` and `localhost:9000`.

A plain `up` starts only these backing services and Dozzle. The app sits in the `full` compose profile and Ollama in the
`ollama` compose profile. From the repository root:

```bash
docker compose -f local-development/docker-compose.yml up -d
```

Distractor verification uses OpenAI `gpt-4.1`. To try the local Ollama verifier instead (it judged distractors
unreliably, see [ADR003](../docs/adr/ADR003-grounded-reason-first-verification.md)), add `--profile ollama` and set
`kg.testgen.verification.provider=ollama`. If Ollama already runs natively on port 11434, run
`ollama pull qwen2.5:7b-instruct` once instead.

```bash
mvn -pl knowgauge-service-rest-api spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile has the local database and MinIO credentials built in, but `OPENAI_API_KEY` must be set in your
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
| Logs (Dozzle) | http://localhost:8888 |
| MinIO console | http://localhost:9001 (log in with `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`) |

## Logs

Dozzle shows the logs of every container on your Docker host at **http://localhost:8888**: live tail, search
(plain text or regex), level filters, several containers side by side, and download. It reads the Docker socket
and stores nothing, so it shows what Docker still keeps for each container. It has no login and listens on
`localhost` only.

The app run from the IDE logs to your IDE console, not to Docker, so Dozzle only shows it when the app runs in a
container (option 2). The `local` profile logs plain text. To see the JSON format used in production, uncomment
`LOGGING_STRUCTURED_FORMAT_CONSOLE: ecs` under `knowgauge-service` in `docker-compose.yml`: Dozzle then shows each
line as fields, including `documentId` during ingestion and `testId` during test generation.

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

Pass `--profile full` to `down` so it also removes the app container (add `--profile ollama` if you started Ollama). The last command also deletes the volumes, so the databases and stored files start empty next time.

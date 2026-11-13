# 09 – Configuration & Deployment

## Configuration files

All application configuration is in `knowgauge-service-rest-api/src/main/resources/`:

| File | Purpose |
|---|---|
| `application.properties` | Values common to all environments: multipart limits, actuator, springdoc, JPA `validate`, Flyway, JDBC drivers, all shared `kg.*` properties |
| `application-local.properties` | Local development against `local-development/docker-compose.yml` on `localhost` (5432, 5433, 9000). Local credentials, seed topic migration and seed PDFs, DEBUG logging. The app container in compose uses it too, with hostnames overridden through env vars |
| `application-test.properties` | Automated tests only. Schema migrations without seed data, no seeder, a placeholder OpenAI key. Datasources and the MinIO endpoint are supplied by the test (for example Testcontainers) |
| `application-prod.properties` | Production. Every environment-specific value comes from an env var (see below); required ones have no default, so a missing one fails startup. No seed data, Swagger off, actuator limited to `health,info,metrics` |
| `resilience4j.properties` | Retry, bulkhead and circuit breaker instances (imported through `spring.config.import`) |

**Profiles**: `local`, `test` and `prod`. Exactly one must be active: the base file does not set the datasources,
the storage endpoint or `kg.context.tenant-id`, so the application **does not start without a profile**. The
`Dockerfile` sets `SPRING_PROFILES_ACTIVE=prod`; compose and the IDE launch configuration use `local`.

`DefaultTenantProvider` and `DefaultUserProvider` return the fixed `kg.context.tenant-id` / `kg.context.user-id`
(`1` / `1` in `local` and `test`, `KG_TENANT_ID` / `KG_USER_ID` in `prod`). They stand in until authentication
provides the tenant and user, so a prod deployment serves one tenant.

### Production environment variables

| Variable | Required | Property |
|---|---|---|
| `KG_DB_URL`, `KG_DB_USERNAME`, `KG_DB_PASSWORD` | yes | `spring.datasource.*` |
| `KG_VECTOR_DB_URL`, `KG_VECTOR_DB_USERNAME`, `KG_VECTOR_DB_PASSWORD` | yes | `kg.pgvector.datasource.*` |
| `KG_STORAGE_ENDPOINT`, `KG_STORAGE_ACCESS_KEY`, `KG_STORAGE_SECRET_KEY` | yes | `kg.storage.endpoint`, `accessKey`, `secretKey` |
| `KG_STORAGE_BUCKET` | no (`knowgauge-content`) | `kg.storage.bucket`. The bucket must exist unless `KG_STORAGE_CREATE_BUCKET=true` |
| `KG_STORAGE_CREATE_BUCKET` | no (`false`) | `kg.storage.create-bucket-on-startup`: create the bucket at startup if missing (`StartupConfig`); startup fails if storage is unreachable |
| `OPENAI_API_KEY` | yes | `kg.embedding.openai.api-key`, `kg.testgen.chat-model.openai.api-key` |
| `KG_TENANT_ID` | yes | `kg.context.tenant-id` |
| `KG_USER_ID` | no (empty: no user) | `kg.context.user-id` |
| `KG_GENERATION_MODEL`, `KG_VERIFICATION_MODEL` | no (`gpt-4.1`) | `kg.testgen.defaults.generation-model`, `kg.testgen.verification.chat-model.openai.model` |
| `KG_VERIFICATION_PROVIDER`, `KG_OLLAMA_BASE_URL` | no (`openai`, `http://localhost:11434`) | `kg.testgen.verification.provider`, `chat-model.ollama.base-url` |
| `KG_INGESTION_JOB_ENABLED`, `KG_INGESTION_JOB_CRON`, `KG_INGESTION_JOB_BATCH_SIZE` | no (`true`, `0 */5 * * * *`, `5`) | `kg.ingestion.job.*` (see [04](04-document-ingestion.md#scheduled-ingestion-job)) |
| `KG_LOG_LEVEL` | no (`INFO`) | `logging.level.com.knowgauge` |
| `KG_LOG_FORMAT` | no (`ecs`) | `logging.structured.format.console`: JSON log lines (`ecs`, `logstash` or `gelf`) |
| `KG_ENVIRONMENT` | no (`prod`) | `logging.structured.ecs.service.environment` |
| `KG_ACTUATOR_ENDPOINTS` | no (`health,info,metrics`) | `management.endpoints.web.exposure.include` |
| `KG_SWAGGER_ENABLED` | no (`false`) | `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled` |

## Custom property namespaces (`kg.*`)

| Prefix | Bound by | Keys |
|---|---|---|
| `kg.storage.*` | `StorageProperties`, `MinIoConfig`, `MinIoStorageService` | `type`, `bucket`, `storage-key-template`, `endpoint`, `accessKey`, `secretKey`, `connectTimeout`, `readTimeout`, `writeTimeout`, `callTimeout` |
| `kg.chunking.*` | `ChunkingProperties` | `maxChunkSizeChars`, `overlapSizeChars`, `trimWhitespace`, `includePageMetadata` |
| `kg.ingestion.job.*` | `IngestionJobProperties`, `ScheduledIngestionJob`, `SchedulingConfig` | `enabled`, `cron`, `batch-size` |
| `kg.embedding.*` | `OpenAiEmbeddingConfig`, `OpenAiEmbeddingServiceImpl` | `dimension`, `provider`, `openai.api-key`, `openai.model`, `openai.base-url`, `openai.timeout` |
| `kg.testgen.defaults.*` | `TestGenerationDefaultsProperties` | See [06 – tunables](06-test-generation.md#tunables-defaults) |
| `kg.testgen.prompt.templates.*` | `ClasspathPromptTemplateLoader`, `SchemaProvidersConfig` | `classpathBase`, `output-schema-file` |
| `kg.testgen.chat-model.openai.*` | `OpenAiChatModelProperties`, `OpenAiConfig` | `api-key`, `model`, `temperature`, `max-output-tokens`, `timeout-seconds`, `structured-output-model-prefixes`, `reasoning-model-prefixes` (`gpt-5`), `reasoning-effort` (`low`), `reasoning-max-completion-tokens` (`8000`); see `OpenAiReasoningModels` |
| `kg.testgen.verification.*` | `OpenAiVerificationModelProperties`, `OllamaVerificationModelProperties`, `VerificationPromptBuilder`, `SchemaProvidersConfig` | `provider` (`openai`/`ollama`), `chat-model.openai.*`, `chat-model.ollama.*` (`base-url`, `model`, `temperature`, `max-output-tokens`, `timeout-seconds`), `prompt.templates.*`, `defaults.prompt-template-id` |
| `kg.pgvector.datasource.*` | `PgVectorJpaConfig` | `url` / `jdbc-url`, `username`, `password`, `driver-class-name` |
| `kg.pgvector.flyway.*` | `PgVectorFlywayConfig` | `enabled`, `locations`, `schemas` |
| `kg.pgvector.jpa.*` | (declared but not read by the vector EMF) | `hibernate.ddl-auto`, `show-sql`, … |
| `kg.dev.seed.enabled` | `DevDocumentSeeder` | Uploads the sample PDFs listed in `SEED_DOCUMENTS` from `resources/devdocs/` under the fixed seed topics (`local` profile only) |
| `kg.context.*` | `DefaultTenantProvider`, `DefaultUserProvider` | `tenant-id` (required), `user-id` (optional) |

Property-name styles are mixed (`camelCase` in `kg.chunking`/`kg.storage` timeouts, `kebab-case` elsewhere).
Spring relaxed binding accepts both for `@ConfigurationProperties`, but `@Value` needs the exact form.

## Secrets and environment variables

Compose is for local development only, so every variable in `docker-compose.yml` has a `${VAR:-default}` fallback
that matches `application-local.properties`, and no `.env` file is required. `local-development/.env` (gitignored) is
an optional override file; `local-development/.env.example` is its template:

| Variable | Default | Used for |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `knowgauge`, `knowgauge_user`, `knowgauge_pass` | Main DB container and `SPRING_DATASOURCE_*` |
| `POSTGRES_VECTORS_DB`, `POSTGRES_VECTORS_USER`, `POSTGRES_VECTORS_PASSWORD` | `knowgauge_vectors`, `knowgauge_user`, `knowgauge_pass` | Vector DB container and `KG_PGVECTOR_DATASOURCE_*` |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | `minio`, `minio12345` | MinIO container and `KG_STORAGE_ACCESSKEY/SECRETKEY` |
| `MINIO_BUCKET` | – | Informational (the app uses `kg.storage.bucket`) |
| `OPENAI_API_KEY` | none | `kg.embedding.openai.api-key`, `kg.testgen.chat-model.openai.api-key` |
| `KG_TESTGEN_VERIFICATION_PROVIDER` | `openai` | Verifier for the app container; `ollama` needs `--profile ollama` |
| `KG_OLLAMA_MODEL` | `qwen2.5:7b-instruct` | Model pulled by `ollama-pull` and used by the verifier (`kg.testgen.verification.chat-model.ollama.model`) |

The `local` profile has local credentials hard-coded (`knowgauge_user/knowgauge_pass`, `minio/minio12345`), but
`OPENAI_API_KEY` must still be set in the environment. The OpenAI verification model inherits the chat model's API
key. All profiles verify distractors with OpenAI `gpt-4.1`. Setting `kg.testgen.verification.provider=ollama` moves
that pass to the local Ollama model, which needs no API key but judged distractors unreliably (ADR003).

## Docker Compose

The compose file lives in [`local-development/`](../../local-development/README.md), with the repository root as the
app's build context (`..`). The app service is in the `full` compose profile, so a plain `up` starts only the backing
services, and `docker compose -f local-development/docker-compose.yml --profile full up --build` also starts the app.
Ollama is in the `ollama` compose profile and starts only with `--profile ollama`.
All services join the `knowgauge-network` bridge:

| Service | Image | Ports | Volume |
|---|---|---|---|
| `postgres` | `pgvector/pgvector:pg15` | 5432 | `postgres_data` |
| `postgres-vectors` | `pgvector/pgvector:pg15` | 5433→5432 | `postgres_vectors_data` |
| `minio` | `cgr.dev/chainguard/minio:latest` | 9000 (S3), 9001 (console) | `minio_data` |
| `ollama` (profile `ollama`) | `ollama/ollama:latest` | 11434 | `ollama_data` |
| `ollama-pull` (profile `ollama`, one-shot) | `ollama/ollama:latest` | – | – (pulls `KG_OLLAMA_MODEL` into `ollama_data`, then exits) |
| `dozzle` | `amir20/dozzle:latest` | 8888→8080 on `127.0.0.1` (log viewer, no login) | – (reads the Docker socket) |
| app (`knowgauge-service`, profile `full`) | built from `Dockerfile` | 8080, 5005 (JDWP, `suspend=n`) | – |

The databases and the app have healthchecks; MinIO has none (its image has no shell or curl). The app `depends_on`
the databases with `condition: service_healthy` and on MinIO with `condition: service_started`. Its own healthcheck polls `/actuator/health/readiness`. The app uses
`SPRING_PROFILES_ACTIVE=local`, and the compose file overrides the `localhost` values with the service hostnames
through env vars, which take precedence over properties files (`SPRING_DATASOURCE_*`, `KG_PGVECTOR_DATASOURCE_*`,
`KG_STORAGE_ENDPOINT`/`ACCESSKEY`/`SECRETKEY`, `OPENAI_API_KEY`, `KG_TESTGEN_VERIFICATION_*`).

For local development in the IDE, run only the backing services
(`docker compose -f local-development/docker-compose.yml up -d`) and start the app with `-Dspring.profiles.active=local` (see `.vscode/launch.json`).

### Deployment compose

[`deploy/docker-compose.yml`](../../deploy/README.md) runs the same stack on one server with the `prod` profile:
the app (all settings from `deploy/.env`, `KG_STORAGE_CREATE_BUCKET=true`), both databases, MinIO, the UI built
from a `know-gauge-ui` clone (the only published port, `KG_HTTP_PORT`, default 80) and Dozzle on `127.0.0.1:8888`
with a login from `deploy/dozzle/users.yml`. Every container restarts `unless-stopped` and rotates its Docker log
(3 × 20 MB). The compose project is named `knowgauge`. See [ADR007](../adr/ADR007-json-logs-and-dozzle.md).

## Build & image

- Maven reactor from the root `pom.xml`: `mvn clean install` (or `-DskipTests`; there are no tests yet).
- The parent POM imports the BOMs for Spring Boot 3.5.16, LangChain4j 1.1.0 and Resilience4j 2.2.0, and configures
  the annotation processors **Lombok, then MapStruct** in that order. The order matters for MapStruct to see the
  Lombok-generated accessors.
- Multi-stage `Dockerfile`: copy all module POMs and run `mvn validate` to warm the cache layer, copy the sources,
  run `mvn -DskipTests clean package`, and copy `knowgauge-service-rest-api/target/*.jar` into a
  `temurin:21-jre-alpine` runtime image. The image sets `SPRING_PROFILES_ACTIVE=prod`.
- **Adding a module** means updating the root `<modules>`, the `Dockerfile` POM `COPY` list and the `rest-api`
  dependencies.

## Observability

- Logs: SLF4J/Logback, to the console. `local` and `test` log plain text; `prod` logs one JSON object per line in the
  Elastic Common Schema (`logging.structured.format.console`, `KG_LOG_FORMAT`), with the MDC keys `documentId`
  (ingestion) and `testId` (test generation, from the moment the test is persisted) as fields. Both compose files
  include Dozzle to browse and filter container logs ([ADR007](../adr/ADR007-json-logs-and-dozzle.md)).
  `com.knowgauge` logs at DEBUG in `local` and `test`, `KG_LOG_LEVEL` (default INFO) in `prod`. Ingestion and generation steps log progress
  lines prefixed with `***` (start and end) and indented step messages. Validators log each rejection with its
  reason. LLM calls log token usage and finish reason.
- Actuator: health (liveness/readiness), metrics, env, beans, loggers. `env` and `beans` are exposed with no auth,
  which is only acceptable locally; `prod` exposes only `health,info,metrics` unless `KG_ACTUATOR_ENDPOINTS` says
  otherwise.
- There is no correlation ID, tracing or metrics for LLM cost yet (on the backlog).

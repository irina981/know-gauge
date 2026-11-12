# 09 – Configuration & Deployment

## Configuration files

All application configuration is in `knowgauge-service-rest-api/src/main/resources/`:

| File | Purpose |
|---|---|
| `application.properties` | Shared defaults: multipart limits, actuator, springdoc, JPA `validate`, Flyway, all `kg.*` properties |
| `application-dev.properties` | Local run against containers on `localhost` (5432, 5433, 9000). Adds dev seed migrations. DEBUG logging. DevTools |
| `application-docker.properties` | Inside Docker Compose (service hostnames). Credentials come from env vars |
| `resilience4j.properties` | Retry, bulkhead and circuit breaker instances (imported through `spring.config.import`) |

**Profiles**: `dev` and `docker`. The only `CurrentTenantProvider` and `CurrentUserProvider` beans are annotated
`@Profile({"dev","docker"})`, so the application **does not start without one of these profiles**. A production
profile needs real implementations first.

## Custom property namespaces (`kg.*`)

| Prefix | Bound by | Keys |
|---|---|---|
| `kg.storage.*` | `StorageProperties`, `MinIoConfig`, `MinIoStorageService` | `type`, `bucket`, `storage-key-template`, `endpoint`, `accessKey`, `secretKey`, `connectTimeout`, `readTimeout`, `writeTimeout`, `callTimeout` |
| `kg.chunking.*` | `ChunkingProperties` | `maxChunkSizeChars`, `overlapSizeChars`, `trimWhitespace`, `includePageMetadata` |
| `kg.embedding.*` | `OpenAiEmbeddingConfig`, `OpenAiEmbeddingServiceImpl` | `dimension`, `provider`, `openai.api-key`, `openai.model`, `openai.base-url`, `openai.timeout` |
| `kg.testgen.defaults.*` | `TestGenerationDefaultsProperties` | See [06 – tunables](06-test-generation.md#tunables-defaults) |
| `kg.testgen.prompt.templates.*` | `ClasspathPromptTemplateLoader`, `SchemaProvidersConfig` | `classpathBase`, `output-schema-file` |
| `kg.testgen.chat-model.openai.*` | `OpenAiChatModelProperties`, `OpenAiConfig` | `api-key`, `model`, `temperature`, `max-output-tokens`, `timeout-seconds`, `structured-output-model-prefixes` |
| `kg.testgen.verification.*` | `OpenAiVerificationModelProperties`, `VerificationPromptBuilder`, `SchemaProvidersConfig` | `chat-model.openai.*`, `prompt.templates.*`, `defaults.prompt-template-id` |
| `kg.pgvector.datasource.*` | `PgVectorJpaConfig` | `url` / `jdbc-url`, `username`, `password`, `driver-class-name` |
| `kg.pgvector.flyway.*` | `PgVectorFlywayConfig` | `enabled`, `locations`, `schemas` |
| `kg.pgvector.jpa.*` | (declared but not read by the vector EMF) | `hibernate.ddl-auto`, `show-sql`, … |
| `kg.dev.seed.enabled` | `DevDocumentSeeder` | Uploads 4 PDFs from `resources/devdocs/` under the fixed seed topics (dev/docker only) |

Property-name styles are mixed (`camelCase` in `kg.chunking`/`kg.storage` timeouts, `kebab-case` elsewhere).
Spring relaxed binding accepts both for `@ConfigurationProperties`, but `@Value` needs the exact form.

## Secrets and environment variables

Compose is for local development only, so every variable in `docker-compose.yml` has a `${VAR:-default}` fallback
that matches `application-dev.properties`, and no `.env` file is required. `local-development/.env` (gitignored) is
an optional override file; `local-development/.env.example` is its template:

| Variable | Default | Used for |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `knowgauge`, `knowgauge_user`, `knowgauge_pass` | Main DB container and `SPRING_DATASOURCE_*` |
| `POSTGRES_VECTORS_DB`, `POSTGRES_VECTORS_USER`, `POSTGRES_VECTORS_PASSWORD` | `knowgauge_vectors`, `knowgauge_user`, `knowgauge_pass` | Vector DB container and `KG_PGVECTOR_DATASOURCE_*` |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | `minio`, `minio12345` | MinIO container and `KG_STORAGE_ACCESSKEY/SECRETKEY` |
| `MINIO_BUCKET` | – | Informational (the app uses `kg.storage.bucket`) |
| `OPENAI_API_KEY` | none | `kg.embedding.openai.api-key`, `kg.testgen.chat-model.openai.api-key` |

The `dev` profile has local credentials hard-coded (`knowgauge_user/knowgauge_pass`, `minio/minio12345`), but
`OPENAI_API_KEY` must still be set in the environment. The verification model inherits the chat model's API key.

## Docker Compose

The compose file lives in [`local-development/`](../../local-development/README.md), with the repository root as the
app's build context (`..`). `docker compose -f local-development/docker-compose.yml up --build` starts four services
on the `knowgauge-network` bridge:

| Service | Image | Ports | Volume |
|---|---|---|---|
| `postgres` | `pgvector/pgvector:pg15` | 5432 | `postgres_data` |
| `postgres-vectors` | `pgvector/pgvector:pg15` | 5433→5432 | `postgres_vectors_data` |
| `minio` | `minio/minio:latest` | 9000 (S3), 9001 (console) | `minio_data` |
| app (`knowgauge-service`) | built from `Dockerfile` | 8080, 5005 (JDWP, `suspend=n`) | – |

Every service has a healthcheck. The app `depends_on` the other three with `condition: service_healthy`, and its own
healthcheck polls `/actuator/health/readiness`. The app uses `SPRING_PROFILES_ACTIVE=docker`, and the compose file
sets the datasource URLs and credentials through env vars (`SPRING_DATASOURCE_*`, `KG_PGVECTOR_DATASOURCE_*`, `KG_STORAGE_*`, `OPENAI_API_KEY`).

For local development in the IDE, run only the backing services
(`docker compose -f local-development/docker-compose.yml up postgres postgres-vectors minio`) and start the app with `-Dspring.profiles.active=dev` (see `.vscode/launch.json`).

## Build & image

- Maven reactor from the root `pom.xml`: `mvn clean install` (or `-DskipTests`; there are no tests yet).
- The parent POM imports the BOMs for Spring Boot 3.5.16, LangChain4j 1.1.0 and Resilience4j 2.2.0, and configures
  the annotation processors **Lombok, then MapStruct** in that order. The order matters for MapStruct to see the
  Lombok-generated accessors.
- Multi-stage `Dockerfile`: copy all module POMs and run `mvn validate` to warm the cache layer, copy the sources,
  run `mvn -DskipTests clean package`, and copy `knowgauge-service-rest-api/target/*.jar` into a
  `temurin:21-jre-alpine` runtime image.
- **Adding a module** means updating the root `<modules>`, the `Dockerfile` POM `COPY` list and the `rest-api`
  dependencies.

## Observability

- Logs: SLF4J/Logback. `com.knowgauge` logs at DEBUG in dev/docker. Ingestion and generation steps log progress
  lines prefixed with `***` (start and end) and indented step messages. Validators log each rejection with its
  reason. LLM calls log token usage and finish reason.
- Actuator: health (liveness/readiness), metrics, env, beans, loggers. `env` and `beans` are exposed with no auth,
  which is only acceptable locally.
- There is no correlation ID, tracing or metrics for LLM cost yet (on the backlog).

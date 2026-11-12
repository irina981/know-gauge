# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**KnowGauge** is a RAG-based Spring Boot service. Users upload PDFs into a hierarchical topic tree. The service
ingests them (PDFBox → LangChain4j splitter → OpenAI embeddings → pgvector) and generates grounded MCQ tests with
an LLM, plus a second LLM pass that verifies the incorrect options.

Full architecture docs: [`docs/architecture/README.md`](docs/architecture/README.md). Read the relevant page
before changing ingestion, retrieval, test generation or persistence wiring.

## Tech stack

Java 21 · Spring Boot 3.5.16 · Maven multi-module (no wrapper; use a local `mvn`) · LangChain4j 1.1.0 (OpenAI, Ollama)
· PostgreSQL 15 + pgvector · Flyway · MinIO · Resilience4j 2.2 · MapStruct 1.5.5 · Lombok 1.18.46 · springdoc.

## Build & run

```bash
mvn clean install -DskipTests                                    # build all modules
docker compose -f local-development/docker-compose.yml up -d   # backing services only (default)
mvn -pl knowgauge-service-rest-api spring-boot:run -Dspring-boot.run.profiles=local   # run app locally
docker compose -f local-development/docker-compose.yml --profile full up --build   # app in a container too (Spring profile: local)
```

Compose files and `.env.example` live in [`local-development/`](local-development/README.md).

- Spring profiles: `local` (development; localhost ports 5432/5433/9000, also used by the compose app container with
  env-var host overrides), `test` (automated tests only) and `prod` (every environment-specific value from env vars,
  required ones without defaults; the `Dockerfile` defaults to it). `application.properties` holds the values common
  to all. One profile is **required**: without it the datasources and `kg.context.tenant-id` are missing and startup
  fails. The tenant and user come from `kg.context.tenant-id` / `user-id` (`KG_TENANT_ID` / `KG_USER_ID` in prod).
- Distractor verification runs on OpenAI `gpt-4.1` in every profile (`kg.testgen.verification.provider=openai`).
  A local Ollama adapter exists (`provider=ollama`, `qwen2.5:7b-instruct` on port 11434, compose
  `--profile ollama`) but judged distractors unreliably ([ADR002](docs/adr/ADR002-local-verification-model.md), [ADR003](docs/adr/ADR003-grounded-reason-first-verification.md)).
- `OPENAI_API_KEY` must be set in the environment (or in `local-development/.env` for compose). Compose is for
  local development only and needs no `.env`: every other variable defaults to the `application-local.properties`
  values. Keep those defaults in sync when changing dev credentials.
- Swagger UI: http://localhost:8080/swagger-ui.html · health: `/actuator/health`.
- Unit tests exist for the core (`knowgauge-service-core/src/test/java`: question model, validator, review
  services). Run them with `mvn -pl knowgauge-service-core test`. They also run on newer local JDKs (tested
  on 23). There are no integration tests yet.
- Versions not managed by Spring Boot are pinned by hand and must follow Boot upgrades: `hibernate.version` in the
  root `pom.xml` (for `hibernate-vector`), `hypersistence-utils-hibernate-63`, springdoc and `swagger-annotations`.
- Dev shell is Windows (PowerShell or Git Bash).

## Module map

| Module | Role |
|---|---|
| `knowgauge-service-core` | Domain model, services, **port interfaces**, prompt templates (`src/main/resources/prompttemplates`) |
| `knowgauge-service-rest-contract` | Public DTO records (`*Dto`, `*Input`) |
| `knowgauge-service-rest-api` | Boot app + composition root: controllers, DTO mappers, exception handler, `application*.properties`, **all Flyway SQL** (`db/migration`, `db/dev-migration`, `db/pgvector-migration`) |
| `knowgauge-service-rest-client` | Empty placeholder |
| `knowgauge-service-infra/*-repository-springdata-jpa` | Main DB: JPA entities, Spring Data repos, `*JpaRepositoryAdapter` implementing core repository ports |
| `knowgauge-service-infra/*-vectorstore-pgvector` | Vector DB: `ChunkEmbeddingEntity`, `PgVectorStore` (native SQL sampling) |
| `knowgauge-service-infra/*-storage-minio` | `MinIoStorageService` |
| `knowgauge-service-infra/*-embedding-langchain4j-openai` | `OpenAiEmbeddingServiceImpl` |
| `knowgauge-service-infra/*-testgeneration-langchain4j-openai` | LLM generation + verification adapters, LLM-response DTOs and mappers |
| `knowgauge-service-infra/*-documentparsing-pdfbox` | `PdfPageExtractionService` |
| `knowgauge-service-infra/*-documentsplitting-langchain4j` | `LangChainDocumentSplitter` |

## Architecture rules (follow these)

- **Hexagonal**: core defines ports in `com.knowgauge.core.port.*`. Infra modules implement them. **Core must not
  import infra, JPA entities, LangChain4j, MinIO or OpenAI types.** Infra depends on core, never the reverse.
- **Three model layers**: contract DTO ↔ domain (`core.model`) ↔ JPA entity. Map with **MapStruct** (API mappers
  in `restapi.mapper`, entity mappers in `infra.*.mapper`). Don't leak entities or DTOs into core.
- Domain objects are Lombok POJOs (`@SuperBuilder`, `@Getter/@Setter`) extending `AuditableObject`, with no
  persistence annotations.
- **Two databases, two transaction managers**: `transactionManager` (main, `@Primary`) and
  `vectorTransactionManager`. Vector writes need
  `@Transactional(transactionManager = "vectorTransactionManager")`. There are no cross-DB transactions.
- **Keep external calls out of DB transactions.** Orchestrators (`IngestionServiceImpl`,
  `TestGenerationServiceImpl`) are non-transactional and call separate `*TransactionalServiceImpl` beans for short
  transactions. Don't add `@Transactional` to the orchestrators, and don't self-invoke transactional methods.
- **Schema is owned by Flyway** (`ddl-auto=validate`). Any entity change needs a matching SQL change in
  `knowgauge-service-rest-api/src/main/resources/db/...`. Prefer a new `V{n}__*.sql` over editing `V1` once data
  must be preserved.
- In the pgvector repository, use explicit `@Modifying` JPQL for deletes. Derived delete methods break on the
  secondary persistence unit.
- Embedding dimension (1536) is set in three places that must agree: `kg.embedding.dimension`, `vector(1536)` in
  the pgvector migration, and `@Array(length = 1536)` on `ChunkEmbeddingEntity`.
- Always filter vector queries by `tenant_id` **and** `embedding_model`.
- Translate provider errors to typed core exceptions (`Storage*`, `Embedding*`, `LlmResponseParsingException`) in
  the adapter. Resilience4j retry configs whitelist the transient types. Add HTTP mappings in
  `GlobalControllerExceptionHandler`.
- Get the tenant and user from `ExecutionContext`. Don't hard-code tenant IDs in new code; query with `tenantId`.
- Config: custom properties live under `kg.*`. Typed groups use `@ConfigurationProperties` classes in core
  (`ChunkingProperties`, `TestGenerationDefaultsProperties`, `StorageProperties`).

## Test generation essentials

- Pipeline: defaults → `TestDraftValidator` → persist `CREATED` → `RetrievingService` (balanced random sampling,
  **not** similarity search) → `QuestionBatchGenerator` batches of [prompt → LLM → pre-validate → verify
  distractors (flag only) → post-validate] → persist slots + version 1 → `GENERATED` / `REQUIRES_REVIEW` / `FAILED`.
- Questions are **versioned** ([ADR001](docs/adr/ADR001-review-process.md)): `TestQuestion` is a slot,
  `TestQuestionVersion` is immutable content. **Never update a persisted version.** Every change writes a new one
  through `TestReviewTransactionalServiceImpl.applyNewVersion` (optimistic switch on `expectedCurrentVersionId`).
- Review actions (`TestReviewService`): accept fix, regenerate, manual replace. Each writes a `TestEvent` with the
  actor. Record every LLM call through `GenerationRunRecorder` and every state change through
  `TestHistoryRecorder`, in the same transaction as the change.
- Prompt templates are `{{var}}` text files in `core/src/main/resources/prompttemplates/`. For `MULTIPLE_CORRECT`
  the builder appends `-multiple-correct` to the template ID. Output "schemas" (`*-output-template.json`) are
  example JSON that `OpenAiClientImpl` converts to a strict JSON schema for models matching
  `structured-output-model-prefixes`.
- Each option has `text`, `correct` (the answer key) and the verifier's `flagged` / `suggestedText` /
  `flagReason`. **Flags never change `correct`.** `getCorrectOptions()` and `getFlaggedOptions()` are derived.
  Don't mutate the `getOptions()` view (it is unmodifiable).
- Validation rules live in `TestQuestionValidator` as `ValidationStrategy` steps. Pre-LLM, post-LLM and manual
  replace use different strategy lists. Each rule returns a reason string.
- `FOCUSED` coverage mode and `avoidRepeats` are not implemented.

## Conventions

- Commit messages: Conventional Commits with scope, one capitalised summary line plus a body of `- ` items. **Use the
  `commit` skill ([`.claude/skills/commit/SKILL.md`](.claude/skills/commit/SKILL.md)) for every commit.** Always show
  the proposed message and wait for the user's approval before running `git commit`.
- Packages: `com.knowgauge.core.*`, `com.knowgauge.infra.<capability>.<tech>.*`, `com.knowgauge.restapi.*`,
  `com.knowgauge.contract.*`.
- Logging: `@Slf4j`. Include the `testId`/`documentId` and batch number in log lines (match the existing style).
- When adding a Maven module, update the root `pom.xml` `<modules>`, the `Dockerfile` POM `COPY` list, and the
  `rest-api` dependencies.
- Keep `docs/architecture/` and `DOMAIN_MODEL.md` in sync when behaviour changes. Record significant decisions as
  ADRs in `docs/adr/` (use `adr-template.md`).

## Gotchas

- Ingestion (`POST /api/content/documents/ingestion/{id}`) and generation (`POST /api/tests`) are synchronous and
  can take a long time. `ScheduledIngestionJob` also ingests the oldest `UPLOADED` documents on the
  `kg.ingestion.job.cron` schedule (`batch-size` per run; disabled in the `test` profile).
- Most failures inside `ingest()`/`generate()` are wrapped in `RuntimeException`, so they return 500 whatever the
  typed handler would map.
- The `local` profile seeds a fixed topic tree (`R__dev_seed_topics.sql`, tenant 1) and uploads the sample PDFs from
  `resources/devdocs/` at startup (`kg.dev.seed.enabled=true`). The seeder also creates the MinIO bucket. Other
  profiles must create it beforehand.
- Audit `createdBy`/`updatedBy` are hard-coded to `1` in `AuditableEntity` (`@PrePersist`).
- Known issues and backlog: [`docs/architecture/11-known-issues-and-roadmap.md`](docs/architecture/11-known-issues-and-roadmap.md), [`TODO.md`](TODO.md).

# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**KnowGauge** is a RAG-based Spring Boot service. Users upload PDFs into a hierarchical topic tree. The service
ingests them (PDFBox → LangChain4j splitter → OpenAI embeddings → pgvector) and generates grounded MCQ tests with
an LLM, plus a second LLM pass that verifies the incorrect options.

Full architecture docs: [`docs/architecture/README.md`](docs/architecture/README.md). Read the relevant page
before changing ingestion, retrieval, test generation or persistence wiring.

## Tech stack

Java 21 · Spring Boot 3.5.16 · Maven multi-module (no wrapper; use a local `mvn`) · LangChain4j 1.23.0 (OpenAI, Ollama)
· PostgreSQL 15 + pgvector · Flyway · MinIO · Resilience4j 2.2 · MapStruct 1.5.5 · Lombok 1.18.46 · springdoc.

## Build & run

```bash
mvn clean install -DskipTests                                    # build all modules
docker compose -f local-development/docker-compose.yml up -d   # backing services only (default)
mvn -pl knowgauge-service-rest-api spring-boot:run -Dspring-boot.run.profiles=local   # run app locally
docker compose -f local-development/docker-compose.yml --profile full up --build   # app in a container too (Spring profile: local)
```

Compose files and `.env.example` live in [`local-development/`](local-development/README.md). The single-server
production setup (prod profile, UI, Dozzle with login) is [`deploy/`](deploy/README.md).

- Spring profiles: `local` (development; localhost ports 5432/9000/8180, also used by the compose app container with
  env-var host overrides), `test` (automated tests only) and `prod` (every environment-specific value from env vars,
  required ones without defaults; the `Dockerfile` defaults to it). `application.properties` holds the values common
  to all. One profile is **required**: without it the datasources are missing and startup fails.
- Security mode `kg.security.mode` ([ADR010](docs/adr/ADR010-keycloak-sign-in-and-personal-tenants.md)):
  - `fixed` (default, and the `test` profile): no sign-in. Every request runs as `kg.context.tenant-id` / `user-id`
    (`KG_TENANT_ID` / `KG_USER_ID` in prod).
  - `oidc` (`KG_SECURITY_MODE=oidc`): Keycloak bearer tokens, validated against `kg.security.oidc.issuer-uri`. On the
    first sign-in a user gets a `users` row and a personal tenant. `/api/public/**`, health/info and Swagger stay
    open; `/api/admin/**` and the rest of actuator need the `ADMIN` role.
  - Roles are KnowGauge's, never Keycloak's ([ADR012](docs/adr/ADR012-roles-kept-by-knowgauge.md)): tables `roles` and
    `user_roles` (several per user). Every user is `TENANT_ADMIN` of their tenant; a verified email in
    `kg.account.admin-emails` (`KG_ADMIN_EMAIL`) gets `ADMIN` at sign-in. Don't add roles to the realm files.
  - An `ADMIN` reads any user's workspace (library, tests, attempts), read-only, by sending its tenant id in
    `X-KG-Tenant` on `GET /api/**` reads; `GET /api/admin/tenants` lists them
    ([ADR013](docs/adr/ADR013-admins-view-every-workspace.md)). In the UI a tenant is a "library"; a "workspace" is
    everything a user works with: their library, tests and attempts, plus the shared libraries.
  - Local Keycloak: compose service `keycloak` on http://localhost:8180, realm `knowgauge` from
    `local-development/keycloak/knowgauge-realm.json`. Its users and their passwords are in the
    [local-development README](local-development/README.md#sign-in-keycloak).
- Distractor verification runs on OpenAI `gpt-4.1` in every profile (`kg.testgen.verification.provider=openai`).
  A local Ollama adapter exists (`provider=ollama`, `qwen2.5:7b-instruct` on port 11434, compose
  `--profile ollama`) but judged distractors unreliably ([ADR002](docs/adr/ADR002-local-verification-model.md), [ADR003](docs/adr/ADR003-grounded-reason-first-verification.md)).
- `OPENAI_API_KEY` must be set in the environment (or in `local-development/.env` for compose). Compose is for
  local development only and needs no `.env`: every other variable defaults to the `application-local.properties`
  values. Keep those defaults in sync when changing dev credentials.
- Swagger UI: http://localhost:8080/swagger-ui.html · health: `/actuator/health` · logs (Dozzle): http://localhost:8888 · mail (Mailpit): http://localhost:8025.
- Logs are plain text in `local`/`test` and JSON (ECS) in `prod`, with `documentId`/`testId` as MDC fields
  ([ADR007](docs/adr/ADR007-json-logs-and-dozzle.md)).
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
| `knowgauge-service-rest-api` | Boot app + composition root: controllers, DTO mappers, exception handler, security (`restapi.security`: `SecurityConfig`, JWT → local user in `AccountResolutionFilter`), execution context (`restapi.context`), `application*.properties`, **all Flyway SQL** (`db/migration`, `db/dev-migration`, `db/pgvector-migration`) |
| `knowgauge-service-rest-client` | Empty placeholder |
| `knowgauge-service-infra/*-repository-springdata-jpa` | Main DB: JPA entities, Spring Data repos, `*JpaRepositoryAdapter` implementing core repository ports |
| `knowgauge-service-infra/*-vectorstore-pgvector` | Vector DB: `ChunkEmbeddingEntity`, `PgVectorStore` (native SQL sampling) |
| `knowgauge-service-infra/*-storage-minio` | `MinIoStorageService` |
| `knowgauge-service-infra/*-mail-springmail` | `SpringMailInvitationMailer` (invitation emails over SMTP) |
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
- **One Postgres database, two app schemas, two transaction managers** ([ADR011](docs/adr/ADR011-one-postgres-with-three-schemas.md)):
  schema `knowgauge` with `transactionManager` (`@Primary`) and schema `vectors` with `vectorTransactionManager`
  (each datasource URL sets `?currentSchema=`; each Flyway has its own history). Keycloak uses a third schema,
  `keycloak`, with its own login (`postgres/initdb`). Vector writes need
  `@Transactional(transactionManager = "vectorTransactionManager")`. There are no cross-schema transactions or FKs.
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
  Content (topics, documents, chunks, embeddings) is read from `ContentScope.readableTenantIds()` (own tenant +
  `LIBRARY`) and changed only after `ContentScope.requireWritable` (library: `ADMIN` only, ADR010, ADR012).
- Config: custom properties live under `kg.*`. Typed groups use `@ConfigurationProperties` classes in core
  (`ChunkingProperties`, `TestGenerationDefaultsProperties`, `StorageProperties`).

## Test generation essentials

- Pipeline: defaults → `TestDraftValidator` → persist `CREATED` → `RetrievingService` (balanced random sampling,
  **not** similarity search) → `QuestionBatchGenerator` batches of [prompt → LLM → pre-validate → verify
  distractors (flag only) → post-validate] → persist slots + version 1 → `GENERATED` / `REQUIRES_REVIEW` / `FAILED`.
- Questions are **versioned** ([ADR001](docs/adr/ADR001-review-process.md)): `TestQuestion` is a slot,
  `TestQuestionVersion` is immutable content. **Never update a persisted version.** Every change writes a new one
  through `TestReviewTransactionalServiceImpl.applyNewVersion` (optimistic switch on `expectedCurrentVersionId`).
- Review actions (`TestReviewService`): accept fix, regenerate, manual replace, keep as is (`dismissFlags`,
  comment required, [ADR008](docs/adr/ADR008-keep-question-as-is.md)). Each writes a `TestEvent` with the
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
- `FOCUSED` coverage embeds the test's `focusQuery` and takes the nearest chunks (`VectorStore.findNearest`, exact
  IVFFlat search via `ivfflat.probes`), keeping only chunks close to it (`kg.retrieval.focused.*`). A topic selected
  for generation stands for its whole subtree. `avoidRepeats` is not implemented.

## Taking tests (ADR009)

- Participants never use the admin API. They open a shared link `/t/{token}` (created or regenerated on request, `SharedLinkService`; name + email, no attempt limit) or a
  personal invitation `/i/{token}` (name only; the email is the invited address; one attempt per link) and call
  `/api/public/**`, which addresses everything by random token and never returns the answer key: after scoring only the score and
  the participant's own answers marked right or wrong. `AttemptResultNotifier` emails the score and the results
  link `/r/{token}` after the submit commits.
- Invitation emails go out from `InvitationServiceImpl` (non-transactional) between short transactions of
  `InvitationTransactionalServiceImpl`. A failed send is stored on the invitation, not thrown. Locally Mailpit
  (compose service `mailpit`) catches them.



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
  profiles must create it beforehand, or set `KG_STORAGE_CREATE_BUCKET=true` (`deploy/` does).
- Audit `createdBy`/`updatedBy` come from Spring Data auditing (`AuditingConfig`, `AuditorAware` =
  `ExecutionContext.userId()`). The values are:
  - the signed-in user (or the fixed user);
  - `0`, the reserved system user (`users` row 0, no tenant, `ExecutionContext.SYSTEM_USER_ID`), for jobs, startup
    seeding and the rows sign-in writes (the user and their tenant, `callAsSystemWithoutTenant`);
  - `NULL` for anonymous participants.

  Work outside a request must set its tenant with `ExecutionContext.runAs` or `runAsSystem`; in `oidc` mode
  `tenantId()` throws otherwise.
- Known issues and backlog: [`docs/architecture/11-known-issues-and-roadmap.md`](docs/architecture/11-known-issues-and-roadmap.md), [`TODO.md`](TODO.md).

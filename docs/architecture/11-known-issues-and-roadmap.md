# 11 – Known Issues, Risks & Roadmap

This page collects the gaps found while documenting the current code. Severity reflects impact on correctness or
operability, not effort.

## Correctness issues

| # | Area | Issue | Severity | Location |
|---|---|---|---|---|
| 1 | Test generation | ~~`formatChunk` wrote `topicId=` with the document ID~~ **Fixed** with ADR001 | – | `TestGenerationPromptBuilder.formatChunk` |
| 2 | Test generation | Typed errors (`LlmResponseParsingException`, `UnsupportedOperationException` for FOCUSED) are wrapped in `RuntimeException`, so the API returns 500 instead of the mapped 400/502/507 | Medium | `TestGenerationServiceImpl.generate`, `IngestionServiceImpl.ingest` |
| 3 | Test generation | ~~Flagged SINGLE_CORRECT questions were silently dropped~~ **Fixed** by ADR001: flags no longer change the answer key, so flagged questions are kept for review | – | `VerificationResponseMapper`, `TestQuestionValidator` |
| 4 | Test generation | Default `max-consecutive-zero-progress-batches=1`: one empty batch (for example the multi-correct quota not being met) fails the whole test | Medium (config) | `application.properties` |
| 5 | Test generation | Dedupe only within a batch. Every batch uses the same context, so cross-batch duplicate questions are possible | Medium | `TestQuestionValidator` |
| 6 | Test generation | ~~`VerificationPromptBuilder` injected twice~~ **Fixed**: the orchestrator was rewritten around `QuestionBatchGenerator` | – | `TestGenerationServiceImpl` |
| 7 | Test generation | `enforceJsonOnly` asks for "a valid JSON **array**" while both schemas are JSON **objects** | Low | `OpenAiClientImpl.enforceJsonOnly` |
| 8 | Draft validation | Documents under **descendant** topics are not included in the source set, only documents attached directly to the selected topics, although the topic IDs are expanded | Medium (UX) | `TestDraftValidator` |
| 9 | Storage | MinIO fallback methods have return types that don't match their primary methods (`StoredObject` for `InputStream` and `void`) | Medium | `MinIoStorageService` |
| 10 | Upload | MinIO `put` runs inside the DB transaction, so a failed commit leaves an orphaned object | Low | `DocumentServiceImpl.uploadDocument` |
| 11 | Delete | Deleting a document or topic doesn't delete its embeddings (separate DB, no FK). Deleting a topic doesn't delete its MinIO objects. Orphan embeddings are filtered out by `document_id IN (...)` but still use space | Medium | `DocumentServiceImpl.delete`, `TopicServiceImpl.delete` |
| 12 | Ingestion | `markIngesting`'s returned row count is ignored, so concurrent ingestion of the same document is not prevented | Low | `IngestionServiceImpl` |
| 13 | Ingestion | One `embedAll` call per document, with no sub-batching. Large documents can exceed provider request limits | Medium | `OpenAiEmbeddingServiceImpl` |
| 14 | Ingestion | Re-ingestion deletes chunks, which cascades to `test_used_chunks` of existing tests and loses their provenance. Regenerating a question of such a test then fails (502, no context) | Medium | FK `ON DELETE CASCADE` |
| 15 | API | `GET /topics/{id}` and `/documents/{id}` return 200 with an empty body when not found. Service-level "not found" is `IllegalArgumentException` → 400 | Low | `ContentController`, services |
| 16 | Startup | No `CurrentTenantProvider` or `CurrentUserProvider` outside the `dev`/`docker` profiles, so the context fails to start | High (prod) | `restapi/context` |
| 17 | Config | `PgVectorJpaConfig` has `@EntityScan("com.knowgauge.pgvector.repo.jpa.entity")`, a stale package. It is harmless because the EMF sets `packagesToScan` explicitly. `kg.pgvector.jpa.*` properties are not applied | Low | `PgVectorJpaConfig` |
| 18 | Compose | `APP_STORAGE_ACCESSKEY/SECRETKEY` env vars in `local-development/docker-compose.yml` don't map to any property. It works only because `application-docker.properties` reads `MINIO_ROOT_*` | Low | `local-development/docker-compose.yml` |

## Structural debt

- **Synchronous long-running work**: ingestion and generation run inside HTTP requests. Tests that crash mid-run
  stay `CREATED` and documents stay `INGESTING`, and nothing recovers them. Planned: async jobs or an outbox,
  worker retries and a dead-letter queue, status polling.
- **No automated tests** in the repository (`src/test` is absent everywhere). The most valuable first targets are
  `ChunkingServiceImpl`, `RetrievingServiceImpl.calculateMaxChunksPerDocument`, `TestQuestionValidator`, the
  option verdict logic, the response mappers, and Testcontainers for both Flyway schemas.
- **Schema evolution**: `V1__create_initial_schema.sql` is edited in place. Switch to incremental migrations
  before any persistent environment exists.
- **Unused model pieces**: `Attempt`/`AttemptAnswer`, `DocumentSection`, `User` and `Tenant` have no services or
  endpoints. `rest-client` is a placeholder.
- **Per-call `OpenAiChatModel` construction**: simple and flexible, but it rebuilds the HTTP client every call.
- **Package naming**: the backlog item to move all adapters under `com.knowgauge.infra.*` is already done.

## Roadmap (from TODO.md and code TODOs)

| Theme | Items |
|---|---|
| Retrieval | `FOCUSED` similarity search on the existing IVFFlat index. Avoid-repeats using `test_used_chunks`. Optional re-ranking |
| Generation | Resilience4j on the LLM adapters (retry, CB, bulkhead, timeouts). More question types (multi-select UX, short answer). Better difficulty and coverage control. A Spring AI adapter to compare with LangChain4j |
| Review workflow | **Done** ([ADR001](../adr/ADR001-review-process.md)): versions, event log, accept fix / regenerate / manual replace. Next: review UI, optional `DISMISS_FLAG` action, LLM second opinion on manual questions, real user identity in `actorUserId` |
| Attempts | Start, answer, submit and score attempts using the existing tables |
| Ingestion | Layout-aware PDF extraction, section detection, semantic Markdown-aware splitter (A/B against the recursive one), async workers |
| Tenancy & auth | Tenant entity, Keycloak OIDC, roles, tenant-scoped queries everywhere, Spring Data auditing |
| API quality | Consistent validation and enum error payloads, proper 404s, storage error mapping |
| Ops | Correlation IDs, structured storage logs, LLM cost dashboard, CI pipeline, indexes and constraints review |

## Candidate ADRs

[`docs/adr/`](../adr/) contains [ADR001 – Review process](../adr/ADR001-review-process.md) (ACCEPTED). These
other decisions are already in the code and are worth recording:

1. Hexagonal architecture with one Maven module per adapter.
2. A separate PostgreSQL database for embeddings, with no cross-DB transactions.
3. Balanced random sampling instead of similarity search for test context.
4. Batch generation with token-length backoff and a zero-progress guard.
5. LLM verification of distractors that is best effort (never fails generation).
6. Structured output (JSON schema derived from example templates) gated by model-name prefix.
7. Transactional helper beans to keep external calls outside DB transactions.

# 10 – Cross-Cutting Concerns

## Resilience

Resilience4j annotations are placed on **infra adapters**. Core stays unaware of them. Instances are configured in
`resilience4j.properties`.

| Instance | Applied to | Retry | Bulkhead (semaphore) | Circuit breaker |
|---|---|---|---|---|
| `minio` | `MinIoStorageService.put/download/delete` | max-attempts **1** (effectively off), retries only `StorageUnavailableException` | 20 concurrent, no wait | defaults (none configured) |
| `openaiEmbedding` | `OpenAiEmbeddingServiceImpl.embed(String)` | 3 attempts, 300 ms, retries Unavailable/RateLimited | 8 concurrent | count-based 50, min 20 calls, 50% → open 20 s |
| `openaiEmbeddingBatch` | `OpenAiEmbeddingServiceImpl.embed(List)` | 2 attempts, 500 ms | 2 concurrent | defaults |
| – | LLM generation / verification | **none**. Retry exists only for token-length (batch shrink). Verification errors are swallowed | – | – |

Design notes:

- **Exception translation happens first**. Adapters turn provider errors into typed core exceptions
  (`Storage*`, `Embedding*`). The retry config then whitelists the transient types (`Unavailable`, `RateLimited`)
  and ignores the permanent ones (`BadRequest`, `Auth`, `Unexpected`).
- **MinIO retries are disabled on purpose**: a consumed `InputStream` cannot be replayed. Upload needs a
  retry-safe design first (buffer to a file, use a `Supplier<InputStream>`, or retry at a higher level).
- Fallback methods rethrow a wrapping exception, so they add context but do not degrade gracefully.
- Note the fallback **return types** in `MinIoStorageService`: `downloadFallback` and `deleteFallback` are declared
  to return `StoredObject` although their primary methods return `InputStream` and `void`. Resilience4j matches
  fallbacks by signature, so these fallbacks may never be found or may fail when invoked. This needs checking.

## Multi-tenancy

Current state: **single tenant, designed for multi-tenancy**.

- `tenant_id` columns exist on `topics`, `documents`, `document_chunks`, `tests`, `test_questions`,
  `test_question_versions`, `test_events`, `generation_runs`, `attempts`, `attempt_answers` and `chunk_embeddings`. They do **not** exist on
  `document_sections` or `test_question_version_options` (scoped through their version).
- The `ExecutionContext` port provides `tenantId()` (always present) and `userId()` (optional, for jobs and
  anonymous use). Its implementation (`DefaultExecutionContext` with `DefaultTenantProvider` and
  `DefaultUserProvider`) returns `1`.
- Isolation is enforced in **some** paths: test queries (`findByTenantIdAnd…`), chunk loading, vector queries
  (always filtered by tenant) and `TestDraftValidator` (topic and document ownership checks).
- Isolation is **missing** in others: topic queries (`getRoots`, `getChildren`, `findByPathPrefix`, name
  uniqueness), document `get`/`delete`/`download` by ID and `findByTopicId`. The topic and document unique
  constraints are also global rather than per tenant.
- Planned (TODO.md): a `Tenant` entity (`INDIVIDUAL`/`ORGANIZATION`), Keycloak OIDC, roles
  (`APPLICATION_ADMIN`, `TENANT_ADMIN`, `TENANT_USER`), and per-tenant storage prefix or bucket policies.

## Auditing

- Domain: `AuditableObject` (`createdAt/By`, `updatedAt/By`).
- Entities: `AuditableEntity` carries Spring Data `@CreatedDate/@CreatedBy/...` annotations and the
  `AuditingEntityListener`. Auditing is **not enabled** (no `@EnableJpaAuditing`). The values come from
  `@PrePersist`/`@PreUpdate` with `createdBy = updatedBy = 1L` hard-coded. TODO: wire `AuditorAware` to
  `ExecutionContext.userId()`.
- The vector module has its own copies of `PersistedEntity` and `AuditableEntity`.

## Exceptions

```
RuntimeException
├── BaseException (abstract)
│   ├── StorageException (abstract) → StorageNotFound / Auth / BadRequest / Unavailable / Unexpected
│   ├── DuplicateDocumentNameException, DuplicateDocumentContentException
├── EmbeddingException (abstract) → EmbeddingAuth / BadRequest / RateLimited / Unavailable / Unexpected
└── LlmResponseParsingException (reason: LENGTH | PARSING_ERROR | MAPPING_ERROR | INVALID_RESPONSE | OTHER)
```

The core services still mostly use `IllegalArgumentException` and `IllegalStateException` for domain errors.
For the HTTP mapping and its quirks, see [08 – Error model](08-rest-api.md#error-model).

## Concurrency and execution model

- Everything runs on the servlet request thread. There is no `@Async`, no queue and no scheduler.
- Long operations (ingestion, generation) hold an HTTP connection for their whole duration. Client and proxy
  timeouts must allow for this.
- The state guards in the DB updates (`markIngesting` only from `UPLOADED|FAILED`) prevent some concurrent
  re-entry, but `IngestionServiceImpl` ignores the returned row count. Two concurrent ingestions of the same
  document can therefore both continue.
- Validators and prompt builders are stateless singletons. `TestQuestionValidator` uses `ThreadLocalRandom` for
  shuffling.

## Security

- No authentication or authorization on the API. Actuator `env` and `beans` are exposed outside `prod`.
- Secrets come from environment variables (`OPENAI_API_KEY`, DB and MinIO credentials). `.env` is gitignored, but
  the `local` profile has local credentials in the properties file; `prod` reads every secret from env vars.
- Uploads: 50 MB limit and SHA-256 dedupe. There is no content-type allow-list beyond the fact that only
  `application/pdf` can be ingested, and no malware scanning.
- Prompt injection: document text is inserted verbatim into the LLM prompts. The closed-world instructions and the
  validators reduce the risk, but nothing isolates it.
- The `Content-Disposition` filename on download comes from user input and is not sanitized.

## Logging conventions

- Lombok `@Slf4j`. Messages include the `testId` or `documentId`, plus a batch number for generation.
- `IngestionServiceImpl.ingest` puts `documentId` and `TestGenerationServiceImpl.generate` puts `testId` into the
  MDC for the duration of the call, so JSON logs carry them as fields. Keep them in the message text as well:
  the `local` profile logs plain text. Work started on another thread does not inherit the MDC.
- Rejected questions are logged at WARN with a reason string. Flagged options are logged at INFO with the
  verifier's explanation.
- Full prompts are logged at DEBUG. With DEBUG on (the dev and docker default), document content and LLM output
  end up in the logs.

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

Current state: **one tenant per user** when sign-in is on (security mode `oidc`), one fixed tenant without it
(`fixed`), plus shared read-only `LIBRARY` tenants (ADR010). A user's tenant is the personal one created at their
first sign-in, or one an `ADMIN` created and invited them to: a `PERSONAL` tenant for one owner or an
`ORGANIZATION` for several users (ADR015).

- `tenant_id` columns exist on `topics`, `documents`, `document_chunks`, `tests`, `test_questions`,
  `test_question_versions`, `test_events`, `generation_runs`, `attempts`, `attempt_answers` and `chunk_embeddings`. They do **not** exist on
  `document_sections` or `test_question_version_options` (scoped through their version).
- The `ExecutionContext` port provides `tenantId()` and `userId()` (optional: empty for anonymous participants).
  `DefaultExecutionContext` takes them from the signed-in user (`TokenTenantProvider` / `TokenUserProvider`, mode
  `oidc`) or from `kg.context.*` (mode `fixed`). Work outside a request sets them for the current thread with
  `runAs(tenantId, userId, …)` or `runAsSystem(tenantId, …)`: the scheduled ingestion job runs per tenant as the
  system user (`users` row 0, `SYSTEM_USER_ID`), and so does the startup seeder.
- Accounts: `AccountResolutionFilter` maps the token's subject to a `users` row through `AccountService.signIn`,
  which creates the user (role `TENANT_ADMIN`) and a `PERSONAL` tenant on the first sign-in, and caches the
  result per subject for `kg.security.oidc.account-cache-minutes`. A first sign-in whose verified email has a
  pending workspace invitation joins that tenant instead (ADR015).
- Workspace invitations (ADR015, `tenant_invitations`): an `ADMIN` creates a tenant of any type
  (`TenantProvisioningService`) and invites people by email. Accepting (`TenantMembershipService`, `TenantJoiner`)
  moves the user: `users.tenant_id` becomes the invited tenant and the `TENANT_*` roles are replaced by the invited
  one (`TENANT_ADMIN`). The tenant they leave keeps its content, without users. `AccountResolver.evict` drops the
  cached account. A new library refreshes `ContentScope`'s library ids.
- Every repository port method that reads or writes tenant data is filtered by tenant. Writes take the one tenant
  written to (`updatePath(tenantId, …)`, `markIngested(tenantId, …)`, …). Content reads (topics, documents,
  chunks, embeddings) take a set (`findByTenantIdIn…`, `tenant_id IN (:tenantIds)`): the own tenant plus the
  `LIBRARY` tenants, from `ContentScope` (ADR010). Tests, questions, attempts and invitations stay in the own tenant only.
- Another tenant's id behaves like a missing row (404 or empty list), also as a parent topic or upload target.
  Library content can be read by everyone and changed only by an `ADMIN` (otherwise 403,
  `ReadOnlyContentException`); `TopicDto` / `DocumentDto.readOnly` reflect this. Sub-topics and documents live in
  their topic's tenant, and ingestion runs in the document's tenant (`ExecutionContext.runAs`).
  `document_sections` have no tenant column and are filtered through their document.
- Deliberate exceptions: lookups by public token (`/api/public/**`, the token's row gives the tenant),
  `DocumentRepository.findTenantIdsByStatus` (the scheduled ingestion job, which then works per tenant), and
  `TenantRepository` / `UserRepository` (account resolution).
- Root topic names are unique per tenant (checked in `TopicServiceImpl`). The other unique constraints are per
  parent topic or per topic, so they are per tenant as well; `UNIQUE(path)` is global but paths are built from ids.
- Planned (TODO.md): members of several tenants with a switcher (Keycloak Organizations), enforced `TENANT_USER` /
  `TENANT_VIEWER` roles, and per-tenant storage prefix or bucket policies.

## Auditing

- Domain: `AuditableObject` (`createdAt/By`, `updatedAt/By`).
- Entities: `AuditableEntity` carries Spring Data `@CreatedDate/@CreatedBy/...` and the `AuditingEntityListener`.
  `AuditingConfig` (rest-api) enables auditing with `AuditorAware` = `ExecutionContext.userId()` and `Instant.now()`.
  Both entity manager factories use Spring's bean container (`SpringBeanContainer`), so the listener gets its handler
  in the vectors schema too.
- `created_by` / `updated_by`: the signed-in (or fixed) user, `0` for the system user (jobs, seeding, and the user and
  tenant rows written at sign-in, before anyone is signed in), `NULL` when
  nobody is signed in (participants on public links).
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

- Authentication: Keycloak bearer tokens in security mode `oidc` (`SecurityConfig`, ADR010): issuer, signature and
  audience are checked; sessions are stateless and CSRF is off (no cookies). Authorization is the app's (ADR012): the
  `ADMIN` role from `user_roles` guards `/api/admin/**` and actuator beyond health/info; the token's roles are ignored. In mode `fixed` (the default for now) the API has no
  authentication, and actuator `env` and `beans` are exposed outside `prod`.
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

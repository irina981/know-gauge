# KnowGauge – TODO


## ✅ Backend — Small fixes (quick wins)
> Small, self-contained tasks. Typically < 1 day each.

### General
- [ ] Add "infra" to all adapters packages (e.g. com.knowgauge.infra.repo.jpa.entity)

### Storage / MinIO
- [ ] Add storage exception → HTTP mapping (ControllerAdvice)
- [ ] MinIO upload retry safety — retry with InputStream is not safe: implement a retry-safe approach for upload operations (options: buffer to temporary file/byte[] before upload, accept Supplier<InputStream> or a rewindable stream, or mark upload as non-retryable and retry at a higher level). Add integration tests and update resilience4j configuration.
- [ ] Currently, storage key pattern is: {tenantId}/docs/by-id/{shard3}/{documentId}/v{version}/source/content. Investigate: Is it a bad practice/security concern to expose internal system's IDs to external parties.

### REST API / Validation
- [ ] Return consistent error payload for validation errors (field + message)
- [ ] Standardize enum handling for all client `Input` DTOs: parse case-insensitively, validate unknown values centrally, and return consistent 400 error messages instead of raw `IllegalArgumentException`.
- [ ] Introduce shared enum parsing/validation utility (e.g., `EnumParser`) plus `@ControllerAdvice` mapper for enum conversion errors to a unified API error response format.


### Observability
- [ ] Add correlationId in logs (filter/interceptor)
- [ ] Add structured log for storage ops: key, size, duration, result

### DB / Schema quality
- [ ] Add indexes for common queries (topicId, documentId, createdAt)
- [ ] Add constraints (FKs, NOT NULL, unique where needed)
- [ ] Configurable rate/temperature defaults and a usage dashboard or logs to estimate model costs.

### User Management
- [ ] Implement User Mangement (core)
- [x] Roles: ADMIN, TENANT_ADMIN (assigned), TENANT_USER, TENANT_VIEWER in `roles` / `user_roles` (ADR012)
- [x] Tenant admins of an organization invite people as TENANT_ADMIN, TENANT_USER or TENANT_VIEWER; Users and Workspace settings pages (ADR016)
- [x] Enforce TENANT_USER and TENANT_VIEWER: `@RolesAllowed` on controllers and checks in core (ADR017)
- [ ] Screen or API to grant and revoke roles (today: `kg.account.admin-emails` and SQL)

### Multi-tenant support
- [x] Tenant domain entity
- [x] Tenant types PERSONAL, ORGANIZATION, LIBRARY; admins create any of them and invite people (ADR015)
- [ ] Membership in several tenants and a workspace switcher (today accepting an invitation moves the user)
- [x] Add tenantId to all domain objects (topics and documents are misisng it for sure) and make all the topics and documents queries tenant scoped

### Spring Data auditing
- [x] Wire Spring Data auditing (instead of @PrePersist, @PreUpdate) - to update createdBy, modifiedBy (ADR010)


---

## 🧩 Backend — Large missing modules (big rocks)
> Larger features/epics. Usually multiple days/weeks each.

### 1) Ingestion pipeline (PDF → sections → chunks)
- [ ] PDF text extraction (p1: simple text; p2: layout-aware)
- [ ] Section detection (TOC / headings / heuristics)
- [ ] Chunking policy (size, overlap, “don’t split code blocks”)
- [ ] Persist sections + chunks + chunk metadata

### 1.1) RAG chunking quality upgrade (semantic vs recursive)
- [ ] Evaluate current recursive splitter vs semantic-hybrid splitter on retrieval quality (Recall@k / nDCG@k / top-k relevance) and MCQ quality.
- [ ] Design `SemanticMarkdownSplitter` rules: preserve fenced code blocks, split on headings, keep heading + first paragraph, preserve lists/tables.
- [ ] Implement hybrid fallback for oversized segments (token/size-based split) with deterministic overlap.
- [ ] Add unit tests for boundary integrity (code fences, headings, overlap correctness, non-Markdown safety).
- [ ] Run A/B benchmark on representative technical docs; compare hallucination rate and question grounding.
- [ ] Decide rollout: keep recursive as fallback behind config flag, remove LangChain4j splitter only if semantic-hybrid passes benchmarks.

### 2) Embeddings + Vector retrieval (pgvector)
- [ ] "Avoid repeats" policy: filter out chunks used in previous tests (query test_used_chunks for tests with status=GENERATED, exclude those chunk IDs from vector retrieval)
- [ ] Re-ranking (optional phase)

### 3) Test generation module
- [ ] Prompt templates + model selection (“balanced/focused”)
- [ ] Question types (MCQ, multi-select, short answer)
- [ ] Difficulty control + coverage control
- [ ] Store tests + test sessions + answers + scoring
- [x] Map provider/transport errors in test generation to domain-specific exceptions (`LlmResponseParsingException` with typed reasons)
- [x] Implement automatic retry logic for LLM token length limit errors (decrease batch size and retry)
- [ ] Add annotation-based resilience in `LlmTestGenerationServiceImpl` (similar to `OpenAiEmbeddingServiceImpl`): `@Retry`, `@CircuitBreaker`, `@Bulkhead` for general errors (+ timeout support if possible).
- [ ] Add/adjust `resilience4j.properties` entries for test-generation calls (retry, circuit breaker, bulkhead, timeout parameters).
- [ ] Implement "avoid duplicates" logic: exclude chunks already used in previous GENERATED tests (query test_used_chunks for existing tests with status=GENERATED and filter out those chunk IDs from retrieval)
- [x] Implement FOCUSED mode test generation: use similarity search based on user query/topic description to retrieve most relevant chunks (instead of balanced retrieval across documents)
- [ ] Implement SpringAI-based adapter for `LlmTestGenerationService` (alongside current LangChain4j implementation): investigate SpringAI capabilities including tool calling, structured output support, and response parsing vs current JSON parsing approach. Compare features, performance, and developer experience to evaluate potential migration from LangChain4j to SpringAI for all LLM adapters.

### 3.1) Code questions for algorithm topics
Generated tests on algorithms (e.g. Programming › Algorithms for Coding Interviews) are mostly theoretical. Code-based
multiple-choice questions test whether someone can read and reason about code. Question types that fit MCQ: pattern
recognition (which approach fits this problem), what does this snippet return for a given input, find the bug (numbered
lines), fill in the missing line, complexity of a snippet, which input breaks this solution.
- [ ] Add short, correct Java solutions (10–20 lines) to the sample tasks in `demo-content/algorithms`, so the
  generator has code to ask about.
- [ ] Render code in questions and options: support fenced ```code``` blocks (monospace, whitespace preserved) on the
  review page, in the test runner and on the participant results page.
- [ ] Add a code-question prompt template (e.g. `mcq-code`) that uses the question types above, keeps snippets short
  and puts code in fenced blocks; expose it as a **Question style** choice (Conceptual / Code reading / Mixed) in the
  generate form instead of a raw template ID.
- [ ] Answer-key correctness: "what does this return" questions need the model to trace code correctly. Keep snippets
  short and rely on the verifier and review; later, optionally run snippets in a sandbox to confirm the expected
  output.
- [ ] Later, separate product decision: real coding tasks (editor + test cases run in a code-execution sandbox, new
  grading) instead of multiple choice.

### 4) Async jobs / orchestration
- [ ] Queue-based ingestion (outbox table or message broker later)
- [ ] Background worker for ingestion + embeddings
- [ ] Retry strategy for jobs (dead letter / backoff)
- [ ] Status tracking (UPLOADED → INGESTING → READY → FAILED)

### 5) Auth & multi-tenancy
- [x] Keycloak integration (OIDC) for API (ADR010, security mode `oidc`)
- [ ] Keycloak and security mode `oidc` in `deploy/` (prod realm, admin from env, HTTPS)
- [x] Tenant model (tenantId on topics/docs/chunks/tests; `tenants` / `users`, personal tenant per user, `LIBRARY` tenant)
- [ ] Role-based access (admin/editor/viewer)
- [ ] Per-tenant storage prefix / bucket policy strategy

### 6) Search & navigation
- [ ] Document browsing endpoints (pagination + sorting)
- [ ] Topic tree endpoints (fetch subtree, move topic)
- [ ] Full-text search (optional: Postgres tsvector)

---

## 🧪 Tests (cross-cutting)
- [ ] Unit tests for mappers + exception translator
- [ ] Integration tests: MinIO via Testcontainers
- [ ] Integration tests: Postgres+pgvector migrations

## 🐳 DevOps / Local env
- [ ] CI pipeline: build + tests

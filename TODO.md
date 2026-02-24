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
- [ ] Roles: APPLICATION_ADMIN, TENANT_ADMIN, TENANT_USER

### Multi-tenant support
- [ ] Tenant domain entity
- [ ] Two types of tenants: PERSONAL, ORGANIZATION
- [ ] Add tenantId to all domain objects (topics and documents are misisng it for sure) and make all the topics and documents queries tenant scoped

### Spring Data auditing
- [ ] Wire Spring Data auditing (instead of @PrePersist, @PreUpdate) - to update createdBy, modifiedBy


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
- [ ] Implement FOCUSED mode test generation: use similarity search based on user query/topic description to retrieve most relevant chunks (instead of balanced retrieval across documents)
- [ ] Implement SpringAI-based adapter for `LlmTestGenerationService` (alongside current LangChain4j implementation): investigate SpringAI capabilities including tool calling, structured output support, and response parsing vs current JSON parsing approach. Compare features, performance, and developer experience to evaluate potential migration from LangChain4j to SpringAI for all LLM adapters.

### 4) Async jobs / orchestration
- [ ] Queue-based ingestion (outbox table or message broker later)
- [ ] Background worker for ingestion + embeddings
- [ ] Retry strategy for jobs (dead letter / backoff)
- [ ] Status tracking (UPLOADED → INGESTING → READY → FAILED)

### 5) Auth & multi-tenancy
- [ ] Keycloak integration (OIDC) for API
- [ ] Tenant model (tenantId on topics/docs/chunks/tests)
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

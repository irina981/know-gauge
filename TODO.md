# KnowGauge – TODO


## ✅ Backend — Small fixes (quick wins)
> Small, self-contained tasks. Typically < 1 day each.

### Storage / MinIO
- [ ] Add storage exception → HTTP mapping (ControllerAdvice)

### REST API / Validation
- [ ] Add `@Valid` for `@RequestPart("meta")` and `@RequestBody`
- [ ] Return consistent error payload for validation errors (field + message)

### Mapping / DTOs
- [ ] Finish MapStruct mappers: `TopicMapper`, `DocumentMapper`
- [ ] Move “fill from MultipartFile if missing” logic into mapper (`@AfterMapping`)
- [ ] Stop returning domain objects directly from controllers (return DTOs)

### Observability
- [ ] Add correlationId in logs (filter/interceptor)
- [ ] Add structured log for storage ops: key, size, duration, result
- [ ] Enable actuator endpoints: health/info/metrics

### DB / Schema quality
- [ ] Add indexes for common queries (topicId, documentId, createdAt)
- [ ] Add constraints (FKs, NOT NULL, unique where needed)
- [ ] Standardize audit columns (createdAt, updatedAt)

---

## 🧩 Backend — Large missing modules (big rocks)
> Larger features/epics. Usually multiple days/weeks each.

### 1) Ingestion pipeline (PDF → sections → chunks)
- [ ] PDF text extraction (p1: simple text; p2: layout-aware)
- [ ] Section detection (TOC / headings / heuristics)
- [ ] Chunking policy (size, overlap, “don’t split code blocks”)
- [ ] Persist sections + chunks + chunk metadata

### 2) Embeddings + Vector retrieval (pgvector)
- [ ] Embedding generation adapter (LLM provider abstraction)
- [ ] Store embeddings (pgvector) with metadata
- [ ] Retrieval service: topic + descendants + filters
- [ ] “Avoid repeats” policy + test_used_chunks tracking
- [ ] Re-ranking (optional phase)

### 3) Test generation module
- [ ] Prompt templates + model selection (“balanced/focused”)
- [ ] Question types (MCQ, multi-select, short answer)
- [ ] Difficulty control + coverage control
- [ ] Store tests + test sessions + answers + scoring

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
- [ ] Docker compose: healthchecks, volumes, init scripts
- [ ] Dev seed data script (topics + docs)
- [ ] CI pipeline: build + tests

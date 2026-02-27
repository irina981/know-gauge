# KnowGauge Service Core - Domain Model

This project implements the complete domain model for the KnowGauge RAG-based knowledge assessment platform.

## Domain Entities

### 1. Topic (Tree-based Taxonomy)
Represents both top-level topics and subtopics via `parentId`.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier (multi-tenancy support)
- `parentId` (Long, nullable) - Reference to parent topic (null = root topic)
- `name` (String) - Topic name
- `description` (String, optional) - Topic description
- `path` (String, optional) - Hierarchical path (e.g., `/java/collections/list`)
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who created the topic
- `updatedBy` (Long) - User who last updated the topic

**Constraints:**
- UNIQUE(parentId, name) - Prevent duplicate topic names under same parent
- UNIQUE(path) - Ensure canonical path uniqueness

**Benefits:**
- ✅ Unlimited depth support (Java → Collections → List → ArrayList)
- ✅ Easy filtering in RAG metadata
- ✅ Matches real-world knowledge trees
- ✅ Tenant-scoped hierarchies

### 2. Document (Uploaded Knowledge Sources)
Represents uploaded documents (PDF support).

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `topicId` (Long) - Foreign key to Topic (any level in hierarchy)
- `title` (String, optional) - Document title
- `originalFileName` (String) - Original file name
- `contentType` (String) - MIME type (e.g., application/pdf)
- `fileSizeBytes` (Long) - File size in bytes
- `storageKey` (String) - Storage location (MinIO path)
- `checksum` (String) - SHA checksum for duplicate detection
- `version` (Integer) - Document version number
- `status` (DocumentStatus) - UPLOADED / INGESTING / INGESTED / FAILED
- `ingestedAt` (Instant, nullable) - Ingestion completion time
- `errorMessage` (String, nullable) - Error details if failed
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who uploaded the document
- `updatedBy` (Long) - User who last updated the document

**Constraints:**
- UNIQUE(topicId, originalFileName) - Prevent duplicate file names per topic
- UNIQUE(topicId, checksum) - Prevent duplicate content by hash per topic
- CHECK status IN ('UPLOADED', 'INGESTING', 'INGESTED', 'FAILED')

**Statuses:**
- `UPLOADED`: File received in storage
- `INGESTING`: Chunks being extracted and embedded
- `INGESTED`: Ready for RAG queries
- `FAILED`: Extraction/embedding failed

### 3. DocumentSection (Optional Document Structure)
Useful for headline-based chunking and better citations.

**Fields:**
- `id` (Long) - Primary key
- `documentId` (Long) - Foreign key to Document
- `title` (String) - Section title (e.g., "2.3 HashMap Internals")
- `orderIndex` (Integer) - Section order within document
- `startPage` (Integer, nullable) - Start page number
- `endPage` (Integer, nullable) - End page number
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who created the section
- `updatedBy` (Long) - User who last updated the section

### 4. DocumentChunk (Logical Chunks for RAG)
Represents stable, auditable chunks for grounding RAG queries to source documents.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `documentId` (Long) - Foreign key to Document
- `topicId` (Long) - Foreign key to Topic
- `sectionId` (Long, nullable) - Foreign key to DocumentSection
- `documentVersion` (Integer) - Document version this chunk belongs to
- `ordinal` (Integer) - Chunk order within document (immutable sequential number)
- `chunkText` (String) - Chunk content text
- `checksum` (String, nullable) - SHA checksum for deduplication
- `startPage` (Integer, nullable) - Start page number
- `endPage` (Integer, nullable) - End page number
- `charStart` (Integer, nullable) - Character position start
- `charEnd` (Integer, nullable) - Character position end
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who created the chunk
- `updatedBy` (Long) - User who last updated the chunk

**Constraints:**
- UNIQUE(tenantId, documentId, documentVersion, ordinal) - Ensure consistent chunk ordering per document version

**Bridge between:**
- 💡 Relational DB (audit, reproducibility, multi-tenancy)
- 💡 Vector DB (semantic search via embeddings)

**Key Design:**
- documentVersion allows tracking chunks across document updates
- ordinal provides immutable ordering for chunk references
- Tenant-scoped for multi-tenancy data isolation

### 5. ChunkEmbeddingEntity (Vector Storage)
Maps to pgvector for semantic search.

**Fields:**
- `id` (Long) - Primary key
- `chunkId` (Long) - Foreign key to DocumentChunk
- `documentId` (Long) - Foreign key to Document
- `topicId` (Long) - Foreign key to Topic
- `sectionId` (Long, nullable) - Foreign key to DocumentSection
- `chunkIndex` (Integer) - Chunk index
- `embedding` (float[1536]) - Vector embedding (pgvector)
- `embeddingModel` (String) - Model used for embedding
- `createdAt` (Instant) - Timestamp

**Indexes:**
- IVFFlat index for similarity search (cosine distance)
- Indexes on topic_id, document_id for metadata filtering

### 6. Test (Generated Tests with Reproducibility)
Represents generated tests with full audit trail and generation lifecycle.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `difficulty` (TestDifficulty) - EASY / MEDIUM / HARD
- `coverageMode` (CoverageMode) - BALANCED_PER_DOCS / BALANCED_PER_DOC_CHUNKS / FOCUSED
- `avoidRepeats` (Boolean, default: true) - Avoid using same chunks twice
- `questionCount` (Integer) - Number of questions to generate (must be > 0)
- `answerCardinality` (AnswerCardinality) - SINGLE_CORRECT / MULTIPLE_CORRECT
- `status` (TestStatus) - CREATED / GENERATED / REQUIRES_REVIEW / REVIEWED / FAILED
- `promptTemplateId` (String, optional) - Which prompt template was used
- `generationModel` (String, optional) - AI model used (e.g., gpt-4)
- `language` (String) - ISO language code (EN, DE, FR, ES, IT, SR)
- `generationParamsJson` (JSONB, optional) - Generation configuration details
- `generationStartedAt` (Instant, nullable) - When generation began
- `generationFinishedAt` (Instant, nullable) - When generation succeeded
- `generationFailedAt` (Instant, nullable) - When generation failed
- `generationErrorMessage` (String, nullable) - Error details if generation failed
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who initiated the test generation
- `updatedBy` (Long) - User who last updated the test

**Constraints:**
- CHECK status IN ('CREATED', 'GENERATED', 'REQUIRES_REVIEW', 'REVIEWED', 'FAILED')
- CHECK difficulty IN ('EASY', 'MEDIUM', 'HARD')
- CHECK coverageMode IN ('BALANCED_PER_DOCS', 'BALANCED_PER_DOC_CHUNKS', 'FOCUSED')
- CHECK answerCardinality IN ('SINGLE_CORRECT', 'MULTIPLE_CORRECT')
- CHECK language IN ('EN', 'DE', 'FR', 'ES', 'IT', 'SR')
- CHECK questionCount > 0
- Lifecycle: generationStartedAt ≤ generationFinishedAt; generationStartedAt ≤ generationFailedAt

**Status Lifecycle:**
- `CREATED`: Test record initialized, waiting for generation
- `GENERATED`: Questions generated successfully
- `REQUIRES_REVIEW`: Generated content flagged for human review
- `REVIEWED`: Human review completed
- `FAILED`: Generation process failed

**Relationships (Junction Tables):**
- Many-to-Many with DocumentChunk via test_used_chunks (source chunks for generation)
- Many-to-Many with Topic via test_covered_topics (topics covered by questions)
- Many-to-Many with Document via test_covered_documents (documents used as sources)

### 7. TestQuestion (Test Questions)
Individual questions with multiple-choice options via TestQuestionOption.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `testId` (Long) - Foreign key to Test
- `questionIndex` (Integer) - Question order within test (0-based)
- `questionText` (String) - Question text
- `explanation` (String, optional) - Explanation for the answer
- `sourceChunkIdsJson` (JSONB, optional) - JSON array of source chunk IDs for traceability
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who created the question
- `updatedBy` (Long) - User who last updated the question

**Constraints:**
- UNIQUE(testId, questionIndex) - Ensure one question per position per test

**Relationship:**
- One-to-Many with TestQuestionOption (4 options per question via OptionLetter.A/B/C/D)
- Correct answer is determined by which TestQuestionOption has verdict=true

**Guarantees:**
- 🎯 Deterministic scoring (verdict values are immutable once set)
- 🎯 Traceability (source chunks recorded)
- 🎯 No hallucinated answers (options traced back to source)

### 8. TestQuestionOption (Answer Options with Verification Tracking)
Individual answer options with three-tier text and verdict override capability for quality control.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `questionId` (Long) - Foreign key to TestQuestion
- `letter` (OptionLetter) - Option letter (A, B, C, or D)
- `originalText` (String) - Original AI-generated option text
- `replacementText` (String, optional) - AI-suggested text correction/improvement (from verification)
- `finalText` (String, optional) - User override text (highest priority for display)
- `originalVerdict` (Boolean, optional) - AI verdict (true = correct answer)
- `verifiedVerdict` (Boolean, optional) - LLM verification of the verdict
- `finalVerdict` (Boolean, optional) - User override verdict (true = correct answer, overrides AI/LLM)
- `state` (OptionState) - Option state (OK, FLAGGED, or RESOLVED)
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - User who created the option
- `updatedBy` (Long) - User who last updated the option

**Constraints:**
- UNIQUE(questionId, letter) - One option per letter per question
- CHECK letter IN ('A', 'B', 'C', 'D')
- CHECK state IN ('OK', 'FLAGGED', 'RESOLVED')

**Priority Logic:**
- **Get Effective Text for Display**: finalText (if set) → originalText (default)
  - replacementText is optional middle suggestion for human review but never displayed as final
- **Get Effective Verdict for Scoring**: finalVerdict (if set) → verifiedVerdict (if set) → originalVerdict (default)
- **Requires Review Flag**: true when verifiedVerdict differs from originalVerdict AND finalVerdict is null

**State Values:**
- `OK`: Normal, no issues detected
- `FLAGGED`: Marked as needing human attention/review
- `RESOLVED`: Issue has been addressed and resolved

**Workflow:**
1. 📝 AI generation creates option with originalText and originalVerdict
2. ✅ LLM verification step optionally suggests replacementText and may change verdict to verifiedVerdict
3. 🚩 If verification differs from AI, option is flagged for review (state or requires_review flag)
4. 👤 Human reviewer can accept/override with finalText and finalVerdict
5. 🔍 System uses getEffectiveText() and getEffectiveVerdict() for final display and scoring

### 9. Attempt (Test Attempts with Scoring)
Tracks user test attempts and scoring results.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `testId` (Long) - Foreign key to Test
- `userId` (Long) - User identifier who is taking the test
- `status` (AttemptStatus) - STARTED / SUBMITTED / SCORED
- `totalQuestions` (Integer) - Total number of questions in test
- `correctCount` (Integer, default: 0) - Number of correctly answered questions
- `wrongCount` (Integer, default: 0) - Number of incorrectly answered questions
- `scorePercent` (Double, default: 0.0) - Score as percentage (0-100)
- `startedAt` (Instant) - When attempt was started (auto-timestamp)
- `submittedAt` (Instant, nullable) - When user submitted answers
- `scoredAt` (Instant, nullable) - When scoring was complete
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - System/user who created the attempt
- `updatedBy` (Long) - User who last updated the attempt

**Constraints:**
- CHECK status IN ('STARTED', 'SUBMITTED', 'SCORED')
- CHECK totalQuestions > 0

**Status Lifecycle:**
- `STARTED`: Answer sheet created, user is answering questions
- `SUBMITTED`: User submitted all answers
- `SCORED`: Answer sheet evaluated, results calculated

**Relationship:**
- One-to-Many with AttemptAnswer (one answer record per question)

### 10. AttemptAnswer (Individual Question Answers)
Records user's answer to a single question in an attempt.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `attemptId` (Long) - Foreign key to Attempt
- `questionId` (Long) - Foreign key to TestQuestion
- `correct` (Boolean) - Whether the answer was correct (based on verdict comparison)
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - System who created the answer record
- `updatedBy` (Long) - User/system who last updated it

**Constraints:**
- UNIQUE(attemptId, questionId) - One answer per question per attempt

**Relationship:**
- User's chosen answer(s) are stored in separate AttemptAnswerChosenOption table
- Supports multiple choices for questions with MULTIPLE_CORRECT cardinality

### 10.1 AttemptAnswerChosenOption (Chosen Answer Letters)
Junction table storing which option(s) the user selected for a question.

**Fields:**
- `attemptAnswerId` (Long) - Foreign key to AttemptAnswer
- `chosenOption` (String) - Selected option letter (A, B, C, or D)

**Constraints:**
- PRIMARY KEY (attemptAnswerId, chosenOption)
- CHECK chosenOption IN ('A', 'B', 'C', 'D')

**Design:**
- Supports single or multiple selections per question
- Each selected option creates one row
- For SINGLE_CORRECT questions: typically one row per answer
- For MULTIPLE_CORRECT questions: multiple rows per answer

### 11. GenerationRun (Generation Audit Trail)
Tracks individual RAG generation attempts for debugging and reproducibility.

**Fields:**
- `id` (Long) - Primary key
- `testId` (Long) - Foreign key to Test (which test was this generation for?)
- `model` (String) - AI model used (e.g., gpt-4)
- `promptTemplateVersion` (String) - Which prompt template version was used
- `retrievedChunkIdsJson` (JSONB, optional) - JSON array of chunk IDs retrieved before generation
- `rawResponse` (String, optional) - Raw API response from LLM (for debugging)
- `status` (GenerationRunStatus) - SUCCESS / FAILED
- `errorMessage` (String, optional) - Error details if generation failed
- `createdAt`, `updatedAt` (Instant) - Timestamps
- `createdBy` (Long) - System/user who initiated the generation
- `updatedBy` (Long) - System/user who updated the record

**Constraints:**
- CHECK status IN ('SUCCESS', 'FAILED')

**Use Cases:**
- 🏆 Debugging RAG quality (which chunks were retrieved?)
- 🏆 Reproducibility (regenerate with same model/template)
- 🏆 A/B testing different prompts and models
- 🏆 Performance analysis (model choice vs question quality)

## Enums

### DocumentStatus
- `UPLOADED`: File received in storage, awaiting ingestion
- `INGESTING`: Chunks being extracted and embedded
- `INGESTED`: Ready for RAG queries
- `FAILED`: Extraction/embedding failed

### TestDifficulty
- `EASY`: Basic/foundational level questions
- `MEDIUM`: Intermediate level questions  
- `HARD`: Advanced/expert level questions

### TestStatus
- `CREATED`: Test record initialized, waiting for generation
- `GENERATED`: Questions generated successfully
- `REQUIRES_REVIEW`: Generated content flagged for human review
- `REVIEWED`: Human review completed
- `FAILED`: Generation process failed

### CoverageMode
- `BALANCED_PER_DOCS`: Distribute questions across documents equally
- `BALANCED_PER_DOC_CHUNKS`: Distribute questions across chunks equally
- `FOCUSED`: Concentrate on specific high-value topics

### AnswerCardinality
- `SINGLE_CORRECT`: Multiple choice with one correct answer (traditional MCQ)
- `MULTIPLE_CORRECT`: Multiple choice with multiple correct answers (select all that apply)

### AttemptStatus
- `STARTED`: Answer sheet created, user is answering
- `SUBMITTED`: User submitted all answers
- `SCORED`: Results calculated

### OptionLetter
- `A`, `B`, `C`, `D` - Answer option letters

### OptionState
- `OK`: Normal, no issues detected
- `FLAGGED`: Marked as needing human attention/review
- `RESOLVED`: Issue has been addressed

### GenerationRunStatus
- `SUCCESS`: Generation completed without errors
- `FAILED`: Generation encountered an error

## Junction Tables (Many-to-Many Relationships)

### test_used_chunks
Maps which DocumentChunks were used as source material for generating a Test.

**Fields:**
- `testId` (Long) - Foreign key to Test
- `chunkId` (Long) - Foreign key to DocumentChunk
- PRIMARY KEY (testId, chunkId)

**Purpose:** Track which chunks were retrieved and used during RAG generation

### test_covered_topics
Maps which Topics are covered by questions in a Test.

**Fields:**
- `testId` (Long) - Foreign key to Test
- `topicId` (Long) - Foreign key to Topic
- PRIMARY KEY (testId, topicId)

**Purpose:** Enable filtering tests by topic coverage

### test_covered_documents
Maps which Documents are cited as sources in a Test.

**Fields:**
- `testId` (Long) - Foreign key to Test
- `documentId` (Long) - Foreign key to Document
- PRIMARY KEY (testId, documentId)

**Purpose:** Track document usage for test statistics and audit

## Vector Database (Separate PostgreSQL with pgvector)

This is a separate database (postgres-vectors) containing only embedding data for efficient similarity search.

### ChunkEmbedding (Vector Storage)
Maps DocumentChunks to their vectorized embeddings for semantic search.

**Fields:**
- `id` (Long) - Primary key
- `tenantId` (Long) - Tenant identifier
- `chunkId` (Long) - Foreign key to DocumentChunk  
- `documentId` (Long) - Foreign key to Document
- `topicId` (Long) - Foreign key to Topic (for semantic filtering)
- `sectionId` (Long, nullable) - Foreign key to DocumentSection
- `chunkIndex` (Integer) - Chunk order
- `embedding` (float[1536]) - Vector embedding (pgvector type, 1536 dimensions for OpenAI)
- `embeddingModel` (String) - Model used to generate embedding (e.g., text-embedding-3-small)
- `createdAt` (Instant) - Timestamp

**Indexes:**
- `IVFFlat index` on embedding column using cosine distance for efficient similarity search
- Regular indexes on topicId, documentId for metadata filtering
- Composite indexes on (tenantId, topicId) and (tenantId, documentId) for multi-tenant semantic search

**Design Rationale:**
- Separate database for vector storage allows independent scaling
- IVFFlat index provides efficient k-NN search for RAG queries
- Metadata columns enable filtering results by topic, document before similarity ranking
- Tenant-scoped for data isolation

## Database Schema

The complete database schema is defined in:
```
src/main/resources/db/migration/V1__create_initial_schema.sql
```

### Design Principles

#### Multi-Tenancy
- Every core entity includes a `tenantId` field for data isolation
- Queries are scoped by tenant to prevent cross-tenant data leaks
- Document chunks, vectors, and attempts are all tenant-scoped

#### Audit Trail
- All domain entities include standard audit fields:
  - `createdAt`, `updatedAt` (Instant) - Timestamps
  - `createdBy`, `updatedBy` (Long/BIGINT) - User/system identifiers
- Enables tracking of who made changes and when
- Junction tables are minimal and not audited

#### Versioning
- Documents track `version` (Integer) to support updates without breaking existing chunks
- DocumentChunks reference `documentVersion` for version-aware chunk retrieval
- Allows safe document re-ingestion without invalidating historical references

### Key Features:
- ✅ Multi-tenant isolation with tenantId on all core entities
- ✅ Complete audit trail (createdBy, updatedBy, createdAt, updatedAt)
- ✅ pgvector extension enabled in vector database
- ✅ All tables with proper constraints and CHECK clauses
- ✅ Foreign keys with appropriate cascade rules (CASCADE or SET NULL)
- ✅ Unique constraints for data integrity (e.g., UNIQUE(tenantId, documentId, ordinal))
- ✅ Performance indexes on common query patterns (tenantId, status, topic_id, etc.)
- ✅ Vector similarity index (IVFFlat with cosine distance)
- ✅ Lifecycle constraint validation (e.g., generation_started_at <= generation_finished_at)

## Building

```bash
mvn clean compile
```

## Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/knowgauge
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Prerequisites

- Java 21
- PostgreSQL with pgvector extension
- Maven 3.6+

## pgvector Installation

Install pgvector extension in PostgreSQL:
```sql
CREATE EXTENSION vector;
```

## Tech Stack

- Spring Boot 3.2.1
- Spring Data JPA
- PostgreSQL with pgvector
- Flyway for migrations
- Lombok 1.18.30 for boilerplate reduction
- MapStruct 1.5.5 for DTO mapping

# KnowGauge Service Core - Domain Model

This project implements the complete domain model for the KnowGauge RAG-based knowledge assessment platform.

## Domain Entities

### 1. Topic (Tree-based Taxonomy)
Represents both top-level topics and subtopics via `parentId`.

**Fields:**
- `id` (Long) - Primary key
- `parentId` (Long, nullable) - Reference to parent topic (null = root topic)
- `name` (String) - Topic name
- `description` (String, optional) - Topic description
- `path` (String, optional) - Hierarchical path (e.g., `/java/collections/list`)
- `depth` (Integer) - Tree depth (0 = root)
- `createdAt`, `updatedAt` (Instant) - Timestamps

**Benefits:**
- ✅ Unlimited depth support (Java → Collections → List → ArrayList)
- ✅ Easy filtering in RAG metadata
- ✅ Matches real-world knowledge trees

### 2. Document (Uploaded Knowledge Sources)
Represents uploaded documents (PDF support).

**Fields:**
- `id` (Long) - Primary key
- `title` (String) - Document title
- `originalFileName` (String) - Original file name
- `contentType` (String) - MIME type (PDF for now)
- `fileSizeBytes` (Long) - File size
- `storageKey` (String) - Storage location (local FS / S3 / MinIO)
- `topicId` (Long) - Foreign key to Topic (any level)
- `version` (String, optional) - Version identifier
- `status` (DocumentStatus) - UPLOADED / INGESTING / INGESTED / FAILED
- `uploadedAt` (Instant) - Upload timestamp
- `ingestedAt` (Instant, nullable) - Ingestion completion time
- `uploadedBy` (String, nullable) - User identifier
- `errorMessage` (String, nullable) - Error details if failed

### 3. DocumentSection (Optional Document Structure)
Useful for headline-based chunking and better citations.

**Fields:**
- `id` (Long) - Primary key
- `documentId` (Long) - Foreign key to Document
- `title` (String) - Section title (e.g., "2.3 HashMap Internals")
- `orderIndex` (Integer) - Section order
- `startPage`, `endPage` (Integer, nullable) - Page range
- `createdAt` (Instant) - Timestamp

### 4. DocumentChunk (Logical Chunks)
Represents stable, auditable chunks for grounding.

**Fields:**
- `id` (Long) - Primary key
- `documentId` (Long) - Foreign key to Document
- `topicId` (Long) - Foreign key to Topic
- `sectionId` (Long, nullable) - Foreign key to DocumentSection
- `chunkIndex` (Integer) - Chunk order within document
- `chunkText` (String) - Chunk content
- `startPage`, `endPage` (Integer, nullable) - Page range
- `charStart`, `charEnd` (Integer, nullable) - Character positions
- `checksum` (String, optional) - For deduplication/debugging
- `createdAt` (Instant) - Timestamp

**Bridge between:**
- 💡 Relational DB (audit, reproducibility)
- 💡 Vector DB (semantic search)

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

### 6. Test (Generated Tests)
Represents generated tests with reproducibility.

**Fields:**
- `id` (Long) - Primary key
- `topicId` (Long) - Foreign key to Topic
- `difficulty` (TestDifficulty) - EASY / MEDIUM / HARD
- `questionCount` (Integer) - Number of questions
- `status` (TestStatus) - CREATED / GENERATED / FAILED
- `generationModel` (String, optional) - AI model used
- `generationParamsJson` (String, optional) - Generation parameters
- `createdAt` (Instant) - Timestamp

### 7. TestQuestion (Test Questions)
Individual questions with multiple-choice answers.

**Fields:**
- `id` (Long) - Primary key
- `testId` (Long) - Foreign key to Test
- `questionIndex` (Integer) - Question order
- `questionText` (String) - Question text
- `optionA`, `optionB`, `optionC`, `optionD` (String) - Answer options
- `correctOption` (AnswerOption) - Correct answer (A/B/C/D)
- `explanation` (String, optional) - Explanation
- `sourceChunkIdsJson` (String, optional) - Source citations
- `createdAt` (Instant) - Timestamp

**Guarantees:**
- 🎯 Deterministic scoring
- 🎯 Traceability
- 🎯 No hallucinated answers

### 8. Attempt (Test Attempts)
Tracks user test attempts and scoring.

**Fields:**
- `id` (Long) - Primary key
- `testId` (Long) - Foreign key to Test
- `userId` (String, nullable) - User identifier
- `status` (AttemptStatus) - STARTED / SUBMITTED / SCORED
- `totalQuestions` (Integer) - Total questions
- `correctCount` (Integer, default: 0) - Correct answers
- `wrongCount` (Integer, default: 0) - Wrong answers
- `scorePercent` (Double, default: 0.0) - Score percentage
- `startedAt` (Instant) - Start timestamp
- `submittedAt` (Instant, nullable) - Submission time
- `scoredAt` (Instant, nullable) - Scoring time

### 9. AttemptAnswer (User Answers)
Individual user answers to questions.

**Fields:**
- `id` (Long) - Primary key
- `attemptId` (Long) - Foreign key to Attempt
- `questionId` (Long) - Foreign key to TestQuestion
- `chosenOption` (AnswerOption) - User's choice (A/B/C/D)
- `correct` (Boolean) - Whether answer was correct
- `answeredAt` (Instant) - Timestamp

### 10. GenerationRun (Optional Auditability)
Tracks RAG generation runs for debugging.

**Fields:**
- `id` (Long) - Primary key
- `testId` (Long) - Foreign key to Test
- `model` (String) - AI model used
- `promptTemplateVersion` (String) - Prompt template version
- `retrievedChunkIdsJson` (String, optional) - Retrieved chunks
- `rawResponse` (String, optional) - Raw AI response
- `status` (GenerationRunStatus) - SUCCESS / FAILED
- `errorMessage` (String, optional) - Error details
- `createdAt` (Instant) - Timestamp

**Gold for:**
- 🏆 Debugging RAG quality
- 🏆 Reproducibility
- 🏆 A/B testing different prompts

## Enums

- `DocumentStatus`: UPLOADED, INGESTING, INGESTED, FAILED
- `TestDifficulty`: EASY, MEDIUM, HARD
- `TestStatus`: CREATED, GENERATED, FAILED
- `AttemptStatus`: STARTED, SUBMITTED, SCORED
- `AnswerOption`: A, B, C, D
- `GenerationRunStatus`: SUCCESS, FAILED

## Database Schema

The complete database schema is defined in:
```
src/main/resources/db/migration/V1__create_initial_schema.sql
```

### Key Features:
- ✅ pgvector extension enabled
- ✅ All tables with proper constraints
- ✅ Foreign keys with appropriate cascade rules
- ✅ Unique constraints for data integrity
- ✅ Performance indexes
- ✅ Vector similarity index (IVFFlat with cosine distance)

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

- Java 17+
- PostgreSQL with pgvector extension
- Maven 3.6+

## pgvector Installation

Install pgvector extension in PostgreSQL:
```sql
CREATE EXTENSION vector;
```

## Tech Stack

- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL with pgvector
- Flyway for migrations
- Lombok for boilerplate reduction

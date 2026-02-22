<p align="center">
  <img src="https://github.com/user-attachments/assets/6775b4e9-0870-4bda-bebf-df967c2298dc" alt="KnowGauge Logo" width="200"/>
</p>

# KnowGauge

KnowGauge is a RAG-based Spring Boot platform for generating intelligent knowledge assessments from documents organized by hierarchical topics.

## Key Features

- 📚 **Document Management**: Upload and organize documents (PDFs) by hierarchical topics
- 🧠 **RAG-Powered Generation**: Generate MCQ tests using Retrieval-Augmented Generation
- 🎯 **Semantic Search**: pgvector-powered similarity search for relevant content retrieval
- 📊 **Comprehensive Domain Model**: Complete entity model covering documents, tests, attempts, and embeddings
- 🔄 **Reproducibility**: Full audit trail for generation runs and test attempts
- 🎨 **Landing Page**: Static web interface with logo and branding
- 📦 **Object Storage**: MinIO integration for document storage
- 🌳 **Hierarchical Topics**: Unlimited depth topic trees for knowledge organization

## Project Structure

This is a multi-module Maven project with the following structure:

```
knowgauge-service/
├── pom.xml (parent)
├── docker-compose.yml
├── Dockerfile
├── DOMAIN_MODEL.md                                     # Complete domain model documentation
├── knowgauge-service-core/                             # Core business logic and domain models
├── knowgauge-service-rest-contract/                    # API contracts and DTOs
├── knowgauge-service-rest-client/                      # Client library for consuming services
├── knowgauge-service-rest-api/                         # REST API layer (executable application)
│   └── src/main/resources/static/                      # Static web content (landing page, logo)
└── knowgauge-service-infra/                            # Infrastructure layer
  ├── knowgauge-service-infra-repository-springdata-jpa/         # Spring Data JPA repository adapter
  ├── knowgauge-service-infra-vectorstore-pgvector/              # PGVector vector store adapter
  ├── knowgauge-service-infra-storage-minio/                     # MinIO storage adapter
  ├── knowgauge-service-infra-embedding-langchain4j-openai/      # Embedding adapter
  ├── knowgauge-service-infra-testgeneration-langchain4j-openai/ # Test generation adapter
  ├── knowgauge-service-infra-documentparsing-pdfbox/            # PDF parsing adapter
  └── knowgauge-service-infra-documentsplitting-langchain4j/     # Document splitting adapter
```

## Modules

### knowgauge-service-core
Core business logic and domain models. Contains all domain entities (Topic, Document, DocumentSection, DocumentChunk, Test, TestQuestion, Attempt, etc.) with JPA annotations, and enums. Uses Lombok for boilerplate reduction.

### knowgauge-service-rest-contract
API contracts and DTOs (Data Transfer Objects). Defines the interface contracts that other modules and external clients can depend on.

### knowgauge-service-rest-client
Client library for consuming KnowGauge services. This module can be used by external applications to interact with the KnowGauge service.

### knowgauge-service-rest-api
REST API layer built with Spring Boot. This is the executable application that exposes the REST endpoints.
- Contains the main `@SpringBootApplication` class
- Depends on core, rest-contract, and infra modules
- Contains Flyway database migration scripts
- Serves static web content (landing page with logo)

### knowgauge-service-infra
Infrastructure layer split into specialized modules for better separation of concerns:

#### knowgauge-service-infra-repository-springdata-jpa
- JPA/Hibernate repositories for database access
- PostgreSQL support
- References domain entities from core module

#### knowgauge-service-infra-vectorstore-pgvector
- PostgreSQL pgvector extension integration
- Vector similarity retrieval primitives
- Candidate chunk retrieval for core orchestration

#### knowgauge-service-infra-storage-minio
- MinIO object storage integration
- Document upload and storage
- File management

#### knowgauge-service-infra-embedding-langchain4j-openai
- OpenAI embedding model integration via LangChain4j
- Embedding generation for document chunks

#### knowgauge-service-infra-testgeneration-langchain4j-openai
- OpenAI chat model integration via LangChain4j
- LLM-based test question generation

#### knowgauge-service-infra-documentparsing-pdfbox
- PDF parsing and page text extraction via PDFBox

#### knowgauge-service-infra-documentsplitting-langchain4j
- Document splitting/chunking via LangChain4j

## Building the Project

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher

### Build Commands

Build all modules:
```bash
mvn clean install
```

Build without tests:
```bash
mvn clean install -DskipTests
```

Build and package the application:
```bash
mvn clean package
```

## Running the Application

### Using Docker Compose (Recommended)

The easiest way to run the application with all dependencies:

```bash
docker-compose up
```

This will start:
- PostgreSQL database with pgvector extension
- KnowGauge service

The application will be available at: http://localhost:8080

### Running Locally

1. Start a PostgreSQL database with pgvector extension:
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=knowgauge \
  -e POSTGRES_USER=knowgauge_user \
  -e POSTGRES_PASSWORD=knowgauge_pass \
  pgvector/pgvector:pg15
```

2. Build the project:
```bash
mvn clean install -DskipTests
```

3. Run the application:
```bash
java -jar knowgauge-service-rest-api/target/knowgauge-service-rest-api-1.0.0-SNAPSHOT.jar
```

## Database Schema

The application uses PostgreSQL with pgvector extension and Flyway migrations. The database schema includes:

**Migration V1 (Initial Schema):**
- **topics**: Hierarchical topic structure (supports unlimited depth via parent_id)
- **documents**: Uploaded documents with metadata and status tracking
- **document_sections**: Optional document structure for better chunking
- **document_chunks**: Logical text chunks for grounding
- **tests**: Generated MCQ tests with reproducibility tracking
- **test_questions**: Individual questions with multiple-choice answers
- **attempts**: User test attempts and scoring
- **attempt_answers**: User answers to questions
- **generation_runs**: Audit trail for RAG generation runs

**Migration V2 (pgvector and Embeddings):**
- **chunk_embeddings**: Vector embeddings storage with pgvector
- IVFFlat index for similarity search (cosine distance)
- Indexes on chunk_id and embedding_model for efficient queries

See `DOMAIN_MODEL.md` for complete details on all entities, fields, and relationships.

## Development

### Module Dependencies

```
rest-api → core, rest-contract, infra modules (repository-springdata-jpa, testgeneration-langchain4j-openai, storage-minio, vectorstore-pgvector, embedding-langchain4j-openai, documentparsing-pdfbox, documentsplitting-langchain4j)
client → rest-contract
infra/repository-springdata-jpa → core
infra/testgeneration-langchain4j-openai → core
infra/storage-minio → core
infra/vectorstore-pgvector → core
infra/embedding-langchain4j-openai → core
infra/documentparsing-pdfbox → core
infra/documentsplitting-langchain4j → core
core → (independent)
rest-contract → (independent)
```

### Adding New Features

1. Add domain logic to `knowgauge-service-core`
2. Define API contracts in `knowgauge-service-rest-contract`
3. Implement REST endpoints in `knowgauge-service-rest-api`
4. Add database entities and repositories in `knowgauge-service-infra/knowgauge-service-infra-repository-springdata-jpa`
5. Implement external integrations in appropriate infra modules (`...-testgeneration-langchain4j-openai`, `...-embedding-langchain4j-openai`, `...-storage-minio`, `...-vectorstore-pgvector`, `...-documentparsing-pdfbox`, `...-documentsplitting-langchain4j`)

## Technology Stack

- Java 21
- Spring Boot 3.2.1
- Spring Data JPA
- Flyway (database migrations)
- PostgreSQL with pgvector extension
- Lombok (boilerplate reduction)
- Maven
- Docker & Docker Compose
- MinIO (object storage)
- LLM integration (RAG-based question generation)
- LangChain4j (OpenAI integration for embeddings and test generation)


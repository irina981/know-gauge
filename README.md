# know-gauge

KnowGauge is a RAG-based Spring Boot platform for generating intelligent knowledge assessments. Build a RAG-based Spring Boot application that generates multiple-choice tests (MCQs) from stored documents organized by topics and subtopics.

## Project Structure

This is a multi-module Maven project with the following structure:

```
knowgauge-service/
├── pom.xml (parent)
├── docker-compose.yml
├── Dockerfile
├── knowgauge-service-core/          # Core business logic and domain models
├── knowgauge-service-contract/      # API contracts and DTOs
├── knowgauge-service-client/        # Client library for consuming services
├── knowgauge-service-rest-api/      # REST API layer (executable application)
└── knowgauge-service-infra/         # Infrastructure layer (database, migrations)
```

## Modules

### knowgauge-service-core
Core business logic and domain models. This module is framework-independent and contains the heart of the application logic.

### knowgauge-service-contract
API contracts and DTOs (Data Transfer Objects). Defines the interface contracts that other modules and external clients can depend on.

### knowgauge-service-client
Client library for consuming KnowGauge services. This module can be used by external applications to interact with the KnowGauge service.

### knowgauge-service-rest-api
REST API layer built with Spring Boot. This is the executable application that exposes the REST endpoints.
- Contains the main `@SpringBootApplication` class
- Depends on core, contract, and infra modules

### knowgauge-service-infra
Infrastructure layer including database access and external integrations.
- JPA/Hibernate for database access
- Flyway for database migrations
- PostgreSQL support

## Building the Project

### Prerequisites
- Java 17 or higher
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
- PostgreSQL database
- KnowGauge service

The application will be available at: http://localhost:8080

### Running Locally

1. Start a PostgreSQL database:
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=knowgauge \
  -e POSTGRES_USER=knowgauge_user \
  -e POSTGRES_PASSWORD=knowgauge_pass \
  postgres:15-alpine
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

The application uses PostgreSQL with Flyway migrations. The initial schema (V1__init.sql) includes:

- **documents**: Stores documents with title, content, topic, and subtopic
- **questions**: Stores generated MCQ questions with options and correct answer

## Development

### Module Dependencies

```
rest-api → core, contract, infra
client → contract
infra → core
core → (independent)
contract → (independent)
```

### Adding New Features

1. Add domain logic to `knowgauge-service-core`
2. Define API contracts in `knowgauge-service-contract`
3. Implement REST endpoints in `knowgauge-service-rest-api`
4. Add database entities and repositories in `knowgauge-service-infra`

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- Flyway (database migrations)
- PostgreSQL
- Maven
- Docker & Docker Compose


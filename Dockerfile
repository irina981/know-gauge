# Multi-stage build for KnowGauge Service
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent POM and module POMs
COPY pom.xml .
COPY knowgauge-service-core/pom.xml knowgauge-service-core/
COPY knowgauge-service-rest-contract/pom.xml knowgauge-service-rest-contract/
COPY knowgauge-service-rest-client/pom.xml knowgauge-service-rest-client/
COPY knowgauge-service-rest-api/pom.xml knowgauge-service-rest-api/
COPY knowgauge-service-infra/knowgauge-service-infra-embedding-langchain4j-openai/pom.xml knowgauge-service-infra/knowgauge-service-infra-embedding-langchain4j-openai/
COPY knowgauge-service-infra/knowgauge-service-infra-testgeneration-langchain4j-openai/pom.xml knowgauge-service-infra/knowgauge-service-infra-testgeneration-langchain4j-openai/
COPY knowgauge-service-infra/knowgauge-service-infra-repository-springdata-jpa/pom.xml knowgauge-service-infra/knowgauge-service-infra-repository-springdata-jpa/
COPY knowgauge-service-infra/knowgauge-service-infra-storage-minio/pom.xml knowgauge-service-infra/knowgauge-service-infra-storage-minio/
COPY knowgauge-service-infra/knowgauge-service-infra-vectorstore-pgvector/pom.xml knowgauge-service-infra/knowgauge-service-infra-vectorstore-pgvector/
COPY knowgauge-service-infra/knowgauge-service-infra-documentparsing-pdfbox/pom.xml knowgauge-service-infra/knowgauge-service-infra-documentparsing-pdfbox/
COPY knowgauge-service-infra/knowgauge-service-infra-documentsplitting-langchain4j/pom.xml knowgauge-service-infra/knowgauge-service-infra-documentsplitting-langchain4j/

# go-offline is flaky; just warm cache by validating model
RUN mvn -B -DskipTests -q validate

# now copy everything
COPY . .

# build from root reactor
RUN mvn -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/knowgauge-service-rest-api/target/*.jar knowgauge-service.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "knowgauge-service.jar"]

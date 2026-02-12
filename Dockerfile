# Multi-stage build for KnowGauge Service
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy parent POM and module POMs
COPY pom.xml .
COPY knowgauge-service-core/pom.xml knowgauge-service-core/
COPY knowgauge-service-contract/pom.xml knowgauge-service-contract/
COPY knowgauge-service-client/pom.xml knowgauge-service-client/
COPY knowgauge-service-rest-api/pom.xml knowgauge-service-rest-api/
COPY knowgauge-service-infra/knowgauge-service-lang-chain/pom.xml knowgauge-service-infra/knowgauge-service-lang-chain/
COPY knowgauge-service-infra/knowgauge-service-jpa-repo/pom.xml knowgauge-service-infra/knowgauge-service-jpa-repo/
COPY knowgauge-service-infra/knowgauge-service-minio-storage/pom.xml knowgauge-service-infra/knowgauge-service-minio-storage/
COPY knowgauge-service-infra/knowgauge-service-pg-vector/pom.xml knowgauge-service-infra/knowgauge-service-pg-vector/
COPY knowgauge-service-infra/knowgauge-service-pdfbox/pom.xml knowgauge-service-infra/knowgauge-service-pdfbox/

# go-offline is flaky; just warm cache by validating model
RUN mvn -B -DskipTests -q validate

# now copy everything
COPY . .

# build from root reactor
RUN mvn -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/knowgauge-service-rest-api/target/*.jar knowgauge-service.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "knowgauge-service.jar"]

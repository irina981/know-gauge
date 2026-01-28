# Multi-stage build for KnowGauge Service
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy parent POM and module POMs
COPY pom.xml .
COPY knowgauge-service-core/pom.xml knowgauge-service-core/
COPY knowgauge-service-contract/pom.xml knowgauge-service-contract/
COPY knowgauge-service-client/pom.xml knowgauge-service-client/
COPY knowgauge-service-rest-api/pom.xml knowgauge-service-rest-api/
COPY knowgauge-service-infra/pom.xml knowgauge-service-infra/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY knowgauge-service-core/src knowgauge-service-core/src
COPY knowgauge-service-contract/src knowgauge-service-contract/src
COPY knowgauge-service-client/src knowgauge-service-client/src
COPY knowgauge-service-rest-api/src knowgauge-service-rest-api/src
COPY knowgauge-service-infra/src knowgauge-service-infra/src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/knowgauge-service-rest-api/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

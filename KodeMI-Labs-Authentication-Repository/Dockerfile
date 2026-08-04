# -------- Build Stage --------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy only required files first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests


# -------- Runtime Stage --------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy built jar
COPY --from=builder /build/target/*.jar app.jar

# Set port via environment (cloud-friendly)
ENV SERVER_PORT=8090

EXPOSE 8090

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]

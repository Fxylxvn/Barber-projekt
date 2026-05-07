# ==========================================
# Dockerfile - Multi-stage Build
# ==========================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/Barber-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run application with PostgreSQL profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]

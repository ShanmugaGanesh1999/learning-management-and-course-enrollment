# Multi-stage build
# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-focal AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .

# Install Maven (builder-only) and prefetch deps for faster rebuilds
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven ca-certificates \
    && mvn -q -DskipTests dependency:go-offline \
    && rm -rf /var/lib/apt/lists/*

COPY src src

# Build application
RUN mvn -q clean package -DskipTests \
    && rm -rf ~/.m2

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-slim

# Healthcheck helper
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN useradd -m -u 1000 appuser

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/*SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (service-specific)
EXPOSE 8003

# Health check (Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8003/api/enrollments/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]

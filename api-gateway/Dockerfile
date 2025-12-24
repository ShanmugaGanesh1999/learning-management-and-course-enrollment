# Multi-stage build
# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-focal AS builder

WORKDIR /app

COPY pom.xml .

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven ca-certificates \
    && mvn -q -DskipTests dependency:go-offline \
    && rm -rf /var/lib/apt/lists/*

COPY src src

RUN mvn -q clean package -DskipTests \
    && rm -rf ~/.m2

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-slim

# Healthcheck helper
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -u 1000 appuser

WORKDIR /app

COPY --from=builder /app/target/*SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

# ==========================================
# Stage 1: Build Java 21 Application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

RUN apk add --no-cache maven

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests=true

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create persistent storage directories owned by non-root user
RUN mkdir -p /app/backup_storage /app/metadata /app/restore /app/sample_data \
    && chown -R appuser:appgroup /app

COPY --from=builder /build/target/ThreadVault-1.0-SNAPSHOT.jar /app/app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", "--server"]


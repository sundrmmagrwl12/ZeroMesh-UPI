# ── Stage 1: Build with Maven ─────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy POMs for dependency caching
COPY pom.xml .
COPY common-mesh-dto/pom.xml    common-mesh-dto/pom.xml
COPY ingestion-service/pom.xml  ingestion-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml

RUN mvn dependency:go-offline -pl common-mesh-dto,settlement-service \
    --also-make -q 2>/dev/null || true

# Copy source
COPY common-mesh-dto/src    common-mesh-dto/src
COPY settlement-service/src settlement-service/src

# Package settlement-service (common-mesh-dto built as dependency)
RUN mvn package -pl common-mesh-dto,settlement-service \
    --also-make -DskipTests -q

# ── Stage 2: Lightweight Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/settlement-service/target/settlement-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 1: Build with Maven ─────────────────────────────────────────────────
# Full Maven + JDK image to compile and package the JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy parent POM and all module POMs first (for dependency caching)
COPY pom.xml .
COPY common-mesh-dto/pom.xml   common-mesh-dto/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml

# Download dependencies before copying source (Docker layer cache optimization)
RUN mvn dependency:go-offline -pl common-mesh-dto,ingestion-service \
    --also-make -q 2>/dev/null || true

# Copy source code
COPY common-mesh-dto/src   common-mesh-dto/src
COPY ingestion-service/src ingestion-service/src

# Build — skip tests (tests need live Redis/Kafka, not available at build time)
RUN mvn package -pl common-mesh-dto,ingestion-service \
    --also-make -DskipTests -q

# ── Stage 2: Lightweight Runtime ──────────────────────────────────────────────
# Alpine JRE only — no Maven, no JDK sources, final image ~180MB vs ~600MB
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/ingestion-service/target/ingestion-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

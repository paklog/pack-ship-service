# ============================================
# Multi-stage Dockerfile for Pack SERVICE_TITLE Ship Service
# Best Practices: Multi-stage build, non-root user, layer caching, security hardening
# ============================================

# ============================================
# Stage 1: Build Stage
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set build arguments
ARG APP_VERSION=1.0.0-SNAPSHOT
ARG BUILD_DATE
ARG VCS_REF

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /build

# Copy Maven files for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B || true

# Copy application source
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B && \
    mv target/*.jar target/app.jar

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Metadata labels (OCI standard)
LABEL org.opencontainers.image.title="Pack SERVICE_TITLE Ship Service" \
      org.opencontainers.image.description="Intelligent packing and shipping service" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.vendor="Paklog" \
      org.opencontainers.image.authors="Paklog Engineering Team" \
      org.opencontainers.image.source="https://github.com/paklog/pack-ship-service" \
      com.paklog.service.tier="execution" \
      com.paklog.service.phase="3"

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder --chown=spring:spring /build/target/app.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true"

# Use dumb-init to handle signals properly
ENTRYPOINT ["/usr/bin/dumb-init", "--"]

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

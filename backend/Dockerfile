# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy dependency descriptors first for layer caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Pre-fetch dependencies (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon -q || true

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user
RUN addgroup -S careerforge && adduser -S careerforge -G careerforge

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

RUN chown careerforge:careerforge app.jar

USER careerforge

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]

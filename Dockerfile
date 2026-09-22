# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Pre-download dependencies (cached layer unless build.gradle/settings.gradle change)
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Copy the rest of the source and build the jar (skip tests for faster image builds)
COPY src ./src
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage - compiles the application and packages the runnable jar.
# Uses the project's own Maven wrapper (mvnw), matching local dev/CI usage.
# Vaadin has no separate frontend toolchain to install here: this project
# ships no custom npm dependencies, so the Flow production bundle is
# resolved automatically as part of the normal Maven build.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# The project's mvnw (Maven Wrapper 3.x, "only-script" distribution) downloads
# Maven itself on first run and needs curl/wget + unzip to do so - none of
# which are present in a bare JDK-alpine image.
RUN apk add --no-cache curl unzip

# Copy only what's needed to resolve dependencies first, so this (expensive,
# network-bound) layer is cached across rebuilds that only change source code.
# The cache mount persists the local Maven repo across builds without baking
# it into any image layer (it never ends up in the final image either way,
# since only the runtime stage below is kept).
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q dependency:go-offline || true

# Now copy the actual source and build the jar. Tests are skipped here: they
# run in CI/locally, not as part of the image build.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Runtime stage - minimal JRE image containing only the built jar.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# curl backs the container healthcheck (see docker-compose.yml), which polls
# the app's already-public "/login" route - no application code change needed.
RUN apk add --no-cache curl \
    && addgroup -S gymtracker && adduser -S gymtracker -G gymtracker

WORKDIR /app
COPY --from=build --chown=gymtracker:gymtracker /workspace/target/*.jar app.jar
USER gymtracker

# Sensible container defaults; every one of these is overridable at
# `docker run -e ...` / docker-compose `environment:` time.
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

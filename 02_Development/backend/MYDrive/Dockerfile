# syntax=docker/dockerfile:1

# ─── Stage 1: build ──────────────────────────────────────────────────────────
# Use a Maven image with JDK 17 (matches <java.version>17</java.version>).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM first and pre-fetch dependencies so this layer is cached and
# only re-runs when pom.xml changes (not on every source edit).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy sources and build the executable WAR. Tests are skipped to keep the
# image build fast and deterministic; drop -DskipTests to run them.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
# Slim JRE-only image for a smaller, lower-attack-surface runtime.
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# curl is needed for the HEALTHCHECK (not bundled in the temurin image); add a
# non-root user to run the app.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring && useradd --system --gid spring spring

# Copy the repackaged (executable) WAR produced by spring-boot-maven-plugin.
# finalName defaults to artifactId-version, so the wildcard captures it.
COPY --from=build /build/target/*.war app.war

# Container-friendly defaults for the on-disk paths the app needs. These map to
# the app.storage.* / app.signing.* properties via Spring's relaxed binding and
# can be overridden at run time. The keystore is auto-generated on first run.
ENV APP_STORAGE_PATH=/app/storage \
    APP_SIGNING_KEYSTORE_PATH=/app/signing/mydrive-demo.p12 \
    SERVER_PORT=8083 \
    JAVA_OPTS=""

# Create the data directories and hand ownership to the non-root user. Declare
# them as volumes so uploaded files and the signing keystore survive restarts.
RUN mkdir -p /app/storage /app/signing && chown -R spring:spring /app
VOLUME ["/app/storage", "/app/signing"]

USER spring
EXPOSE 8083

# Hit the Actuator health endpoint. Spring Security secures it (returns 401),
# so we treat any non-5xx response as "the app is up and serving" — a refused
# connection yields code 000 and fails. (To get a true "UP" check, permit
# /actuator/health in SecurityConfig and switch this to grep for '"status":"UP"'.)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${SERVER_PORT}/actuator/health"); [ "$code" -ge 200 ] && [ "$code" -lt 500 ]

# Use the shell form so $JAVA_OPTS expands; exec keeps Java as PID 1 for signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.war"]

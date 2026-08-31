# syntax=docker/dockerfile:1

# 1. Build the single page application on its own, so the JVM stage never needs Node.
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Build the fat jar, with the compiled web app dropped straight into the resources.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY core core
COPY server server
COPY frontend/build.gradle.kts frontend/build.gradle.kts
COPY --from=web /web/dist server/src/main/resources/static
RUN chmod +x gradlew && ./gradlew :server:buildFatJar -PincludeFrontend=false --no-daemon

# 3. Ship a plain JRE with the jar and nothing else.
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --system --create-home --uid 10001 cardgame
WORKDIR /app
COPY --from=build /src/server/build/libs/server-all.jar app.jar
USER cardgame
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 CMD curl -fsS http://localhost:8080/api/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

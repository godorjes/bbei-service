# syntax=docker/dockerfile:1

FROM node:20-bookworm-slim AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-11 AS backend-build

WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -ntp package

FROM eclipse-temurin:11-jre-jammy AS runtime

RUN groupadd --gid 10001 bibei \
    && useradd --uid 10001 --gid 10001 --no-create-home --home-dir /nonexistent --shell /usr/sbin/nologin bibei \
    && mkdir -p /app /data \
    && chown -R 10001:10001 /app /data

WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/backend/target/bibei-backend-0.0.1-SNAPSHOT.jar /app/app.jar

ENV PORT=8080
ENV BIBEI_DB_URL="jdbc:h2:file:/data/bibei;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"

VOLUME ["/data"]
EXPOSE 8080
STOPSIGNAL SIGTERM

USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

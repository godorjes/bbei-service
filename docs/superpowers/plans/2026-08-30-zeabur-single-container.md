# Zeabur Single-Container Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify a production-ready Zeabur image that serves the Vue SPA and Spring Boot API from one Java 11 container while persisting H2 under `/data`.

**Architecture:** A Node 20 stage builds Vue, a Maven 3.9/Temurin 11 stage embeds the generated files into Spring Boot static resources, and a Temurin 11 JRE stage runs the JAR as a non-root user. Spring handles same-origin API traffic, explicit CORS exceptions, health checks, and extension-aware SPA history fallback.

**Tech Stack:** Vue 3, Vite 5, Node 20, Java 11, Spring Boot 2.7, MyBatis, H2, Maven 3.9, Docker.

**Spec:** `docs/superpowers/specs/2026-08-30-zeabur-single-container-design.md`

## Global Constraints

- Do not change existing APIs, business data models, Vue interactions, or Capacitor compatibility.
- Keep Java 11 and Spring Boot 2.7; do not use Docker Compose.
- Production is one container, one domain, one H2 writer, and exactly one replica.
- No password or wildcard CORS origin may be baked into the image.

---

### Task 1: HTTP deployment behavior

**Files:**
- Create: `backend/src/test/java/com/bibei/WebDeploymentIntegrationTest.java`
- Create: `backend/src/test/resources/static/index.html`
- Create: `backend/src/test/resources/static/assets/test-app.js`
- Create: `backend/src/main/java/com/bibei/config/SpaFallbackFilter.java`
- Create: `backend/src/main/java/com/bibei/controller/HealthController.java`
- Modify: `backend/src/main/java/com/bibei/config/WebConfig.java`

**Interfaces:**
- Produces: `GET /healthz`, SPA forwarding to `/index.html`, `BIBEI_CORS_ALLOWED_ORIGINS` parsing.

- [ ] Write integration tests asserting HTML for `/`, `/organize`, and `/scenes/1`; 404 without HTML for `/api/not-found`; direct JS response for `/assets/test-app.js`; minimal JSON for `/healthz`; trusted-only CORS headers.
- [ ] Run `mvn -Dtest=WebDeploymentIntegrationTest test` and confirm history routes and health fail before production code exists.
- [ ] Implement `SpaFallbackFilter`, `HealthController`, and explicit CORS configuration.
- [ ] Re-run the focused test and all backend tests.

### Task 2: Runtime configuration

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/resources/application.yml`

**Interfaces:**
- Consumes: `PORT`, `BIBEI_DB_URL`, `BIBEI_DB_USERNAME`, `BIBEI_DB_PASSWORD`, `BIBEI_CORS_ALLOWED_ORIGINS`.
- Produces: local file H2 default, graceful shutdown, idempotent schema initialization.

- [ ] Change `server.port` to `${PORT:8080}` and datasource properties to environment-backed values.
- [ ] Remove `AUTO_SERVER`, add `DB_CLOSE_ON_EXIT=FALSE` and `WRITE_DELAY=0`, and retain `spring.sql.init.mode=always`.
- [ ] Run all backend tests and `mvn package`.

### Task 3: Root container build

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

**Interfaces:**
- Consumes: `frontend/package-lock.json`, frontend source, backend Maven project.
- Produces: one non-root Java 11 image exposing default port 8080 with writable `/data`.

- [ ] Add Node 20 frontend build, Maven 3.9/Temurin 11 backend build, and Temurin 11 JRE runtime stages.
- [ ] Copy the frontend distribution before Maven packaging and configure the runtime user, `/data`, `PORT`, and production `BIBEI_DB_URL`.
- [ ] Exclude repository metadata, docs, generated outputs, mobile projects, logs, and IDE files from build context.
- [ ] Run `docker build -t bibei:zeabur-test .` and inspect the resulting JAR/runtime behavior.

### Task 4: Deployment documentation

**Files:**
- Modify: `README.md`

**Interfaces:**
- Produces: exact Zeabur root deployment, `/data` volume, environment, single-replica, backup, Cloudflare, and Cloudflare Access instructions.

- [ ] Add the Zeabur deployment section and local Docker build/run commands.
- [ ] State that destroying the server destroys the attached storage unless `/data` is downloaded first.

### Task 5: Full acceptance verification

**Files:**
- Verify all changed files and generated image behavior.

**Interfaces:**
- Consumes: final repository state and locally built `bibei:zeabur-test` image.
- Produces: recorded command results for handoff.

- [ ] Run `npm ci`, `npm test`, `npm run build`, `mvn test`, and `mvn package` from clean inputs.
- [ ] Run the container with internal `PORT=9091`, request `/healthz`, `/organize`, `/scenes/1`, static assets, and an unknown API path.
- [ ] Create an item through `/api/items` on a named temporary volume, remove the container, recreate it with the same volume, and verify the item remains.
- [ ] Confirm the container UID is non-zero and `/data` files are writable by that UID.
- [ ] Review `git diff --check`, repository status, ignored artifacts, and the requirements checklist.

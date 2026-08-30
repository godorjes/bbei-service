# Zeabur Production Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a reproducible, single-service Zeabur deployment that packages the Vue 2 frontend in the Spring Boot JAR, uses Zeabur MySQL with strict Flyway migrations, and has no Redis runtime dependency.

**Architecture:** A Node 20 build stage produces `frontend/dist`; a Maven 3.9/Temurin 11 stage copies that output into Spring Boot static resources and packages the JAR; a Temurin 11 JRE stage runs it as a non-root user. Spring uses profile-specific MySQL settings, Flyway owns schema changes, and Caffeine supplies both five-minute business caches and a separate seven-day login-token store.

**Tech Stack:** Java 11, Spring Boot 2.7.18, Maven 3.9, MyBatis-Plus 3.5.5, Flyway, Caffeine, MySQL 8, Vue 2.7, Vue CLI 5, Node 20, Docker.

**Spec:** `docs/superpowers/specs/2026-08-30-zeabur-production-deployment-design.md`

## Global Constraints

- Preserve Java 11, Spring Boot 2.7.18, Vue 2.7, all existing API paths, request/response payloads, and product interactions.
- Build and run one application service and one Zeabur MySQL service; do not require Redis, Docker Compose, or an application volume.
- Preserve pre-existing staged and unstaged user work, including the unfinished authentication code, while replacing its Redis token storage.
- Never place database passwords, tokens, `.env` contents, request arguments, or full response objects in the image or logs.
- Production must fail clearly when required MySQL configuration is absent or the database cannot be reached.
- Flyway must migrate blank MySQL 8 schemas and must not silently baseline non-empty legacy schemas by default.

---

### Task 1: Add the health endpoint through a red-green test

**Files:**
- Create: `src/test/java/com/daiqi/controller/HealthControllerTest.java`
- Create: `src/main/java/com/daiqi/controller/HealthController.java`

**Interfaces:**
- Consumes: Spring MVC `GET` request handling.
- Produces: `GET /healthz` returning HTTP 200 and `{"status":"ok"}`.

- [ ] **Step 1: Write the failing MVC test**

```java
package com.daiqi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class HealthControllerTest {
    @Test
    void healthzReturnsOnlyPublicStatus() throws Exception {
        MockMvc mvc = standaloneSetup(new HealthController()).build();
        mvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -Dtest=HealthControllerTest test`

Expected: compilation fails because `HealthController` does not exist.

- [ ] **Step 3: Implement the minimal endpoint**

```java
package com.daiqi.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "ok");
    }
}
```

- [ ] **Step 4: Run the test and verify GREEN**

Run: `mvn -Dtest=HealthControllerTest test`

Expected: one passing test and zero failures.

- [ ] **Step 5: Commit the endpoint independently**

```bash
git add src/main/java/com/daiqi/controller/HealthController.java src/test/java/com/daiqi/controller/HealthControllerTest.java
git commit -m "feat: add container health endpoint"
```

### Task 2: Replace Redis caches and tokens with Caffeine

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/com/daiqi/config/CacheConfig.java`
- Create: `src/main/java/com/daiqi/auth/AuthTokenStore.java`
- Modify: `src/main/java/com/daiqi/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/daiqi/interceptor/AuthInterceptor.java`
- Modify: `src/main/java/com/daiqi/config/WebMvcConfig.java`
- Create: `src/test/java/com/daiqi/auth/AuthTokenStoreTest.java`
- Create: `src/test/java/com/daiqi/config/CacheConfigTest.java`

**Interfaces:**
- Consumes: existing `@Cacheable`/`@CacheEvict` cache names and opaque string authentication tokens.
- Produces: `AuthTokenStore.put(String, Long)`, `getUserId(String)`, and `remove(String)`; a `CacheManager` with `tags`, `cards`, and `scenes` caches.

- [ ] **Step 1: Write failing token-store tests**

```java
package com.daiqi.auth;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenStoreTest {
    @Test
    void tokenCanBeReadAndRemoved() {
        AuthTokenStore store = new AuthTokenStore(Ticker.systemTicker(), Duration.ofDays(7));
        store.put("token", 42L);
        assertThat(store.getUserId("token")).isEqualTo(42L);
        store.remove("token");
        assertThat(store.getUserId("token")).isNull();
    }

    @Test
    void tokenExpiresAfterSevenDays() {
        AtomicLong nanos = new AtomicLong();
        AuthTokenStore store = new AuthTokenStore(nanos::get, Duration.ofDays(7));
        store.put("token", 42L);
        nanos.set(Duration.ofDays(7).plusSeconds(1).toNanos());
        assertThat(store.getUserId("token")).isNull();
    }
}
```

- [ ] **Step 2: Write a failing business-cache test**

```java
package com.daiqi.config;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {
    @Test
    void cacheManagerProvidesExistingBusinessCaches() {
        CacheManager manager = new CacheConfig().cacheManager();
        assertThat(manager.getCacheNames()).containsExactlyInAnyOrderElementsOf(
                Arrays.asList("tags", "cards", "scenes"));
    }
}
```

- [ ] **Step 3: Run the tests and verify RED**

Run: `mvn -Dtest=AuthTokenStoreTest,CacheConfigTest test`

Expected: compilation fails because Caffeine and `AuthTokenStore` are absent and `CacheConfig` still requires Redis.

- [ ] **Step 4: Replace Maven dependencies and implement Caffeine components**

Remove `spring-boot-starter-data-redis`; add:

```xml
<dependency>
  <groupId>com.github.ben-manes.caffeine</groupId>
  <artifactId>caffeine</artifactId>
</dependency>
```

Implement `AuthTokenStore` as a Spring component backed by a Caffeine cache with `maximumSize(10000)` and the injected write TTL. Replace `CacheConfig.cacheManager(RedisConnectionFactory)` with `cacheManager()` returning a `CaffeineCacheManager("tags", "cards", "scenes")`, configured with `maximumSize(500)` and `expireAfterWrite(Duration.ofMinutes(5))`.

```java
package com.daiqi.auth;

import java.time.Duration;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

@Component
public class AuthTokenStore {
    private final Cache<String, Long> tokens;

    public AuthTokenStore() {
        this(Ticker.systemTicker(), Duration.ofDays(7));
    }

    AuthTokenStore(Ticker ticker, Duration ttl) {
        this.tokens = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build();
    }

    public void put(String token, Long userId) { tokens.put(token, userId); }
    public Long getUserId(String token) { return tokens.getIfPresent(token); }
    public void remove(String token) { tokens.invalidate(token); }
}
```

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager("tags", "cards", "scenes");
    manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5)));
    return manager;
}
```

- [ ] **Step 5: Replace Redis usages without changing auth endpoints**

Inject `AuthTokenStore` into `UserServiceImpl` and `AuthInterceptor`. Replace token writes, reads, and deletes with `put`, `getUserId`, and `remove`. Inject `AuthTokenStore` into `WebMvcConfig` when constructing the interceptor. Remove all `StringRedisTemplate` imports and do not change `/api/auth/*` payloads or statuses.

```java
// UserServiceImpl
private final AuthTokenStore authTokenStore;

// buildLoginResponse
authTokenStore.put(token, user.getId());

// logout
authTokenStore.remove(token);
```

```java
// AuthInterceptor
private final AuthTokenStore authTokenStore;
Long userId = authTokenStore.getUserId(token);
if (userId == null) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return false;
}
UserContext.setUserId(userId);
```

- [ ] **Step 6: Run the focused and full tests**

Run: `mvn -Dtest=AuthTokenStoreTest,CacheConfigTest test`

Expected: three passing tests.

Run: `mvn test`

Expected: all tests pass and Spring compiles without Redis classes.

- [ ] **Step 7: Commit the cache and token migration**

```bash
git add pom.xml src/main/java/com/daiqi/auth/AuthTokenStore.java src/main/java/com/daiqi/config/CacheConfig.java src/main/java/com/daiqi/config/WebMvcConfig.java src/main/java/com/daiqi/interceptor/AuthInterceptor.java src/main/java/com/daiqi/service/impl/UserServiceImpl.java src/test/java/com/daiqi/auth/AuthTokenStoreTest.java src/test/java/com/daiqi/config/CacheConfigTest.java
git commit -m "refactor: replace Redis with local Caffeine storage"
```

### Task 3: Make logging safe for production

> Review correction: the final implementation uses `HttpRequestLoggingFilter` instead of the initially planned controller aspect. The filter logs after the servlet chain completes so `status` is the final HTTP response status, including `@ResponseStatus` and handled-error responses. `HttpRequestLoggingFilterTest` verifies final status and query-parameter redaction.

**Files:**
- Modify: `src/main/java/com/daiqi/aspect/WebLogAspect.java`
- Modify: `src/main/resources/logback-spring.xml`
- Create: `src/test/java/com/daiqi/aspect/WebLogAspectTest.java`

**Interfaces:**
- Consumes: controller invocations and the current HTTP request.
- Produces: DEBUG request metadata containing method, path, status, and duration, with no arguments, return payload, Authorization header, or token.

- [ ] **Step 1: Write the failing log-redaction test**

```java
package com.daiqi.aspect;

import java.util.stream.Collectors;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebLogAspectTest {
    @Test
    void requestLoggingNeverIncludesArgumentsOrResponsePayload() throws Throwable {
        Logger logger = (Logger) LoggerFactory.getLogger(WebLogAspect.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{"argument-secret"});
        when(point.proceed()).thenReturn("response-secret");

        try {
            assertThat(new WebLogAspect().doAround(point)).isEqualTo("response-secret");
            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain("argument-secret", "response-secret");
        } finally {
            RequestContextHolder.resetRequestAttributes();
            logger.detachAppender(appender);
        }
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -Dtest=WebLogAspectTest test`

Expected: failure because the current aspect logs request arguments and the returned object.

- [ ] **Step 3: Implement metadata-only request logging**

Use `try/finally`; call `joinPoint.proceed()` exactly once; in `finally`, emit one DEBUG event with HTTP method, request URI, response status, handler name, and elapsed milliseconds. Remove `Arrays.toString(joinPoint.getArgs())` and all result logging. Configure Logback with only a console appender, root INFO, `com.daiqi` INFO, and MyBatis/JDBC WARN.

```java
public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
    long startedAt = System.currentTimeMillis();
    ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes == null ? null : attributes.getRequest();
    try {
        return joinPoint.proceed();
    } finally {
        String method = request == null ? "N/A" : request.getMethod();
        String path = request == null ? "N/A" : request.getRequestURI();
        int status = attributes == null ? 0 : attributes.getResponse().getStatus();
        log.debug("HTTP {} {} status={} handler={} durationMs={}", method, path, status,
                joinPoint.getSignature().toShortString(), System.currentTimeMillis() - startedAt);
    }
}
```

- [ ] **Step 4: Run the focused test**

Run: `mvn -Dtest=WebLogAspectTest test`

Expected: the test passes and neither secret appears in captured events.

- [ ] **Step 5: Commit safe logging**

```bash
git add src/main/java/com/daiqi/aspect/WebLogAspect.java src/main/resources/logback-spring.xml src/test/java/com/daiqi/aspect/WebLogAspectTest.java
git commit -m "fix: keep credentials out of production logs"
```

### Task 4: Enable strict Flyway migrations and Zeabur profiles

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-prod.yml`
- Verify: `src/main/resources/db/migration/V1__init.sql`
- Verify: `src/main/resources/db/migration/V2__user.sql`

**Interfaces:**
- Consumes: `SPRING_PROFILES_ACTIVE`, `PORT`, `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`.
- Produces: a MySQL datasource with bounded timeouts and a strict Flyway startup migration.

- [ ] **Step 1: Add Flyway to Maven**

Add `org.flywaydb:flyway-core` and the matching `org.flywaydb:flyway-mysql` module. Spring Boot 2.7.18 controls the core version; use its inherited `${flyway.version}` for the database module.

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
  <version>${flyway.version}</version>
</dependency>
```

- [ ] **Step 2: Rewrite shared and development configuration**

Set `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}` and keep `server.port: ${PORT:8080}`. In dev, default host/port/database/username to localhost values but require `DB_PASSWORD`; remove every Redis property and set cache type to Caffeine.

```yaml
# application.yml
server:
  port: ${PORT:8080}
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cache:
    type: caffeine
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:dqi}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
```

- [ ] **Step 3: Rewrite production configuration**

Build the JDBC URL from the five `MYSQL_*` connection variables, add `connectTimeout=10000` and `socketTimeout=30000`, and configure Hikari with maximum pool 5, minimum idle 1, connection timeout 10000 ms, validation timeout 3000 ms, idle timeout 600000 ms, and max lifetime 1800000 ms. Enable Flyway validation and migrations while setting `baseline-on-migrate: false`.

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT:3306}/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8&connectTimeout=10000&socketTimeout=30000
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 10000
      validation-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
  flyway:
    enabled: true
    validate-on-migrate: true
    baseline-on-migrate: false
    connect-retries: 10
logging:
  level:
    com.daiqi: info
    org.mybatis: warn
    org.springframework.jdbc: warn
```

- [ ] **Step 4: Compile configuration changes**

Run: `mvn -DskipTests package`

Expected: build success without Redis auto-configuration classes.

- [ ] **Step 5: Commit database configuration**

```bash
git add pom.xml src/main/resources/application.yml src/main/resources/application-dev.yml src/main/resources/application-prod.yml src/main/resources/db/migration/V1__init.sql src/main/resources/db/migration/V2__user.sql
git commit -m "feat: configure Zeabur MySQL and Flyway"
```

### Task 5: Build the latest frontend into a minimal image

**Files:**
- Modify: `frontend/vue.config.js`
- Create: `Dockerfile`
- Create: `.dockerignore`

**Interfaces:**
- Consumes: repository root, `frontend/package-lock.json`, Maven `pom.xml`, runtime `PORT` and Spring/MySQL environment variables.
- Produces: a non-root Java 11 runtime image exposing port 8080 and serving the compiled Vue application plus `/api`.

- [ ] **Step 1: Move frontend output to `frontend/dist`**

Set `outputDir: 'dist'` and keep `publicPath: '/'` plus the existing local `/api` proxy unchanged.

- [ ] **Step 2: Prove the clean frontend build**

Remove only generated `frontend/node_modules` and `frontend/dist` in a disposable verification copy or rely on `npm ci` to recreate dependencies, then run `npm ci`, `npm run test:ui-redesign`, and `npm run build`. Expected output is `frontend/dist/index.html` plus JS/CSS assets.

- [ ] **Step 3: Add the multi-stage Dockerfile**

Use stages named `frontend-build`, `backend-build`, and `runtime`. The Maven stage copies `/workspace/frontend/dist` into `/workspace/backend/src/main/resources/static` before `mvn -B package`. The runtime stage creates UID/GID 10001, copies `target/daiqi-0.0.1-SNAPSHOT.jar` to `/app/app.jar`, declares `EXPOSE 8080`, switches to the non-root user, and runs `java -jar /app/app.jar`.

```dockerfile
FROM node:20-bookworm-slim AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-11 AS backend-build
WORKDIR /workspace/backend
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B package

FROM eclipse-temurin:11-jre-jammy AS runtime
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid 10001 --system --no-create-home app
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/backend/target/daiqi-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 4: Add the Docker build-context exclusions**

Exclude `.git`, local editors, docs, `target`, logs, `src/main/resources/static`, frontend dependencies/output, tests and reports, screenshots, local environment files, and Compose files. Do not exclude `frontend/public` or frontend source assets.

```dockerignore
.git
.idea
.vscode
.claude
docs
target
logs
src/main/resources/static
frontend/node_modules
frontend/dist
frontend/tests
test-results
playwright-report
*.log
.env*
docker-compose*.yml
debug-*.png
*ui.png
```

- [ ] **Step 5: Build the image**

Run: `docker build -t daiqi-zeabur:verify .`

Expected: all three stages complete, Maven tests pass inside the build, and the final image is created.

- [ ] **Step 6: Commit the build pipeline**

```bash
git add frontend/vue.config.js Dockerfile .dockerignore
git commit -m "build: add reproducible Zeabur container"
```

### Task 6: Remove tracked generated artifacts without deleting local copies

**Files:**
- Modify: `.gitignore`
- Remove from Git index: `target/`, `logs/`, `src/main/resources/static/`, root debug/test screenshots.

**Interfaces:**
- Consumes: current Git index and local generated files.
- Produces: a source-only repository whose clean checkout contains neither compiled frontend nor Maven outputs.

- [ ] **Step 1: Enumerate exact tracked artifacts**

Run: `git ls-files target logs src/main/resources/static "*.png"`

Expected: inspect every path before changing the index; retain product documentation images that are not test/debug output.

- [ ] **Step 2: Complete ignore rules**

Add root-anchored rules for `/src/main/resources/static/`, `/test-results/`, `/playwright-report/`, `/frontend/test-results/`, `/frontend/playwright-report/`, `/debug-*.png`, `/*ui.png`, `/product-business-map-preview.png`, and `/product-business-map.html`.

- [ ] **Step 3: Remove generated paths from the index only**

Use the following explicit index-only commands. They leave local files recoverable while ensuring the next commit removes them from Git.

```bash
git rm -r --cached --ignore-unmatch target logs src/main/resources/static
git rm --cached --ignore-unmatch debug-tag.png "场景ui.png" "新增组合ui.png" "标签ui.png"
```

- [ ] **Step 4: Verify ignore behavior**

Run: `git status --short`

Run: `git check-ignore -v target/classes/application.yml logs/app.2026-04-08.log frontend/dist/index.html src/main/resources/static/index.html`

Expected: every generated path is ignored and no product source file is unintentionally ignored.

- [ ] **Step 5: Commit repository hygiene**

```bash
git add .gitignore
git commit -m "chore: stop tracking generated artifacts"
```

### Task 7: Document Zeabur, Cloudflare, and legacy database migration

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the final Dockerfile and Spring environment contract.
- Produces: exact deployment, migration, backup, security, and troubleshooting instructions.

- [ ] **Step 1: Update local build instructions**

Document Java 11, Maven, Node 20, MySQL 8, the required local `DB_PASSWORD`, `npm ci`, frontend build/test, and Maven test/package commands. State that Redis is no longer required.

- [ ] **Step 2: Add the Zeabur deployment section**

List one GitHub application service plus one MySQL template service, root-directory deployment, no application volume, single replica, `SPRING_PROFILES_ACTIVE=prod`, `PORT`, and the five required `MYSQL_*` variables. Explain that Zeabur-provided variable references should be used rather than copying secrets into the image.

- [ ] **Step 3: Add migration and recovery instructions**

Document blank-database Flyway behavior and the explicit legacy V1/V2 baseline choices from the design. Require a verified export before migration or old-service shutdown and removal of temporary baseline variables after first success.

- [ ] **Step 4: Add Cloudflare and access protection instructions**

Document binding the hostname in Zeabur, creating the proxied Cloudflare DNS record to the target Zeabur provides, using Cloudflare SSL/TLS `Full` as Zeabur currently requires, and protecting the entire hostname with Cloudflare Access because API authorization is not complete. State that `Full (strict)` is appropriate only after independently confirming a publicly trusted, hostname-matching origin certificate.

- [ ] **Step 5: Commit documentation**

```bash
git add README.md
git commit -m "docs: add Zeabur production deployment guide"
```

### Task 8: Run full acceptance verification

**Files:**
- Verify: all modified files and built artifacts.

**Interfaces:**
- Consumes: Docker Engine and temporary task-specific Docker resources.
- Produces: fresh evidence for every acceptance criterion.

- [ ] **Step 1: Run clean frontend commands**

Run in `frontend/`: `npm ci`, `npm run test:ui-redesign`, `npm run build`.

Expected: all commands exit 0.

- [ ] **Step 2: Run Maven commands independently**

Run: `mvn test`

Run: `mvn package`

Expected: all tests and both builds exit 0.

- [ ] **Step 3: Rebuild the image from the root**

Run: `docker build --no-cache -t daiqi-zeabur:verify .`

Expected: exit 0 and a final image whose configured user is non-root.

- [ ] **Step 4: Verify blank MySQL 8 migration**

Create a uniquely named temporary Docker network and MySQL 8 container with a task-only test password. Wait for `mysqladmin ping`, run the application image with `SPRING_PROFILES_ACTIVE=prod`, all five `MYSQL_*` variables, and a non-default `PORT`. Query MySQL for `flyway_schema_history`, `tag`, `card`, `scene`, `scene_card_check`, and `user`.

Expected: all tables exist and Flyway records V1 and V2 as successful.

- [ ] **Step 5: Verify HTTP and existing API behavior**

Request `/healthz`, `/`, the JS/CSS URLs parsed from the delivered index, and existing list/create API endpoints. Expected: health and homepage return 200, assets return correct non-HTML content, and existing API payloads/statuses remain compatible.

- [ ] **Step 6: Verify required-config failure and secret redaction**

Run the application image with `SPRING_PROFILES_ACTIVE=prod` but without MySQL variables. Expected: non-zero exit and a clear missing-placeholder message. Search both successful and failed container logs for the task-only password and test token; expected: no matches.

- [ ] **Step 7: Inspect the final runtime image**

Use `docker image inspect` and `docker run --entrypoint` checks. Expected: user is UID 10001, Java is present, Node and Maven executables are absent, and no source/build directories exist.

- [ ] **Step 8: Clean up only task-specific Docker resources**

Stop and remove the explicitly named verification containers and network. Verify their names before removal; do not remove any pre-existing images, volumes, containers, networks, or user databases.

- [ ] **Step 9: Review final repository state**

Run: `git diff --check`

Run: `git status --short`

Run: `git diff --stat`

Expected: no whitespace errors, only intended source/documentation changes plus pre-existing user changes, and no generated artifacts shown as untracked.

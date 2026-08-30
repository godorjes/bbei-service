# Zeabur Production Deployment Design

## 目标

将当前 Vue 2 + Spring Boot 2.7 单仓库应用改造成可从 GitHub 仓库根目录直接部署到 Zeabur 的单服务生产版本。Vue 前端在镜像构建期编译并进入 Spring Boot JAR；运行期只有 Java 11 JRE 和应用 JAR。MySQL 由独立的 Zeabur MySQL 模板服务提供，应用不依赖 Redis 或本地持久化卷。

本改造不改变现有页面交互、业务模型、API 路径或请求响应格式，不升级 Java、Spring Boot、Vue 的大版本，并保留工作区中尚未提交的登录功能。

## 当前状态与约束

- 仓库根目录是 Spring Boot 后端，`frontend/` 是 Vue 2/Vue CLI 前端。
- `frontend/vue.config.js` 当前把构建结果写进 `src/main/resources/static`，导致生成文件与源码混杂并被提交。
- Spring 默认固定启用 `dev`，生产配置仍使用 Railway 风格变量。
- 数据库脚本位于 `src/main/resources/db/migration`，但 Maven 尚未引入 Flyway。
- Redis 同时被业务缓存以及工作区内未提交的登录令牌实现使用。
- 工作区存在用户的未提交修改。本任务只修改部署相关文件和为取消 Redis 所必需的登录令牌实现，不回退或覆盖其他改动。
- Zeabur 运行一个应用副本。进程内缓存和令牌不需要跨实例同步；应用重启后登录令牌失效是可接受的，MySQL 中的业务与用户数据不受影响。

## 方案选择

采用“单 JAR + MySQL + 全进程内缓存”方案：

1. Node 构建阶段生成独立的 `frontend/dist`。
2. Maven 构建阶段复制该目录到后端静态资源目录，再生成可执行 JAR。
3. Java 运行阶段以非 root 用户启动 JAR，通过 `PORT` 监听 HTTP。
4. Flyway 在应用启动时验证或迁移 MySQL。
5. Caffeine 同时承担约 5 分钟的业务缓存和 7 天登录令牌缓存。

不保留 Redis，因为单实例没有跨进程缓存一致性需求。也不把登录改成 JWT，因为这会改变现有令牌生命周期和注销语义。

## 容器构建

根目录新增多阶段 `Dockerfile`：

- `node:20` 阶段只先复制 `package.json` 与锁文件并执行 `npm ci`，再复制前端源码并执行 `npm run build`，保证依赖安装可复现且利用构建缓存。
- `maven:3.9-eclipse-temurin-11` 阶段先复制 `pom.xml` 获取依赖，再复制后端源码和前端 `dist`，最后执行 `mvn -B package`。Maven 的 package 生命周期会执行测试。
- `eclipse-temurin:11-jre` 阶段只复制生成的 JAR，创建固定 UID/GID 的普通用户并以该用户运行。
- 镜像声明默认端口 8080，Spring 使用 `${PORT:8080}`；Zeabur 注入其他 `PORT` 时无需重建镜像。
- 镜像不设置数据库密码，也不把 `.env`、日志、构建目录或旧静态产物复制进构建上下文。

`frontend/vue.config.js` 的输出目录改为前端自身的 `dist/`。这样本地执行 `npm run build` 不再修改后端源码；Docker 构建负责把最新产物放入 JAR。

## Spring 配置

基础 `application.yml`：

- `server.port` 为 `${PORT:8080}`。
- `spring.profiles.active` 为 `${SPRING_PROFILES_ACTIVE:dev}`，本地仍默认使用 `dev`，生产部署显式设置 `prod`。
- 只保留所有环境共有的 MyBatis 和基础日志设置。

`application-dev.yml`：

- 主机、端口、库名和用户名保留本地默认值。
- 数据库密码只读取 `DB_PASSWORD`，不在仓库中提供默认密码。
- 使用 Caffeine，不再包含 Redis 连接配置。

`application-prod.yml`：

- JDBC URL 由 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE` 组成。
- 用户名和密码分别来自 `MYSQL_USERNAME`、`MYSQL_PASSWORD`，均不提供会掩盖错误的生产默认值。
- JDBC 连接与 socket 超时有限，Hikari 最大连接数为 5、最小空闲连接数为 1、获取连接超时为 10 秒，适合小内存单实例。
- MyBatis、Spring JDBC 和 SQL 参数日志在生产保持 WARN 或更高。
- Flyway 开启迁移与校验，默认 `baseline-on-migrate=false`。

缺少 MySQL 环境变量或数据库不可达时，应用应在启动阶段失败，并由占位符解析、Hikari 或 Flyway 给出明确错误；日志不得打印密码或完整认证令牌。

## Flyway 与旧库兼容

Maven 增加与 Spring Boot 2.7 兼容的 Flyway 依赖。`V1__init.sql` 和工作区已有的 `V2__user.sql` 作为正式版本迁移保留。

新建空白 MySQL 8 数据库时，Flyway 创建 `flyway_schema_history` 并依次执行 V1、V2。迁移脚本必须通过真实 MySQL 8 容器验证，而不是只做文本检查。

旧 Railway 数据库可能已经包含表，但没有 Flyway 历史表。生产默认不自动基线，因此首次连接这种数据库会停止启动，避免静默把未知结构标记为正确。README 提供以下显式流程：

1. 从旧平台导出并校验完整备份。
2. 先恢复到临时或 Zeabur MySQL，核对 V1/V2 对应的表、列、索引和约束。
3. 若结构只相当于 V1，首次启动临时设置 `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` 和 `SPRING_FLYWAY_BASELINE_VERSION=1`，由 Flyway 继续执行 V2。
4. 若结构已完整包含 V1、V2，首次启动显式使用基线版本 2。
5. 首次成功后移除两个临时基线变量并重新部署，使后续启动恢复严格校验。

若旧库结构与迁移脚本不一致，应先编写新的、可审计的迁移脚本，不修改已经发布的迁移，也不通过默认基线跳过检查。

## 缓存和登录令牌

删除 Spring Data Redis 依赖及 Redis CacheManager，增加 Caffeine 依赖。

- 业务缓存仍沿用现有 `@Cacheable` 和 `@CacheEvict`，缓存名及写操作后的清理行为不变；CacheManager 使用约 5 分钟写后过期和有限条目数。
- 新增独立的登录令牌存储组件，以 Caffeine 保存 `token -> userId`，写后 7 天过期，并提供创建、查询和注销操作。
- `UserServiceImpl` 与 `AuthInterceptor` 只依赖该令牌存储接口，不再依赖 `StringRedisTemplate`。
- 令牌不得出现在 INFO、DEBUG 或异常日志中。

应用不提供 Redis 配置，也不要求部署 Redis 服务。单实例重启后现有登录令牌失效，用户重新登录即可。

## 日志与健康检查

`logback-spring.xml` 只配置控制台输出，移除文件 appender 和 Windows 路径。生产日志由 Zeabur 收集。

`WebLogAspect` 不再记录参数、返回对象或 Authorization 值。每次请求最多记录 HTTP 方法、路径、状态和耗时；常规请求日志使用 DEBUG，生产 INFO 不输出完整访问明细。

新增 `GET /healthz`，固定返回 HTTP 200 和最小 JSON 状态。该接口不回显环境变量、数据库地址、版本、主机名或凭据，也不执行会暴露数据库细节的诊断查询。

## 仓库清理

新增 `.dockerignore` 并完善 `.gitignore`，覆盖：

- `.git/`、`.idea/`、`.vscode/`、`.claude/`；
- `target/`、`logs/`、`frontend/node_modules/`、`frontend/dist/`；
- 后端旧静态构建目录、测试输出、Playwright 报告、调试截图和本地环境文件；
- Docker Compose 文件不参与生产镜像上下文。

从 Git 索引移除已跟踪的 `target/`、日志、旧前端静态产物和明确属于测试/调试的截图，但不重写历史。优先使用仅从索引移除的方式保留本地文件，避免误删用户产物。

## 验证策略

自动化测试先行覆盖新增的可观察行为：

- `/healthz` 返回 200、最小 JSON，并可在没有完整业务数据库上下文的 MVC 测试中执行。
- 进程内令牌存储支持保存、读取和注销；认证拦截器通过该存储保持原有授权行为。
- Caffeine 业务 CacheManager 提供现有缓存名并保持有限容量与约 5 分钟 TTL。

最终验证顺序：

1. 从清理后的前端目录执行 `npm ci`、现有前端测试和 `npm run build`。
2. 执行 `mvn test` 和 `mvn package`。
3. 执行根目录 `docker build`，检查 JAR 内含最新 `index.html`、JS 和 CSS。
4. 启动临时 MySQL 8 容器，使用空库运行应用镜像，确认 Flyway 执行 V1/V2 并创建业务表与历史表。
5. 使用非默认 `PORT` 启动应用容器，访问 `/healthz`、首页、静态 JS/CSS，并调用现有 `/api` 读写接口。
6. 验证不提供 MySQL 配置时容器快速失败且错误信息指出缺失配置；输出中不出现测试密码或令牌。
7. 检查最终镜像用户不是 root，且镜像中不存在 Node、Maven、源码、日志或密码。

临时容器、网络和卷使用明确的任务专用名称，验证结束后删除；不会操作用户现有的 MySQL、Redis 或 Docker 数据卷。

## Zeabur 与 Cloudflare 运维说明

README 将说明只需在一个 Zeabur 项目中创建：一个 GitHub 应用服务和一个 MySQL 模板服务。应用服务从仓库根目录构建，不挂载持久化卷；设置 `SPRING_PROFILES_ACTIVE=prod` 并连接 Zeabur 暴露的 `MYSQL_*` 变量。Redis 不再需要。

域名先在 Zeabur 绑定，再在 Cloudflare 建立指向 Zeabur 所给目标的代理 DNS 记录。按 Zeabur 当前公网域名文档，Cloudflare SSL/TLS 模式应使用 `Full`；Zeabur 明确提示其源站证书场景可能无法通过 `Full (strict)` 校验。只有确认源站提供与自定义域名匹配且受信任的有效证书后，才切换为 `Full (strict)`。当前业务 API 没有完整的逐用户授权隔离，即使仓库内已有基础登录代码，私人部署仍应使用 Cloudflare Access 保护整个主机名。

迁移旧平台前必须导出数据库；确认 Zeabur 新服务与数据无误后再下线旧服务。任何销毁旧平台或 MySQL 服务的操作都必须在备份下载并验证后进行。

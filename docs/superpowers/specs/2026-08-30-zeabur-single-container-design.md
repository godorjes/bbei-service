# Zeabur 单容器生产部署设计

## 目标

将现有 `frontend/` 与 `backend/` monorepo 改造成可从 GitHub 仓库根目录直接部署到 Zeabur 的单服务应用。生产镜像在构建期编译 Vue 前端并注入 Spring Boot JAR，运行期只启动一个 Java 11 进程，通过同一域名提供网页、静态资源、`/api/**` 与 `/healthz`。

## 约束

- 保持 Java 11、Spring Boot 2.7、Vue 3、Vite、MyBatis、H2 与现有 API/数据模型不变。
- 不使用 Docker Compose，不拆分前后端服务，不改成哈希路由。
- H2 仅允许一个应用副本访问，生产数据全部写入挂载于 `/data` 的持久化卷。
- 镜像不保存密码，应用以非 root 用户运行。
- 公开域名当前没有应用级鉴权，私人部署必须由 Cloudflare Access 保护整个域名。

## 构建与运行

根目录多阶段 `Dockerfile` 依次使用 Node 20、Maven 3.9 + Temurin 11、Temurin 11 JRE。前端阶段执行 `npm ci` 与 `npm run build`；后端阶段将 `frontend/dist` 复制到 `backend/src/main/resources/static` 后执行 `mvn package`；最终镜像只复制可执行 JAR。

容器默认 `PORT=8080`，通过 Spring 配置 `${PORT:8080}` 接收 Zeabur 注入值。Docker 镜像为 `BIBEI_DB_URL` 提供 `/data/bibei` 的非敏感生产默认值；本地直接运行 Spring Boot 时仍使用 `./data/bibei`。

## 数据持久化

H2 使用嵌入式文件模式，移除 `AUTO_SERVER=TRUE`，保留 MySQL 兼容模式，并启用适合单容器快速停止的立即写盘和显式生命周期参数。H2 的数据库、锁和跟踪文件共享 `/data/bibei` 前缀，因此均位于持久化卷。

`schema.sql` 继续使用 `CREATE ... IF NOT EXISTS` 与幂等迁移，首次启动自动建表，后续启动不清表。关闭 `AUTO_SERVER` 后，同一数据库文件不能被多个应用进程共享；Zeabur 必须固定为一个副本。

## HTTP 与前端路由

新增一个只处理 GET 请求的 SPA 回退过滤器：无扩展名且不属于 `/api`、`/healthz`、`/error` 的路径转发到 `/index.html`。因此 `/`、`/organize`、`/scenes/:id` 和未来同类前端路由可直接访问或刷新。

`/api` 与 `/api/**` 永不回退；包含扩展名的 JS、CSS、manifest、service worker、图标及其他静态资源由 Spring 静态资源处理器正常提供，文件不存在时保持 404。

## CORS 与健康检查

同域生产网页直接请求 `/api`，不依赖 CORS。CORS 只允许现有 localhost/127.0.0.1 开发来源、`capacitor://localhost`，以及 `BIBEI_CORS_ALLOWED_ORIGINS` 中以逗号分隔的精确可信来源；拒绝配置通配来源。

`GET /healthz` 仅返回固定的 `{"status":"UP"}`，不暴露数据库路径、环境变量、版本或内部状态。

## 验证

- 保留现有 13 个后端测试，并增加真实 HTTP 集成测试验证 `/`、`/organize`、`/scenes/1`、API 404、静态资源和 `/healthz`。
- 运行 `npm ci`、`npm test`、`npm run build`、`mvn test`、`mvn package`。
- 运行 `docker build`，检查镜像用户、非默认 `PORT`、同域 API 与 SPA 页面。
- 使用临时 Docker volume 创建物品，删除并重建容器后确认物品仍存在，随后清理临时容器和卷。

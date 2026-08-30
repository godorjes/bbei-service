# 带齐（Daiqi）

“带齐”是一款场景化标签清单应用。用户通过标签组织卡片，再将多个标签组合成出门、上班、旅行等场景，并逐项确认需要携带的物品。

## 技术栈

- 后端：Java 11、Spring Boot 2.7、MyBatis-Plus、Flyway、MySQL 8
- 缓存：单实例进程内 Caffeine，不依赖 Redis
- 前端：Vue 2.7、Vue CLI、Axios
- 容器：Node 20 + Maven 3.9/Temurin 11 多阶段构建，Temurin 11 JRE 运行

## 本地开发

需要安装 JDK 11、Maven 3.8+、Node.js 20 和 MySQL 8。先创建一个空数据库，例如 `dqi`，然后设置本地数据库密码；密码不保存在仓库中。

PowerShell 示例：

```powershell
$env:DB_PASSWORD = "你的本地 MySQL 密码"
mvn spring-boot:run
```

可选的本地变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile |
| `PORT` | `8080` | 后端 HTTP 端口 |
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `dqi` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | 无 | 必填，只从环境变量读取 |

Flyway 会在空数据库中自动执行 `V1__init.sql` 和 `V2__user.sql`。

前端开发：

```powershell
cd frontend
npm ci
npm run serve
```

开发服务器默认使用 <http://localhost:5173>，并把 `/api` 代理到 <http://localhost:8080>。生产构建输出到 `frontend/dist`，不会再修改后端源码目录：

```powershell
cd frontend
npm run test:ui-redesign
npm run build
```

## 测试与构建

```powershell
cd frontend
npm ci
npm run test:ui-redesign
npm run build

cd ..
mvn test
mvn package
docker build -t daiqi-zeabur .
```

Docker 构建会重新执行 `npm ci` 和前端生产构建，将最新 `frontend/dist` 放入 Spring Boot JAR，再执行 Maven 测试和打包。因此可以从不含 `target`、`node_modules` 和已编译前端文件的干净 Git 仓库构建。

## Zeabur 部署

只需要一个 Zeabur 项目中的两个服务：

1. 从 GitHub 仓库根目录创建一个应用服务。根目录的 `Dockerfile` 会被自动识别，不需要分别创建前端和后端服务。
2. 从 Zeabur 模板创建一个 MySQL 8 服务。

应用本身不挂载持久化卷。页面、API 和 `/healthz` 由同一个 Spring Boot 容器提供，业务数据全部保存在 MySQL 服务中。应用服务应保持一个副本；进程内缓存和登录令牌不会跨副本同步。

### 环境变量

在应用服务中确认以下变量可见：

| 变量 | 是否必需 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 是 | 固定设置为 `prod` |
| `MYSQL_HOST` | 是 | Zeabur MySQL 服务主机 |
| `MYSQL_PORT` | 是 | Zeabur MySQL 服务端口，通常为 `3306` |
| `MYSQL_DATABASE` | 是 | 数据库名 |
| `MYSQL_USERNAME` | 是 | 数据库用户名 |
| `MYSQL_PASSWORD` | 是 | 数据库密码，只通过 Zeabur 变量引用传入 |
| `PORT` | 通常自动注入 | Spring 读取该端口，未设置时为 `8080` |

Zeabur 的 MySQL 模板通常会向同项目服务注入上述 `MYSQL_*` 变量。部署前仍应在应用服务的 Variables 页面逐项确认；如需手动关联，使用 Zeabur 的服务变量引用，不要把密码写入 Dockerfile、Git 或 README。

Redis 不再需要，也不应为本应用额外部署 Redis 服务。业务缓存 TTL 约为 5 分钟；当前登录令牌保存在单实例内存中，容器重启后用户需要重新登录。

部署后可检查：

```text
GET /healthz  -> 200 {"status":"ok"}
GET /         -> Vue 页面
POST /api/... -> 现有业务 API
```

生产日志只输出到标准输出，由 Zeabur 收集。日志不会记录完整请求参数、响应对象、数据库密码或登录令牌。

### 空数据库和 Flyway

全新的空白 MySQL 8 数据库无需手工建表。应用启动时 Flyway 会：

1. 创建 `flyway_schema_history`；
2. 执行 `V1__init.sql`；
3. 执行 `V2__user.sql`；
4. 后续部署校验已执行脚本的校验和。

生产默认 `baseline-on-migrate=false`。如果数据库不是空库、又没有 `flyway_schema_history`，应用会停止启动，而不是静默假设旧结构正确。

### 从 Railway 或其他旧平台迁移

1. 在旧平台下线前，使用 `mysqldump --single-transaction --routines --triggers` 导出数据库；使用交互式 `-p` 输入密码，避免把密码写进命令历史。
2. 保留原始备份，并把它恢复到临时 MySQL 或新的 Zeabur MySQL。确认表、行数、索引和约束后再连接正式应用。
3. 检查旧库是否已有 `flyway_schema_history`。如果已有，直接让 Flyway 校验，不要重新基线。
4. 如果没有历史表但结构与 V1 完全一致，首次启动临时设置：

   ```text
   SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
   SPRING_FLYWAY_BASELINE_VERSION=1
   ```

   Flyway 会把 V1 记录为基线并继续执行 V2。
5. 如果旧库已经完整包含 V1 和 V2 的全部表、列、索引及约束，首次启动可显式使用基线版本 `2`。
6. 首次成功后立即移除 `SPRING_FLYWAY_BASELINE_ON_MIGRATE` 和 `SPRING_FLYWAY_BASELINE_VERSION`，重新部署并确认严格校验通过。
7. 如果旧库结构与 V1/V2 不一致，不要强行基线。先为差异编写新的、可审计的版本迁移并在数据库副本上验证。

只有在新域名、页面、API、数据量和 Flyway 状态全部核对无误后，才下线或销毁旧平台。销毁前必须下载并验证最后一次数据库备份；Zeabur MySQL 服务也应启用并定期下载备份。

### Cloudflare 域名

1. 先在 Zeabur 应用服务的 Domains 中添加自定义域名。
2. 按 Zeabur 显示的目标，在 Cloudflare DNS 中创建 CNAME（根域可使用 Cloudflare CNAME Flattening），然后开启代理，即橙色云朵。
3. 按 Zeabur 当前文档将 Cloudflare SSL/TLS 模式设为 `Full`。Zeabur 提醒其源站证书场景可能无法通过 `Full (strict)`；只有确认源站提供与域名匹配、未过期且受信任的证书后，才切换为 `Full (strict)`。

项目虽然包含基础登录接口，但现有业务 API 尚未实现完整的逐用户授权隔离。私人部署应在 Cloudflare Zero Trust 中创建 Self-hosted Access 应用，并保护整个主机名，而不是只保护首页路径。

## 安全提示

- 不要把 MySQL 密码或 Zeabur 变量值提交到 Git。
- 不要公开暴露数据库端口。
- `/healthz` 只返回固定状态，不提供环境、数据库或版本详情。
- 销毁 MySQL 或旧平台前始终先下载并验证备份。

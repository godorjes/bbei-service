# 带上

“带上”是一个轻量的个人出门物品清单工具。选择场景后即可直接开始勾选，平时不需要先创建一次性行程，也不需要维护标签体系。

## 产品逻辑

- **场景**：例如周末旅行、商务出差，用来决定本次清单包含哪些分区。
- **分区**：例如证件、数码、洗漱，可独立创建，并可绑定多个场景。
- **物品**：例如身份证、充电器，可独立创建，并可绑定多个分区。
- 同一物品即使通过多个分区进入同一场景，也只会在该场景清单里出现一次。
- 场景清单的勾选状态会自动保存；需要从头准备时，可使用“重新准备”。

首页只保留两个主要入口：

- **清单**：选择场景并直接开始准备。
- **整理**：按需维护分区、物品及它们的绑定关系。

## 技术栈

- 后端：Java 11、Spring Boot 2.7、MyBatis、H2
- 前端：Vue 3、Vue Router、Vite、JavaScript
- App：Capacitor，与移动网页共用前端源码

## 项目结构

```text
bibei/
├─ backend/   Spring Boot API 与本地持久化
├─ frontend/  Vue 3 移动端界面
└─ docs/      产品设计与实施记录
```

## 本地运行

先启动后端：

```powershell
cd D:\code\bibei\backend
mvn spring-boot:run
```

再启动前端：

```powershell
cd D:\code\bibei\frontend
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。数据持久化在 `backend/data`，不依赖外部数据库。

## 测试与构建

```powershell
cd D:\code\bibei\backend
mvn test

cd D:\code\bibei\frontend
npm test
npm run build
```

## Zeabur 部署

项目已支持从 GitHub 仓库根目录直接部署为一个完整网站。Zeabur 会自动识别根目录的 `Dockerfile`，依次构建 Vue 前端和 Spring Boot 后端，并最终只运行一个 Java 11 容器；不需要为 `frontend` 和 `backend` 分别创建服务，也不需要 Docker Compose。

### 1. 创建服务

1. 在 Zeabur 项目中选择从 GitHub 创建服务，并选择本仓库。
2. 构建根目录保持仓库根目录，不要设置为 `frontend` 或 `backend`。
3. 确认 Zeabur 使用根目录 `Dockerfile` 构建。
4. 服务副本数固定为 **1**。H2 是单进程文件数据库，不能由多个实例同时访问同一个数据库文件。
5. 如果服务设置中提供 HTTP 健康检查，将路径设为 `/healthz`。

Zeabur 的根目录 Dockerfile 部署说明见[官方文档](https://zeabur.com/docs/en-US/deploy/methods/dockerfile)。

### 2. 挂载持久化卷

必须在该服务的 Volumes 页面创建并挂载持久化卷：

```text
挂载点：/data
```

数据库主体文件、锁文件和可能产生的跟踪文件都会使用 `/data/bibei` 前缀。未挂载 `/data` 时，重新部署或重建容器会丢失数据库文件。Zeabur 持久卷的挂载方式见[官方文档](https://zeabur.com/docs/en-US/data-management/volumes)。

挂载卷后，正常重新部署不会清空已有数据。销毁服务器、删除服务或删除持久卷之前，必须先下载并备份整个 `/data` 目录；持久卷删除后数据无法恢复。

### 3. 环境变量

推荐配置如下：

| 变量 | 推荐值 | 说明 |
| --- | --- | --- |
| `BIBEI_DB_URL` | `jdbc:h2:file:/data/bibei;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0` | 生产 H2 文件必须位于持久卷；镜像已提供此默认值，建议在 Zeabur 中显式设置便于核对 |
| `PORT` | 使用 Zeabur 自动注入值 | 应用会读取该值；未设置时默认 `8080` |
| `BIBEI_CORS_ALLOWED_ORIGINS` | 留空 | 同域网页不需要 CORS；只有其他可信前端需要跨域时，才填写以逗号分隔的完整来源，例如 `https://app.example.com`，禁止使用 `*` |
| `BIBEI_DB_USERNAME` | `sa` | 可选，H2 用户名默认值 |
| `BIBEI_DB_PASSWORD` | 通过 Zeabur Secret 设置 | 可选；不要写进 Dockerfile 或提交到 Git。已有数据库一旦设置密码，不要随意变更 |

本地直接运行后端时，若未设置 `BIBEI_DB_URL`，数据仍写入 `backend/data/bibei`。生产容器的默认地址则是 `/data/bibei`。

### 4. 域名与 Cloudflare

1. 先在 Zeabur 服务的 Domains 页面添加自定义域名，按页面给出的 DNS 目标配置 Cloudflare 记录。
2. Cloudflare DNS 记录开启代理（橙色云），SSL/TLS 模式使用 **Full (strict)**；Zeabur 会为绑定域名提供 HTTPS 证书。
3. 不要缓存 `/api/*`、`/healthz` 和 `index.html`，避免接口响应或旧入口页面被边缘缓存。
4. 当前项目没有用户登录或应用级鉴权，知道域名的访问者可能修改全部清单数据。私人使用时，必须在 Cloudflare Zero Trust 中创建 Self-hosted 应用，用 Access 策略保护整个主机名，而不是只保护某个页面路径。Cloudflare 的[自托管应用](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/)和[整站路径保护](https://developers.cloudflare.com/cloudflare-one/access-controls/policies/app-paths/)文档包含当前配置入口。

Cloudflare Access 只能拦截经过 Cloudflare 代理的流量。如果 Zeabur 分配的公开域名仍可直接访问，还应避免公开该地址，并根据实际部署条件限制绕过 Cloudflare 的访问路径。

### 5. 本地构建生产镜像

在仓库根目录执行：

```bash
docker build -t bibei:local .
docker run --rm -p 8080:8080 -v bibei-data:/data bibei:local
```

访问 `http://127.0.0.1:8080/healthz` 应返回 `{"status":"UP"}`。生产网页、静态资源和 `/api` 均由同一个 Spring Boot 进程与域名提供。

## App 打包准备

前端已包含 Capacitor 配置。首次创建 Android 工程时运行：

```powershell
cd D:\code\bibei\frontend
npm run build
npx cap add android
npm run cap:sync
```

iOS 工程需要在 macOS 环境中创建和构建。

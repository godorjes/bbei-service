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

## App 打包准备

前端已包含 Capacitor 配置。首次创建 Android 工程时运行：

```powershell
cd D:\code\bibei\frontend
npm run build
npx cap add android
npm run cap:sync
```

iOS 工程需要在 macOS 环境中创建和构建。

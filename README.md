## OJPT / OJPS - 在线刷题与训练平台

OJPT（Online Judge Platform for Training）是面向算法训练和竞赛准备的在线刷题与训练平台，支持多角色协同（学员、教师、校方、管理员），覆盖日常练习、训练计划、竞赛与数据分析等典型场景。  
本仓库是 OJPT 的**总仓库**，聚合了后端服务、前端应用以及部署配置三大部分。

- **后端**：`OJPT-backend` - 基于 Java 17 + Spring Boot 4 的单体应用，提供用户与组织管理、RBAC 权限、基础统计等核心 API。
- **前端**：`OJPT-frontend` - 基于 Vue 3 + Vite 的单页应用（SPA），为不同角色提供完整的 Web 界面。
- **部署配置**：`OJPT-deploy` - Nginx 等部署相关配置，负责前后端统一入口和静态资源服务。

---

## 项目整体结构

仓库顶层结构（节选）：

```text
OJPT/
├── OJPT-backend/      # 后端 Spring Boot 服务
├── OJPT-frontend/     # 前端 Vue 3 + Vite SPA
├── OJPT-deploy/       # 部署与 Nginx 配置
└── .gitignore         # Git 忽略规则等
```

各子模块的详细文档请查看对应目录下的 README：

- 后端：`[OJPT-backend](OJPT-backend/README.md)`
- 前端：`[OJPT-frontend](OJPT-frontend/README.md)`
- 部署：`[OJPT-deploy](OJPT-deploy/README.md)`

---

## 技术栈概览

### 后端（OJPT-backend）

- **语言与框架**
  - Java 17
  - Spring Boot 4.0.x（基于 Spring Framework 6）
- **核心组件**
  - Spring Web：RESTful API
  - Spring Security 6：JWT 认证与方法级权限控制
  - MyBatis-Plus：ORM 与数据访问
  - Flyway：数据库版本管理
  - Spring Data Redis：缓存与 Token 管理
  - SpringDoc OpenAPI：接口文档（Swagger UI）
  - Spring Boot Actuator：健康检查与基础运维端点
- **基础设施**
  - MySQL 8：主业务数据库
  - Redis：缓存、排行榜、JWT、会话相关数据

当前后端代码重点实现了：

- 多角色用户中心（学员、教师、校方、管理员）
- 学校 / 院系 / 班级等组织管理
- 角色与权限（RBAC）模型
- 部分统计与后台看板基础数据

题库、评测、训练与竞赛等 OJ 核心能力将按里程碑逐步落地。

### 前端（OJPT-frontend）

- **框架与构建**
  - Vue 3（组合式 API）
  - Vite（开发服务器与构建工具）
  - TypeScript
- **路由与状态管理**
  - `vue-router`：按角色与业务域划分的路由与路由守卫
  - `pinia`：全局状态管理（认证信息、管理端数据等）
- **UI 与交互**
  - `element-plus` + `@element-plus/icons-vue`：主要 UI 组件库
  - 自定义布局与通用组件（导航栏、布局容器、表格、对话框、状态标签等）
- **表单与校验**
  - `vee-validate` + `yup`：登录、注册、信息编辑等表单校验
- **网络与安全**
  - `axios`：统一的请求封装（`src/api/request.ts`），处理 baseURL、错误提示、Token 刷新、ID 精度等
  - JWT 存储与续期逻辑：`src/stores/auth.ts`、`src/hooks/useAuth.ts`、`src/utils/jwt-utils.ts` 等
  - 路由守卫：基于路由 `meta` 中的 `requiresAuth`、`requiredRole` 等字段控制访问

### 部署与运维（OJPT-deploy）

- **核心职责**
  - 管理 Nginx 配置（前后端统一入口）
  - 规划 Docker、部署脚本与文档（预留目录）
- **当前内容**
  - `nginx/nginx.conf`：Nginx 主配置文件，负责：
    - 监听 `80` 端口，提供前端静态资源服务（`/`）
    - 将 `/api/` 代理到后端 Spring Boot 服务（默认 `http://localhost:8080`）
    - 将 `/uploads/` 映射到本地上传目录（与后端头像等上传路径对齐）
    - 配置 CORS 跨域策略（联调与生产环境）

更多部署细节请参考 `[OJPT-deploy/README.md](OJPT-deploy/README.md)`。

---

## 快速开始（本地开发）

### 1. 前置依赖

- **JDK**：17+
- **Node.js**：建议按前端 `package.json` 中 `engines` 要求（≥ 20.19.0 或 ≥ 22.12.0）
- **数据库与缓存**
  - MySQL 8
  - Redis
- **Web 服务器（可选但推荐）**
  - Nginx（本地或服务器上，用于前端静态资源与反向代理）

> 提示：后端会通过 `application.properties` 连接 MySQL / Redis，并配置 JWT、上传路径等，请在首次启动前根据本地环境调整相关配置。

### 2. 克隆仓库

```bash
git clone <your-repo-url> OJPT
cd OJPT
```

> 若后端与前端子仓库托管在 GitHub（参考部署 README 中的链接），也可以分别克隆子仓，但建议在总仓内统一管理。

### 3. 启动后端（OJPT-backend）

进入后端目录：

```bash
cd OJPT-backend
```

#### 3.1 配置应用参数

打开 `src/main/resources/application.properties`，根据本地环境检查并调整（示例）：

- 数据库连接：
  - `spring.datasource.url=jdbc:mysql://localhost:3306/ojpt?...`
  - `spring.datasource.username=...`
  - `spring.datasource.password=...`
- Redis：
  - `spring.data.redis.host=localhost`
  - `spring.data.redis.port=6379`
- JWT：
  - `ojpt.jwt.secret=...`（请使用安全的随机密钥）
  - `ojpt.jwt.access-exp-seconds`、`ojpt.jwt.refresh-exp-seconds`
- 上传路径（与 Nginx `/uploads` 对应）：
  - 例如：`ojpt.upload.base-path=C:/Program Files/nginx-1.28.0/uploads`

#### 3.2 启动服务

确保 MySQL 与 Redis 已启动后，在 `OJPT-backend` 目录执行：

```bash
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn clean package
java -jar target/*.jar
```

服务默认监听在 `http://localhost:8080`，并自动执行 Flyway 数据库迁移。

### 4. 启动前端（OJPT-frontend）

打开新的终端，进入前端目录：

```bash
cd OJPT-frontend
npm install
npm run dev
```

- 默认会启动 Vite 开发服务器（通常为 `http://localhost:5173`）。
- 前端会根据 `src/api/request.ts` 与 Vite 配置请求后端服务，可使用：
  - 直接请求 Nginx 统一入口（如 `http://localhost` 下的 `/api` 路径），或
  - 在 Vite `server.proxy` 中配置 `/api` → `http://localhost:8080`。

更多开发脚本（测试、类型检查、构建等）请参考 `[OJPT-frontend/README.md](OJPT-frontend/README.md)`。

### 5. 通过 Nginx 联调（可选但推荐）

要模拟生产环境的一致入口，可以使用 `OJPT-deploy` 中的 Nginx 配置：

1. 将 `OJPT-deploy/nginx/nginx.conf` 复制到本机 Nginx 配置路径（示例）：
   - Windows：`C:\Program Files\nginx-1.28.0\conf\nginx.conf`
   - Linux：`/etc/nginx/nginx.conf`
2. 验证与重载配置：

```bash
nginx -t
nginx -s reload
```

3. 前端生产构建（可选）：

   在 `OJPT-frontend` 目录执行：

   ```bash
   npm run build
   ```

   - 根据 `vite.config.ts` 配置，构建产物会输出到 `C:\Program Files\nginx-1.28.0\html`。
   - 注意：`build.emptyOutDir = true` 会清空该目录原有内容，请确认专用于本项目。

完成后，可通过浏览器访问 `http://localhost`，由 Nginx 统一转发前端与后端请求。

---

## 功能规划与当前进度（概要）

根据整体规划，OJPT 目标能力包括但不限于：

- **题库管理**：题目 CRUD、标签、难度、导入导出、题解与讨论区
- **评测系统**：多语言提交、沙箱判题、结果判定与实时反馈
- **训练与竞赛**：训练计划、作业、限时竞赛、榜单与封榜策略
- **多角色用户中心**：学员 / 教师 / 校方 / 管理员的权限隔离与协同
- **学习分析**：个人与班级/学校的能力画像、报表与看板
- **运维支撑**：配置管理、日志监控、告警与灰度发布等

> 当前代码阶段主要聚焦在「多角色用户中心 + 组织管理 + RBAC 权限 + 基础统计」，题库与评测等模块将按规划逐步落地，详细说明可参考各子模块的 README。

---

## 贡献与开发建议

- **建议阅读顺序**
  1. 本根仓库 `README.md`（了解整体结构与职责分工）
  2. `[OJPT-backend/README.md](OJPT-backend/README.md)`（后端领域模型与接口）
  3. `[OJPT-frontend/README.md](OJPT-frontend/README.md)`（前端模块与开发脚本）
  4. `[OJPT-deploy/README.md](OJPT-deploy/README.md)`（Nginx 配置与部署说明）
- **统一工具**
  - 后端：使用 Maven 进行构建与依赖管理
  - 前端：使用 npm（或兼容的包管理器）+ ESLint + Vitest + Playwright
- **提交与分支规范**
  - 可在后续补充具体的分支策略与 Commit Message 规范（例如基于 Conventional Commits），当前阶段先以模块内 README 为准。

---

## 许可证与说明

当前项目的开源许可证尚未明确确定，如需在生产环境使用或二次开发，请先与项目维护者确认许可方式。  
后续一旦确定具体 License，将在此处与各子模块 README 中同步更新。


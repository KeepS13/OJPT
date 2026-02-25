# OJPT_frontend - 在线刷题平台前端 (Vue 3 + Vite)

OJPT / OJPS（Online Judge Platform for Training）是面向算法训练的在线刷题平台，本仓库为其 **Web 前端单页应用（SPA）** 实现，主要为学员、教师、校方与管理员等角色提供交互界面：

- **学员**：题库浏览、在线做题、提交记录、个人中心、安全设置等
- **教师 / 校方**：班级/学校维度的数据查看与管理（预留扩展）
- **管理员**：用户与权限管理、学校管理、系统看板等运营向界面

后端实现与整体规划参考后端仓库的 `README.md`（Spring Boot 项目），本前端项目与之配合完成整个 OJ 训练平台。

---

## 一、技术栈与架构概览

- **框架与构建**
  - Vue 3（组合式 API）
  - Vite（开发服务器与构建工具）
  - TypeScript
- **路由与状态管理**
  - `vue-router`：多角色、多模块路由配置（学员、教师、校方、管理员等）
  - `pinia`：全局状态管理（认证信息、加载状态、管理端数据等）
- **UI 与交互**
  - `element-plus` + `@element-plus/icons-vue`：管理端与用户中心的主要 UI 组件库
  - 自定义 `layout` 与 `common` 组件：如顶部导航、用户中心布局、通用表格、对话框、状态标签等
- **表单与校验**
  - `vee-validate` + `yup`：登录、注册、信息编辑等表单的校验与错误提示
- **网络与接口封装**
  - `axios`：通过 `src/api/request.ts` 与 `src/api/base.ts` 进行统一封装
  - 统一响应格式、错误提示、Token 刷新与大数 ID 精度保护
- **安全与认证**
  - JWT 令牌存储与续期逻辑：`src/stores/auth.ts`、`src/hooks/useAuth.ts`、`src/utils/jwt-utils.ts`、`src/utils/storage.ts`
  - 路由守卫：基于 `meta.requiresAuth` 与 `meta.requiredRole` 的角色访问控制（见 `src/router/index.ts`）

### 目录结构（节选）

> 以下仅列出与前端职责紧密相关的核心目录，具体实现可参考源码。

- `src/main.ts`：应用入口，注册路由、Pinia、Element Plus 等
- `src/router/`：前端路由配置（角色与模块路由守卫）
- `src/views/`：页面级视图，按业务域拆分，例如：
  - `HomeView.vue`：首页与入口聚合
  - `ProblemSetView.vue`：题库列表
  - `ProblemSolveView.vue`：做题页面（代码编辑与提交）
  - `ProfileView.vue` / `SecurityView.vue`：个人资料与安全设置
  - `StudentView.vue` / `TeacherView.vue` / `SchoolView.vue`：不同角色的工作台
  - `views/admin/*`：管理员后台（用户、角色、权限、学校等管理页面与布局）
- `src/components/layout/`：全局布局组件（如顶部导航、用户中心布局等）
- `src/components/common/`：通用 UI 组件（如 `DataTable`、`FormDialog`、`PageCard`、`StatusTag`、`SearchBar` 等）
- `src/components/auth/`：认证相关组件（如 `LoginDialog`）
- `src/stores/`：Pinia 状态（认证、加载态、管理端数据等）
- `src/api/`：按领域划分的接口封装（`auth.ts`、`user.ts`、`admin.ts`、`teacher.ts`、`student.ts`、`school.ts` 等）
- `src/utils/`：工具方法（JWT 解析、存储封装等）
- `src/validation/`：表单校验规则与 Schema
- `src/assets/`：样式与静态资源（SCSS 变量、全局样式、Logo 等）

---

## 二、运行环境要求

- **Node.js 版本**
  - 根据 `package.json` 中的 `engines` 声明：**Node.js ≥ 20.19.0**（或 ≥ 22.12.0）
- **包管理器**
  - 示例命令使用 `npm`，如使用 `pnpm` / `yarn`，可自行替换为对应命令。
- **后端依赖**
  - 本前端默认通过 HTTP（如 `http://localhost/api`）访问后端 Spring Boot 服务，请确保后端服务已可用，或在开发阶段配置 Vite 代理 / Nginx 反向代理。

---

## 三、本地开发与常用脚本

在项目根目录执行以下命令。

### 1. 安装依赖

```bash
npm install
```

### 2. 启动本地开发服务器（含热更新）

```bash
npm run dev
```

- 默认会启动 Vite 开发服务器（通常为 `http://localhost:5173`，具体以终端输出为准）。
- 在开发环境中，前端会直接调用配置在 `src/api/request.ts` 中的后端地址（如 `http://localhost/api`），或通过 Vite 代理转发到后端。

### 3. 生产构建（打包静态资源）

```bash
npm run build
```

- 内部脚本为：`npm-run-all2 type-check "build-only {@}" --`
  - `type-check`：使用 `vue-tsc` 进行 TypeScript 类型检查。
  - `build-only`：调用 `vite build`，输出构建产物。
- 构建输出目录见下方「部署与 Nginx 集成」章节。

### 4. 本地预览生产构建

```bash
npm run preview
```

- 使用 Vite 内置静态服务器预览打包结果，用于本地验证生产构建是否正常。

### 5. 运行单元测试

```bash
npm run test:unit
```

- 使用 [Vitest](https://vitest.dev/) + `@vue/test-utils` 进行组件与业务逻辑单元测试。

### 6. 类型检查

```bash
npm run type-check
```

- 使用 `vue-tsc --build` 对整个项目进行类型检查，确保 TypeScript 与 `.vue` 组件类型安全。

### 7. 代码规范检查（ESLint）

```bash
npm run lint
```

- 使用 ESLint + `eslint-plugin-vue` 对代码进行规范与潜在问题检查。
- 默认启用 `--fix --cache`，会对部分问题进行自动修复，并缓存结果提升后续检测速度。

### 8. 端到端测试（E2E - Playwright）

- 本项目前端内集成了 Playwright 端到端测试，入口脚本：

```bash
npm run test:e2e
```

- 默认使用根目录下的 `playwright.config.ts`，测试目录为 `tests/e2e/`。
- 当前已内置一个**登录流程基础用例**：`tests/e2e/login.spec.ts`，通过顶部导航的“登录”按钮打开 `LoginDialog` 并完成登录。
- 为了让该用例真正执行（而不是被跳过），需要在运行测试前配置测试账号环境变量：

```bash
set OJPT_E2E_USERNAME=你的测试账号
set OJPT_E2E_PASSWORD=对应密码
npm run test:e2e -- tests/e2e/login.spec.ts
```

> 提示：如在 PowerShell / Linux / macOS 终端，请使用对应的环境变量设置语法。

---

## 四、前后端关系与职责分工

从整体视角看，OJPT 的交互路径可以简化为：

```mermaid
flowchart LR
  user[UserBrowser] --> frontend["OJPT Frontend (Vue3 + Vite)"]
  frontend --> backend["OJPT Backend (Spring Boot)"]
  backend --> judge["Judge Cluster / Sandbox"]
```

- **前端（本仓库）**
  - 提供面向不同角色的 Web 界面：题库、做题页、管理后台、个人中心等。
  - 负责表单校验、交互体验、状态管理、路由守卫等前端逻辑。
- **后端（Spring Boot 项目）**
  - 提供题库管理、评测任务、训练/竞赛、用户与权限、数据分析等 RESTful API。
  - 对接判题集群 / 沙箱环境，承担评测安全与性能等核心能力。
- **判题集群 / 沙箱**
  - 按后端调度运行用户提交代码，返回评测结果。

前端通过 `src/api` 中按领域划分的接口进行后端访问，并统一通过 `request.ts` 中创建的 `axios` 实例处理 Token、错误提示、重试与数据解析。

---

## 五、构建与部署（Nginx 集成）

### 1. 构建输出目录

在 `vite.config.ts` 中配置了构建输出路径：

- `build.outDir: "C:\\Program Files\\nginx-1.28.0\\html"`
- `build.emptyOutDir: true`：构建前会**清空整个输出目录**

这意味着：

- 每次执行 `npm run build` 时，会将旧有的静态文件全部清空并替换为本次构建产物。
- **务必确认该目录确实是用于部署本应用的 Nginx `html` 目录，避免误删其他站点文件。**

### 2. 典型部署流程（示例）

1. **部署并启动后端服务**
   - 按后端 Spring Boot 项目的 `README.md` 完成数据库准备、配置文件编写与服务启动。
2. **在前端项目中执行生产构建**
   - 在 `OJPT_frontend` 目录下执行：
     ```bash
     npm install
     npm run build
     ```
   - 构建完成后，静态资源会输出到 `C:\Program Files\nginx-1.28.0\html`。
3. **配置 Nginx**
   - 将 Nginx 根目录指向上述 `html` 路径，处理前端路由（SPA 场景通常需要对未知路径回退到 `index.html`）。
   - 针对接口前缀（如 `/api`）配置反向代理到后端 Spring Boot 服务地址（例如 `http://localhost:8080`）。
4. **重启 / 重新加载 Nginx 配置**
   - 通过 `nginx -s reload` 或服务管理方式重载配置，使前端新版本立即生效。

> 提示：不同环境（测试 / 预发布 / 生产）可通过调整 Nginx 配置与构建目标路径来实现多环境部署。

---

## 六、前后端联调与环境配置

当前项目中，`src/api/request.ts` 使用 `axios.create` 初始化请求实例，并设置默认 `baseURL`（例如 `http://localhost/api`）。在实际部署或多环境场景中，推荐：

- 通过 Vite 环境变量控制 API 基础地址，例如：
  - 在 `.env.development` / `.env.production` 中定义 `VITE_API_BASE_URL`。
  - 在 `request.ts` 中读取 `import.meta.env.VITE_API_BASE_URL` 作为 `baseURL`。
- 本地开发时：
  - 可通过 Vite `server.proxy` （在 `vite.config.ts` 中配置）将 `/api` 代理到后端服务，避免跨域问题。
- 生产环境时：
  - 建议由 Nginx 统一做反向代理与路径管理，前端仍以相对路径 `/api` 调用。

后续可根据实际 `.env.*` 配置与部署体系，对本节进行更细化的说明。

---

## 七、开发工具与推荐配置

### 1. IDE 推荐

- **VS Code**
  - 插件建议：
    - [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
    - TypeScript 相关支持（VS Code 内置）
  - 建议关闭旧版 Vetur 插件，避免与 Volar 冲突。

### 2. 浏览器开发工具

- **Chromium 内核浏览器（Chrome / Edge / Brave 等）**
  - 安装 [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - 在 DevTools 设置中开启 Custom Object Formatter（便于调试 Vue 响应式对象）
- **Firefox**
  - 安装 [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - 参考官方文档开启 Custom Object Formatter 支持

---

## 八、后续规划（前端方向 TODO）

结合后端整体规划，前端侧可以逐步完善以下能力：

- **认证与权限体验优化**
  - 登录态过期提醒、重新登录引导
  - 更丰富的权限控制提示与跳转策略
- **评测与训练体验**
  - 提交过程与评测状态的可视化（队列、执行状态、重判等）
  - 训练计划、竞赛榜单等页面的交互优化
- **数据可视化与分析**
  - 图表化展示个人/班级/学校维度的做题数据
  - 与后端分析服务进行联调，对齐指标口径
- **UI / UX 统一与无障碍支持**
  - 统一表单、按钮、颜色与间距等设计规范
  - 对键盘操作与屏幕阅读器支持做适配（可按需引入）

更多里程碑与总体愿景请参考后端仓库 `README.md` 中的「项目规划」「里程碑」等章节。


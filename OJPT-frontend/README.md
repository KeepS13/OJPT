# OJPT Frontend

前端围绕“个人算法训练”体验组织，重点是题库、做题页和最小后台维护，而不是复杂的平台门户。

## 当前页面

### 公共页面

- `/`
- `/problemset`
- `/problems/:problemNo`

### 普通用户

- `/profile`
- `/profile/security`

### 管理员

- `/admin`
- `/admin/users`
- `/admin/problems`
- `/admin/problems/:problemId`

## 当前特点

- 首页作为个人训练入口，不堆叠复杂运营信息
- 做题页采用标准输入输出风格模板
- 管理端只保留题目和标签维护所需能力

## 技术栈

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Element Plus
- Axios
- Vitest
- Playwright

## 推荐启动方式

默认通过根目录 `docker-compose.yml` 使用 Nginx 承载前端构建产物：

```bash
cp .env.example .env
docker compose up --build -d frontend
```

默认入口：`http://localhost`

说明：

- 当前前端头像组件会把相对头像地址拼成 `http://localhost/uploads/...`
- 因此 compose 默认把 Nginx 暴露在 `80` 端口；如果修改 `FRONTEND_PORT`，头像资源地址也需要同步处理

## 本地开发

```bash
cd OJPT-frontend
npm install
npm run dev
```

默认地址：`http://localhost:8110`

Vite dev server 当前会：

- 监听 `127.0.0.1:8110`
- 将 `/api` 代理到 `http://127.0.0.1:8111`

## 构建说明

仓库内 `vite.config.ts` 的默认 `build.outDir` 面向某个本机 Nginx 目录，不适合作为通用文档步骤。可复现构建请使用以下两种方式之一：

- 直接使用根目录 `docker compose`，由 `OJPT-deploy/docker/frontend.Dockerfile` 在容器内执行构建
- 本地手动构建时显式覆盖输出目录：`npm run build -- --outDir ./dist`

## 常用命令

```bash
npm run dev
npm run build -- --outDir ./dist
npm run preview
npm run type-check
npm run lint
npm run test:unit
npm run test:e2e
```

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

## 本地开发

```bash
cd OJPT-frontend
npm install
npm run dev
```

默认地址：`http://localhost:8110`

## 常用命令

```bash
npm run dev
npm run build
npm run preview
npm run type-check
npm run lint
npm run test:unit
npm run test:e2e
```

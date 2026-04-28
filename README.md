# OJPT

OJPT 是一个偏个人算法训练场景的在线刷题项目。当前仓库只保留和“个人做题训练 + 最小后台维护”直接相关的能力，不再展开历史上的完整平台化规划。

## 当前定位

- 面向个人训练，而不是复杂教学平台
- 核心体验是题库浏览、按题号进入、标准输入输出方式练习
- 管理端只保留维持题库内容所需的最小闭环

## 当前角色

- `USER`：普通训练用户
- `ADMIN`：管理员

## 当前能力

- 登录、登出、刷新 token
- 个人中心、账号安全、头像上传
- 题库列表、题目详情、做题页
- 管理员用户管理、题目管理、标签管理、基础统计

## 开发入口

### 后端

```bash
cd OJPT-backend
mvn spring-boot:run
```

默认地址：`http://localhost:8111`

### 前端

```bash
cd OJPT-frontend
npm install
npm run dev
```

默认地址：`http://localhost:8110`

## 主要文档

- [OJPT-backend/README.md](OJPT-backend/README.md)
- [OJPT-backend/API.md](OJPT-backend/API.md)
- [OJPT-frontend/README.md](OJPT-frontend/README.md)
- [OJPT-frontend/API.md](OJPT-frontend/API.md)
- [OJPT-deploy/README.md](OJPT-deploy/README.md)

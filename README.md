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

## 推荐启动方式

仓库根目录已经提供 `docker-compose.yml`，可直接启动 `mysql`、`redis`、`backend`、`frontend`（由 Nginx 承载前端构建产物）。

```bash
cp .env.example .env
docker compose up --build -d
```

默认入口：

- 前端：`http://localhost`
- 后端 API：`http://localhost:8111`
- Swagger UI：`http://localhost:8111/swagger-ui/index.html`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

说明：

- `.env.example` 只给出示例键和值格式，实际密码、JWT secret 请写入本地 `.env`
- 当前前端头像资源 URL 固定为 `http://localhost/uploads/...`，因此 `FRONTEND_PORT` 默认应保持 `80`

## 初始化与默认账号

首次启动空库时，后端会自动执行 Flyway 迁移：

- `V1_0__baseline_schema_and_seed.sql`：初始化表结构、角色、默认账号、题库与测试用例
- `V1_1__password_reset_requests.sql`：补充密码重置申请表

默认账号全部来自 `V1_0__baseline_schema_and_seed.sql`，初始密码均为 `123456`：

- `admin`
- `admin1`
- `user`
- `user1`

重置整套数据并重新导入题库/seed：

```bash
docker compose down -v
docker compose up --build -d
```

如果数据库卷未清空，Flyway 不会重复执行已成功的旧迁移；需要变更种子数据时，应新增迁移，或在本地删除卷后重建。

## 本地开发

### 后端

```bash
cd OJPT-backend
mvn spring-boot:run
```

- 默认监听：`http://localhost:8111`
- 本地开发默认读取 `OJPT-backend/src/main/resources/application.properties`
- 如果不想使用其中的本机默认值，请通过环境变量或 `--spring.*` / `--ojpt.*` 参数覆盖

### 前端

```bash
cd OJPT-frontend
npm install
npm run dev
```

- 默认监听：`http://localhost:8110`
- Vite dev server 会将 `/api` 代理到 `http://127.0.0.1:8111`

## 主要文档

- [OJPT-backend/README.md](OJPT-backend/README.md)
- [OJPT-backend/API.md](OJPT-backend/API.md)
- [OJPT-frontend/README.md](OJPT-frontend/README.md)
- [OJPT-frontend/API.md](OJPT-frontend/API.md)
- [OJPT-deploy/README.md](OJPT-deploy/README.md)

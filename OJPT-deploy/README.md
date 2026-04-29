# OJPT Deploy

该目录存放 OJPT 的部署相关文件。当前仓库已经补齐可复现的 `docker-compose` 方案，并保留一份独立主机 Nginx 模板。

## 文件说明

- `../docker-compose.yml`：根目录一键启动入口
- `../.env.example`：环境变量示例
- `docker/backend.Dockerfile`：后端构建与运行镜像
- `docker/frontend.Dockerfile`：前端构建并交给 Nginx 承载
- `nginx/default.conf`：compose 中 frontend 容器使用的 Nginx 配置
- `nginx/nginx.conf`：不使用 compose 时，可部署到独立主机 Nginx 的模板

## Docker Compose 启动

1. 复制环境变量示例并按需修改：

```bash
cp .env.example .env
```

2. 启动所有服务：

```bash
docker compose up --build -d
```

3. 查看后端启动日志，等待 Flyway 和 Spring Boot 完成初始化：

```bash
docker compose logs -f backend
```

默认入口：

- 前端：`http://localhost`
- 后端：`http://localhost:8111`
- Swagger UI：`http://localhost:8111/swagger-ui/index.html`

## 初始化与默认数据

首次对空 MySQL 数据卷启动时，Flyway 会自动执行：

- `V1_0__baseline_schema_and_seed.sql`
- `V1_1__password_reset_requests.sql`

其中 `V1_0` 已包含：

- 表结构初始化
- 角色、权限、默认账号
- 首批题库、测试用例、标签和部分样例数据

默认账号如下，初始密码均为 `123456`：

- `admin`
- `admin1`
- `user`
- `user1`

## 题库迁移 / seed 说明

- 题库和测试用例不是通过额外脚本导入，而是直接由 Flyway baseline migration 写入
- 已成功执行过的迁移会记录在 `flyway_schema_history`，后续 `docker compose up` 不会重复导入旧 seed
- 如果要调整首批题库并重新导入，本地开发环境通常有两种做法：
  - 新增一条 Flyway migration，增量修改题库
  - 删除数据卷后重建，让基线 seed 从头执行

重置整套数据：

```bash
docker compose down -v
docker compose up --build -d
```

## 判题相关说明

compose 方案默认会把 `/var/run/docker.sock` 挂载给后端容器，后端容器内再通过 `docker` CLI 调用判题镜像。这样可以直接复用宿主机当前的 Docker Engine 完成代码编译与运行。

如果宿主机 Docker 环境不可用：

- 页面、登录、题库浏览、管理端基础功能仍可启动
- 提交代码、判题和判题环境健康检查会受影响

## 独立主机 Nginx 模板

`nginx/nginx.conf` 适用于“不跑 compose、自己准备后端和静态文件目录”的场景。它已经去掉了本机 `Program Files` 路径，但仍需要你根据自己的部署目录修改：

- `root`
- `/uploads/` 的 `alias`
- `/api/` 的 `proxy_pass`

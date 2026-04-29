# OJPT Backend

后端围绕“个人算法训练”这个主线组织，只保留登录鉴权、题库读取、做题页所需数据，以及管理员维护题库的最小后台能力。

## 当前角色模型

- `USER`
- `ADMIN`

系统返回中的 `roleType` 和 `roles` 只会出现这两种角色。

## 当前接口范围

### 认证

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`
- `POST /api/auth/password-reset-requests`

### 普通用户

- `POST /api/users/me/avatar`
- `GET /api/users/me`
- `GET /api/users/me/detail`
- `PUT /api/users/me`
- `PUT /api/users/me/username`
- `PUT /api/users/me/email`
- `PUT /api/users/me/phone`
- `PUT /api/users/me/password`
- `DELETE /api/users/me`

### 题库

- `GET /api/problems`
- `GET /api/problems/{id}`
- `GET /api/problems/no/{problemNo}`

### 管理员

- `/api/admin/users/**`
- `/api/admin/problems/**`
- `/api/admin/tags/**`
- `/api/admin/statistics/**`
- `/api/admin/judge-environment`

## 技术栈

- Java 17
- Spring Boot
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- Flyway

## 推荐启动方式

优先使用仓库根目录的 `docker-compose.yml`：

```bash
cp .env.example .env
docker compose up --build -d backend
```

默认后端入口：

- API：`http://localhost:8111`
- Swagger UI：`http://localhost:8111/swagger-ui/index.html`
- OpenAPI：`http://localhost:8111/v3/api-docs`
- Health：`http://localhost:8111/actuator/health`

Compose 会通过环境变量覆盖数据库、Redis、JWT secret、上传目录和判题 Docker 可执行文件，不依赖本机 `application.properties` 里的默认路径和密码。

## 本地开发

```bash
cd OJPT-backend
mvn spring-boot:run
```

本地直接运行时，Spring Boot 默认读取 `src/main/resources/application.properties`。如果要改成本机以外的配置，优先使用环境变量覆盖：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `OJPT_JWT_SECRET`
- `OJPT_UPLOAD_BASE_PATH`
- `OJPT_JUDGE_DOCKER_EXECUTABLE`

## 初始化、迁移与 seed

Flyway 脚本位于 `src/main/resources/db/migration`：

- `V1_0__baseline_schema_and_seed.sql`
  - 初始化基础表结构
  - 导入角色、权限、默认账号
  - 导入首批题库、标签、提交记录、测试用例
- `V1_1__password_reset_requests.sql`
  - 增加密码重置申请表

默认 seed 账号如下，初始密码均为 `123456`：

- `admin`
- `admin1`
- `user`
- `user1`

登录支持用户名、邮箱或手机号；上面这些账号的用户名、邮箱、手机号都已在基线 seed 中写入。

注意：

- Flyway 只会执行尚未记录到 `flyway_schema_history` 的迁移
- 已初始化数据库不会自动重复导入旧 seed
- 如果你改了种子 SQL，想在本地重新导入，需要删除数据库卷后重建，或新增一个新的 Flyway migration

## 判题环境说明

后端做题判题依赖 Docker CLI 调用编译/运行镜像。根目录 compose 方案会：

- 在后端镜像中提供 `docker` CLI
- 将宿主机 Docker daemon 的 `/var/run/docker.sock` 挂进后端容器

这样可以直接复用当前 Docker Engine 运行 `gcc`、`eclipse-temurin`、`python` 判题镜像。若本地只想调接口、不需要判题，也可以单独运行后端，但提交代码与判题健康检查会依赖 Docker 是否可用。

## 说明

当前后端不再把它当作多角色教学平台来描述。历史上的 `STUDENT`、`TEACHER`、`SCHOOL` 等设计不再属于现行对外交付范围。

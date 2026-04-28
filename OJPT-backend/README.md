# OJPT Backend

后端围绕“个人算法训练”这个主线组织，只保留登录鉴权、题库读取、做题页所需数据，以及管理员维护题库的最小后台能力。

## 当前角色模型

- `USER`
- `ADMIN`

系统返回中的 `roleType` 和 `roles` 只会出现这两种角色。

## 当前接口范围

### 认证

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`

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

## 技术栈

- Java 17
- Spring Boot
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- Flyway

## 启动

```bash
cd OJPT-backend
mvn spring-boot:run
```

默认地址：`http://localhost:8111`

常用入口：

- Swagger UI：`/swagger-ui/index.html`
- OpenAPI：`/v3/api-docs`
- Health：`/actuator/health`

## 说明

当前后端不再把它当作多角色教学平台来描述。历史上的 `STUDENT`、`TEACHER`、`SCHOOL` 等设计不再属于现行对外交付范围。

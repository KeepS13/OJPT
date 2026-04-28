# OJPT Backend API

本文档只保留当前项目仍在使用、且与个人算法训练主线直接相关的接口。

## 角色约束

- `USER`
- `ADMIN`

接口示例中的 `roleType` 和 `roles` 只应出现这两种角色。

## 通用返回

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1714200000000
}
```

## 认证接口

### `POST /api/auth/login`

请求体：

```json
{
  "account": "user@example.com",
  "password": "Password123"
}
```

普通用户响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "access-token",
    "expiresIn": 3600,
    "refreshToken": "refresh-token",
    "refreshExpiresIn": 604800,
    "userId": 10001,
    "username": "demo-user",
    "email": "user@example.com",
    "avatar": null,
    "roleType": "USER",
    "roles": ["USER"]
  },
  "timestamp": 1714200000000
}
```

管理员响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "access-token",
    "expiresIn": 3600,
    "refreshToken": "refresh-token",
    "refreshExpiresIn": 604800,
    "userId": 1,
    "username": "admin",
    "email": "admin@example.com",
    "avatar": null,
    "roleType": "ADMIN",
    "roles": ["ADMIN"]
  },
  "timestamp": 1714200000000
}
```

### `POST /api/auth/refresh`

```json
{
  "refreshToken": "refresh-token"
}
```

### `GET /api/auth/me`

返回当前登录用户信息。

### `POST /api/auth/logout`

使当前登录态失效。

## 普通用户接口

以下接口均要求登录：

- `POST /api/users/me/avatar`
- `GET /api/users/me`
- `GET /api/users/me/detail`
- `PUT /api/users/me`
- `PUT /api/users/me/username`
- `PUT /api/users/me/email`
- `PUT /api/users/me/phone`
- `PUT /api/users/me/password`
- `DELETE /api/users/me`

## 题库接口

以下接口允许匿名访问：

- `GET /api/problems`
- `GET /api/problems/{id}`
- `GET /api/problems/no/{problemNo}`

`GET /api/problems` 支持：

- `page`
- `size`
- `keyword`
- `difficulty`
- `tagId`
- `status`
- `orderBy`

## 管理员接口

以下接口要求 `ADMIN`：

- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}`
- `DELETE /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}/status`
- `GET /api/admin/problems`
- `GET /api/admin/problems/{problemId}`
- `PUT /api/admin/problems/{problemId}`
- `POST /api/admin/problems/{problemId}:publish`
- `POST /api/admin/problems/{problemId}:archive`
- `GET /api/admin/tags`
- `POST /api/admin/tags`
- `PUT /api/admin/tags/{tagId}`
- `DELETE /api/admin/tags/{tagId}`
- `POST /api/admin/problems/{problemId}/tags?tagId=1`
- `DELETE /api/admin/problems/{problemId}/tags?tagId=1`
- `GET /api/admin/statistics/overview`
- `GET /api/admin/statistics/users`

## 当前不再保留的接口说明

以下内容不再作为当前项目文档的一部分：

- `/api/student/**`
- `/api/teacher/**`
- `/api/school/**`
- `/api/admin/roles/**`
- `/api/admin/permissions/**`
- `/api/admin/schools/**`
- 多角色分配、角色权限 CRUD、学校后台治理说明

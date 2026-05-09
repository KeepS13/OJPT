# OJPT Frontend API

前端当前只对接个人训练主线需要的接口，不再保留历史上的多角色平台接口说明。

## 角色

- `USER`
- `ADMIN`

前端状态层只识别这两种角色。

## 路由

### 匿名可访问

- `/`
- `/problemset`
- `/problems/:problemNo`

### 登录后可访问

- `/profile`
- `/profile/security`

### 仅管理员可访问

- `/admin`
- `/admin/users`
- `/admin/problems`
- `/admin/problems/:problemId`

## 认证接口

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`
- `POST /api/auth/logout`

登录后，前端只接受：

```json
{
  "roleType": "USER",
  "roles": ["USER"]
}
```

或：

```json
{
  "roleType": "ADMIN",
  "roles": ["ADMIN"]
}
```

## 普通用户接口

- `GET /api/users/me`
- `GET /api/users/me/detail`
- `PUT /api/users/me`
- `PUT /api/users/me/username`
- `PUT /api/users/me/email`
- `PUT /api/users/me/phone`
- `PUT /api/users/me/password`
- `POST /api/users/me/avatar`
- `DELETE /api/users/me`

## 题库接口

- `GET /api/problems`
- `GET /api/problems/{id}`
- `GET /api/problems/no/{problemNo}`

其中：

- `/problemset` 使用 `GET /api/problems`
- `/problems/:problemNo` 使用 `GET /api/problems/no/{problemNo}`

## 管理员接口

- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}`
- `DELETE /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}/status`
- `POST /api/admin/problems`
- `GET /api/admin/problems`
- `GET /api/admin/problems/{problemId}`
- `PUT /api/admin/problems/{problemId}`
- `POST /api/admin/problems/{problemId}:publish`
- `POST /api/admin/problems/{problemId}:archive`
- `GET /api/admin/tags`
- `POST /api/admin/tags`
- `PUT /api/admin/tags/{tagId}`
- `DELETE /api/admin/tags/{tagId}`
- `POST /api/admin/problems/{problemId}/tags`
- `DELETE /api/admin/problems/{problemId}/tags`
- `GET /api/admin/statistics/overview`
- `GET /api/admin/statistics/users`

## 当前不再保留的说明

以下内容不再属于前端现行文档范围：

- `/api/student/**`
- `/api/teacher/**`
- `/api/school/**`
- 多角色页面矩阵
- 角色权限管理端说明

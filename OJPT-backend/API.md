## 🗄️ 数据库迁移
- 初始化脚本已拆分为结构与数据两步，位于 `src/main/resources/db/migration/`
  - `V1_0__init_user.sql`：仅表结构（用户/权限/学校/班级等）
  - `V1_1__init_data.sql`：基础角色、账号及演示数据
- 执行顺序按文件名前缀保证先结构后数据。

## 📋 状态码说明

### HTTP 状态码
- **401 Unauthorized（未授权）**：认证失败
  - `accessToken` 或 `refreshToken` 过期
  - Token 签名无效或格式错误
  - 未提供 Token 或 Token 无效
  - **注意**：后端已统一处理，所有 token 过期/无效的情况都会返回 401（不再出现 403）
- **403 Forbidden（禁止访问）**：权限不足
  - 账号被封禁（返回封禁剩余时间）
  - 账号待审核
  - 角色权限不足（如非管理员访问管理员接口）
  - **注意**：仅当用户已认证但权限不足时才会返回 403

### 状态码统一处理机制
后端通过自定义 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 确保：
- 所有 token 过期/无效的情况统一返回 **401**（带 JSON body：`{"code":401,"message":"未授权：token 过期、无效或未提供"}`）
- 只有真正的权限不足才返回 **403**（带 JSON body：`{"code":403,"message":"权限不足：访问被拒绝"}`）
- 避免了 Spring Security 默认行为导致的"过期 token 返回 403"问题

---

## 📄 分页说明

以下列表类接口均支持分页，请求参数与响应格式统一如下。

**请求参数（Query）**：
- `page`：页码，从 1 开始，默认 `1`
- `size`：每页条数，默认 `10`，最大 `100`

**响应结构**：`Result<PageResult<T>>`，其中 `data` 为分页对象：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ /* 当前页数据列表 */ ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  },
  "timestamp": 1703123456789
}
```

**涉及接口**：学校模块（院系/班级/教师/学生列表）、教师模块（班级/学生/申请/教师列表）、学生模块（我的班级/班级成员列表）等，详见各模块接口说明。管理员模块的用户列表、学校列表已支持分页；角色与权限列表暂未分页。

---

## 🔐 接口文档（认证模块）

### 登录
- `POST /api/auth/login`
  - 请求：
    ```json
    {
      "account": "邮箱 或 手机号",
      "password": "******"
    }
    ```
  - 响应（LoginResponseVO）：
    ```json
    {
      "tokenType": "Bearer",
      "accessToken": "xxx",
      "expiresIn": 900,
      "refreshToken": "yyy",
      "refreshExpiresIn": 604800,
      "userId": 1998338632572506113,
      "username": "admin",
      "email": "admin@qq.com",
      "avatar": "/uploads/avatars/1998338632572506113_1703123456789.jpg",
      "roleType": "ADMIN",
      "roles": ["ADMIN","USER"]
    }
    ```
  - 说明：
    - `account` 字段表示登录账号，仅支持 **邮箱或手机号** 登录
    - `roles` 为去掉 `ROLE_` 前缀后的角色列表
    - `avatar` 为头像 URL，相对路径，通常通过 nginx 直接访问：`http(s)://<域名>/uploads/avatars/xxx.jpg`
  - 逻辑：
    - 若 `account` 看起来是邮箱（包含 `@`），则先按邮箱查用户
    - 否则按手机号查用户
    - 用户不存在：返回 401，body：`{ code, message: "用户不存在" }`
    - 账号被封禁：返回 403，body：
      ```json
      { "code": 403, "message": "账号已被封禁", "remainingSeconds": 12345 }
      ```
    - 校验账号密码通过后：
      - 检查账号状态（待审核/封禁等）
      - 生成 access/refresh，refresh jti 写入 Redis

### 刷新 Token
- `POST /api/auth/refresh`
  - **请求：**
    ```json
    {
      "refreshToken": "<上一次登录或刷新时返回的 refreshToken>"
    }
    ```
  - **响应：** 同登录，返回新的 `LoginResponseVO`（新的 access/refresh + 用户基础信息）：
    ```json
    {
      "tokenType": "Bearer",
      "accessToken": "new-access-token",
      "expiresIn": 900,
      "refreshToken": "new-refresh-token",
      "refreshExpiresIn": 604800,
      "userId": 1998338632572506113,
      "username": "admin",
      "email": "admin@qq.com",
      "avatar": "/uploads/avatars/1998338632572506113_xxx.jpg",
      "roleType": "ADMIN",
      "roles": ["ADMIN","USER"]
    }
    ```
  - **逻辑：**
    - 解析并校验 `refreshToken`：签名、是否过期、`type == "refresh"`
    - 从 token 中取出 `userId`、`username`、`roles`、`jti`
    - 到 Redis 校验 `(userId, jti) -> refreshToken` 是否存在且匹配
    - 校验通过后：
      - 生成新的 access/refresh
      - 删除旧 refresh 记录
      - 写入新的 `(userId, newJti) -> newRefreshToken`
  - **错误处理：**
    - `refreshToken` 为空或未提供：返回 **401 Unauthorized**
    - `refreshToken` 过期：返回 **401 Unauthorized**（不再返回 500 错误）
    - `refreshToken` 无效（签名错误、格式错误等）：返回 **401 Unauthorized**
    - `refreshToken` 类型不是 "refresh"：返回 **400 Bad Request**
    - Redis 中不存在或校验失败：返回 **401 Unauthorized**
    - 账号被封禁：返回 **403 Forbidden**（带封禁剩余时间）
    - **错误情况：**
      - 若 `refreshToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若 `refreshToken` 不在 Redis 中或已失效：返回 `401`
    - 若用户处于封禁状态：返回 `403`，body：`{ code, message, remainingSeconds }`

### 登出
- `POST /api/auth/logout`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **请求体：** 空
  - **响应：**
    ```json
    { "message": "登出成功" }
    ```
  - **逻辑：**
    - 从 access token 中解析：`jti`、`sub`(userId)、`jti_refresh`（绑定的 refresh jti，可选）
    - 若存在绑定的 refresh：删除 Redis 中对应 refresh 记录
    - 将当前 access 的 `jti` 加入黑名单，TTL = refreshToken 的有效期

### 获取当前登录用户信息（基于 accessToken）
- `GET /api/auth/me`
  - **说明：**
    - 前端在本地已有 accessToken 时，用此接口“自动登录/恢复会话”
    - 依赖后端的 JWT 过滤器校验 token 的有效性与黑名单状态
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **响应（CurrentUserVO）：**
    ```json
    {
      "userId": 1998338632572506113,
      "username": "admin",
      "email": "admin@qq.com",
      "avatar": "/uploads/avatars/1998338632572506113_1703123456789.webp",
      "roleType": "ADMIN",
      "status": 1,
      "roles": ["ADMIN", "USER"],
      "createdAt": "2025-12-18T10:00:00",
      "updatedAt": "2025-12-18T10:00:00"
    }
    ```
  - **逻辑：**
    - `JwtAuthenticationFilter` 从请求头获取 accessToken，解析并校验：
      - 签名、是否过期、`type == "access"`
      - 用户是否在黑名单（被封禁）
      - 当前 token 的 `jti` 是否在黑名单（已登出）
    - 将解析出的用户ID和角色放入 `SecurityContext`（principal 为 userId，Long 类型）
      - **重要**：使用 userId 而不是 username 作为 principal，因为 userId 是稳定的，不会因为用户名修改而改变
      - 这确保了即使用户修改了用户名，token 仍然有效，不会出现 404 错误
    - Controller 从 `SecurityContext` 获取认证信息：
      - 兼容多种 principal 类型：
        - `LoginUserDetails`（通过 userId 查询）
        - `Long`（userId，推荐方式，用户名修改后仍有效）
        - `String`（username，兼容旧 token）
      - 查询 User 表获取用户信息
      - 从 `SecurityContext` 的 authorities 中提取角色列表
      - 构造 `CurrentUserVO` 返回
    - **错误情况：**
      - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若未登录或 token 无效：返回 `401`
      - 若用户不存在：返回 `404`

## 👑 接口文档（管理员模块）
> 所有接口需管理员角色的 Bearer access token（`ROLE_ADMIN`）

### 创建用户
- `POST /api/admin/users`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体（AdminUserCreateDTO）：**
    ```json
    {
      "username": "new_user",
      "password": "PlainPassword123!",
      "email": "new_user@example.com",
      "phone": "13800001111",
      "roleCodes": ["ADMIN", "TEACHER"]
    }
    ```
    - `username`：必填，唯一
    - `password`：必填，明文，后端使用 BCrypt 加密
    - `email`：可选，若填则需唯一
    - `phone`：可选
    - `roleCodes`：角色编码列表（大写），如 `["ADMIN","TEACHER","STUDENT","USER"]`
  - **响应（AdminUserVO）：**
    ```json
    {
      "id": 1998338632572506200,
      "username": "new_user",
      "email": "new_user@example.com",
      "phone": "13800001111",
      "status": 1,
      "roleType": "ADMIN",
      "roleCodes": ["ADMIN", "TEACHER"],
      "createdAt": "2025-12-18T10:00:00",
      "updatedAt": "2025-12-18T10:00:00"
    }
    ```
  - **逻辑：**
    - 校验用户名是否唯一，冲突则返回 409：`"用户名已存在"`
    - 若 `email` 非空，校验唯一性，冲突则返回 409：`"邮箱已存在"`
    - 根据 `roleCodes` 查询角色表，若有不存在的编码，返回 400：`"角色不存在：ADMIN,XXX"`
    - 按角色 `level` 计算主角色 `roleType`（level 越大优先级越高）
    - 创建用户并落库（密码加密、默认启用状态等）
    - 写入用户与角色关系表（user_role），`bind_source="ADMIN_CREATE"`

### 封禁用户（拉黑）
- `POST /api/admin/users/{userId}/blacklist`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **路径参数：**
    - `userId`：要封禁的用户 ID
  - **查询参数：**
    - `durationSeconds`（可选）：封号时长（秒），未传或 `<= 0` 使用默认 30 天
  - **响应：**
    ```json
    {
      "message": "用户已封禁",
      "durationSeconds": 2592000
    }
    ```
  - **逻辑：**
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 计算封禁时长：使用传入的 `durationSeconds` 或默认 30 天（`30 * 24 * 60 * 60`）
    - 调用黑名单服务，将用户加入黑名单（userId -> TTL）
    - 封禁期间：
      - 登录/刷新接口会返回 403，并携带剩余封禁秒数
      - 业务接口可通过黑名单服务拦截

### 解禁用户
- `DELETE /api/admin/users/{userId}/blacklist`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **路径参数：**
    - `userId`：要解禁的用户 ID
  - **响应：**
    ```json
    { "message": "用户已恢复" }
    ```
  - **逻辑：**
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 从黑名单中移除用户，恢复其正常访问权限

## 👤 接口文档（用户模块）

### 查询个人详情
- `GET /api/users/me`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 返回当前登录用户的基础信息和角色列表（当前版本仅包含 `user` 表字段，不包含 `user_profile` 扩展字段）
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **响应（CurrentUserVO）：**
    ```json
    {
      "userId": 1998338632572506113,
      "username": "admin",
      "email": "admin@qq.com",
      "avatar": "/uploads/avatars/1998338632572506113_1703123456789.webp",
      "roleType": "ADMIN",
      "status": 1,
      "roles": ["ADMIN", "USER"],
      "createdAt": "2025-12-18T10:00:00",
      "updatedAt": "2025-12-18T10:00:00"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`（兼容 `LoginUserDetails` 和 `String` username 两种 principal 类型）
    - 查询用户基础信息
    - 查询用户的所有角色（从 `user_role` 表关联 `role` 表）
    - **错误情况：**
      - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若未登录或 token 无效：返回 `401`
      - 若用户不存在：返回 `404`

### 查询个人详情（含扩展信息）
- `GET /api/users/me/detail`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 返回当前登录用户的完整详情，包含 `user` 基础字段与 `user_profile` 扩展字段
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **响应（UserDetailVO）：**
    ```json
    {
      "userId": 1998338632572506113,
      "username": "admin",
      "email": "admin@qq.com",
      "phone": "13800001111",
      "avatar": "/uploads/avatars/1998338632572506113_1703123456789.webp",
      "roleType": "ADMIN",
      "status": 1,
      "roles": ["ADMIN", "USER"],
      "createdAt": "2025-12-18T10:00:00",
      "updatedAt": "2025-12-18T10:00:00",
      "gender": 1,
      "birthday": "1990-01-01",
      "address": "北京市朝阳区",
      "website": "https://example.com",
      "github": "username",
      "company": "某某公司",
      "position": "高级工程师",
      "skills": "Java,Spring Boot,MySQL",
      "studentNo": "2021001",
      "schoolId": 1000000000000000001,
      "bio": "这是我的简介",
      "tags": "技术,编程",
      "identityStatus": 1
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`（兼容 `LoginUserDetails` 和 `String` username 两种 principal 类型）
    - 查询 `user` 表的基础信息
    - 查询 `user_profile` 表的扩展信息（可能不存在，字段为 null）
    - 查询用户的所有角色（从 `user_role` 表关联 `role` 表）
    - 聚合上述数据构造 `UserDetailVO` 返回
    - **错误情况：**
      - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若未登录或 token 无效：返回 `401`
      - 若用户不存在：返回 `404`

### 修改个人信息
- `PUT /api/users/me`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - **不允许修改**：`username`、`password`、`avatar`（头像需通过单独接口上传）
    - 支持更新用户基础信息和扩展信息（UserProfile）
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体（UserUpdateDTO）：**
    ```json
    {
      "email": "new_email@example.com",
      "phone": "13800001111",
      "gender": 1,
      "birthday": "1990-01-01",
      "address": "北京市朝阳区",
      "website": "https://example.com",
      "github": "username",
      "company": "某某公司",
      "position": "高级工程师",
      "skills": "Java,Spring Boot,MySQL",
      "studentNo": "2021001",
      "schoolId": 1000000000000000001,
      "bio": "这是我的简介",
      "tags": "技术,编程"
    }
    ```
    - **用户基础信息字段：**
      - `email`：可选，邮箱格式，若提供则需唯一
      - `phone`：可选，手机号格式（1开头的11位数字），若提供则需唯一
    - **用户扩展信息字段（UserProfile）：**
      - `gender`：可选，性别（0未知/1男/2女）
      - `birthday`：可选，生日（格式：YYYY-MM-DD）
      - `address`：可选，现住址
      - `website`：可选，个人网站（博客或作品集等）
      - `github`：可选，GitHub 用户名或链接
      - `company`：可选，所在公司
      - `position`：可选，职位
      - `skills`：可选，技能（逗号分隔或 JSON）
      - `studentNo`：可选，学号/工号
      - `schoolId`：可选，学校ID
      - `bio`：可选，简介
      - `tags`：可选，标签（逗号分隔或 JSON）
  - **响应：**
    ```json
    {
      "message": "个人信息更新成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`（兼容 `LoginUserDetails` 和 `String` username 两种 principal 类型）
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 若提供了新邮箱且与当前邮箱不同：
      - 校验邮箱格式
      - 校验邮箱唯一性，冲突则返回 409：`"邮箱已被使用"`
    - 若提供了新手机号且与当前手机号不同：
      - 校验手机号格式
      - 校验手机号唯一性，冲突则返回 409：`"手机号已被使用"`
    - 更新用户基础信息（仅更新提供的字段）
    - 更新或创建用户扩展信息（UserProfile）：
      - 如果 profile 已存在，则更新提供的字段
      - 如果 profile 不存在且提供了至少一个 profile 字段，则创建新的 profile
    - **错误情况：**
      - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若未登录或 token 无效：返回 `401`

### 上传头像（当前用户）
- `POST /api/users/me/avatar`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - **上传头像**：前端需将头像预处理为 320×320 像素的 webp 格式
    - **删除头像**：传入 `file=null`（或 `file` 字段为空），将删除后端存储的头像文件并将数据库中的 `avatar` 字段设置为 `null`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: multipart/form-data
    ```
  - **请求体：**
    - form-data 字段名：`file`
    - **上传头像**：类型 `File`（webp 图片）
    - **删除头像**：`file` 字段为 `null` 或不传（`required = false`）
  - **响应（上传成功）：**
    ```json
    {
      "message": "头像上传成功",
      "avatar": "/uploads/avatars/1998338632572506113_1703123456789.webp"
    }
    ```
  - **响应（删除成功）：**
    ```json
    {
      "message": "头像删除成功",
      "avatar": ""
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`（兼容 `LoginUserDetails` 和 `String` username 两种 principal 类型）
    - **上传头像**：
      - 校验文件格式（仅支持 webp）、大小（≤1MB）
      - 删除旧头像文件（如果存在）
      - 保存新头像文件（文件名格式：`{userId}_{timestamp}.webp`）
      - 更新数据库中的 `avatar` 字段
    - **删除头像**：
      - 删除后端存储的头像文件（如果存在）
      - 将数据库中的 `avatar` 字段设置为 `null`
    - 校验文件：
      - 非空
      - 大小不超过 1MB（后端安全兜底，防止恶意超大文件占用带宽/内存）
      - Content-Type 为 `image/webp` 或文件名以 `.webp` 结尾
    - 查询用户当前头像，如果存在则删除旧头像文件（不存储历史头像）
    - 生成新文件名：`{userId}_{timestamp}.webp`
    - 保存新头像到本地：`C:\Program Files\nginx-1.28.0\uploads\avatars\{filename}`
    - 将数据库中 `user.avatar` 字段更新为相对路径：`/uploads/avatars/{filename}`
    - 前端可通过 nginx 直接访问头像：`http://localhost/uploads/avatars/{filename}` 或 `http(s)://<域名>/uploads/avatars/{filename}`
    - **错误情况：**
      - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
      - 若未登录或 token 无效：返回 `401`

### 获取头像（静态资源，由 Nginx 直接返回）
- `GET /uploads/avatars/{filename}`
  - **说明：**
    - 该接口由 Nginx 静态资源服务直接处理，不会经过后端 Spring Boot
    - 后端在保存头像时，仅将相对路径（如 `/uploads/avatars/{filename}`）写入 `user.avatar`
    - 前端展示头像时，直接拼接域名访问，例如：
      - 本地环境：`http://localhost/uploads/avatars/{filename}`
      - 线上环境：`https://<你的域名>/uploads/avatars/{filename}`
  - **Nginx 配置（节选）：**
    ```nginx
    location /uploads/ {
        alias "C:/Program Files/nginx-1.28.0/uploads/";
        expires 30d;  # 缓存 30 天
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*" always;
        try_files $uri =404;
    }
    ```
  - **行为：**
    - 请求的 URL 形如：`/uploads/avatars/1998338632572506113_1703123456789.webp`
    - Nginx 在 `C:/Program Files/nginx-1.28.0/uploads/avatars/` 目录下查找对应文件并返回
    - 文件不存在时直接返回 `404`，不会回退到前端或后端

## 🔒 接口文档（账号安全模块）

### 修改用户名
- `PUT /api/users/me/username`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 修改当前登录用户的用户名
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体：**
    ```json
    {
      "username": "new_username"
    }
    ```
    - `username`：必填，新用户名
      - 长度：2-20个字符
      - 格式：允许中文、字母、数字、下划线、空格以及特殊字符（上标、下标、修饰字母等）
      - 示例：`x²-y²`, `x⁵⁽ⁿ⁻⁶⁾`, `H₂O`, `CO₂`, `H₂SO₄`, `H⁺`, `Fe²⁺`, `Al³`, `ᴴᵉˡˡᵒ`, `ᵂᵉˡᶜᵒᵐᵉ`, `Lₒᵥₑ Yₒᵤ`, `Cₐₗₗ Mₑ`, `¹⁹⁹⁹₀₂.₁₆`
  - **响应：**
    ```json
    {
      "message": "用户名修改成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 校验用户名格式（长度、字符限制等）
    - 校验用户名唯一性，冲突则返回 409：`"用户名已被使用"`
    - 更新用户名
  - **错误情况：**
    - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
    - 若未登录或 token 无效：返回 `401`
    - 若用户名格式不正确：返回 `400`（Bad Request）
    - 若用户名已被使用：返回 `409`（Conflict）

### 修改邮箱
- `PUT /api/users/me/email`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 修改当前登录用户的邮箱（无需验证码，直接修改）
    - **注意**：也可以复用现有的 `PUT /api/users/me` 接口，传入 `email` 字段即可
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体：**
    ```json
    {
      "email": "new_email@example.com"
    }
    ```
    - `email`：必填，新邮箱地址
  - **响应：**
    ```json
    {
      "message": "邮箱修改成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 校验邮箱格式
    - 校验邮箱唯一性，冲突则返回 409：`"邮箱已被使用"`
    - 更新邮箱
    - **TODO**：未来可添加邮箱验证码验证逻辑
  - **错误情况：**
    - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
    - 若未登录或 token 无效：返回 `401`
    - 若邮箱格式不正确：返回 `400`（Bad Request）
    - 若邮箱已被使用：返回 `409`（Conflict）

### 修改手机号
- `PUT /api/users/me/phone`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 修改当前登录用户的手机号（无需验证码，直接修改）
    - **注意**：也可以复用现有的 `PUT /api/users/me` 接口，传入 `phone` 字段即可
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体：**
    ```json
    {
      "phone": "13800001111"
    }
    ```
    - `phone`：必填，新手机号
      - 格式：1开头的11位数字
  - **响应：**
    ```json
    {
      "message": "手机号修改成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 校验手机号格式（1开头的11位数字）
    - 校验手机号唯一性，冲突则返回 409：`"手机号已被使用"`
    - 更新手机号
    - **TODO**：未来可添加手机号验证码验证逻辑
  - **错误情况：**
    - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
    - 若未登录或 token 无效：返回 `401`
    - 若手机号格式不正确：返回 `400`（Bad Request）
    - 若手机号已被使用：返回 `409`（Conflict）

### 修改密码
- `PUT /api/users/me/password`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 修改当前登录用户的密码
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    Content-Type: application/json
    ```
  - **请求体：**
    ```json
    {
      "oldPassword": "old_password",
      "newPassword": "new_password"
    }
    ```
    - `oldPassword`：必填，原密码
    - `newPassword`：必填，新密码
      - 长度：至少8个字符
      - 复杂度：必须包含字母和数字
  - **响应：**
    ```json
    {
      "message": "密码修改成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 校验旧密码是否正确，错误则返回 400：`"原密码错误"`
    - 校验新密码不能与旧密码相同，相同则返回 400：`"新密码不能与原密码相同"`
    - 校验新密码强度（长度、复杂度等）
    - 更新密码（使用 BCrypt 加密存储）
  - **错误情况：**
    - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
    - 若未登录或 token 无效：返回 `401`
    - 若原密码错误：返回 `400`（Bad Request）
    - 若新密码格式不正确或强度不足：返回 `400`（Bad Request）
    - 若新密码与原密码相同：返回 `400`（Bad Request）

### 注销账号
- `DELETE /api/users/me`
  - **说明：**
    - 需要登录（任意角色均可），从当前 access token 中解析 userId
    - 注销当前登录用户的账号（软删除）
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **请求体：** 无
  - **响应：**
    ```json
    {
      "message": "账号注销成功"
    }
    ```
  - **逻辑：**
    - 从 SecurityContext 中获取当前登录用户 `userId`
    - 校验用户存在，不存在则返回 404：`"用户不存在"`
    - 软删除：将用户状态标记为已注销（`status = 0`）
    - 清理相关 token：删除该用户的所有 refresh token（从 Redis 中删除）
    - **TODO**：可选：保留数据一段时间（如 30 天）后再真正删除（可通过定时任务实现）
  - **错误情况：**
    - 若 `accessToken` 过期、签名无效或格式错误：返回 `401`（未授权）
    - 若未登录或 token 无效：返回 `401`
    - 若用户不存在：返回 `404`（Not Found）

### 账号安全接口汇总

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 修改用户名 | PUT | `/api/users/me/username` | 需要校验唯一性 |
| 修改邮箱 | PUT | `/api/users/me/email` 或 `/api/users/me` | 无需验证码，直接修改 |
| 修改手机号 | PUT | `/api/users/me/phone` 或 `/api/users/me` | 无需验证码，直接修改 |
| 修改密码 | PUT | `/api/users/me/password` | 需要提供旧密码 |
| 注销账号 | DELETE | `/api/users/me` | 软删除或硬删除 |

**说明：**
- 邮箱和手机号修改：可复用现有的 `PUT /api/users/me`，传入对应字段即可
- 用户名和密码：需要单独接口，因为 `PUT /api/users/me` 文档中明确不允许修改这两个字段
- 验证码：按需求，邮箱和手机号修改无需验证码，直接修改即可（已预留 TODO 注释，未来可添加验证码验证）
- 错误处理：统一返回格式，包含 `message` 字段说明错误原因

---

## 🎓 接口文档（学员中心模块）
> 所有接口需学员角色的 Bearer access token（`ROLE_STUDENT`）

### 获取个人完整信息
- `GET /api/student/profile`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **响应：** 同 `GET /api/users/me/detail`，返回 `UserDetailVO`
  - **说明：** 复用用户模块的接口逻辑

### 获取我加入的班级列表
- `GET /api/student/classes`
  - **请求头：**
    ```http
    Authorization: Bearer <accessToken>
    ```
  - **响应：**
    ```json
    [
      {
        "id": 1998338632572507201,
        "departmentId": 1998338632572507101,
        "departmentName": "计算机科学与技术系",
        "schoolId": 1998338632572507001,
        "schoolName": "清华大学",
        "name": "2024级1班",
        "year": "2024",
        "teacherId": 1998338632572506116,
        "teacherName": "test_teacher",
        "merk": "算法训练班",
        "joinStatus": "APPROVED",
        "joinType": "APPLY",
        "joinAt": "2025-01-01T10:00:00",
        "createdAt": "2025-01-01T10:00:00",
        "updatedAt": "2025-01-01T10:00:00"
      }
    ]
    ```
  - **说明：** 返回当前学员加入的所有班级，包含加入状态（PENDING/APPROVED/REJECTED）

### 获取班级详情
- `GET /api/student/classes/{classId}`
  - **路径参数：**
    - `classId`：班级ID
  - **响应：** 同 `GET /api/student/classes`，返回单个 `ClassVO`
  - **说明：** 包含当前学员在该班级的加入状态

### 申请加入班级
- `POST /api/student/classes/{classId}/apply`
  - **路径参数：**
    - `classId`：班级ID
  - **响应：**
    ```json
    {
      "message": "申请成功，等待审核"
    }
    ```
  - **逻辑：**
    - 创建 `class_user` 记录，`join_type='APPLY'`, `join_status='PENDING'`
    - 如果已申请或加入，返回错误

### 退出班级
- `DELETE /api/student/classes/{classId}/quit`
  - **路径参数：**
    - `classId`：班级ID
  - **响应：**
    ```json
    {
      "message": "退出成功"
    }
    ```
  - **逻辑：** 删除 `class_user` 记录

### 查看班级成员列表
- `GET /api/student/classes/{classId}/members`
  - **路径参数：**
    - `classId`：班级ID
  - **响应：**
    ```json
    [
      {
        "userId": 1998338632572506117,
        "username": "test_user",
        "email": "test_user@qq.com",
        "avatar": "/uploads/avatars/xxx.webp",
        "studentNo": "2024001",
        "joinAt": "2025-01-01T10:00:00",
        "joinType": "APPLY"
      }
    ]
    ```
  - **说明：** 仅返回已通过审核的成员（`join_status='APPROVED'`）

---

## 👨‍🏫 接口文档（教师后台模块）
> 所有接口需教师角色的 Bearer access token（`ROLE_TEACHER`）

### 获取我管理的班级列表
- `GET /api/teacher/classes`
  - **响应：** 返回当前教师作为班主任或助教的所有班级列表（`ClassVO` 数组）

### 创建班级
- `POST /api/teacher/classes`
  - **请求体（ClassCreateDTO）：**
    ```json
    {
      "departmentId": 1998338632572507101,
      "name": "2024级3班",
      "year": "2024",
      "teacherId": 1998338632572506116,
      "merk": "数据结构训练班"
    }
    ```
    - `departmentId`：必填，院系ID
    - `name`：必填，班级名称
    - `year`：可选，届/年份
    - `teacherId`：可选，班主任ID（默认当前用户）
    - `merk`：可选，班级类型/简介
  - **响应：** 返回创建的班级信息（`ClassVO`）

### 获取班级详情
- `GET /api/teacher/classes/{classId}`
  - **权限校验：** 当前用户必须是该班级的教师（班主任或助教）

### 更新班级信息
- `PUT /api/teacher/classes/{classId}`
  - **请求体（ClassUpdateDTO）：**
    ```json
    {
      "name": "2024级3班（更新）",
      "year": "2024",
      "teacherId": 1998338632572506120,
      "merk": "算法训练班"
    }
    ```
  - **权限校验：** 当前用户必须是该班级的教师

### 删除班级
- `DELETE /api/teacher/classes/{classId}`
  - **权限校验：** 只有班主任可以删除班级
  - **逻辑：** 删除班级及关联的 `class_user` 和 `class_teacher` 记录

### 获取班级学员列表（已通过审核的）
- `GET /api/teacher/classes/{classId}/students`
  - **响应：** 返回 `ClassMemberVO` 数组

### 获取加入申请列表（待审核）
- `GET /api/teacher/classes/{classId}/applications`
  - **响应：**
    ```json
    [
      {
        "id": 1998338632572507303,
        "classId": 1998338632572507202,
        "className": "2024级2班",
        "userId": 1998338632572506117,
        "username": "test_user",
        "email": "test_user@qq.com",
        "avatar": "/uploads/avatars/xxx.webp",
        "studentNo": "2024001",
        "joinType": "APPLY",
        "joinStatus": "PENDING",
        "joinAt": null,
        "reviewerId": null,
        "reviewerName": null,
        "reviewAt": null,
        "reviewComment": null
      }
    ]
    ```

### 批准加入申请
- `POST /api/teacher/classes/{classId}/applications/{applicationId}/approve`
  - **请求体（可选，ApplicationReviewDTO）：**
    ```json
    {
      "reviewComment": "审核通过"
    }
    ```
  - **逻辑：** 更新 `join_status='APPROVED'`, `reviewer_id=当前用户ID`, `review_at=当前时间`, `join_at=当前时间`

### 拒绝加入申请
- `POST /api/teacher/classes/{classId}/applications/{applicationId}/reject`
  - **请求体（可选，ApplicationReviewDTO）：**
    ```json
    {
      "reviewComment": "班级已满"
    }
    ```
  - **逻辑：** 更新 `join_status='REJECTED'`, `reviewer_id=当前用户ID`, `review_at=当前时间`

### 邀请学员加入
- `POST /api/teacher/classes/{classId}/students/{studentId}/invite`
  - **逻辑：** 创建 `class_user` 记录，`join_type='INVITE'`, `join_status='PENDING'`

### 移除学员
- `DELETE /api/teacher/classes/{classId}/students/{studentId}`
  - **逻辑：** 删除 `class_user` 记录

### 获取班级的教师列表
- `GET /api/teacher/classes/{classId}/teachers`
  - **响应：**
    ```json
    [
      {
        "teacherId": 1998338632572506116,
        "username": "test_teacher",
        "email": "test_teacher@qq.com",
        "avatar": "/uploads/avatars/xxx.webp",
        "role": "班主任",
        "createdAt": "2025-01-01T10:00:00"
      },
      {
        "teacherId": 1998338632572506120,
        "username": "only_teacher",
        "email": "only_teacher@qq.com",
        "avatar": "/uploads/avatars/xxx.webp",
        "role": "助教",
        "createdAt": "2025-01-01T10:00:00"
      }
    ]
    ```

### 添加教师到班级
- `POST /api/teacher/classes/{classId}/teachers?teacherId={teacherId}&role={role}`
  - **查询参数：**
    - `teacherId`：要添加的教师ID
    - `role`：可选，角色（如"助教"、"任课教师"）
  - **逻辑：** 创建 `class_teacher` 记录
  - **注意：** 不能添加班主任（班主任在 `class.teacher_id` 字段）

### 移除班级教师
- `DELETE /api/teacher/classes/{classId}/teachers/{teacherId}`
  - **注意：** 不能删除班主任，需先更换班主任

---

## 🏫 接口文档（校方管理模块）
> 所有接口需校方角色的 Bearer access token（`ROLE_SCHOOL`）

### 获取当前校方管理的学校信息
- `GET /api/school/info`
  - **逻辑：** 从当前用户的 `user_profile.school_id` 获取学校ID
  - **响应：** 返回 `SchoolVO`（含统计信息）

### 更新学校信息
- `PUT /api/school/info`
  - **请求体（SchoolUpdateDTO）：**
    ```json
    {
      "name": "清华大学（更新）",
      "contact": "010-62785002",
      "status": 1
    }
    ```

### 获取学校认证状态
- `GET /api/school/certification`
  - **响应：** 返回 `SchoolVO`，包含 `status`（1启用/0禁用/2待认证）和 `certifiedAt`

### 获取院系列表
- `GET /api/school/departments`
  - **响应：** 返回当前学校下的所有院系列表（`DepartmentVO` 数组）

### 创建院系
- `POST /api/school/departments`
  - **请求体（DepartmentCreateDTO）：**
    ```json
    {
      "name": "人工智能学院"
    }
    ```
  - **逻辑：** `school_id` 自动设置为当前用户的学校ID

### 获取院系详情
- `GET /api/school/departments/{departmentId}`
  - **权限校验：** 院系必须属于当前用户的学校

### 更新院系信息
- `PUT /api/school/departments/{departmentId}`
  - **请求体（DepartmentUpdateDTO）：**
    ```json
    {
      "name": "人工智能学院（更新）"
    }
    ```

### 删除院系
- `DELETE /api/school/departments/{departmentId}`
  - **注意：** 如果院系下还有班级，无法删除

### 获取学校下所有班级列表
- `GET /api/school/classes`
  - **响应：** 返回当前学校下所有班级列表（`ClassVO` 数组）

### 获取指定院系下的班级列表
- `GET /api/school/departments/{departmentId}/classes`
  - **权限校验：** 院系必须属于当前用户的学校

### 获取班级详情
- `GET /api/school/classes/{classId}`
  - **权限校验：** 班级必须属于当前用户的学校

### 更新班级信息（如更换班主任）
- `PUT /api/school/classes/{classId}`
  - **请求体：** 同 `PUT /api/teacher/classes/{classId}`

### 删除班级
- `DELETE /api/school/classes/{classId}`
  - **逻辑：** 删除班级及关联数据

### 获取学校下所有教师列表
- `GET /api/school/teachers`
  - **逻辑：** 查询 `user_profile.school_id` 匹配且拥有 `TEACHER` 角色的用户
  - **响应：** 返回 `TeacherVO` 数组

### 添加教师（绑定角色和学校）
- `POST /api/school/teachers`
  - **请求体：**
    ```json
    {
      "userId": 1998338632572506120
    }
    ```
  - **逻辑：**
    1. 绑定 `TEACHER` 角色（`user_role`）
    2. 更新 `user_profile.school_id`

### 获取教师详情
- `GET /api/school/teachers/{teacherId}`
  - **权限校验：** 教师必须属于当前用户的学校

### 更新教师信息
- `PUT /api/school/teachers/{teacherId}`
  - **请求体：** 同 `PUT /api/users/me`（`UserUpdateDTO`）

### 移除教师角色
- `DELETE /api/school/teachers/{teacherId}`
  - **逻辑：** 删除 `user_role` 记录（role_id = TEACHER），不删除用户

### 获取教师管理的班级列表
- `GET /api/school/teachers/{teacherId}/classes`
  - **权限校验：** 教师必须属于当前用户的学校

### 获取学校下所有学员列表
- `GET /api/school/students`
  - **逻辑：** 查询 `user_profile.school_id` 匹配且拥有 `USER` 角色的用户
  - **响应：** 返回 `UserDetailVO` 数组

### 获取指定院系的学员列表
- `GET /api/school/departments/{departmentId}/students`
  - **逻辑：** 通过该院系下的班级关联查询学员

### 获取指定班级的学员列表
- `GET /api/school/classes/{classId}/students`
  - **响应：** 返回 `ClassMemberVO` 数组

### 获取学员详情
- `GET /api/school/students/{studentId}`
  - **权限校验：** 学员必须属于当前用户的学校

### 更新学员信息（如学号、院系等）
- `PUT /api/school/students/{studentId}`
  - **请求体：** 同 `PUT /api/users/me`（`UserUpdateDTO`）

### 获取学校整体数据概览
- `GET /api/school/statistics/overview`
  - **响应：**
    ```json
    {
      "totalCount": 100,
      "statusCount": {
        "departments": 5,
        "classes": 20,
        "teachers": 30,
        "students": 45
      }
    }
    ```

### 获取各院系数据统计
- `GET /api/school/statistics/departments`
  - **响应：**
    ```json
    [
      {
        "departmentId": 1998338632572507101,
        "departmentName": "计算机科学与技术系",
        "classCount": 5,
        "studentCount": 150
      }
    ]
    ```

### 获取各班级数据统计
- `GET /api/school/statistics/classes`
  - **响应：**
    ```json
    [
      {
        "classId": 1998338632572507201,
        "className": "2024级1班",
        "studentCount": 30,
        "teacherCount": 2
      }
    ]
    ```

---

## 🔧 接口文档（管理员控制台模块）
> 所有接口需管理员角色的 Bearer access token（`ROLE_ADMIN`）

### 获取用户列表（分页、筛选）
- `GET /api/admin/users?page=1&size=10&status=1&roleType=USER&keyword=test`
  - **查询参数：**
    - `page`：页码（默认1）
    - `size`：每页数量（默认10）
    - `status`：可选，用户状态（0禁用/1启用/2待审核）
    - `roleType`：可选，角色类型（USER/STUDENT/TEACHER/SCHOOL/ADMIN）
    - `keyword`：可选，关键词（搜索用户名/邮箱/手机号）
  - **响应：** 返回 `UserDetailVO` 数组

### 获取用户详情
- `GET /api/admin/users/{userId}`
  - **响应：** 返回 `UserDetailVO`（含所有角色）

### 更新用户信息
- `PUT /api/admin/users/{userId}`
  - **请求体：** 同 `PUT /api/users/me`（`UserUpdateDTO`）

### 删除用户（软删除）
- `DELETE /api/admin/users/{userId}`
  - **逻辑：** 设置 `is_deleted=1`

### 修改用户状态（启用/禁用/待审核）
- `PUT /api/admin/users/{userId}/status`
  - **请求体：**
    ```json
    {
      "status": 1
    }
    ```
    - `status`：0禁用/1启用/2待审核

### 修改用户角色绑定
- `PUT /api/admin/users/{userId}/roles`
  - **请求体（UserRoleUpdateDTO）：**
    ```json
    {
      "roleCodes": ["ADMIN", "TEACHER"]
    }
    ```
  - **逻辑：** 删除旧的角色绑定，创建新的角色绑定

### 获取角色列表
- `GET /api/admin/roles`
  - **响应：**
    ```json
    [
      {
        "id": 1000000000000000101,
        "code": "USER",
        "name": "学员",
        "description": "普通学员",
        "level": 100,
        "permissionCount": 5,
        "createdAt": "2025-01-01T10:00:00",
        "updatedAt": "2025-01-01T10:00:00"
      }
    ]
    ```

### 创建角色
- `POST /api/admin/roles`
  - **请求体（RoleCreateDTO）：**
    ```json
    {
      "code": "CUSTOM_ROLE",
      "name": "自定义角色",
      "description": "自定义角色描述",
      "level": 150
    }
    ```
    - `code`：必填，角色编码（唯一）
    - `name`：必填，角色名称
    - `description`：可选，描述
    - `level`：可选，角色层级（默认0）

### 获取角色详情（含权限列表）
- `GET /api/admin/roles/{roleId}`
  - **响应：** 返回 `RoleVO`（含 `permissions` 数组）

### 更新角色信息
- `PUT /api/admin/roles/{roleId}`
  - **请求体（RoleUpdateDTO）：**
    ```json
    {
      "name": "学员（更新）",
      "description": "更新后的描述",
      "level": 110
    }
    ```
  - **注意：** `code` 通常不允许修改

### 删除角色
- `DELETE /api/admin/roles/{roleId}`
  - **注意：** 如果角色下还有用户或权限关联，无法删除

### 获取权限列表
- `GET /api/admin/permissions?resource=/api/users&action=GET&keyword=user`
  - **查询参数：**
    - `resource`：可选，资源标识
    - `action`：可选，操作动作
    - `keyword`：可选，关键词（搜索resource/action/description）
  - **响应：** 返回 `PermissionVO` 数组

### 创建权限
- `POST /api/admin/permissions`
  - **请求体（PermissionCreateDTO）：**
    ```json
    {
      "resource": "/api/users",
      "action": "GET",
      "conditionJson": "{\"schoolId\": \"${user.schoolId}\"}",
      "description": "查询用户列表"
    }
    ```
    - `resource`：必填，资源标识
    - `action`：必填，操作动作
    - `conditionJson`：可选，ABAC条件JSON
    - `description`：可选，描述

### 获取权限详情
- `GET /api/admin/permissions/{permissionId}`
  - **响应：** 返回 `PermissionVO`

### 更新权限
- `PUT /api/admin/permissions/{permissionId}`
  - **请求体（PermissionUpdateDTO）：**
    ```json
    {
      "conditionJson": "{\"schoolId\": \"${user.schoolId}\"}",
      "description": "更新后的描述"
    }
    ```
  - **注意：** `resource` 和 `action` 通常不允许修改（唯一约束）

### 删除权限
- `DELETE /api/admin/permissions/{permissionId}`
  - **注意：** 如果权限已被角色使用，无法删除

### 为角色分配权限
- `POST /api/admin/roles/{roleId}/permissions`
  - **请求体（RolePermissionAssignDTO）：**
    ```json
    {
      "permissionIds": [1, 2, 3]
    }
    ```
  - **逻辑：** 批量创建 `role_permission` 记录（如果已存在则跳过）

### 移除角色权限
- `DELETE /api/admin/roles/{roleId}/permissions/{permissionId}`
  - **逻辑：** 删除 `role_permission` 记录

### 获取学校列表（分页、筛选）
- `GET /api/admin/schools?page=1&size=10&status=1&keyword=清华`
  - **查询参数：**
    - `page`：页码（默认1）
    - `size`：每页数量（默认10）
    - `status`：可选，学校状态（1启用/0禁用/2待认证）
    - `keyword`：可选，关键词（搜索学校名称）
  - **响应：** 返回 `SchoolVO` 数组（含统计信息）

### 创建学校
- `POST /api/admin/schools`
  - **请求体（SchoolCreateDTO）：**
    ```json
    {
      "name": "清华大学",
      "contact": "010-62785001",
      "status": 1
    }
    ```
    - `name`：必填，学校名称
    - `contact`：可选，联系人/电话
    - `status`：可选，状态（默认1）

### 获取学校详情
- `GET /api/admin/schools/{schoolId}`
  - **响应：** 返回 `SchoolVO`（含统计信息：院系数、班级数、教师数、学员数）

### 更新学校信息
- `PUT /api/admin/schools/{schoolId}`
  - **请求体（SchoolUpdateDTO）：**
    ```json
    {
      "name": "清华大学（更新）",
      "contact": "010-62785002",
      "status": 1
    }
    ```

### 删除学校
- `DELETE /api/admin/schools/{schoolId}`
  - **注意：** 需要先删除关联的院系、班级等数据

### 修改学校状态
- `PUT /api/admin/schools/{schoolId}/status`
  - **请求体：**
    ```json
    {
      "status": 1
    }
    ```
    - `status`：1启用/0禁用/2待认证

### 认证学校
- `POST /api/admin/schools/{schoolId}/certify`
  - **逻辑：** 更新 `status=1`, `certified_at=当前时间`

### 取消认证
- `DELETE /api/admin/schools/{schoolId}/certify`
  - **逻辑：** 更新 `status=2`, `certified_at=NULL`

### 获取平台整体数据概览
- `GET /api/admin/statistics/overview`
  - **响应：**
    ```json
    {
      "totalCount": 1000,
      "statusCount": {
        "users": 500,
        "schools": 10
      }
    }
    ```

### 获取用户数据统计
- `GET /api/admin/statistics/users`
  - **响应：**
    ```json
    {
      "totalCount": 500,
      "statusCount": {
        "0": 10,
        "1": 480,
        "2": 10
      },
      "roleCount": {
        "USER": 400,
        "TEACHER": 50,
        "SCHOOL": 30,
        "ADMIN": 20
      }
    }
    ```

### 获取学校数据统计
- `GET /api/admin/statistics/schools`
  - **响应：**
    ```json
    {
      "totalCount": 10,
      "statusCount": {
        "0": 1,
        "1": 8,
        "2": 1
      }
    }
    ```


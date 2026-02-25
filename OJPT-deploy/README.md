# OJPT 部署配置仓库

本仓库包含 OJPT 项目的所有部署相关配置文件。

## 目录结构

```
OJPT-deploy/
├── nginx/              # Nginx 配置文件
│   └── nginx.conf      # Nginx 主配置文件
├── docker/             # Docker 相关配置（待添加）
├── scripts/             # 部署脚本（待添加）
└── docs/                # 部署文档（待添加）
```

## Nginx 配置说明

### 配置文件位置
- 开发环境：`nginx/nginx.conf`
- 生产环境：根据实际部署路径调整

### 主要功能
- 前端静态资源服务（端口 80）
- 后端 API 代理（`/api/` -> `http://localhost:8080`）
- 头像等静态文件服务（`/uploads/`）
- CORS 跨域配置

### 部署步骤

1. **复制配置文件**
   ```bash
   # Windows
   copy nginx\nginx.conf "C:\Program Files\nginx-1.28.0\conf\nginx.conf"
   
   # Linux
   cp nginx/nginx.conf /etc/nginx/nginx.conf
   ```

2. **验证配置**
   ```bash
   nginx -t
   ```

3. **重载配置**
   ```bash
   nginx -s reload
   ```

## 注意事项

⚠️ **重要**：上传到 GitHub 前，请确保：
- 已移除敏感信息（如密钥、密码）
- 路径已改为相对路径或环境变量
- 生产环境配置已单独管理

## 相关仓库

- 后端代码：https://github.com/KeepS13/OJPT
- 前端代码：https://github.com/KeepS13/OJPT-frontend


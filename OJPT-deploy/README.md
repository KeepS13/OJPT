# OJPT Deploy

部署目录当前只保留本项目联调所需的 Nginx 配置。

## 当前作用

- 提供前端静态资源入口
- 将 `/api` 代理到后端 `http://localhost:8111`
- 代理 `/uploads` 静态资源

## 主要文件

- `nginx/nginx.conf`

## 使用方式

1. 将 `nginx/nginx.conf` 复制到本机 Nginx 配置目录
2. 执行：

```bash
nginx -t
nginx -s reload
```

## 当前默认端口

- 前端开发：`http://localhost:8110`
- 后端开发：`http://localhost:8111`
- Nginx 联调入口：`http://localhost`

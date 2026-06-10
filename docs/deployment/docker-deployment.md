# Docker Compose 部署说明

## 服务清单

Docker Compose 包含以下服务：

- `mysql`：MySQL 8，自动初始化 schema 和 seed。
- `redis`：Redis 7，作为后续缓存与会话扩展预留。
- `ai-service`：Python FastAPI AI 微服务，默认 Mock 模式。
- `backend`：Spring Boot Java 主后端。
- `frontend`：Nginx 托管 Vue 构建产物，并反向代理 `/api`。

## 启动步骤

```powershell
Copy-Item .env.example .env
.\scripts\start.ps1 -Build
```

也可以直接执行：

```powershell
docker compose up -d --build
```

## 访问地址

- 前端：http://127.0.0.1:5173
- 后端：http://127.0.0.1:8080/api/health
- AI 服务：http://127.0.0.1:8000/ai/health
- Knife4j：http://127.0.0.1:8080/doc.html

## 数据持久化

MySQL 和 Redis 使用命名卷 `mysql_data` 与 `redis_data`。如果需要重新初始化演示数据，可先备份后执行：

```powershell
docker compose down -v
docker compose up -d --build
```

## 注意事项

前端容器只访问 Java 后端 `/api`，不会直接访问 Python AI 服务。Java 后端通过容器网络访问 `http://ai-service:8000`。

# 本地开发部署说明

## 目标

本地部署用于开发、调试和答辩前联调。推荐启动顺序为 MySQL、AI 服务、Java 后端、Vue 前端。

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- MySQL 8
- Redis 7 可选

## 数据库初始化

```powershell
mysql -uroot -p < backend/src/main/resources/sql/schema.sql
mysql -uroot -p eduagent_studio < backend/src/main/resources/sql/seed.sql
```

演示账号为 `demo_student / 123456`、`demo_teacher / 123456`、`demo_admin / 123456`。

## 启动 AI 服务

```powershell
cd ai-service
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

健康检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/ai/health
```

## 启动后端

```powershell
cd backend
mvn.cmd spring-boot:run
```

如 MySQL 密码不是空值，可设置：

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="your_password"
```

## 启动前端

```powershell
cd frontend
npm.cmd install --cache .\.npm-cache
npm.cmd run dev
```

访问 `http://127.0.0.1:5173`。

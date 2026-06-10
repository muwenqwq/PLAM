# EduAgent Studio AI 服务

本目录是 EduAgent Studio / LearnAgent-A3 的 Python FastAPI AI 微服务。当前阶段优先提供 Mock AI 能力，保证没有真实 API Key 时也能完成演示链路。

## 启动方式

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

本服务不连接 MySQL。Java Spring Boot 后端负责用户认证、业务数据保存和数据库访问，Python AI 服务只接收 Java 传入的模型配置和业务上下文，并返回结构化 AI 结果。

## 接口

- `GET /ai/health`
- `POST /ai/model/test`
- `POST /ai/chat`
- `POST /ai/chat/intent`
- `POST /ai/agents/run`
- `POST /ai/resources/generate`


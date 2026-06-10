# API 测试说明

## 接口文档地址

- Knife4j：`http://127.0.0.1:8080/doc.html`
- OpenAPI JSON：`http://127.0.0.1:8080/v3/api-docs`
- AI 服务 Swagger：`http://127.0.0.1:8000/docs`

## 认证流程

1. 调用 `POST /api/auth/login`。
2. 从响应 `data.accessToken` 取得 JWT。
3. 后续请求添加请求头 `Authorization: Bearer <token>`。

## 主要测试链路

```text
/api/auth/login
/api/model-providers/default
/api/model-providers/{id}/test
/api/chat/conversations
/api/chat/conversations/{id}/messages
/api/agent-tasks
/api/resources/generate
/api/learning-paths/generate
/api/quizzes/generate
/api/reports/overview
```

## Mock AI

默认模型配置为 Mock 类型，接口测试不依赖真实大模型 API。该策略保证答辩现场即使无外网或无 API Key，也能走通核心演示流程。

# Java 后端与 Python AI 服务集成说明

Java 后端通过 `AiServiceClient` 调用 Python AI 服务。AI 服务地址来自：

```yaml
eduagent:
  ai-service:
    base-url: http://localhost:8000
```

前端不直接访问 Python AI 服务。所有用户认证、权限校验、数据库保存、API Key 解密和数据隔离均由 Java 后端完成。

当前调用关系：

- 模型测试：`POST /ai/model/test`
- 智能对话：`POST /ai/chat`
- 意图识别：`POST /ai/chat/intent`
- Agent 任务：`POST /ai/agents/run`

AI 服务不可用时，Java 后端返回统一业务异常，提示启动 Python AI 服务。


# Python AI 服务总览

Python AI 服务基于 FastAPI，默认监听 `http://localhost:8000`。它不连接 MySQL，只接收 Java 后端传入的模型配置、用户上下文和任务输入，并返回结构化 JSON。

当前服务提供：

- `/ai/health`
- `/ai/model/test`
- `/ai/chat`
- `/ai/chat/intent`
- `/ai/agents/run`
- `/ai/resources/generate`

当前重点是 Mock 模式演示，后续可以扩展真实 OpenAI-compatible 调用、RAG 检索和异步 Agent 工作流。


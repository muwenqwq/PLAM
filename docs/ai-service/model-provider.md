# AI 服务 Provider 设计

`MockLLMProvider` 用于无 API Key 演示，永远不调用外部网络。`OpenAICompatibleProvider` 当前只做配置封装和安全检查，真实外部请求在后续阶段补充。

Provider 工厂规则：

1. `provider_type = mock` 时使用 Mock。
2. 没有 API Key 时使用 Mock。
3. `openai_compatible`、`deepseek`、`qwen`、`custom` 可进入 OpenAI-compatible 封装。


# AI 模型配置接口说明

所有接口需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/model-providers` | 创建模型配置 |
| `GET` | `/api/model-providers` | 分页查询模型配置 |
| `GET` | `/api/model-providers/default` | 查询默认模型 |
| `GET` | `/api/model-providers/{id}` | 查询模型详情 |
| `PUT` | `/api/model-providers/{id}` | 更新模型配置 |
| `DELETE` | `/api/model-providers/{id}` | 逻辑删除模型配置 |
| `POST` | `/api/model-providers/{id}/default` | 设置默认模型 |
| `POST` | `/api/model-providers/{id}/test` | 测试模型连接 |

创建 Mock 配置示例：

```json
{
  "providerName": "本地 Mock 模型",
  "providerType": "mock",
  "baseUrl": "mock://local/llm",
  "modelName": "mock-chat-v1",
  "embeddingModel": "mock-embedding-v1",
  "defaultProvider": true
}
```

响应不会包含 `apiKey` 或 `apiKeyEncrypted`。


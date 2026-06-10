# 智能对话接口说明

所有接口需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/chat/conversations` | 创建会话 |
| `GET` | `/api/chat/conversations` | 分页查询会话 |
| `GET` | `/api/chat/conversations/{id}` | 查询会话详情 |
| `GET` | `/api/chat/conversations/{id}/messages` | 查询会话消息 |
| `POST` | `/api/chat/conversations/{id}/messages` | 发送消息并保存 AI 回复 |
| `POST` | `/api/chat/intent` | 识别消息意图 |

发送消息示例：

```json
{
  "modelProviderId": 1,
  "subject": "数据库系统",
  "message": "帮我复习索引和事务"
}
```


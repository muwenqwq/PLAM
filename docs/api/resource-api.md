# 生成资源接口说明

所有接口需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/resources` | 分页查询当前用户生成资源 |
| `GET` | `/api/resources/{id}` | 查询生成资源详情 |

资源由 Agent 任务生成并保存到 `generated_resource`。当前阶段支持 Markdown 内容和结构化 JSON 内容保存。


# 多智能体任务接口说明

所有接口需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/agent-tasks` | 创建并同步执行 Agent 任务 |
| `GET` | `/api/agent-tasks` | 分页查询 Agent 任务 |
| `GET` | `/api/agent-tasks/{id}` | 查询任务详情 |
| `GET` | `/api/agent-tasks/{id}/steps` | 查询执行步骤 |
| `POST` | `/api/agent-tasks/{id}/rerun` | 重新执行任务 |
| `POST` | `/api/agent-tasks/{id}/save-resource` | 返回任务生成资源 |

创建任务示例：

```json
{
  "spaceId": 1,
  "providerId": 1,
  "taskType": "resource_generation",
  "title": "生成数据库系统复习计划",
  "subject": "数据库系统",
  "resourceType": "plan",
  "inputParams": {
    "days": 14,
    "resourceTypes": ["学习计划", "习题集", "知识图谱"]
  }
}
```


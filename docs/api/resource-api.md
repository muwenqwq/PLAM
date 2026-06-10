# 资源生成 API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/resources/generate` | 生成并保存资源 |
| GET | `/api/resources` | 分页查询资源 |
| GET | `/api/resources/{id}` | 查询资源详情 |
| PUT | `/api/resources/{id}` | 更新资源内容 |
| DELETE | `/api/resources/{id}` | 删除资源 |
| POST | `/api/resources/{id}/export/markdown` | 导出 Markdown |
| POST | `/api/resources/{id}/graph` | 基于已有资源生成 Mermaid 知识图谱 |

# 知识库 API

所有接口均要求登录，统一返回 `Result<T>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/knowledge/files` | 创建知识库文件元数据 |
| GET | `/api/knowledge/files` | 分页查询知识库文件 |
| GET | `/api/knowledge/files/{id}` | 查询文件详情 |
| DELETE | `/api/knowledge/files/{id}` | 删除文件 |
| POST | `/api/knowledge/files/{id}/index` | 调用 Python RAG Mock 并保存 chunk |
| POST | `/api/knowledge/search` | 检索知识库 |
| POST | `/api/knowledge/qa` | 知识库问答 |

## 搜索请求示例

```json
{
  "spaceId": 1,
  "fileIds": [1],
  "query": "如何复习数据库索引",
  "topK": 5
}
```

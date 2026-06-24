# 知识库 API

所有接口均要求登录，统一返回 `Result<T>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/knowledge/files` | 创建知识库文件元数据 |
| POST | `/api/knowledge/files/upload` | 上传知识库文件并自动调用 AI 分片索引 |
| GET | `/api/knowledge/files` | 分页查询知识库文件 |
| GET | `/api/knowledge/files/{id}` | 查询文件详情 |
| DELETE | `/api/knowledge/files/{id}` | 删除文件 |
| POST | `/api/knowledge/files/{id}/index` | 调用 Python RAG 重新生成并保存 chunk |
| POST | `/api/knowledge/search` | 检索知识库 |
| POST | `/api/knowledge/qa` | 知识库问答 |

## 上传请求示例

`POST /api/knowledge/files/upload` 使用 `multipart/form-data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `spaceId` | number | 学习空间 ID |
| `file` | file | 支持 `txt`、`md`、`csv`、`json`、`xml`、`html`、`docx`、`pptx` 等文本资料 |

后端会保存原文件，解析正文，并把用户默认模型配置传给 Python AI 服务。若默认模型是 DeepSeek/OpenAI-compatible 且配置有效，AI 服务优先使用真实模型生成知识片段；否则回退到稳定规则切片。

## 搜索请求示例

```json
{
  "spaceId": 1,
  "fileIds": [1],
  "query": "如何复习数据库索引",
  "topK": 5
}
```

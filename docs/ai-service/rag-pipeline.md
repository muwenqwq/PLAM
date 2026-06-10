# Python AI 服务 RAG Mock 设计

## 路由

- `POST /ai/rag/index`
- `POST /ai/rag/search`
- `POST /ai/rag/qa`

## 组件

- `parser.py`：解析输入文本或根据文件名生成 Mock 文本。
- `chunker.py`：生成稳定 chunk、hash 和 embedding 引用。
- `embedding.py`：提供稳定哈希和 Mock 相似度。
- `retriever.py`：返回带来源和分数的检索结果。

## 扩展点

后续接入真实 RAG 时，可替换 parser、embedding 和 retriever；Java 侧接口和数据库结构无需大改。

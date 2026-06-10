# Mock RAG 流程说明

## 当前流程

1. Java 创建 `knowledge_file` 元数据。
2. Java 调用 Python `/ai/rag/index`。
3. Python 使用 Mock parser 读取 `source_text` 或文件名生成演示文本。
4. Python chunker 返回 3 到 5 个切片。
5. Java 保存 `knowledge_chunk`。
6. 搜索和问答接口由 Python Mock retriever 返回带 `source`、`chunkText`、`score` 的结果。

## 设计边界

Python AI 服务只负责 AI/RAG 计算，不直接访问 MySQL。所有业务数据写入必须经过 Java 后端，便于权限控制、审计和答辩说明。

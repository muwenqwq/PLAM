# 知识库模块说明

## 模块定位

知识库模块负责保存用户在学习空间中的资料元数据、资料切片索引状态和 Mock RAG 检索结果。当前阶段暂不实现真实 multipart 文件上传，先提供文件元数据创建、索引、检索和问答接口，保证后端学习闭环可演示。

## 数据模型

- `knowledge_file`：保存文件名称、类型、大小、存储路径、解析状态和切片数量。
- `knowledge_chunk`：保存文件解析后的文本片段、片段哈希、token 估算、metadata 和 embedding 引用。

## 业务规则

1. 所有知识库文件必须归属于当前登录用户。
2. 文件必须绑定学习空间，后端通过 `LearningSpaceService.assertOwned` 校验空间归属。
3. 索引时由 Java 调用 Python `/ai/rag/index`，Python 返回 Mock chunk，Java 负责写入 MySQL。
4. 搜索和问答由 Java 调用 Python `/ai/rag/search`、`/ai/rag/qa`，Python 不直接连接 MySQL。

## 后续扩展

后续可在不改变主表结构的前提下，将 `embedding_ref` 指向 Chroma、Milvus 或其他向量数据库记录，并将 `storage_path` 对接真实对象存储。

# 知识库模块说明

## 模块定位

知识库模块负责保存用户在学习空间中的资料文件、资料切片索引状态和 RAG 检索结果。当前已支持 multipart 文件上传，后端解析正文后调用 Python AI 服务生成知识片段，并将切片写入 MySQL，保证资料上传、AI 分片、查看切片、检索和问答链路可演示。

## 数据模型

- `knowledge_file`：保存文件名称、类型、大小、存储路径、解析状态和切片数量。
- `knowledge_chunk`：保存文件解析后的文本片段、片段哈希、token 估算、metadata 和 embedding 引用。

## 业务规则

1. 所有知识库文件必须归属于当前登录用户。
2. 文件必须绑定学习空间，后端通过 `LearningSpaceService.assertOwned` 校验空间归属。
3. 上传时后端保存原文件，支持文本类资料以及 `docx`、`pptx` 基础正文抽取。
4. 索引时由 Java 调用 Python `/ai/rag/index`，并传入当前用户默认模型配置。Python 优先使用真实模型生成知识片段，失败或 Mock 模型时回退到稳定规则切片，Java 负责写入 MySQL。
5. 搜索和问答由 Java 调用 Python `/ai/rag/search`、`/ai/rag/qa`，Python 不直接连接 MySQL。

## 后续扩展

后续可在不改变主表结构的前提下，将 `embedding_ref` 指向 Chroma、Milvus 或其他向量数据库记录，并将 `storage_path` 对接真实对象存储。

# Python AI 服务 RAG 设计

## 路由

- `POST /ai/rag/index`
- `POST /ai/rag/search`
- `POST /ai/rag/qa`

## 组件

- `parser.py`：解析 Java 后端传入的正文文本，缺省时根据文件名生成演示文本。
- `chunker.py`：生成稳定 chunk、hash 和 embedding 引用，作为 Mock 或真实模型失败时的回退。
- `embedding.py`：提供稳定哈希和 Mock 相似度。
- `retriever.py`：返回带来源和分数的检索结果。

## 真实模型切片

`POST /ai/rag/index` 支持接收 `model_config`。当模型配置为 DeepSeek、OpenAI-compatible、Qwen 或 Custom 且包含 `base_url`、`api_key`、`model_name` 时，AI 服务会优先请求真实模型输出结构化 JSON 片段。若模型不可用、返回不可解析或用户使用 Mock 模型，则自动回退到规则切片，保证演示链路不断。

## 扩展点

后续接入真实 RAG 时，可替换 parser、embedding 和 retriever；Java 侧接口和数据库结构无需大改。

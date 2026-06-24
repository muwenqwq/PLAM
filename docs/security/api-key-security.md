# API Key 安全策略

## 1. 存储原则

系统不保存明文 API Key。用户提交的 API Key 在 Java 后端使用 AES-GCM 加密后写入 `ai_model_provider.api_key_encrypted`。

## 2. 展示原则

前端只展示 `api_key_masked`，例如：

```text
sk-a****1234
```

接口响应不返回 `api_key_encrypted`，也不返回明文 API Key。

## 3. 调用原则

Java 后端在调用 Python AI 服务前临时解密 API Key，并作为模型配置的一部分传给 Python。Python 服务不持久化 API Key，不连接 MySQL。

## 4. 后续增强

生产阶段建议把加密主密钥迁移到环境变量、KMS 或部署平台密钥管理服务，并增加 API Key 轮换、访问审计和敏感日志脱敏。

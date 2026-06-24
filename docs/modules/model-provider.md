# AI 模型配置中心说明

## 1. 模块定位

AI 模型配置中心负责管理当前用户可用的 AI Provider。Java 后端保存配置并调用 Python AI 服务，前端只访问 Java 接口，不直接接触 Python 服务地址或真实 API Key。

## 2. 功能范围

- 新增、修改、删除模型配置。
- 分页查询当前用户模型配置。
- 查询详情、查询默认模型、设置默认模型。
- 调用 Python AI 服务测试模型连接。
- Mock 模型无需 API Key，可以直接演示成功。
- OpenAI-compatible 配置支持 `base_url`、`model_name`、`api_key`，真实外部调用留到后续安全联调阶段。

## 3. API Key 策略

`api_key_encrypted` 只保存在数据库中，不返回前端。当前实现使用 AES-GCM 加密，密钥来自后端配置。前端展示只使用 `api_key_masked`，例如 `sk-a****1234`。

## 4. 默认模型策略

同一用户只应有一个默认模型。设置默认模型时，Service 层会取消当前用户其他模型的默认状态。如果用户没有任何模型配置，聊天和 Agent 任务会自动使用系统 Mock 配置，不强制写入数据库。


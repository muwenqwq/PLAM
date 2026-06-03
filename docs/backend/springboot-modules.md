# Spring Boot 模块说明

## 1. 当前已完成模块

### 1.1 启动模块

- 文件：`backend/src/main/java/com/edustudio/EduStudioApplication.java`
- 职责：Spring Boot 应用入口。

### 1.2 统一响应模块

- 文件：`common/api/Result.java`
- 文件：`common/api/PageResult.java`
- 文件：`common/api/ResultCode.java`

`Result<T>` 包含 `code`、`message`、`data`、`timestamp`、`success`。  
`PageResult<T>` 包含 `records`、`total`、`pageNum`、`pageSize`、`pages`。

### 1.3 异常处理模块

- 文件：`common/exception/BusinessException.java`
- 文件：`common/exception/GlobalExceptionHandler.java`

当前处理：

- 业务异常
- 参数校验异常
- 请求参数缺失
- 请求体格式错误
- 请求方法不支持
- 通用异常

异常响应不向前端返回完整堆栈。

### 1.4 MyBatis-Plus 配置模块

- 文件：`common/config/MybatisPlusConfig.java`
- 文件：`common/config/MetaObjectHandlerConfig.java`

当前配置：

- MySQL 分页插件。
- Mapper 扫描。
- `deleted` 逻辑删除配置。
- `createdAt`、`updatedAt` 自动填充预留。

### 1.5 接口文档模块

- 文件：`common/config/OpenApiConfig.java`

提供 Swagger OpenAPI 信息，并说明 `EduAgent Studio / LearnAgent-A3` 命名过渡。

### 1.6 跨域模块

- 文件：`common/config/CorsConfig.java`

允许本地开发环境中的 `localhost` 和 `127.0.0.1` 前端访问 `/api/**`。

### 1.7 健康检查模块

- 文件：`module/health/controller/HealthController.java`

提供：

- `GET /api/health`
- `GET /api/health/db`

## 2. 后续模块计划

第 3 阶段：认证安全模块。

- `module/auth`
- `module/user`
- `module/role`
- `common/security`

第 4 阶段：核心业务 CRUD。

- 学习空间
- 用户画像
- 学习偏好
- 对话
- 知识库元数据
- 智能体任务
- 生成资源
- 学习路径
- 测验
- 报告
- 操作日志

第 5 阶段：AI 模型配置中心。

- 模型配置 CRUD
- API Key 加密与脱敏
- Mock 和真实模型连接测试

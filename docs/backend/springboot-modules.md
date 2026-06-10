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

### 1.8 认证安全模块

- 文件：`module/auth/controller/AuthController.java`
- 文件：`module/auth/service/AuthService.java`
- 文件：`module/user/controller/UserController.java`
- 文件：`module/user/entity/SysUser.java`
- 文件：`module/role/entity/SysRole.java`
- 文件：`module/role/entity/SysUserRole.java`
- 文件：`common/security/SecurityConfig.java`
- 文件：`common/security/JwtTokenUtil.java`
- 文件：`common/security/JwtAuthenticationFilter.java`

当前提供：

- 用户注册。
- 用户登录。
- BCrypt 密码加密与校验。
- JWT 生成与解析。
- 当前登录用户获取。
- 基础角色查询。
- `/api/**` 默认鉴权。
- 认证失败和无权限失败统一 JSON 响应。

### 1.9 学习空间模块

- 文件：`module/learningspace/controller/LearningSpaceController.java`
- 文件：`module/learningspace/service/LearningSpaceService.java`
- 文件：`module/learningspace/entity/LearningSpace.java`

当前提供：

- 创建、修改、逻辑删除学习空间。
- 当前用户学习空间分页查询。
- 学习空间详情、默认空间查询、设置默认空间。
- 学习空间统计摘要。
- 所有业务查询均通过 `LoginUserHolder` 获取当前用户，并携带 `user_id` 条件。

删除默认学习空间时，后端会在用户剩余空间中选择最近更新的一条作为新的默认空间；若没有剩余空间，则用户暂时没有默认空间。

### 1.10 用户画像模块

- 文件：`module/profile/controller/UserProfileController.java`
- 文件：`module/profile/service/UserProfileService.java`
- 文件：`module/profile/entity/UserProfile.java`

当前提供：

- 查询、创建或更新当前用户全局画像。
- 查询、创建或更新学习空间画像。
- 学习目标、学科方向、基础水平、薄弱点、可用学习时间和输出风格维护。
- 空间画像写入前会校验学习空间属于当前用户。

全局画像使用 `space_id = 0` 存储；接口响应中全局画像的 `spaceId` 返回 `null`，空间画像返回实际学习空间 ID。

### 1.11 学习偏好模块

- 文件：`module/profile/controller/LearningPreferenceController.java`
- 文件：`module/profile/service/LearningPreferenceService.java`
- 文件：`module/profile/entity/LearningPreference.java`

当前提供：

- 查询当前用户学习偏好。
- 创建或更新当前用户学习偏好。
- 支持资源类型、内容长度、难度、输出语言、输出风格、知识图谱、测验和复习计划开关。

偏好不存在时，后端返回默认偏好：Markdown 输出、中等难度、中文、启用知识图谱、启用测验和启用复习计划。默认偏好不会强制写入数据库，用户主动更新时再执行 upsert。

### 1.12 AI 模型配置中心

- 文件：`module/modelprovider/controller/ModelProviderController.java`
- 文件：`module/modelprovider/service/ModelProviderService.java`

当前提供模型配置 CRUD、默认模型设置、模型连接测试、API Key 加密存储和脱敏展示。用户没有配置模型时，后端会自动使用 Mock 模型配置作为 AI 调用兜底。

### 1.13 智能对话模块

- 文件：`module/chat/controller/ChatController.java`
- 文件：`module/chat/service/ChatService.java`

当前提供会话创建、会话列表、消息列表、发送消息和意图识别。发送消息时 Java 保存用户消息，调用 Python AI 服务，再保存 AI 回复。

### 1.14 多智能体任务与资源模块

- 文件：`module/agent/controller/AgentTaskController.java`
- 文件：`module/resource/controller/GeneratedResourceController.java`

当前采用同步执行策略：Java 保存任务，调用 Python Mock Agent，保存执行步骤和生成资源。后续可扩展为异步队列和任务进度推送。

## 2. 后续模块计划

第 4 阶段后续部分：核心业务 CRUD。

- 知识库元数据
- 学习路径
- 测验
- 报告
- 操作日志

第 5 阶段：AI 模型配置中心。

- 模型配置 CRUD
- API Key 加密与脱敏
- Mock 和真实模型连接测试

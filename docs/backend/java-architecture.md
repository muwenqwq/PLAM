# Java 后端架构说明

## 1. 命名过渡说明

当前工程实现名称使用 `EduAgent Studio`，前期 `SRS.md` 和 `URD.md` 使用 `LearnAgent-A3`。两者是同一项目在不同阶段的命名：`LearnAgent-A3` 强调中国软件杯 A3 赛题来源，`EduAgent Studio` 强调最终软件产品形态。第 2 阶段暂不大改 SRS/URD，后续文档整理阶段再统一命名。

## 2. 后端定位

Java 后端是系统主后端，负责业务流程、数据库读写、接口响应、日志、安全和 AI 服务编排。Python AI 服务只提供 AI 能力，前端不直接访问 Python AI 服务。

## 3. 技术栈

- Java 21
- Spring Boot 3.x
- Maven
- Spring Web
- Validation
- MyBatis-Plus
- MySQL 8
- Redis 配置预留
- Lombok
- Knife4j / Swagger OpenAPI

本阶段未启用 Spring Security 和 JWT，后续认证安全阶段再引入完整鉴权。

## 4. 包结构

```text
com.edustudio
├── EduStudioApplication
├── common
│   ├── api
│   ├── config
│   ├── exception
│   ├── security
│   └── utils
├── integration
│   └── ai
└── module
    ├── health
    ├── auth
    ├── user
    ├── role
    ├── learningspace
    ├── profile
    ├── modelprovider
    ├── chat
    ├── agent
    ├── knowledge
    ├── resource
    ├── learningpath
    ├── quiz
    ├── report
    └── log
```

## 5. 分层原则

- Controller 只处理 HTTP 请求和响应。
- Service 处理业务逻辑、用户数据隔离和事务。
- Mapper 负责数据库访问。
- Entity、DTO、VO 分离。
- 统一使用 `Result<T>` 返回接口结果。
- 分页统一使用 `PageResult<T>`。
- AI 服务调用统一放在 `integration.ai`。

## 6. 数据库策略

数据库字段使用下划线命名，Java Entity 后续使用驼峰字段并通过 MyBatis-Plus 自动映射。例如：

- `created_at` -> `createdAt`
- `updated_at` -> `updatedAt`
- `api_key_encrypted` -> `apiKeyEncrypted`

逻辑删除字段统一为 `deleted`，未删除为 `0`，已删除为 `1`。时间字段由 `MetaObjectHandlerConfig` 预留自动填充。

## 7. 健康检查

本阶段提供两个基础接口：

- `GET /api/health`：返回应用名、Java 版本、当前 profile 和当前时间。
- `GET /api/health/db`：尝试获取数据库连接，连接失败时返回失败信息，但不影响应用基础启动。

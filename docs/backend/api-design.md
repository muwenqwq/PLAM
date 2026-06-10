# 后端 API 设计说明

## 1. 接口前缀

后端接口统一使用 `/api` 前缀。当前阶段已开放健康检查接口和认证安全接口，后续业务模块继续沿用该前缀。

## 2. 统一响应

所有接口统一返回：

```json
{
  "code": "200",
  "message": "请求成功",
  "data": {},
  "timestamp": "2026-06-03T10:00:00",
  "success": true
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `code` | 业务响应码 |
| `message` | 响应说明 |
| `data` | 响应数据 |
| `timestamp` | 响应生成时间 |
| `success` | 是否成功 |

## 3. 分页响应

分页数据放入 `Result<PageResult<T>>`：

```json
{
  "code": "200",
  "message": "请求成功",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 0
  },
  "timestamp": "2026-06-03T10:00:00",
  "success": true
}
```

## 4. 健康检查接口

### 4.1 基础健康检查

```http
GET /api/health
```

返回示例：

```json
{
  "code": "200",
  "message": "请求成功",
  "data": {
    "status": "UP",
    "applicationName": "eduagent-studio-backend",
    "javaVersion": "21.0.10",
    "activeProfiles": ["dev"],
    "currentProfile": "dev",
    "currentTime": "2026-06-03T10:00:00"
  },
  "timestamp": "2026-06-03T10:00:00",
  "success": true
}
```

### 4.2 数据库健康检查

```http
GET /api/health/db
```

MySQL 启动时返回 `connected = true`。MySQL 未启动时返回 `connected = false` 和失败信息，但接口本身仍然返回统一成功响应，便于前端和开发者调试。

## 5. 认证接口

### 5.1 注册

```http
POST /api/auth/register
```

注册成功后返回用户基础信息和角色列表，不返回密码哈希。

### 5.2 登录

```http
POST /api/auth/login
```

登录成功后返回 `Bearer` 类型 JWT、过期时间、用户信息和角色列表。

### 5.3 当前用户

```http
GET /api/auth/me
Authorization: Bearer <token>
```

返回当前登录用户信息。

### 5.4 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

当前阶段只返回退出成功，Redis 黑名单后续扩展。

### 5.5 用户模块当前用户

```http
GET /api/users/me
Authorization: Bearer <token>
```

返回当前登录用户资料，便于后续扩展用户资料维护。

## 6. 核心业务接口

以下接口均需要 `Authorization: Bearer <token>`，后端通过当前登录用户隔离数据，不允许访问其他用户资源。

### 6.1 学习空间

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/learning-spaces` | 创建学习空间 |
| `GET` | `/api/learning-spaces` | 分页查询当前用户学习空间 |
| `GET` | `/api/learning-spaces/default` | 查询默认学习空间 |
| `GET` | `/api/learning-spaces/{id}` | 查询学习空间详情 |
| `PUT` | `/api/learning-spaces/{id}` | 更新学习空间 |
| `DELETE` | `/api/learning-spaces/{id}` | 逻辑删除学习空间 |
| `POST` | `/api/learning-spaces/{id}/default` | 设置默认学习空间 |
| `GET` | `/api/learning-spaces/{id}/summary` | 查询学习空间摘要 |

创建第一个学习空间时会自动设为默认空间。删除默认空间后，后端会把用户剩余空间中最近更新的一条设为默认空间；如果没有剩余空间，则默认空间为空。

### 6.2 用户画像

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/profiles/me` | 查询当前用户全局画像 |
| `PUT` | `/api/profiles/me` | 创建或更新当前用户全局画像 |
| `GET` | `/api/profiles/space/{spaceId}` | 查询学习空间画像 |
| `PUT` | `/api/profiles/space/{spaceId}` | 创建或更新学习空间画像 |

全局画像不存在时返回空画像结构，`status` 为 `incomplete`。空间画像读写前会校验该学习空间属于当前登录用户。

### 6.3 学习偏好

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/preferences/me` | 查询当前用户学习偏好 |
| `PUT` | `/api/preferences/me` | 创建或更新当前用户学习偏好 |

学习偏好不存在时返回默认偏好：Markdown 输出、中等难度、中文、启用知识图谱、启用测验和启用复习计划。

### 6.4 AI 模型配置

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/model-providers` | 创建模型配置 |
| `GET` | `/api/model-providers` | 分页查询模型配置 |
| `GET` | `/api/model-providers/default` | 查询默认模型 |
| `GET` | `/api/model-providers/{id}` | 查询模型详情 |
| `PUT` | `/api/model-providers/{id}` | 更新模型配置 |
| `DELETE` | `/api/model-providers/{id}` | 逻辑删除模型配置 |
| `POST` | `/api/model-providers/{id}/default` | 设置默认模型 |
| `POST` | `/api/model-providers/{id}/test` | 测试模型连接 |

### 6.5 智能对话

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/chat/conversations` | 创建会话 |
| `GET` | `/api/chat/conversations` | 分页查询会话 |
| `GET` | `/api/chat/conversations/{id}` | 查询详情 |
| `GET` | `/api/chat/conversations/{id}/messages` | 查询消息 |
| `POST` | `/api/chat/conversations/{id}/messages` | 发送消息并保存 AI 回复 |
| `POST` | `/api/chat/intent` | 识别消息意图 |

### 6.6 Agent 任务与生成资源

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/agent-tasks` | 创建并同步执行 Agent 任务 |
| `GET` | `/api/agent-tasks` | 分页查询任务 |
| `GET` | `/api/agent-tasks/{id}` | 查询任务详情 |
| `GET` | `/api/agent-tasks/{id}/steps` | 查询执行步骤 |
| `POST` | `/api/agent-tasks/{id}/rerun` | 重新执行任务 |
| `POST` | `/api/agent-tasks/{id}/save-resource` | 返回任务生成资源 |
| `GET` | `/api/resources` | 分页查询生成资源 |
| `GET` | `/api/resources/{id}` | 查询生成资源详情 |

## 7. 错误响应

参数错误：

```json
{
  "code": "400",
  "message": "请求参数错误",
  "data": null,
  "timestamp": "2026-06-03T10:00:00",
  "success": false
}
```

服务内部错误：

```json
{
  "code": "500",
  "message": "服务内部错误",
  "data": null,
  "timestamp": "2026-06-03T10:00:00",
  "success": false
}
```

后端不会把完整异常堆栈直接返回给前端。

未认证：

```json
{
  "code": "401",
  "message": "请先登录",
  "data": null,
  "timestamp": "2026-06-03T10:00:00",
  "success": false
}
```

## 8. 接口文档地址

- Swagger UI：`/swagger-ui.html`
- Knife4j：`/doc.html`
- OpenAPI JSON：`/v3/api-docs`
# 冲刺包 2 API 补充说明

本阶段新增知识库、资源生成、学习路径、测验掌握度和学习报告接口。所有业务接口均要求登录，均使用 `/api` 前缀和统一 `Result<T>` / `PageResult<T>` 响应。

- 知识库：`/api/knowledge/files`、`/api/knowledge/search`、`/api/knowledge/qa`
- 资源生成：`/api/resources/generate`、`/api/resources/{id}/graph`、`/api/resources/{id}/export/markdown`
- 学习路径：`/api/learning-paths/generate`、`/api/learning-paths/today`、`/api/learning-path-items/{id}/status`
- 测验掌握度：`/api/quizzes/generate`、`/api/quizzes/{id}/submit`、`/api/mastery/me`
- 学习报告：`/api/reports/overview`、`/api/reports/generate`、`/api/reports/{id}/export`
# 前端对接补充

Vue 前端统一通过 `frontend/src/api/request.ts` 调用 Java 后端 `/api/**`。Axios 请求拦截器会自动添加 `Authorization: Bearer <token>`，响应拦截器会解包统一 `Result<T>`，并在 401 时清理登录状态跳转登录页。前端不直接调用 Python AI 服务。

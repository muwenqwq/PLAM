# 后端 API 设计说明

## 1. 接口前缀

后端接口统一使用 `/api` 前缀。当前阶段只开放健康检查接口，后续业务模块继续沿用该前缀。

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

## 5. 错误响应

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

## 6. 接口文档地址

- Swagger UI：`/swagger-ui.html`
- Knife4j：`/doc.html`
- OpenAPI JSON：`/v3/api-docs`

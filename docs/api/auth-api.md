# 认证接口说明

## 1. 注册

```http
POST /api/auth/register
Content-Type: application/json
```

请求示例：

```json
{
  "username": "student01",
  "password": "123456",
  "nickname": "演示学生",
  "email": "student01@example.com"
}
```

规则：

- `username` 必填，且不能重复。
- `password` 必填，长度 6 到 64 位。
- `nickname` 可选，未填写时默认使用用户名。
- `email` 可选，填写时必须符合邮箱格式。
- 返回用户基础信息，不返回密码哈希。

## 2. 登录

```http
POST /api/auth/login
Content-Type: application/json
```

请求示例：

```json
{
  "username": "demo_student",
  "password": "123456"
}
```

成功响应数据包含：

- `tokenType`：固定为 `Bearer`。
- `accessToken`：JWT Token。
- `expiresAt`：Token 过期时间。
- `user`：当前用户基础信息和角色列表。

## 3. 当前登录用户

```http
GET /api/auth/me
Authorization: Bearer <token>
```

返回当前登录用户信息和角色列表，不返回密码哈希。

## 4. 退出登录

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

当前阶段退出登录只清理本次请求上下文并返回成功。Redis Token 黑名单在后续安全增强阶段完善。

## 5. 用户模块当前用户

```http
GET /api/users/me
Authorization: Bearer <token>
```

该接口与 `/api/auth/me` 返回结构保持一致，但归属于用户模块，便于后续扩展用户资料修改。

## 6. 常见错误

未登录或 Token 缺失：

```json
{
  "code": "401",
  "message": "请先登录",
  "data": null,
  "success": false
}
```

Token 无效或过期：

```json
{
  "code": "401",
  "message": "登录状态无效或已过期",
  "data": null,
  "success": false
}
```

参数校验失败：

```json
{
  "code": "400",
  "message": "username: 用户名不能为空",
  "data": null,
  "success": false
}
```

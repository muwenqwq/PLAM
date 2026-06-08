# 认证安全模块说明

## 1. 模块定位

认证安全模块负责用户注册、登录、JWT 令牌签发、当前用户识别和基础角色查询。当前阶段只接入 `sys_user`、`sys_role`、`sys_user_role` 三张表，不实现学习空间、模型配置、对话、多智能体等业务模块。

Java Spring Boot 是主后端，前端后续只调用 Java 后端接口；Python AI 服务不参与认证流程。

## 2. 认证流程

1. 用户调用 `POST /api/auth/register` 完成注册，后端使用 BCrypt 加密密码，并写入 `sys_user.password_hash`。
2. 用户调用 `POST /api/auth/login` 提交用户名和密码。
3. 后端校验用户是否存在、状态是否为 `active`、密码是否匹配。
4. 登录成功后，后端查询用户角色，生成 JWT Token。
5. 前端后续请求在请求头中携带 `Authorization: Bearer <token>`。
6. `JwtAuthenticationFilter` 解析 Token，加载用户和角色，写入 Spring Security 上下文。
7. Controller 或 Service 可通过 `LoginUserHolder` 获取当前登录用户。

## 3. 密码加密

系统使用 Spring Security 提供的 `BCryptPasswordEncoder`。

- 数据库只保存 BCrypt 哈希。
- 不保存明文密码。
- VO 和接口响应不返回 `password_hash`。
- 演示账号明文密码统一为 `123456`，仅用于本地演示和答辩展示。

## 4. JWT 设计

JWT 配置项位于 `application.yml`：

```yaml
eduagent:
  jwt:
    secret: ${JWT_SECRET:eduagent-studio-dev-secret-change-in-production}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:1440}
    issuer: ${JWT_ISSUER:eduagent-studio}
```

Token 至少包含：

- `userId`
- `username`
- `issuer`
- `issuedAt`
- `expiration`

开发环境默认有效期为 1440 分钟。生产环境必须通过环境变量替换 `JWT_SECRET`。

## 5. 放行路径

当前阶段放行：

- `/api/auth/login`
- `/api/auth/register`
- `/api/health`
- `/api/health/db`
- `/doc.html`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/swagger-resources/**`
- `/configuration/**`
- `/webjars/**`
- `/favicon.ico`

## 6. 受保护路径

除放行路径外，其他 `/api/**` 默认需要认证。当前已接入：

- `GET /api/auth/me`
- `POST /api/auth/logout`
- `GET /api/users/me`

## 7. 演示账号

导入 `seed.sql` 后可使用以下账号登录：

| 用户名 | 明文密码 | 角色 |
|---|---|---|
| `demo_admin` | `123456` | `ADMIN` |
| `demo_teacher` | `123456` | `TEACHER` |
| `demo_student` | `123456` | `STUDENT` |

## 8. 后续扩展

后续阶段可扩展：

- Refresh Token。
- Redis JWT 黑名单。
- 用户密码修改和重置。
- 登录失败次数限制。
- 角色权限细粒度控制。
- 操作日志记录登录、退出和认证失败事件。

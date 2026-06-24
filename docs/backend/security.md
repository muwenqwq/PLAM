# 后端安全设计说明

## 1. 安全边界

第 3 阶段采用 Spring Security + JWT 实现基础认证。Java 后端负责所有用户身份校验和权限控制，前端只保存并携带后端签发的 JWT，Python AI 服务不参与认证。

## 2. 核心组件

| 组件 | 职责 |
|---|---|
| `SecurityConfig` | 配置放行路径、受保护路径、无状态会话和过滤器 |
| `JwtTokenUtil` | 生成、解析和校验 JWT |
| `JwtAuthenticationFilter` | 从请求头读取 Token 并写入安全上下文 |
| `CustomUserDetailsService` | 根据用户名加载用户和角色 |
| `UserPrincipal` | Spring Security 当前用户对象 |
| `LoginUserHolder` | 获取当前登录用户 |
| `AuthenticationEntryPointImpl` | 统一未认证响应 |
| `AccessDeniedHandlerImpl` | 统一无权限响应 |

## 3. 密码策略

密码使用 BCrypt 加密。数据库字段为 `sys_user.password_hash`，接口响应不暴露该字段。演示账号明文密码只在文档和 `seed.sql` 注释中说明，用于本地演示。

## 4. JWT 策略

JWT 使用 HS256 签名算法，密钥来自 `eduagent.jwt.secret`。开发环境提供默认值，生产环境必须使用环境变量覆盖。

Token 中包含 `userId` 和 `username`，用于过滤器快速定位用户。过滤器仍会重新加载数据库用户和角色，以便用户被禁用或角色变更后能够及时生效。

## 5. 当前放行路径

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

其他 `/api/**` 默认需要认证。

## 6. 当前限制

当前阶段只实现认证和基础角色查询，尚未实现：

- Redis JWT 黑名单。
- Refresh Token。
- 角色到权限点的细粒度授权。
- 登录失败次数限制。
- 多端登录控制。
- 密码重置和邮箱验证。

这些能力已通过包结构、Redis 配置和 Spring Security 扩展点预留。

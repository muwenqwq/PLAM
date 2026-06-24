# EduAgent Studio Java 后端

本目录是 EduAgent Studio / LearnAgent-A3 的 Java 主后端工程。`EduAgent Studio` 是当前工程实现名称，`LearnAgent-A3` 是前期 URD/SRS 中使用的赛题文档名称；两者在本项目中指向同一系统，后续文档整理阶段再统一命名。

## 技术栈

- Java 21
- Spring Boot 3.x
- Maven
- MyBatis-Plus
- MySQL 8
- Redis 配置预留
- Lombok
- Validation
- Knife4j / Swagger OpenAPI

## 当前阶段范围

第 2 阶段完成后端基础工程，第 3 阶段已接入认证安全模块：

- 启动类
- 统一响应 `Result<T>`
- 分页响应 `PageResult<T>`
- 统一异常处理
- MyBatis-Plus 分页、逻辑删除和自动填充配置
- OpenAPI / Knife4j 配置
- CORS 配置
- 健康检查接口
- Spring Security 无状态鉴权
- JWT 生成与解析
- 用户注册、登录、退出和当前用户接口
- BCrypt 密码加密与校验
- 基础角色查询

当前仍不实现模型配置、学习空间、对话、多智能体等业务模块。

## 启动方式

```bash
cd backend
mvn spring-boot:run
```

如果本地 MySQL 未启动，应用应仍可启动；访问 `/api/health/db` 时会返回数据库连接失败信息，便于调试。

## 访问地址

- 健康检查：`http://localhost:8080/api/health`
- 数据库检查：`http://localhost:8080/api/health/db`
- 登录接口：`http://localhost:8080/api/auth/login`
- 当前用户：`http://localhost:8080/api/auth/me`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Knife4j：`http://localhost:8080/doc.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 演示账号

导入 `seed.sql` 后可使用：

| 用户名 | 明文密码 | 角色 |
|---|---|---|
| `demo_admin` | `123456` | `ADMIN` |
| `demo_teacher` | `123456` | `TEACHER` |
| `demo_student` | `123456` | `STUDENT` |

## 数据库初始化

先执行根目录下的数据库脚本：

```bash
mysql -u root -p < backend/src/main/resources/sql/schema.sql
mysql -u root -p eduagent_studio < backend/src/main/resources/sql/seed.sql
```

默认开发配置连接 `localhost:3306/eduagent_studio`。可以通过环境变量覆盖：

- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_DATABASE`

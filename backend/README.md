# LearnAgent-A3 后端

Java Spring Boot 主后端，负责业务逻辑、数据持久化和 AI 服务调度。

## 技术栈

| 技术 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.2.x | Web 框架 |
| MyBatis-Plus | 3.5.x | ORM |
| MySQL | 8.x | 业务数据库 |
| Lombok | — | 简化代码 |
| JDK | 17 | 运行环境 |
| Maven | 3.8+ | 构建工具 |

## 快速启动

### 1. 配置环境变量

复制 `.env.example` 为 `.env`，填入数据库密码和 API Key：

```bash
cp .env.example .env
# 编辑 .env 填入真实值
```

### 2. 初始化数据库

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p learnagent_a3 < database/seed.sql
```

### 3. 启动后端

```bash
# 方式1：Maven 命令行
mvn spring-boot:run

# 方式2：IDEA 中运行 BackendApplication.java
#   设置 Active profiles = dev
#   在 Environment variables 中加载 .env 文件
```

启动后访问 `http://localhost:8080/api/health` 验证。

## 项目结构

```
src/main/java/com/learnagent/
├── BackendApplication.java          # 启动类
├── controller/                      # 接口层（/api/*）
│   ├── HealthController.java
│   ├── ProfileController.java
│   ├── ResourceController.java
│   ├── StudyPlanController.java
│   ├── TutorController.java
│   ├── AssessmentController.java
│   ├── AnalyticsController.java
│   └── AgentRunController.java
├── service/                         # 业务逻辑层
│   ├── ProfileService.java
│   ├── ResourceService.java
│   ├── StudyPlanService.java
│   ├── TutorService.java
│   ├── AssessmentService.java
│   └── AgentRunService.java
├── mapper/                          # MyBatis-Plus Mapper
├── entity/                          # 数据库实体
├── dto/                             # 请求/响应 DTO
│   ├── request/
│   └── response/
├── client/                          # Python AI 服务调用
│   ├── AiServiceClient.java
│   └── dto/
├── config/                          # 配置类
│   ├── WebConfig.java
│   ├── MyBatisPlusConfig.java
│   └── AiServiceProperties.java
└── exception/                       # 异常处理
    ├── ErrorCode.java
    ├── BizException.java
    └── GlobalExceptionHandler.java
```

## 分层架构

```
Controller（接口层）
  ├── 参数校验（@Valid）
  ├── 调用 Service
  └── 包装 ApiResult 返回

Service（业务层）
  ├── 创建 ai_task 记录
  ├── 调用 AiServiceClient 获取 AI 结果
  ├── 持久化到 MySQL
  ├── 保存 agent_run 记录
  └── 组装响应 DTO

Client（AI 调用层）
  └── RestTemplate 调用 Python /ai/* 接口

Mapper（数据层）
  └── MyBatis-Plus CRUD
```

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

## 接口清单

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| POST | `/api/profile/extract` | 生成学生画像 |
| GET | `/api/profile/{studentId}` | 查询最新画像 |
| GET | `/api/profile/{studentId}/history` | 查询画像历史 |
| POST | `/api/resources/generate` | 生成个性化资源 |
| GET | `/api/resources/{studentId}` | 查询资源列表 |
| GET | `/api/resources/detail/{resourceId}` | 查询资源详情 |
| POST | `/api/study-plan/generate` | 生成学习路径 |
| GET | `/api/study-plan/{studentId}` | 查询学习路径 |
| POST | `/api/tutor/ask` | 智能辅导提问 |
| POST | `/api/assessment/submit` | 提交练习结果 |
| GET | `/api/analytics/{studentId}` | 查询评估数据 |
| GET | `/api/agent-runs/{taskId}` | 查询 Agent 轨迹 |

## 相关文档

- `docs/SRS.md` — 软件需求规格
- `docs/SDD.md` — 软件设计说明
- `docs/IDD.md` — 接口设计说明
- `docs/DBDD.md` — 数据库设计说明

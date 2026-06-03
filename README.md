# 智学工坊 EduAgent Studio

智学工坊 EduAgent Studio 是面向“中国软件杯 A3 赛题：基于大模型的个性化资源生成与学习多智能体系统开发”的完整工程项目。项目定位为“Java 软件开发 + MySQL 数据库设计”实习与竞赛作品，目标是形成一个可运行、可演示、可部署、文档齐全、界面好用的智能学习平台。

## 项目目标

系统面向高校学生、教师和自学用户，围绕学习目标、知识基础、学习时间、兴趣偏好和历史学习表现，调用用户自定义的大模型 API，通过多智能体协作生成个性化学习资源。核心输出包括学习计划、知识图谱、课程讲义、复习提纲、习题集、答案解析、案例任务、项目实践、在线测验和学习报告。

本项目坚持以下边界：

1. Java Spring Boot 后端是系统主体，负责用户、权限、业务数据、数据库、接口、日志和核心流程。
2. MySQL 数据库设计是课程实习和答辩重点，表结构、索引、约束、典型 SQL 和范式分析必须完整。
3. Python AI 微服务只负责大模型调用、多智能体编排、RAG、Embedding 和文档解析，不替代 Java 主后端。
4. 前端只调用 Java 后端，不直接调用 Python AI 服务。
5. 系统必须支持无真实 API Key 的 Mock 模式，保证答辩现场可完整演示。

## 技术架构

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts、Markdown 渲染、Mermaid 渲染。
- Java 主后端：Java 21、Spring Boot 3、Spring Web、Spring Security、JWT、MyBatis-Plus、Maven、Validation、Knife4j / Swagger。
- 数据库：MySQL 8 作为主业务数据库，Redis 作为缓存、会话辅助和任务状态缓存。
- Python AI 微服务：FastAPI、Uvicorn、Pydantic、httpx、OpenAI-compatible client、MockLLMProvider、RAG 解析与检索模块。
- 部署：后续阶段提供 Docker、Docker Compose、`.env.example` 和启动脚本。

## 目录结构

```text
EduAgent-Studio/
├── README.md
├── backend/
│   └── src/main/resources/sql/
│       ├── schema.sql
│       └── seed.sql
├── frontend/
├── ai-service/
├── docs/
│   ├── project/
│   └── database/
├── demo/
├── scripts/
└── submit/
```

## 当前阶段

当前已进入第 0-1 阶段：

- 第 0 阶段：项目规划与目录初始化。
- 第 1 阶段：数据库完整设计。

本阶段重点交付 MySQL 8 可执行的 `schema.sql`、可演示的 `seed.sql`，以及项目需求、产品设计、技术架构、开发计划、创新点和数据库设计文档。

## 数据库初始化

本阶段数据库脚本位于：

- `backend/src/main/resources/sql/schema.sql`
- `backend/src/main/resources/sql/seed.sql`

推荐执行方式：

```bash
mysql -u root -p < backend/src/main/resources/sql/schema.sql
mysql -u root -p eduagent_studio < backend/src/main/resources/sql/seed.sql
```

演示账号和密码说明写在 `seed.sql` 文件头部注释中。演示数据只使用 Mock 模型配置，不保存明文 API Key。

## 后续阶段

第 2 阶段将初始化 Java Spring Boot 主后端工程，接入 MySQL、MyBatis-Plus、统一响应、统一异常、Swagger / Knife4j，并为第 3 阶段认证安全模块做准备。

# 开发计划

## 1. 阶段策略

项目按 0-20 阶段推进。每个阶段完成后输出已创建文件、已修改文件、运行命令、目录树和下一阶段计划。MVP 是阶段性可运行版本，不是最终目标；最终目标是完整、好用、可演示、可部署的软件系统。

## 2. 阶段安排

| 阶段 | 名称 | 主要目标 |
|---|---|---|
| 0 | 项目规划与目录初始化 | 建立项目结构和规划文档 |
| 1 | 数据库完整设计 | 完成 MySQL 表结构、种子数据和数据库文档 |
| 2 | Java 后端基础工程 | 初始化 Spring Boot、MyBatis-Plus、统一响应和 Swagger |
| 3 | Java 后端认证与安全 | 实现注册、登录、JWT 和用户上下文 |
| 4 | Java 后端核心业务模块 | 实现主要业务 CRUD 和分页查询 |
| 5 | AI 模型配置中心 | 实现模型配置、API Key 加密和连接测试 |
| 6 | Python AI 微服务基础工程 | 初始化 FastAPI 和 MockLLMProvider |
| 7 | Java 对接 Python AI 服务 | 统一 AI 服务客户端和异常处理 |
| 8 | 智能对话与意图识别 | 完成对话学习助手 |
| 9 | 多智能体工作流 | 完成智能体任务和步骤展示 |
| 10 | 知识库与 RAG | 完成上传、解析、切分和检索 |
| 11 | 学习资源生成中心 | 完成多类型资源生成与保存 |
| 12 | 知识图谱与 Mermaid | 完成知识结构图谱展示 |
| 13 | 个性化学习路径 | 完成路径生成和进度更新 |
| 14 | 习题、测验与评分 | 完成在线测验闭环 |
| 15 | 学习报告与可视化 | 完成报告和 ECharts 仪表盘 |
| 16 | 前端交互优化 | 完善页面状态、校验和响应式 |
| 17 | 测试与质量保障 | 完成单元测试、接口测试和 smoke test |
| 18 | Docker Compose 与部署 | 完成一键启动和部署文档 |
| 19 | 演示与提交材料 | 完成演示脚本、PPT 大纲和提交清单 |
| 20 | 最终检查与优化 | 整理成可提交版本 |

## 3. 当前执行范围

本次执行第 0 阶段和第 1 阶段，并为第 2 阶段预留 Java 后端、前端和 Python AI 服务目录结构。本阶段不生成业务代码。

## 4. 第 2 阶段准备

第 2 阶段将创建：

- `backend/pom.xml`
- `backend/src/main/java/com/edustudio/EduStudioApplication.java`
- `backend/src/main/java/com/edustudio/common/api/Result.java`
- `backend/src/main/java/com/edustudio/common/api/PageResult.java`
- `backend/src/main/java/com/edustudio/common/api/ResultCode.java`
- `backend/src/main/java/com/edustudio/common/exception/BusinessException.java`
- `backend/src/main/java/com/edustudio/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `docs/backend/java-architecture.md`

第 2 阶段验收标准是后端可启动，Swagger / Knife4j 可访问，MySQL 配置清晰，基础分层结构稳定。

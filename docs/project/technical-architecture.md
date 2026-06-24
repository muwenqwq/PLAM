# 技术架构说明

## 1. 总体架构

系统采用前后端分离和 AI 微服务解耦架构：

```text
Vue 3 前端
    |
    | HTTP / JSON / JWT
    v
Spring Boot Java 主后端
    |                    \
    | MyBatis-Plus        \ HTTP / JSON
    v                      v
MySQL 8 / Redis        FastAPI Python AI 微服务
                            |
                            v
                    MockLLM / OpenAI-compatible / RAG
```

## 2. 职责边界

### 2.1 前端

前端负责页面渲染、表单交互、状态管理、文件上传入口、Markdown 展示、Mermaid 图谱渲染和 ECharts 可视化。前端只调用 Java 后端 API，不直接访问 Python AI 微服务。

### 2.2 Java 主后端

Java 后端是系统主体，负责：

- 用户认证、JWT 鉴权和权限控制。
- 学习空间、画像、模型配置、会话、知识库元数据、智能体任务、资源、路径、测验、报告和日志。
- MySQL 数据读写、业务校验、分页查询和用户数据隔离。
- API Key 加密存储和脱敏展示。
- 统一调用 Python AI 微服务，并保存 AI 返回结果。

### 2.3 Python AI 微服务

Python AI 微服务只负责 AI 能力：

- 模型连接测试。
- Mock 和 OpenAI-compatible 调用。
- 意图识别。
- 多智能体工作流。
- RAG 文档解析、切分、检索和引用来源组织。
- 结构化 JSON 输出。

AI 服务不负责用户认证，不直接操作核心业务数据库，不直接暴露给前端。

## 3. 数据架构

MySQL 8 是主业务数据库。数据库设计采用“逻辑外键为主、数据库强外键为辅”的策略：表结构通过 `user_id`、`space_id`、`task_id` 等字段表达关系，由 Java 后端和 MyBatis-Plus 负责业务约束与逻辑删除处理。本阶段不在 SQL 中强制添加大量外键，避免逻辑删除、批量导入和演示数据初始化受到不必要限制。

Redis 在后续阶段用于验证码、登录辅助、任务状态缓存和热点数据缓存。当前第 0-1 阶段不因 Redis 或 Docker 缺失而阻塞。

## 4. API 设计原则

- 所有 Java API 返回统一 `Result<T>`。
- 分页接口返回统一 `PageResult<T>`。
- 所有写接口使用 DTO 接收请求，VO 返回给前端。
- 前端可见的 API Key 永远使用脱敏字段。
- AI 相关接口由 Java 后端代理，不暴露 Python AI 服务地址。

## 5. 本地开发环境

当前已检测到可用工具：Java 21、Maven 3.9.15、Node 24、`npm.cmd`、Python 3.13、Git、MySQL 8.4。Docker 和 Redis 当前未在 PATH 中，后续部署阶段再处理，不影响本阶段数据库与文档交付。

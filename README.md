# LearnAgent-A3

基于大模型的个性化资源生成与学习多智能体系统。

## 架构

```
Vue 3 前端 (:5173)
    │ /api/*
    ▼
Java Spring Boot (:8080) ─── MySQL (:3306)
    │ /ai/*
    ▼
Python FastAPI (:8000) ─── Chroma 向量库 ─── LLM API
```

## 项目结构

```
PLAM/
├── frontend/        # Vue 3 前端
├── backend/         # Java Spring Boot 主后端
├── aiend/           # Python FastAPI AI 服务
├── docs/            # 文档（URD/SRS/DBDD/SDD/IDD）
└── temp/            # 杂物（gitignored）
```

## 环境要求

| 项目 | 依赖 |
|---|---|
| 前端 | Node.js 20+, npm |
| 后端 | JDK 17, Maven 3.8+, MySQL 8.x |
| AI 服务 | Python 3.11+, venv |

## 快速启动

### 1. 数据库

```bash
mysql -u root -p < backend/database/schema.sql
mysql -u root -p learnagent_a3 < backend/database/seed.sql
```

### 2. 后端

```bash
cd backend
cp .env.example .env   # 编辑 .env 填入数据库密码
mvn spring-boot:run
```

### 3. AI 服务

```bash
cd aiend
python -m venv venv
venv/Scripts/pip install -r requirements.txt
cp .env.example .env   # 编辑 .env 填入 API 配置
venv/Scripts/python -m uvicorn src.main:app --host 0.0.0.0 --port 8000
```

### 4. 前端

```bash
cd frontend
npm install
npm run dev
```

## 演示账号

学号：`demo-student-001`，密码：`123456`

## 文档

| 文档 | 说明 |
|---|---|
| [URD](docs/URD.md) | 用户需求说明书 |
| [SRS](docs/SRS.md) | 软件需求规格说明书 |
| [DBDD](docs/DBDD.md) | 数据库设计说明书 |
| [SDD](docs/SDD.md) | 软件设计说明书 |
| [IDD](docs/IDD.md) | 接口设计说明书 |
| [前端 README](frontend/README.md) | 前端项目说明 |
| [后端 README](backend/README.md) | 后端项目说明 |
| [AI 服务 README](aiend/README.md) | AI 服务说明 |

## 敏感信息提醒

`.env` 文件包含数据库密码、API Key 等敏感数据，已被 `.gitignore` 忽略。
每个子项目都提供 `.env.example` 模板文件，复制并填入真实值即可。

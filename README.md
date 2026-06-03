# LearnAgent-A3

基于大模型的个性化资源生成与学习多智能体系统。

## 架构

```
Vue 3 前端 (:5173)
    │ /api/*                     Vite Proxy
    ▼
Java Spring Boot (:8080) ─── MySQL (:3306)
    │ /ai/*                     HTTP
    ▼
Python FastAPI (:8000) ─── Chroma 向量库
    │                          Anthropic SDK / OpenAI SDK
    ▼
  LLM API（默认 GLM-4-Flash，8s/次）
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

启动后访问 `http://localhost:5173`。

## 演示账号

学号：`demo-student-001`，密码：`123456`

## 接口清单

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 登录 | POST | `/api/login` | 学号+密码 → JWT Token |
| 画像生成 | POST | `/api/profile/extract` | 自然语言 → AI 画像 |
| 画像查询 | GET | `/api/profile/:id` | 最新画像 |
| 画像历史 | GET | `/api/profile/:id/history` | 画像版本列表 |
| 资源生成 | POST | `/api/resources/generate` | AI 生成 5 类资源 |
| 资源列表 | GET | `/api/resources/:id` | 学生资源 |
| 资源详情 | GET | `/api/resources/detail/:id` | 单个资源 |
| 路径生成 | POST | `/api/study-plan/generate` | AI 学习路径 |
| 路径查询 | GET | `/api/study-plan/:id` | 学生路径 |
| 智能辅导 | POST | `/api/tutor/ask` | AI 课程问答 |
| 练习提交 | POST | `/api/assessment/submit` | 提交并评分 |
| 统计数据 | GET | `/api/analytics/:id` | 掌握度+图表 |
| Agent 记录 | GET | `/api/agent-runs/:taskId` | 多智能体轨迹 |

## LLM 模型切换

编辑 `aiend/.env`：

```env
# GLM-4-Flash（默认，免费，~8s/次）
LLM_PROVIDER=openai
LLM_BASE_URL=https://open.bigmodel.cn/api/paas/v4/
LLM_API_KEY=你的Key
LLM_MODEL=glm-4-flash

# 或 MiMo（慢但质量高，~50s/次）
# LLM_PROVIDER=anthropic
# LLM_BASE_URL=https://token-plan-cn.xiaomimimo.com/anthropic
# LLM_API_KEY=你的Key
# LLM_MODEL=mimo-v2.5-pro

# 或 DeepSeek（快，~5-10s/次）
# LLM_PROVIDER=openai
# LLM_BASE_URL=https://api.deepseek.com/v1
# LLM_API_KEY=你的Key
# LLM_MODEL=deepseek-chat
```

修改后重启 Python 服务即可，无需改代码。

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

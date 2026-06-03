# LearnAgent-A3 Python AI 服务

Python FastAPI AI 能力服务——大模型调用、RAG 检索、多智能体编排。

仅供 Java 后端内部调用，前端不可直接访问。

## 技术栈

| 技术 | 用途 |
|---|---|
| FastAPI | Web 框架 |
| uvicorn | ASGI 服务器 |
| OpenAI SDK | LLM 调用（兼容 OpenAI / DeepSeek / 通义千问 / 智谱等） |
| Chroma | 向量库（持久化到本地 `chroma_data/`） |
| Pydantic | 数据校验 |

## 快速启动

### 1. 创建虚拟环境

```bash
python -m venv venv
```

### 2. 安装依赖

```bash
# Windows
venv\Scripts\pip install -r requirements.txt

# Linux / Mac
source venv/bin/activate && pip install -r requirements.txt
```

### 3. 配置 .env

复制 `.env.example` 为 `.env`，填入 LLM API 配置：

```env
LLM_BASE_URL=你的API地址（如 https://api.deepseek.com/v1）
LLM_API_KEY=你的API密钥
LLM_MODEL=deepseek-chat
```

`.env` 已被根目录 `.gitignore` 忽略。

### 4. 启动

```bash
# Windows
venv\Scripts\python -m uvicorn src.main:app --host 0.0.0.0 --port 8000

# Linux / Mac
venv/bin/uvicorn src.main:app --host 0.0.0.0 --port 8000
```

启动后访问 `http://localhost:8000/ai/health` 验证。

## 项目结构

```
aiend/
├── venv/                    # Python 虚拟环境（gitignore）
├── chroma_data/             # Chroma 向量库持久化目录（gitignore）
├── course_docs/             # 课程资料 Markdown 目录
├── .env                     # 环境变量（gitignore）
├── .env.example             # 环境变量模板
├── requirements.txt         # Python 依赖清单
├── README.md
└── src/
    ├── main.py              # FastAPI 入口 + 全局异常处理
    ├── config/
    │   └── settings.py      # .env 配置加载
    ├── schemas/             # Pydantic 数据模型
    │   ├── agent_trace.py
    │   ├── profile.py
    │   ├── resource.py
    │   ├── study_plan.py
    │   ├── tutor.py
    │   └── assessment.py
    ├── routers/             # API 路由（/ai/*）
    │   ├── health.py        # GET  /ai/health
    │   ├── profile.py       # POST /ai/profile/extract
    │   ├── resource.py      # POST /ai/resources/generate
    │   ├── study_plan.py    # POST /ai/study-plan/generate
    │   ├── tutor.py         # POST /ai/tutor/ask
    │   ├── assessment.py    # POST /ai/assessment/analyze
    │   ├── course.py        # POST /ai/course/ingest
    │   └── critic.py        # POST /ai/critic/review
    ├── agents/              # 8 个智能体
    │   ├── profile_agent.py # 画像抽取
    │   ├── retriever_agent.py # 课程知识库检索
    │   ├── planner_agent.py # 资源生成规划
    │   ├── resource_agent.py # 个性化讲义生成
    │   ├── quiz_agent.py    # 练习题生成与分析
    │   ├── code_agent.py    # 代码实操生成
    │   ├── critic_agent.py  # 内容质量审查
    │   └── path_agent.py    # 学习路径规划
    ├── rag/                 # RAG 检索增强
    │   ├── chroma_store.py  # Chroma 向量库持久化
    │   └── chunker.py       # 文档切片
    └── llm_gateway/         # 大模型调用
        └── client.py        # OpenAI 兼容客户端
```

## 接口清单

| 方法 | 路径 | 功能 | 涉及 Agent |
|---|---|---|---|
| GET | `/ai/health` | 健康检查 | — |
| POST | `/ai/profile/extract` | 画像抽取 | ProfileAgent |
| POST | `/ai/resources/generate` | 资源生成 | Retriever + Planner + Resource + Quiz + Code + Critic |
| POST | `/ai/study-plan/generate` | 学习路径生成 | PathAgent |
| POST | `/ai/tutor/ask` | 智能辅导 | Retriever + TutorAgent |
| POST | `/ai/assessment/analyze` | 练习分析 | QuizAgent |
| POST | `/ai/course/ingest` | 课程资料入库 | — |
| POST | `/ai/critic/review` | 内容审查 | CriticAgent |

## 错误处理

当 LLM 调用失败（API Key 未配、网络不通等）时，返回 HTTP 200 + JSON error：

```json
{
  "error": "错误描述",
  "agentTrace": [
    {
      "agentName": "System",
      "status": "failed",
      "outputSummary": "错误描述"
    }
  ]
}
```

Java 后端通过响应中是否包含 `error` 字段判断调用是否成功。

## Chroma 向量库

- 持久化到本地的 `./chroma_data/` 目录
- 每次启动自动读取已有数据
- 通过 `POST /ai/course/ingest` 导入课程资料并切片、向量化
- 查询时通过语义检索返回最相关的文档片段

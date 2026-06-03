# LearnAgent-A3 接口设计说明书（IDD）

Interface Design Description

项目中文名：基于大模型的个性化资源生成与学习多智能体系统开发
项目英文名：LearnAgent: An LLM-Powered Personalized Learning Resource Generation and Multi-Agent System
版本：v1.0
日期：2026-06-03
相关文档：`docs/URD.md`、`docs/SRS.md`、`docs/DBDD.md`、`docs/SDD.md`

---

## 1. 文档目的

本文档定义 LearnAgent-A3 系统的全部接口规格，包括：

1. 前端 → Java 后端的外部接口（`/api/*`）
2. Java 后端 → Python AI 服务的内部接口（`/ai/*`）
3. 请求/响应数据格式
4. 错误码与异常处理
5. 接口与数据库表的映射关系

---

## 2. 接口架构总览

```text
┌──────────┐   /api/*    ┌──────────────┐   /ai/*    ┌──────────────┐
│ Vue 前端  │ ──────────→ │ Java Spring  │ ──────────→ │ Python Fast  │
│          │ ←────────── │ Boot 后端    │ ←────────── │ API AI 服务  │
└──────────┘   JSON      └──────┬───────┘   JSON      └──────┬───────┘
                                │                            │
                                ▼                            ▼
                          ┌──────────┐               ┌──────────────┐
                          │ MySQL    │               │ Chroma + LLM │
                          └──────────┘               └──────────────┘
```

### 2.1 通信规范

| 项目 | 前端 → Java | Java → Python |
|---|---|---|
| 协议 | HTTP/1.1 | HTTP/1.1 |
| 方法 | GET / POST | POST（除 health） |
| Content-Type | application/json | application/json |
| 编码 | UTF-8 | UTF-8 |
| 认证 | Bearer Token（Header） | 内网调用，暂不认证 |
| 超时 | 30s（前端 Axios） | 60s（Java HttpClient） |
| 基础路径 | `/api` | `/ai` |

### 2.2 统一响应格式

Java 后端返回给前端：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

Python AI 服务返回给 Java：

```json
{
  "profileJson": { },
  "agentTrace": []
}
```

Python 不使用 code/message 包装，直接返回业务数据 + agentTrace。

---

## 3. Java 后端外部接口（`/api/*`）

### 3.1 健康检查

#### GET /api/health

检查 Java 后端是否正常运行。

**响应：**

```json
{
  "code": 200,
  "message": "Java service is running",
  "data": {
    "status": "UP",
    "timestamp": "2026-06-03T10:00:00",
    "pythonService": "UP"
  }
}
```

---

### 3.2 画像模块

#### POST /api/profile/extract

生成学生画像。Java 创建 ai_task → 调用 Python → 保存画像 → 返回结果。

**请求：**

```json
{
  "studentId": 1,
  "courseId": 1,
  "studentMessage": "我是计算机大二学生，想两周内学会 A* 搜索算法。我 Python 一般，喜欢图解和代码例子。"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| studentId | Long | 是 | 学生 ID |
| courseId | Long | 是 | 课程 ID |
| studentMessage | String | 是 | 学生自然语言描述 |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260603_001",
    "profileSnapshotId": 1001,
    "profile": {
      "major": "计算机科学与技术",
      "grade": "大二",
      "course": "人工智能导论",
      "goal": "两周内掌握 A* 搜索算法并完成 Python 路径规划实验",
      "foundation": "Python 基础一般，搜索算法基础较弱",
      "weakness": ["不理解启发式函数", "混淆 g(n)、h(n)、f(n)"],
      "preference": ["图解", "短讲解", "代码案例"],
      "timeBudget": "每天约 45 分钟",
      "masteryMap": {
        "BFS/DFS": 0.7,
        "启发式函数": 0.3,
        "A*算法": 0.2
      }
    },
    "createdAt": "2026-06-03T10:00:00"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| taskId | String | 本次 AI 任务 ID |
| profileSnapshotId | Long | 画像快照 ID |
| profile.major | String | 专业 |
| profile.grade | String | 年级 |
| profile.course | String | 课程 |
| profile.goal | String | 学习目标 |
| profile.foundation | String | 基础水平 |
| profile.weakness | String[] | 薄弱点列表 |
| profile.preference | String[] | 学习偏好列表 |
| profile.timeBudget | String | 可用时间 |
| profile.masteryMap | Object | 知识点掌握度（key: 知识点名, value: 0-1） |

**数据库操作：**
1. INSERT `ai_task` (type=profile_extract, status=created)
2. 调用 Python POST `/ai/profile/extract`
3. INSERT `profile_snapshot`
4. INSERT `agent_run` (N 条，来自 agentTrace)
5. UPDATE `ai_task` (status=success)

---

#### GET /api/profile/{studentId}

查询学生最新画像。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| studentId | Long | 学生 ID |

**响应：** 同 POST /api/profile/extract 的 data 部分。

**数据库操作：**
查询 `v_api_latest_profile` 视图。

---

#### GET /api/profile/{studentId}/history

查询学生画像历史版本。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| studentId | Long | 学生 ID |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1001,
      "taskId": "task_20260603_001",
      "profile": { },
      "source": "conversation",
      "createdAt": "2026-06-03T10:00:00"
    },
    {
      "id": 1000,
      "taskId": "task_20260601_001",
      "profile": { },
      "source": "conversation",
      "createdAt": "2026-06-01T08:00:00"
    }
  ]
}
```

**数据库操作：**
查询 `v_api_profile_history` 视图，按 created_at DESC。

---

### 3.3 资源模块

#### POST /api/resources/generate

生成个性化资源。支持指定资源类型。

**请求：**

```json
{
  "studentId": 1,
  "courseId": 1,
  "knowledgePointId": 12,
  "resourceTypes": ["explanation_doc", "mindmap", "quiz", "reading_material", "code_lab"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| studentId | Long | 是 | 学生 ID |
| courseId | Long | 是 | 课程 ID |
| knowledgePointId | Long | 是 | 知识点 ID |
| resourceTypes | String[] | 是 | 资源类型列表 |

**资源类型枚举：**

| 值 | 说明 |
|---|---|
| explanation_doc | 个性化讲义 |
| mindmap | 思维导图 |
| quiz | 分层练习题 |
| reading_material | 拓展阅读 |
| code_lab | 代码实操案例 |
| micro_lesson | 微课脚本（可选） |
| project_task | 实践项目（可选） |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_resource_001",
    "resourceIds": [2001, 2002, 2003, 2004, 2005],
    "status": "success"
  }
}
```

**数据库操作：**
1. INSERT `ai_task` (type=resource_generate)
2. 查询 `profile_snapshot`（最新画像）
3. 查询 `document_chunk`（课程知识库）
4. 调用 Python POST `/ai/resources/generate`
5. INSERT `resource`（批量，每个类型一条）
6. INSERT `agent_run`（N 条）
7. UPDATE `ai_task`

---

#### GET /api/resources/{studentId}

查询学生资源列表。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| studentId | Long | 学生 ID |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 2001,
      "resourceType": "explanation_doc",
      "title": "A* 搜索算法个性化讲义",
      "format": "markdown",
      "qualityScore": 4.5,
      "status": "completed",
      "createdAt": "2026-06-01T10:05:00",
      "content": "# A* 搜索算法\n..."
    },
    {
      "id": 2002,
      "resourceType": "mindmap",
      "title": "A* 搜索算法思维导图",
      "format": "mermaid",
      "qualityScore": 4.2,
      "status": "completed",
      "createdAt": "2026-06-01T10:06:00",
      "content": "mindmap\n  root((A*搜索算法))..."
    }
  ]
}
```

**数据库操作：**
查询 `v_api_resource` 视图，按 created_at DESC。

---

#### GET /api/resources/detail/{resourceId}

查询单个资源详情。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| resourceId | Long | 资源 ID |

**响应：** 单个资源对象，结构同上。

**数据库操作：**
查询 `v_api_resource` 视图，WHERE id = resourceId。

---

### 3.4 学习路径模块

#### POST /api/study-plan/generate

生成学习路径。

**请求：**

```json
{
  "studentId": 1,
  "courseId": 1
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "planId": 3001,
    "studentId": 1,
    "courseId": 1,
    "createdAt": "2026-06-01T10:10:00",
    "nodes": [
      {
        "id": 1,
        "order": 1,
        "knowledgePoint": "BFS/DFS 基础回顾",
        "recommendedResourceIds": [2001],
        "estimatedDuration": "30 分钟",
        "reason": "A* 算法建立在图搜索基础之上，需要先巩固 BFS/DFS 知识",
        "completionCriteria": "能独立画出 BFS/DFS 搜索过程",
        "status": "completed"
      },
      {
        "id": 2,
        "order": 2,
        "knowledgePoint": "启发式函数理解",
        "recommendedResourceIds": [2001, 2004],
        "estimatedDuration": "45 分钟",
        "reason": "画像显示对启发式函数理解较弱，这是 A* 的核心概念",
        "completionCriteria": "能解释 h(n) 的作用并举例说明曼哈顿距离",
        "status": "in_progress"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| planId | Long | 学习路径 ID |
| nodes[] | Array | 路径节点列表 |
| nodes[].order | Int | 学习顺序 |
| nodes[].knowledgePoint | String | 知识点名称 |
| nodes[].recommendedResourceIds | Long[] | 推荐资源 ID 列表 |
| nodes[].estimatedDuration | String | 预计时长（格式化字符串） |
| nodes[].reason | String | 推荐理由 |
| nodes[].completionCriteria | String | 完成条件 |
| nodes[].status | String | 状态：completed / in_progress / not_started |

**数据库操作：**
1. INSERT `ai_task` (type=study_plan_generate)
2. 查询画像、知识点依赖、资源列表
3. 调用 Python POST `/ai/study-plan/generate`
4. INSERT `study_plan`
5. INSERT `study_plan_node`（批量）
6. INSERT `agent_run`
7. UPDATE `ai_task`

---

#### GET /api/study-plan/{studentId}

查询学生学习路径。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| studentId | Long | 学生 ID |

**响应：** 同 POST /api/study-plan/generate 的 data 部分。

**数据库操作：**
查询 `v_api_study_plan` + `v_api_study_plan_node`，Java 层组装 nodes 数组。

---

### 3.5 智能辅导模块

#### POST /api/tutor/ask

智能辅导提问。

**请求：**

```json
{
  "studentId": 1,
  "courseId": 1,
  "question": "A* 中 g(n) 和 h(n) 有什么区别？"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| studentId | Long | 是 | 学生 ID |
| courseId | Long | 是 | 课程 ID |
| question | String | 是 | 学生问题 |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "**g(n) 和 h(n) 的区别：**\n\n- **g(n)**：从起点到节点 n 的实际距离...",
    "sources": [
      { "file": "人工智能导论_第3章_搜索算法.md", "section": "3.4 A*搜索" },
      { "file": "人工智能导论_第3章_搜索算法.md", "section": "3.2 启发式搜索" }
    ],
    "suggestedResources": [2001, 2003],
    "agentTrace": [
      { "agentName": "RetrieverAgent", "status": "success", "outputSummary": "检索到 3 个相关文档片段" },
      { "agentName": "TutorAgent", "status": "success", "outputSummary": "基于课程资料生成回答，引用 2 处来源" }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| answer | String | AI 回答（Markdown 格式） |
| sources[] | Array | 引用来源列表 |
| sources[].file | String | 来源文件名 |
| sources[].section | String | 来源章节 |
| suggestedResources | Long[] | 推荐资源 ID |
| agentTrace[] | Array | Agent 运行轨迹 |

**数据库操作：**
1. INSERT `ai_task` (type=tutor_ask)
2. INSERT `tutor_message` (role=user, 内容=question)
3. 调用 Python POST `/ai/tutor/ask`
4. INSERT `tutor_message` (role=assistant, 内容=answer, sources_json)
5. INSERT `agent_run`
6. UPDATE `ai_task`

---

### 3.6 评估模块

#### POST /api/assessment/submit

提交练习结果。

**请求：**

```json
{
  "studentId": 1,
  "courseId": 1,
  "knowledgePointId": 12,
  "resourceId": 2003,
  "answers": [
    { "questionId": 1, "answer": "B", "correct": true },
    { "questionId": 2, "answer": "C", "correct": false }
  ]
}
```

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_assess_001",
    "score": 66.67,
    "totalScore": 100,
    "masteryUpdate": {
      "A*算法": 0.35
    },
    "details": [
      { "questionId": 1, "correct": true },
      { "questionId": 2, "correct": false, "explanation": "h(n) 必须是可采纳的..." }
    ]
  }
}
```

**数据库操作：**
1. INSERT `ai_task` (type=assessment_submit)
2. 调用 Python POST `/ai/assessment/analyze`
3. INSERT `assessment_result`
4. UPDATE `profile_snapshot.profile_json.masteryMap`
5. INSERT `agent_run`
6. UPDATE `ai_task`

---

#### GET /api/analytics/{studentId}

查询学习评估统计数据。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| studentId | Long | 学生 ID |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1,
    "totalQuizzes": 3,
    "averageScore": 72,
    "masteryMap": {
      "BFS/DFS": 0.7,
      "启发式函数": 0.3,
      "A*算法": 0.2
    },
    "weakPoints": ["启发式函数", "A*算法", "路径规划"],
    "recentResults": [
      {
        "id": 1,
        "quizTitle": "A* 搜索基础测验",
        "score": 60,
        "totalScore": 100,
        "submittedAt": "2026-06-01T11:00:00",
        "details": [
          { "question": "f(n) 的含义", "correct": true },
          { "question": "h(n) 的可采纳性", "correct": false }
        ]
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| totalQuizzes | Int | 完成测验总数 |
| averageScore | Number | 平均分 |
| masteryMap | Object | 知识点掌握度 |
| weakPoints | String[] | 薄弱知识点列表 |
| recentResults[] | Array | 最近测验结果 |
| recentResults[].details[] | Array | 题目对错详情 |

**数据库操作：**
查询 `v_api_analytics` 视图，聚合 assessment_result + profile_snapshot。

---

### 3.7 Agent 运行记录模块

#### GET /api/agent-runs/{taskId}

查询某次任务的 Agent 运行轨迹。

**路径参数：**

| 参数 | 类型 | 说明 |
|---|---|---|
| taskId | String | 任务 ID |

**响应：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "taskId": "task_20260603_001",
      "agentName": "ProfileAgent",
      "inputSummary": "学生输入：\"我是计算机大二学生...\"",
      "outputSummary": "抽取 9 个画像维度，识别薄弱点 2 个",
      "modelName": "MiMo",
      "status": "success",
      "latencyMs": 2340,
      "errorMessage": null,
      "createdAt": "2026-06-03T10:00:00"
    },
    {
      "id": 2,
      "taskId": "task_20260603_001",
      "agentName": "RetrieverAgent",
      "inputSummary": "检索知识点：A* 搜索算法",
      "outputSummary": "从课程知识库检索到 5 个相关文档片段",
      "modelName": "Chroma",
      "status": "success",
      "latencyMs": 890,
      "errorMessage": null,
      "createdAt": "2026-06-03T10:01:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 记录 ID |
| taskId | String | 所属任务 ID |
| agentName | String | Agent 名称 |
| inputSummary | String | 输入摘要 |
| outputSummary | String | 输出摘要 |
| modelName | String | 使用的模型或工具 |
| status | String | success / failed / running |
| latencyMs | Int | 耗时（毫秒） |
| errorMessage | String | 错误信息（失败时） |
| createdAt | String | 创建时间 |

**数据库操作：**
查询 `v_api_agent_run` 视图，WHERE task_id = taskId，ORDER BY created_at。

---

## 4. Python AI 服务内部接口（`/ai/*`）

> 以下接口仅供 Java 后端内部调用，前端不可直接访问。

### 4.1 健康检查

#### GET /ai/health

**响应：**

```json
{
  "status": "UP",
  "chromaStatus": "UP",
  "llmStatus": "UP"
}
```

---

### 4.2 画像抽取

#### POST /ai/profile/extract

从学生自然语言描述中抽取结构化画像。

**请求：**

```json
{
  "taskId": "task_20260603_001",
  "studentMessage": "我是计算机大二学生，想两周内学会 A* 搜索算法。我 Python 一般，喜欢图解和代码例子。",
  "courseContext": []
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| taskId | String | Java 创建的任务 ID |
| studentMessage | String | 学生原始输入 |
| courseContext | Array | 课程上下文（可选） |

**响应：**

```json
{
  "profileJson": {
    "major": "计算机科学与技术",
    "grade": "大二",
    "course": "人工智能导论",
    "goal": "两周内掌握 A* 搜索算法",
    "foundation": "Python 基础一般",
    "weakness": ["不理解启发式函数", "混淆 g(n)、h(n)、f(n)"],
    "preference": ["图解", "代码案例"],
    "timeBudget": "每天约 45 分钟",
    "masteryMap": {}
  },
  "agentTrace": [
    {
      "agentName": "ProfileAgent",
      "status": "success",
      "inputSummary": "学生原始描述",
      "outputSummary": "抽取 9 个画像维度",
      "modelName": "MiMo",
      "latencyMs": 2340
    }
  ]
}
```

**涉及 Agent：** ProfileAgent

**处理逻辑：**
1. ProfileAgent 接收学生描述
2. 调用 LLM，使用结构化 Prompt 抽取画像
3. 校验返回 JSON 字段完整性（至少 6 个维度）
4. 返回画像 JSON + Agent 轨迹

---

### 4.3 资源生成

#### POST /ai/resources/generate

根据画像和知识点生成个性化资源。

**请求：**

```json
{
  "taskId": "task_resource_001",
  "studentId": 1,
  "courseId": 1,
  "knowledgePointId": 12,
  "knowledgePointName": "A* 搜索算法",
  "profileJson": { },
  "resourceTypes": ["explanation_doc", "mindmap", "quiz", "reading_material", "code_lab"],
  "courseChunks": [
    {
      "chunkId": "chunk_001",
      "content": "A* 搜索算法是一种启发式搜索...",
      "sourceFile": "人工智能导论_第3章.md"
    }
  ]
}
```

**响应：**

```json
{
  "resources": [
    {
      "resourceType": "explanation_doc",
      "title": "A* 搜索算法个性化讲义",
      "content": "# A* 搜索算法\n...",
      "format": "markdown",
      "sourcesJson": [{"file": "第3章.md", "section": "3.4"}]
    },
    {
      "resourceType": "mindmap",
      "title": "A* 搜索算法思维导图",
      "content": "mindmap\n  root((A*搜索算法))...",
      "format": "mermaid",
      "sourcesJson": []
    },
    {
      "resourceType": "quiz",
      "title": "A* 搜索分层练习题",
      "content": "# 练习题\n...",
      "format": "markdown",
      "sourcesJson": []
    },
    {
      "resourceType": "reading_material",
      "title": "从 Dijkstra 到 A*",
      "content": "# 拓展阅读\n...",
      "format": "markdown",
      "sourcesJson": [{"file": "推荐资料.md"}]
    },
    {
      "resourceType": "code_lab",
      "title": "A* 路径规划 Python 实操",
      "content": "# 实验\n```python\n...\n```",
      "format": "markdown",
      "sourcesJson": []
    }
  ],
  "reviewResult": {
    "averageScore": 4.3,
    "details": [
      { "resourceType": "explanation_doc", "qualityScore": 4.5, "issues": [], "suggestions": [] },
      { "resourceType": "quiz", "qualityScore": 4.0, "issues": ["难度梯度不够"], "suggestions": ["增加综合题"] }
    ]
  },
  "agentTrace": [
    { "agentName": "RetrieverAgent", "status": "success", "outputSummary": "检索到 5 个相关文档片段" },
    { "agentName": "PlannerAgent", "status": "success", "outputSummary": "规划生成 5 类资源" },
    { "agentName": "ResourceAgent", "status": "success", "outputSummary": "生成讲义 1500 字" },
    { "agentName": "QuizAgent", "status": "success", "outputSummary": "生成 6 道练习题" },
    { "agentName": "CodeAgent", "status": "success", "outputSummary": "生成代码实验模板" },
    { "agentName": "CriticAgent", "status": "success", "outputSummary": "审查通过，平均 4.3 分" }
  ]
}
```

**涉及 Agent：** RetrieverAgent → PlannerAgent → ResourceAgent / QuizAgent / CodeAgent → CriticAgent

**处理逻辑：**
1. RetrieverAgent 从 Chroma 检索相关文档片段
2. PlannerAgent 规划资源生成方案
3. 多个 Agent 并行生成各类资源
4. CriticAgent 审查资源质量
5. 返回资源列表 + 审查结果 + Agent 轨迹

---

### 4.4 学习路径生成

#### POST /ai/study-plan/generate

根据画像、知识点依赖和资源列表规划学习路径。

**请求：**

```json
{
  "taskId": "task_plan_001",
  "studentId": 1,
  "courseId": 1,
  "profileJson": { },
  "knowledgePoints": [
    { "id": 1, "name": "BFS/DFS", "difficulty": 2 },
    { "id": 2, "name": "启发式函数", "difficulty": 3 },
    { "id": 3, "name": "A*算法", "difficulty": 4 }
  ],
  "dependencies": [
    { "from": 1, "to": 2, "relation": "prerequisite" },
    { "from": 2, "to": 3, "relation": "prerequisite" }
  ],
  "resources": [
    { "id": 2001, "type": "explanation_doc", "knowledgePointId": 1 },
    { "id": 2003, "type": "quiz", "knowledgePointId": 3 }
  ]
}
```

**响应：**

```json
{
  "nodes": [
    {
      "order": 1,
      "knowledgePointId": 1,
      "knowledgePoint": "BFS/DFS 基础回顾",
      "recommendedResourceIds": [2001],
      "estimatedMinutes": 30,
      "reason": "A* 算法建立在图搜索基础之上",
      "completionCriteria": "能独立画出 BFS/DFS 搜索过程"
    },
    {
      "order": 2,
      "knowledgePointId": 2,
      "knowledgePoint": "启发式函数理解",
      "recommendedResourceIds": [2001, 2004],
      "estimatedMinutes": 45,
      "reason": "画像显示对启发式函数理解较弱",
      "completionCriteria": "能解释 h(n) 的作用"
    }
  ],
  "agentTrace": [
    { "agentName": "PathAgent", "status": "success", "outputSummary": "生成 5 个学习节点，预计总时长 3.5 小时" }
  ]
}
```

> 注意：Python 返回 `estimatedMinutes`（整数），Java 后端转换为 `"30 分钟"` 字符串后返回前端。

**涉及 Agent：** PathAgent

---

### 4.5 智能辅导

#### POST /ai/tutor/ask

基于 RAG 的课程问答。

**请求：**

```json
{
  "taskId": "task_tutor_001",
  "studentId": 1,
  "courseId": 1,
  "question": "A* 中 g(n) 和 h(n) 有什么区别？",
  "profileJson": { }
}
```

**响应：**

```json
{
  "answer": "**g(n) 和 h(n) 的区别：**\n\n- **g(n)**：从起点到节点 n 的实际距离...",
  "sources": [
    { "file": "人工智能导论_第3章.md", "section": "3.4 A*搜索" }
  ],
  "suggestedResourceIds": [2001, 2003],
  "safetyStatus": "safe",
  "agentTrace": [
    { "agentName": "RetrieverAgent", "status": "success", "outputSummary": "检索到 3 个相关片段" },
    { "agentName": "TutorAgent", "status": "success", "outputSummary": "生成回答，引用 2 处来源" }
  ]
}
```

**涉及 Agent：** RetrieverAgent → TutorAgent

**处理逻辑：**
1. RetrieverAgent 从 Chroma 检索相关课程片段
2. 检查检索结果是否充足
   - 充足：基于片段 + 画像生成回答
   - 不足：返回"课程依据不足，建议教师确认"
3. TutorAgent 生成 Markdown 回答，附带来源引用
4. 返回回答 + 来源 + 推荐资源 + Agent 轨迹

---

### 4.6 练习分析

#### POST /ai/assessment/analyze

分析练习结果，计算得分和掌握度变化。

**请求：**

```json
{
  "taskId": "task_assess_001",
  "studentId": 1,
  "courseId": 1,
  "knowledgePointId": 12,
  "answers": [
    { "questionId": 1, "question": "f(n) 的含义", "studentAnswer": "B", "correctAnswer": "B" },
    { "questionId": 2, "question": "h(n) 的可采纳性", "studentAnswer": "C", "correctAnswer": "A" }
  ],
  "currentMastery": { "A*算法": 0.2 }
}
```

**响应：**

```json
{
  "score": 50,
  "totalScore": 100,
  "masteryDelta": { "A*算法": 0.15 },
  "details": [
    { "questionId": 1, "correct": true },
    { "questionId": 2, "correct": false, "explanation": "h(n) 必须是可采纳的（admissible），即不高估实际距离" }
  ],
  "weakPointsIdentified": ["h(n) 可采纳性"],
  "agentTrace": [
    { "agentName": "QuizAgent", "status": "success", "outputSummary": "分析 2 道题，1 对 1 错" }
  ]
}
```

**涉及 Agent：** QuizAgent

---

### 4.7 课程资料入库

#### POST /ai/course/ingest

将课程资料切片并写入 Chroma 向量库。

**请求：**

```json
{
  "courseId": 1,
  "files": [
    {
      "fileName": "人工智能导论_第3章_搜索算法.md",
      "content": "# 第三章 搜索算法\n\n## 3.1 盲目搜索\n..."
    }
  ]
}
```

**响应：**

```json
{
  "chunksCreated": 15,
  "chunks": [
    {
      "chunkKey": "chunk_3_001",
      "knowledgePointId": 1,
      "sourceFile": "人工智能导论_第3章.md",
      "title": "3.1 盲目搜索",
      "content": "BFS 和 DFS 是两种基本的图搜索策略...",
      "embeddingStatus": "completed"
    }
  ],
  "agentTrace": []
}
```

**处理逻辑：**
1. 按章节标题切分文档
2. 每个片段关联知识点
3. 调用 Embedding 模型向量化
4. 写入 Chroma collection
5. 返回切片元数据（Java 保存到 document_chunk 表）

---

### 4.8 内容审查

#### POST /ai/critic/review

审查 AI 生成内容的质量。

**请求：**

```json
{
  "taskId": "task_resource_001",
  "resourceType": "explanation_doc",
  "content": "# A* 搜索算法\n...",
  "knowledgePointName": "A* 搜索算法"
}
```

**响应：**

```json
{
  "qualityScore": 4.5,
  "issues": [],
  "suggestions": ["可以增加一个具体的搜索过程示例"],
  "agentTrace": [
    { "agentName": "CriticAgent", "status": "success", "outputSummary": "质量分 4.5，无严重问题" }
  ]
}
```

**涉及 Agent：** CriticAgent

---

## 5. Agent 职责定义

| Agent | 职责 | 调用时机 | 依赖 |
|---|---|---|---|
| ProfileAgent | 从自然语言抽取结构化画像 | 画像生成 | LLM API |
| RetrieverAgent | 从课程知识库检索相关文档片段 | 资源生成、智能辅导 | Chroma |
| PlannerAgent | 规划资源生成方案（类型、顺序） | 资源生成 | LLM API |
| ResourceAgent | 生成个性化讲义 | 资源生成 | LLM API + RAG |
| QuizAgent | 生成分层练习题 / 分析练习结果 | 资源生成、评估分析 | LLM API |
| CodeAgent | 生成代码实操案例 | 资源生成 | LLM API + RAG |
| CriticAgent | 审查生成资源的质量 | 资源生成 | LLM API |
| PathAgent | 规划学习路径 | 学习路径生成 | LLM API |

### 5.1 Agent 轨迹记录

每次 Agent 执行时，Python 返回 `agentTrace` 数组，Java 将其写入 `agent_run` 表：

```json
{
  "agentName": "RetrieverAgent",
  "status": "success",
  "inputSummary": "检索知识点：A* 搜索算法",
  "outputSummary": "从课程知识库检索到 5 个相关文档片段",
  "modelName": "Chroma",
  "latencyMs": 890
}
```

---

## 6. 数据库与接口映射

| 接口 | 主要操作表 | 查询视图 |
|---|---|---|
| POST /api/profile/extract | ai_task, profile_snapshot, agent_run | — |
| GET /api/profile/{id} | — | v_api_latest_profile |
| GET /api/profile/{id}/history | — | v_api_profile_history |
| POST /api/resources/generate | ai_task, resource, agent_run | — |
| GET /api/resources/{id} | — | v_api_resource |
| GET /api/resources/detail/{id} | — | v_api_resource |
| POST /api/study-plan/generate | ai_task, study_plan, study_plan_node, agent_run | — |
| GET /api/study-plan/{id} | — | v_api_study_plan + v_api_study_plan_node |
| POST /api/tutor/ask | ai_task, tutor_message, agent_run | — |
| POST /api/assessment/submit | ai_task, assessment_result, agent_run | — |
| GET /api/analytics/{id} | — | v_api_analytics |
| GET /api/agent-runs/{taskId} | — | v_api_agent_run |

---

## 7. 错误码汇总

| code | 含义 | 常见场景 |
|---|---|---|
| 200 | 成功 | 正常响应 |
| 400 | 请求参数错误 | studentId 缺失、resourceTypes 为空 |
| 401 | 未登录 | Token 缺失或过期 |
| 403 | 无权限 | 访问不属于自己的学生数据 |
| 404 | 资源不存在 | studentId / resourceId / taskId 不存在 |
| 500 | 服务器内部错误 | 数据库异常、未知错误 |
| 502 | AI 服务不可用 | Python FastAPI 无法连接 |
| 504 | AI 服务超时 | Python 处理超过 60 秒 |

Python AI 服务错误不使用 HTTP 状态码，而是在响应 JSON 中返回 error 字段：

```json
{
  "error": "LLM API 调用失败",
  "agentTrace": [
    {
      "agentName": "ProfileAgent",
      "status": "failed",
      "errorMessage": "Connection timeout to LLM API",
      "latencyMs": 30000
    }
  ]
}
```

Java 后端收到 Python error 后：
1. 将 agentTrace 写入 agent_run 表（status=failed）
2. 向前端返回 500 + 错误信息

---

## 8. 接口版本管理

当前所有接口为 v1 版本，不做 URL 前缀。后续如需版本升级，在 URL 中加入版本号：

```text
/api/v1/profile/extract
/api/v2/profile/extract
```

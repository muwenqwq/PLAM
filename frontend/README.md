# LearnAgent-A3

基于大模型的个性化资源生成与学习多智能体系统。

## 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 前端框架 | Vue 3 (Composition API) | ^3.5 |
| 构建工具 | Vite | ^8.0 |
| UI 组件库 | Element Plus | ^2.14 |
| 路由 | Vue Router | ^5.1 |
| 状态管理 | Pinia + pinia-plugin-persistedstate | ^3.0 |
| HTTP 客户端 | Axios | ^1.16 |
| Markdown 渲染 | markdown-it + highlight.js | ^14.2 |
| 图表 | ECharts | ^6.1 |
| 图表渲染 | Mermaid | ^11.15 |
| 工具库 | @vueuse/core | ^14.3 |

---

## 启动方式

```bash
cd frontend
npm install
npm run dev
```

后端未启动时，所有 API 自动返回 mock 数据，页面可正常开发调试。

---

## 项目结构

```
frontend/src/
├── api/                          # API 接口层
│   ├── request.js                # Axios 实例（baseURL、token 拦截器、错误处理）
│   ├── mock.js                   # 全量 mock 数据
│   ├── profile.js                # 画像接口
│   ├── resource.js               # 资源接口
│   ├── studyPlan.js              # 学习路径接口
│   ├── tutor.js                  # 智能辅导接口
│   ├── analytics.js              # 评估统计接口
│   └── agentRun.js               # Agent 运行记录接口
├── components/
│   ├── common/                   # 通用业务组件
│   │   ├── index.js              # 统一导出
│   │   ├── MarkdownRenderer.vue  # Markdown 渲染（代码高亮）
│   │   ├── MermaidDiagram.vue    # Mermaid 图表渲染
│   │   ├── ChatMessage.vue       # 聊天气泡
│   │   ├── ResourceCard.vue      # 资源卡片
│   │   ├── ProfileCard.vue       # 画像卡片（维度 + 掌握度进度条）
│   │   ├── TimelineNode.vue      # 学习路径时间线节点
│   │   ├── LoadingState.vue      # 加载动画
│   │   └── EmptyState.vue        # 空数据占位
│   └── navigation/               # 导航组件
│       ├── Sidebar.vue           # 左侧菜单栏
│       └── Navbar.vue            # 顶部导航栏
├── config/
│   └── menu.js                   # 菜单 / 路由配置
├── layouts/
│   └── MainLayout.vue            # 主布局（侧边栏 + 导航栏 + 内容区）
├── router/
│   └── index.js                  # Vue Router 配置
├── stores/                       # Pinia 状态管理
│   ├── user.js                   # 用户信息 + 主题偏好（持久化）
│   ├── profile.js                # 学生画像
│   ├── resource.js               # 学习资源
│   ├── studyPlan.js              # 学习路径
│   ├── tutor.js                  # 辅导对话
│   ├── analytics.js              # 评估统计
│   └── agentRun.js               # Agent 运行记录
├── views/                        # 业务页面
│   ├── Login.vue                 # 登录页
│   ├── Dashboard.vue             # 仪表盘
│   ├── Profile.vue               # 学习画像
│   ├── Resources.vue             # 学习资源
│   ├── Plan.vue                  # 学习路径
│   ├── Tutor.vue                 # 智能辅导
│   ├── Statistics.vue            # 数据统计
│   ├── AgentRecords.vue          # Agent 运行记录
│   └── Setting.vue               # 设置
├── style.css                     # 全局样式 + CSS 设计变量
├── main.js                       # 应用入口
└── App.vue                       # 根组件
```

---

## API 接口层

### 架构设计

```
前端页面  →  stores (Pinia)  →  api/*.js  →  Axios (request.js)  →  Java 后端 /api/*
                                                                         ↓
                                                                   Python AI 服务 /ai/*
```

前端**只调用 Java 后端**（`/api/*`），不直接接触 Python AI 服务。

### Axios 封装 — `api/request.js`

| 功能 | 说明 |
|---|---|
| baseURL | `/api`，通过 Vite proxy 转发到 Java 后端 |
| Token 注入 | 请求拦截器自动从 localStorage 读取 user store 的 token，写入 `Authorization` 头 |
| 统一错误处理 | 响应拦截器按 HTTP 状态码（401/403/500/超时）展示 Element Plus 错误提示 |
| 业务错误处理 | 若响应体含 `code` 字段且非成功，展示 `message` 并 reject |

### Mock 机制

每个 API 模块（`api/profile.js` 等）内置 try-catch：后端正常时走真实请求，后端未就绪时自动返回 `api/mock.js` 中的预置数据。开发阶段无需启动后端即可调试全部页面。

### 接口清单

| 模块 | 方法 | 路径 | 功能 |
|---|---|---|---|
| profile | POST | `/api/profile/extract` | 生成学生画像 |
| profile | GET | `/api/profile/:studentId` | 查询最新画像 |
| profile | GET | `/api/profile/:studentId/history` | 查询画像历史 |
| resource | POST | `/api/resources/generate` | 生成个性化资源 |
| resource | GET | `/api/resources/:studentId` | 查询资源列表 |
| resource | GET | `/api/resources/detail/:resourceId` | 查询资源详情 |
| studyPlan | POST | `/api/study-plan/generate` | 生成学习路径 |
| studyPlan | GET | `/api/study-plan/:studentId` | 查询学习路径 |
| tutor | POST | `/api/tutor/ask` | 智能辅导提问 |
| analytics | POST | `/api/assessment/submit` | 提交练习结果 |
| analytics | GET | `/api/analytics/:studentId` | 查询评估数据 |
| agentRun | GET | `/api/agent-runs/:taskId` | 查询 Agent 运行轨迹 |

---

## Pinia Stores

所有 store 使用 Composition API 风格（`setup function`），返回响应式状态和方法。

| Store | 持久化 | 核心状态 | 核心方法 |
|---|---|---|---|
| `user` | ✅ localStorage | name, target, imgSrc, studentId, courseId, token, theme | setUserInfo, setTheme, clearUserInfo |
| `profile` | ❌ | profile, history, loading | fetchLatest, fetchHistory, generate |
| `resource` | ❌ | resources, currentResource, loading, generating | fetchList, fetchDetail, generate |
| `studyPlan` | ❌ | plan, nodes, loading | fetchPlan, generate, updateNodeStatus |
| `tutor` | ❌ | messages, loading | sendMessage, clearMessages |
| `analytics` | ❌ | analytics, loading, submitting | fetchAnalytics, submitQuiz |
| `agentRun` | ❌ | runs, loading | fetchRuns |

### 使用示例

```js
import { useProfileStore } from '@/stores/profile'

const profileStore = useProfileStore()
await profileStore.generate({ studentId: 1, courseId: 1, studentMessage: '...' })
console.log(profileStore.profile) // 生成的画像数据
```

---

## 通用组件

统一从 `components/common/index.js` 导出，使用时：

```js
import { ProfileCard, ResourceCard, LoadingState } from '@/components/common'
```

| 组件 | 用途 | 关键 Props |
|---|---|---|
| `MarkdownRenderer` | Markdown → HTML，支持代码高亮 | `content: String` |
| `MermaidDiagram` | Mermaid 图表渲染（思维导图、流程图） | `code: String`, `id: String` |
| `ChatMessage` | 单条聊天气泡（用户/AI 两种样式） | `msg: { role, content, sources }` |
| `ResourceCard` | 资源卡片（类型标签 + 评分 + 状态） | `resource: Object` |
| `ProfileCard` | 画像卡片（维度网格 + 薄弱点标签 + 掌握度进度条） | `profile: Object` |
| `TimelineNode` | 学习路径时间线节点（完成/进行中/未开始） | `node: Object`, `isLast: Boolean` |
| `LoadingState` | 加载动画 + 文字 | `text: String`, `size: Number` |
| `EmptyState` | 空数据占位图 + 描述 | `icon: String`, `title: String`, `description: String` |

---

## 业务页面

| 页面 | 路由 | 功能 |
|---|---|---|
| Login | `/login` | 登录表单，mock 验证（admin / 123456） |
| Dashboard | `/dashboard` | 欢迎卡片 + 6 个功能入口 + 画像预览 + 最近资源 |
| Profile | `/profile` | 自然语言输入 → 画像生成（6 维度） + 历史版本时间线 |
| Resources | `/resources` | 资源类型选择 → 生成 5 类资源 + 卡片网格 + 详情抽屉（Markdown / Mermaid 渲染） |
| Plan | `/plan` | 学习路径生成 + 时间线节点展示 + 进度统计 |
| Tutor | `/tutor` | 聊天界面 + Markdown 回答 + 来源引用 + 打字动画 |
| Statistics | `/statistics` | 概览卡片 + ECharts 雷达图 + 薄弱点分析 + 测验结果表格 |
| AgentRecords | `/agent-records` | 按任务 ID 查询 + Agent 时间线（输入 / 输出 / 耗时 / 模型） |
| Setting | `/settings` | 个人信息编辑 + 主题切换（浅色 / 深色）+ 系统信息 + 清空数据 |

---

## 样式管理

### 设计变量体系 — `style.css`

所有颜色通过 CSS 自定义属性（变量）集中管理，组件只引用变量，不写死色值。

```css
:root {
  --color-primary: #3182ce;      /* 主色 */
  --color-primary-dark: #2b6cb0; /* 主色深 */
  --color-primary-bg: #e6f0fd;   /* 主色浅背景 */
  --text-heading: #1a202c;       /* 标题文字 */
  --text-body: #2d3748;          /* 正文 */
  --text-muted: #4a5568;         /* 次要文字 */
  --text-subtle: #718096;        /* 辅助说明 */
  --text-placeholder: #a0aec0;   /* 占位提示 */
  --bg-page: #f8f9fc;            /* 页面背景 */
  --bg-card: #ffffff;            /* 卡片背景 */
  --bg-hover: #f7fafc;           /* 悬停背景 */
  --border-default: #e2e8f0;     /* 边框 */
  --border-light: #edf2f7;       /* 轻边框 */
  --border-divider: #f0f0f0;     /* 分割线 */
  --color-success: #67c23a;      /* 成功 */
  --color-warning: #e6a23c;      /* 警告 */
  --color-danger: #f56c6c;       /* 危险 */
  --color-error: #e53e3e;        /* 错误文字 */
  --color-error-bg: #fff5f5;     /* 错误背景 */
  --color-error-border: #fed7d7; /* 错误边框 */
  /* ...完整定义见 style.css */
}
```

### 使用方式

```css
/* ✅ 正确 — 引用变量 */
.title { color: var(--text-heading); }
.card  { background: var(--bg-card); border: 1px solid var(--border-default); }

/* ❌ 错误 — 硬编码色值 */
.title { color: #1a202c; }
.card  { background: #ffffff; }
```

### 暗色主题

`style.css` 中已定义 `[data-theme="dark"]` 变量覆盖，切换主题只需：

```js
document.documentElement.dataset.theme = 'dark'   // 启用暗色
document.documentElement.dataset.theme = ''         // 恢复浅色
```

Element Plus 暗色主题（`dark/css-vars.css`）已全局导入，会跟随 `.dark` class 自动切换。

### 公共样式

以下样式在 `style.css` 中全局定义，页面直接使用 class 即可，无需重复定义：

| Class | 用途 |
|---|---|
| `.page-header h2` | 页面标题（7 个业务页面共用） |
| `.page-header p` | 页面副标题 |
| `.section-header` | 卡片头部（flex 两端对齐） |

---

## 全局状态管理

### 主题持久化

主题偏好通过 `user` store 的 `theme` 字段持久化到 localStorage。应用启动时 `main.js` 读取该值并恢复主题。用户在设置页切换主题时，`user.setTheme()` 同步更新 store、DOM 属性和 localStorage。

### 数据流

```
用户操作 → Vue 组件 → Pinia store (action) → API 模块 → Axios → 后端
                ↑                                    ↓
                └──── store (state 更新) ←── 响应数据 ←┘
```

组件通过 `useXxxStore()` 获取状态，调用 action 触发请求，store 内部管理 loading 状态和数据更新。组件只需关心展示和交互。

---

## 相关文档

| 文档 | 路径 | 内容 |
|---|---|---|
| 用户需求说明书 | `docs/URD.md` | 用户角色、场景、需求优先级、验收标准 |
| 软件需求规格说明书 | `docs/SRS.md` | 系统架构、API 规格、数据库表结构、功能需求 |

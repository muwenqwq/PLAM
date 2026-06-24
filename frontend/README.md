# EduAgent Studio 前端说明

## 技术栈

前端使用 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts、Markdown-it 和 Mermaid。前端只调用 Java 后端 `/api/**`，不直接调用 Python AI 服务。

## 启动方式

```bash
npm install --cache ./.npm-cache
npm run dev
```

默认访问地址为 `http://127.0.0.1:5173`。

## 环境变量

复制 `.env.example` 并按需修改：

```text
VITE_API_BASE_URL=http://127.0.0.1:8080/api
```

本地开发也可以不配置该变量，Vite 会将 `/api` 代理到 `http://127.0.0.1:8080`。

## 页面清单

- `/` 首页
- `/login` 登录
- `/register` 注册
- `/dashboard` 学习驾驶舱
- `/spaces` 学习空间
- `/profile` 用户画像与学习偏好
- `/models` AI 模型配置
- `/chat` 智能对话
- `/agents` 多智能体工作台
- `/knowledge` 知识库
- `/resources` 资源生成中心
- `/graph` 知识图谱
- `/paths` 学习路径
- `/quiz` 测验评估
- `/reports` 学习报告
- `/help` 演示帮助

## 演示账号

- `demo_student / 123456`
- `demo_teacher / 123456`
- `demo_admin / 123456`

## 构建验证

```bash
npm run build
```

构建产物输出到 `frontend/dist`。

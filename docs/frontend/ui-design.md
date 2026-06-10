# 前端 UI 设计说明

## 设计风格

前端采用简洁、学术科技感的工作台式界面，适合比赛答辩和日常演示。视觉上以白色和浅灰作为主背景，蓝色与青绿色作为强调色，减少装饰性元素，突出学习闭环流程。

## 布局规范

1. 登录后统一使用左侧导航和顶部栏。
2. 页面主体使用 Element Plus Card、Table、Form、Dialog、Timeline、Tabs 等组件。
3. Dashboard 和报告页使用 ECharts。
4. Markdown 内容使用 `MarkdownViewer` 渲染。
5. Mermaid 图谱使用 `MermaidViewer` 渲染。
6. 空状态统一使用 `EmptyState`，加载状态统一使用 `LoadingState`。

## 组件复用

- `PageHeader`：页面标题和操作区。
- `StatCard`：统计指标卡。
- `ResourceCard`：生成资源卡片。
- `AgentStepTimeline`：多智能体执行步骤。
- `ModelProviderForm`：模型配置表单。

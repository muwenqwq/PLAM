# 学生端参考 UI 重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Vue 学生端完整重构为参考 UI 的双主题学习工作台。

**Architecture:** 使用 Pinia 管理并持久化主题和导航状态，使用全局语义 CSS token 统一 Element Plus 与业务组件，再针对应用外壳、公开页和核心业务页面补充布局样式。现有 API、路由地址和业务方法保持不变。

**Tech Stack:** Vue 3、TypeScript、Pinia、Vue Router、Element Plus、CSS variables

---

### Task 1: Theme foundation

**Files:**
- Create: `frontend/src/components/ThemeToggle.vue`
- Modify: `frontend/src/stores/app.ts`
- Modify: `frontend/src/assets/main.css`
- Test: `frontend/scripts/check-ui-theme.mjs`

- [ ] 运行 `node scripts/check-ui-theme.mjs`，确认主题契约测试失败。
- [ ] 实现默认白色、持久化夜间主题和根节点 `data-theme`。
- [ ] 定义完整的浅色与深色语义 token，并覆盖 Element Plus 表面。
- [ ] 再次运行主题契约测试，确认通过。

### Task 2: Responsive student shell

**Files:**
- Modify: `frontend/src/layout/BasicLayout.vue`

- [ ] 按参考 UI 重构品牌区、核心导航、页头和用户区。
- [ ] 增加主题切换、折叠侧栏和移动抽屉。
- [ ] 验证八个导航入口和退出登录仍使用现有路由与 store。

### Task 3: Public and authentication pages

**Files:**
- Modify: `frontend/src/views/Home.vue`
- Modify: `frontend/src/views/Login.vue`
- Modify: `frontend/src/views/Register.vue`

- [ ] 统一公开页品牌、排版和主题入口。
- [ ] 重构登录与注册卡片，同时保留表单校验和真实接口。
- [ ] 验证桌面与移动布局无水平溢出。

### Task 4: Shared visual primitives

**Files:**
- Modify: `frontend/src/components/PageHeader.vue`
- Modify: `frontend/src/components/StatCard.vue`
- Modify: `frontend/src/components/EmptyState.vue`
- Modify: `frontend/src/components/LoadingState.vue`
- Modify: `frontend/src/components/ResourceCard.vue`
- Modify: `frontend/src/components/MermaidViewer.vue`

- [ ] 把公共组件改为语义 token、统一圆角、边框、阴影和状态反馈。
- [ ] 保持组件 props 与事件不变。

### Task 5: Core page alignment

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/views/KnowledgeBase.vue`
- Modify: `frontend/src/views/ResourceGenerator.vue`
- Modify: `frontend/src/views/MyResources.vue`
- Modify: `frontend/src/views/LearningSpace.vue`
- Modify: `frontend/src/views/Quiz.vue`
- Modify: `frontend/src/views/Report.vue`
- Modify: `frontend/src/views/UserProfile.vue`

- [ ] 删除单主题硬编码表面色，统一为双主题 token。
- [ ] 调整首页快捷入口、资源类型、资源卡片和知识片段的视觉层级。
- [ ] 保留上传、检索、生成、修改、删除、测验和报告的现有行为。

### Task 6: Verification and review

**Files:**
- Verify: `frontend/**`

- [ ] 运行 `node scripts/check-ui-theme.mjs`。
- [ ] 运行 `npm.cmd run build`。
- [ ] 启动前端并用本机 Chrome 截取桌面与移动视口。
- [ ] 检查主题持久化、导航、控制台和主要操作。
- [ ] 完成代码审查并修复重要问题。

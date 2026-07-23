import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Home from '@/views/Home.vue'
import BasicLayout from '@/layout/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: Home, meta: { public: true, title: '首页' } },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true, guestOnly: true, title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { public: true, title: '注册' } },
  {
    path: '/',
    component: BasicLayout,
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '学习首页', description: '从资料、问答、资源和测验开始今天的学习。' } },
      { path: 'spaces', component: () => import('@/views/LearningSpace.vue'), meta: { title: '学习空间', description: '按课程或主题整理你的学习内容。' } },
      { path: 'knowledge', component: () => import('@/views/KnowledgeBase.vue'), meta: { title: '我的知识库', description: '上传资料后，可以提问、总结并生成学习资源。' } },
      { path: 'resource-generation', component: () => import('@/views/ResourceGenerator.vue'), meta: { title: '资源生成', description: '选择资料和类型，生成适合复习的学习资源。' } },
      { path: 'resources', component: () => import('@/views/MyResources.vue'), meta: { title: '我的资源', description: '查看、修改和整理已经生成的学习资料。' } },
      { path: 'quiz', component: () => import('@/views/Quiz.vue'), meta: { title: '学习测验', description: '用小测验检查掌握情况。' } },
      { path: 'reports', alias: '/report', component: () => import('@/views/Report.vue'), meta: { title: '学习报告', description: '查看阶段进展和下一步建议。' } },
      { path: 'chat', component: () => import('@/views/ChatAssistant.vue'), meta: { title: 'AI 对话', description: '选择 AI 角色进行学习问答。' } },
      { path: 'roles', component: () => import('@/views/RoleCompanion.vue'), meta: { title: 'AI 角色', description: '创建可保存、可复用的学习陪伴角色。' } },
      { path: 'profile', component: () => import('@/views/UserProfile.vue'), meta: { title: '学习画像', description: '按学习空间维护目标、基础、薄弱点和内容偏好。' } },
      { path: 'models', component: () => import('@/views/ModelProvider.vue'), meta: { title: 'AI 模型', description: '接入并管理学习功能使用的 AI 模型。' } },
      { path: 'agents', component: () => import('@/views/AgentWorkspace.vue'), meta: { title: '多 Agent 工作区', description: '查看多智能体如何拆解目标、生成资源和复核结果。' } },
      { path: 'graph', component: () => import('@/views/KnowledgeGraph.vue'), meta: { title: '知识图谱', description: '选择资源生成或预览 Mermaid 知识图谱。' } },
      { path: 'paths', component: () => import('@/views/LearningPath.vue'), meta: { title: '学习路径', description: '把学习目标拆成阶段任务，并跟踪完成进度。' } },
      { path: 'help', component: () => import('@/views/Help.vue'), meta: { title: '帮助', hidden: true } },
      { path: 'resource-generator', redirect: '/resource-generation' }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guestOnly && auth.isLogin) return '/dashboard'
  if (!to.meta.public && !auth.isLogin) return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '学生学习工作台'
  document.title = `${title} - 智学工坊`
})

export default router

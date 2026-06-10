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
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '仪表盘', icon: 'DataLine' } },
      { path: 'spaces', component: () => import('@/views/LearningSpace.vue'), meta: { title: '学习空间', icon: 'FolderOpened' } },
      { path: 'profile', component: () => import('@/views/UserProfile.vue'), meta: { title: '用户画像', icon: 'User' } },
      { path: 'models', component: () => import('@/views/ModelProvider.vue'), meta: { title: '模型配置', icon: 'Cpu' } },
      { path: 'chat', component: () => import('@/views/ChatAssistant.vue'), meta: { title: '智能对话', icon: 'ChatDotRound' } },
      { path: 'agents', component: () => import('@/views/AgentWorkspace.vue'), meta: { title: 'Agent 工作台', icon: 'Operation' } },
      { path: 'knowledge', component: () => import('@/views/KnowledgeBase.vue'), meta: { title: '知识库', icon: 'Collection' } },
      { path: 'resources', component: () => import('@/views/ResourceGenerator.vue'), meta: { title: '资源生成', icon: 'DocumentAdd' } },
      { path: 'graph', component: () => import('@/views/KnowledgeGraph.vue'), meta: { title: '知识图谱', icon: 'Share' } },
      { path: 'paths', component: () => import('@/views/LearningPath.vue'), meta: { title: '学习路径', icon: 'Guide' } },
      { path: 'quiz', component: () => import('@/views/Quiz.vue'), meta: { title: '测验评估', icon: 'EditPen' } },
      { path: 'reports', component: () => import('@/views/Report.vue'), meta: { title: '学习报告', icon: 'PieChart' } },
      { path: 'help', component: () => import('@/views/Help.vue'), meta: { title: '演示帮助', icon: 'QuestionFilled' } }
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

export default router

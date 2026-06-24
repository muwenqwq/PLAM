<template>
  <div class="shell">
    <transition name="fade">
      <button v-if="app.mobileMenuOpen" class="mobile-overlay" type="button" aria-label="关闭导航" @click="app.closeMobileMenu()" />
    </transition>

    <aside id="student-sidebar" ref="sidebarRef" class="sidebar" :class="{ collapsed: app.collapsed, 'mobile-open': app.mobileMenuOpen }" :inert="isMobile && !app.mobileMenuOpen" :aria-hidden="isMobile && !app.mobileMenuOpen">
      <div class="brand">
        <div class="brand-mark">智</div>
        <div class="brand-copy">
          <strong>智学工坊</strong>
          <span>学生学习工作台</span>
        </div>
      </div>

      <div class="student-card">
        <el-avatar :size="44">{{ avatarText }}</el-avatar>
        <div>
          <strong>{{ displayName }}</strong>
          <span>今天也要稳步前进</span>
        </div>
      </div>

      <nav class="nav-region" aria-label="学生端主导航">
        <el-menu router :collapse="app.collapsed && !app.mobileMenuOpen" :default-active="$route.path" @select="app.closeMobileMenu()">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="sidebar-footer">
        <el-button text :icon="app.collapsed ? Expand : Fold" :aria-label="app.collapsed ? '展开导航' : '收起导航'" @click="app.toggleMenu()">
          <span v-if="!app.collapsed">收起导航</span>
        </el-button>
      </div>
    </aside>

    <div class="workspace" :class="{ 'sidebar-collapsed': app.collapsed }">
      <header class="topbar">
        <div class="top-left">
          <el-button ref="menuButtonRef" class="mobile-menu-button" circle :aria-label="app.mobileMenuOpen ? '关闭导航' : '打开导航'" aria-controls="student-sidebar" :aria-expanded="app.mobileMenuOpen" :icon="Menu" @click="app.toggleMobileMenu()" />
          <div>
            <h1>{{ $route.meta.title || '工作台' }}</h1>
            <p>{{ $route.meta.description || '围绕资料、资源和测验安排今天的学习。' }}</p>
          </div>
        </div>

        <div class="top-actions">
          <ThemeToggle />
          <div class="user-summary">
            <el-avatar :size="38">{{ avatarText }}</el-avatar>
            <div>
              <strong>{{ displayName }}</strong>
              <span>学生</span>
            </div>
          </div>
          <el-tooltip content="退出登录" placement="bottom">
            <el-button circle aria-label="退出登录" :icon="SwitchButton" @click="handleLogout" />
          </el-tooltip>
        </div>
      </header>

      <main id="main-content" class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Expand, Fold, Menu, SwitchButton } from '@element-plus/icons-vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const app = useAppStore()
const auth = useAuthStore()
const router = useRouter()
const sidebarRef = ref<HTMLElement>()
const menuButtonRef = ref<any>()
const isMobile = ref(false)
let mobileQuery: MediaQueryList | undefined
const displayName = computed(() => auth.user?.nickname || auth.user?.username || 'Demo Student')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())

const menuItems = [
  { path: '/dashboard', title: '学习首页', icon: 'House' },
  { path: '/spaces', title: '学习空间', icon: 'FolderOpened' },
  { path: '/knowledge', title: '我的知识库', icon: 'Collection' },
  { path: '/resource-generation', title: '资源生成', icon: 'DocumentAdd' },
  { path: '/resources', title: '我的资源', icon: 'Files' },
  { path: '/quiz', title: '学习测验', icon: 'EditPen' },
  { path: '/reports', title: '学习报告', icon: 'PieChart' },
  { path: '/models', title: 'AI 模型', icon: 'Connection' },
  { path: '/profile', title: '个人中心', icon: 'User' }
]

async function handleLogout() {
  try {
    await auth.logout()
  } finally {
    await router.replace('/login')
  }
}

function updateMobileState(event?: MediaQueryListEvent) {
  isMobile.value = event ? event.matches : Boolean(mobileQuery?.matches)
  if (!isMobile.value) app.closeMobileMenu()
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && app.mobileMenuOpen) app.closeMobileMenu()
}

watch(() => app.mobileMenuOpen, async (open) => {
  document.body.style.overflow = open && isMobile.value ? 'hidden' : ''
  await nextTick()
  if (open) {
    sidebarRef.value?.querySelector<HTMLElement>('.el-menu-item')?.focus()
  } else if (isMobile.value) {
    menuButtonRef.value?.$el?.focus?.()
  }
})

onMounted(async () => {
  app.initializeTheme()
  mobileQuery = window.matchMedia('(max-width: 900px)')
  updateMobileState()
  mobileQuery.addEventListener('change', updateMobileState)
  window.addEventListener('keydown', handleEscape)
  if (auth.token) {
    await auth.loadUser().catch(async () => {
      await auth.logout().catch(() => undefined)
      await router.replace('/login')
    })
  }
})

onBeforeUnmount(() => {
  mobileQuery?.removeEventListener('change', updateMobileState)
  window.removeEventListener('keydown', handleEscape)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.shell { min-height: 100vh; color: var(--text); background: var(--bg); }
.sidebar {
  position: fixed; inset: 0 auto 0 0; z-index: 40; width: 238px;
  display: flex; flex-direction: column; padding: 18px 12px 14px;
  color: var(--sidebar-text); background: var(--sidebar-bg); border-right: 1px solid var(--line);
  box-shadow: var(--shadow-sm); backdrop-filter: blur(20px);
  transition: width .22s ease, transform .22s ease, background-color .2s ease;
}
.sidebar.collapsed { width: 76px; }
.sidebar.collapsed .brand-copy, .sidebar.collapsed .student-card { display: none; }
.brand { min-height: 54px; display: flex; align-items: center; gap: 12px; padding: 0 8px 14px; }
.brand-mark {
  width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center;
  color: #fff; background: linear-gradient(135deg, #7657d6, #5486da); border-radius: 13px;
  box-shadow: 0 10px 24px rgba(118, 87, 214, .25); font-weight: 800;
}
.brand-copy { min-width: 0; }
.brand-copy strong, .brand-copy span { display: block; white-space: nowrap; }
.brand-copy strong { color: var(--text-strong); font-size: 17px; }
.brand-copy span { margin-top: 1px; color: var(--muted); font-size: 11px; }
.student-card {
  display: flex; align-items: center; gap: 11px; margin: 4px 2px 16px; padding: 12px;
  color: var(--text); background: var(--surface-soft); border: 1px solid var(--line); border-radius: var(--radius-md);
}
.student-card strong, .student-card span { display: block; line-height: 1.35; }
.student-card strong { max-width: 128px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.student-card span { margin-top: 3px; color: var(--muted); font-size: 11px; }
.nav-region { flex: 1; min-height: 0; overflow-y: auto; }
:deep(.el-menu) { background: transparent; border-right: 0; }
:deep(.el-menu-item) { min-height: 46px; margin: 3px 0; color: var(--sidebar-text); border-radius: 11px; font-weight: 600; }
:deep(.el-menu-item:hover) { color: var(--sidebar-active-text); background: var(--surface-hover); }
:deep(.el-menu-item.is-active) { color: var(--sidebar-active-text); background: var(--sidebar-active); box-shadow: inset 3px 0 0 var(--primary); }
:deep(.el-menu--collapse .el-menu-item) { justify-content: center; padding: 0 !important; }
.sidebar-footer { padding-top: 10px; border-top: 1px solid var(--line); }
.sidebar-footer .el-button { width: 100%; justify-content: flex-start; color: var(--muted); }
.sidebar.collapsed .sidebar-footer .el-button { justify-content: center; }
.workspace { min-width: 0; min-height: 100vh; margin-left: 238px; transition: margin-left .22s ease; }
.workspace.sidebar-collapsed { margin-left: 76px; }
.topbar {
  position: sticky; top: 0; z-index: 30; min-height: 76px; display: flex; align-items: center;
  justify-content: space-between; gap: 18px; padding: 12px 24px;
  background: var(--surface-glass); border-bottom: 1px solid var(--line); backdrop-filter: blur(20px);
}
.top-left { min-width: 0; display: flex; align-items: center; gap: 12px; }
.top-left h1 { margin: 0; color: var(--text-strong); font-size: 20px; line-height: 1.3; }
.top-left p { margin: 3px 0 0; color: var(--muted); font-size: 12px; }
.top-actions, .user-summary { display: flex; align-items: center; }
.top-actions { gap: 10px; }
.user-summary { gap: 9px; padding: 3px 10px 3px 3px; border: 1px solid var(--line); border-radius: 999px; background: var(--surface-soft); }
.user-summary strong, .user-summary span { display: block; max-width: 130px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.3; }
.user-summary strong { color: var(--text-strong); font-size: 12px; }
.user-summary span { color: var(--muted); font-size: 10px; }
.mobile-menu-button { display: none; }
.content { min-height: calc(100vh - 76px); padding: 24px; overflow: auto; }
.mobile-overlay { position: fixed; inset: 0; z-index: 35; display: none; padding: 0; background: rgba(8, 7, 14, .5); border: 0; backdrop-filter: blur(3px); }
.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 900px) {
  .sidebar, .sidebar.collapsed { width: min(82vw, 276px); transform: translateX(-104%); }
  .sidebar.mobile-open { transform: translateX(0); }
  .sidebar.collapsed .brand-copy { display: block; }
  .sidebar.collapsed .student-card { display: flex; }
  .workspace, .workspace.sidebar-collapsed { margin-left: 0; }
  .mobile-menu-button, .mobile-overlay { display: inline-flex; }
  .topbar { min-height: 68px; padding: 10px 14px; }
  .top-left h1 { font-size: 18px; }
  .top-left p, .user-summary { display: none; }
  .content { min-height: calc(100vh - 68px); padding: 16px 14px 24px; }
}
</style>
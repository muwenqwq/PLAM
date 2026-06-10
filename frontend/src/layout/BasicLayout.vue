<template>
  <el-container class="shell">
    <el-aside :width="app.collapsed ? '72px' : '236px'" class="sidebar">
      <div class="brand">
        <div class="brand-mark">智</div>
        <div v-if="!app.collapsed">
          <strong>智学工坊</strong>
          <span>EduAgent Studio</span>
        </div>
      </div>
      <el-menu
        router
        :collapse="app.collapsed"
        :default-active="$route.path"
        background-color="#0f172a"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="top-left">
          <el-button text :icon="app.collapsed ? Expand : Fold" @click="app.toggleMenu()" />
          <div>
            <h1>{{ $route.meta.title || '工作台' }}</h1>
            <p>Java 主后端 + MySQL + Python AI Mock 微服务演示系统</p>
          </div>
        </div>
        <div class="top-actions">
          <el-tag effect="plain">Mock AI 可演示</el-tag>
          <span class="user-name">{{ auth.user?.nickname || auth.user?.username || '演示用户' }}</span>
          <el-button @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Expand, Fold } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const app = useAppStore()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const menuItems = computed(() => route.matched[0]?.children?.map((item) => ({
  path: `/${item.path}`,
  title: item.meta?.title as string,
  icon: item.meta?.icon as string
})) || [])

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
}

.sidebar {
  background: #0f172a;
  color: #fff;
  transition: width .2s;
}

.brand {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-weight: 800;
}

.brand strong,
.brand span {
  display: block;
}

.brand span {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 2px;
}

.topbar {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--line);
}

.top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-left h1 {
  margin: 0;
  font-size: 18px;
  line-height: 24px;
}

.top-left p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: var(--text);
}

.content {
  padding: 22px;
}

:deep(.el-menu) {
  border-right: 0;
}
</style>

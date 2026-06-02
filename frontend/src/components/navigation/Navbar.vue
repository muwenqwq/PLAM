<template>
  <header class="navbar">
    <div class="nav-left">
      <h1 class="greeting">你好，{{ userStore.name }} 👋</h1>
      <div class="subtitle">
        <span class="target-icon">🎯</span>
        当前目标：{{ userStore.target }}
      </div>
    </div>
    
    <div class="nav-right">
      <button class="icon-btn">🔔</button> 
      <div class="avatar">
        <img :src="userStore.imgSrc" alt="User Avatar" />
      </div>
    </div>
  </header>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.navbar {
  height: 80px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  /* 如果你需要纯白背景可以加上下面两行 */
  /* background-color: #ffffff; */
  /* border-bottom: 1px solid #edf2f7; */
}

.nav-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.greeting {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a202c;
}

.subtitle {
  font-size: 13px;
  color: #718096;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #4a5568;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #e2e8f0;
  cursor: pointer;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
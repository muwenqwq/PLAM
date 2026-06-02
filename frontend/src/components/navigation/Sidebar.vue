<template>
  <aside class="sidebar">
    <div class="logo-area">
      <div class="logo-icon">💡</div> <span class="logo-text">智能学习助手</span>
    </div>

    <nav class="nav-menu">
      <router-link 
        v-for="item in topMenus" 
        :key="item.path" 
        :to="item.path" 
        class="nav-item"
        active-class="active" 
      >
        <span class="icon">{{ item.icon }}</span> {{ item.cn }}
      </router-link>
    </nav>
    
    <div class="bottom-menu">
      <div class="divider"></div>
      <router-link
        v-for="item in bottomMenus"
        :key="item.path"
        :to="item.path"
        class="nav-item"
      >
        <span class="icon">{{ item.icon }}</span>  {{ item.cn }}
      </router-link>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { menuConfig } from '@/config/menu';

const topMenus = computed(() => {
  return menuConfig.filter(item => item.position !== 'bottom');
});
const bottomMenus = computed(() => {
  return menuConfig.filter(item => item.position === 'bottom');
});
</script>



<style scoped>
.sidebar {
  width: 260px;
  background-color: #ffffff;
  border-right: 1px solid #edf2f7;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  flex-shrink: 0;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 12px 32px 12px;
}
.logo-icon {
  font-size: 24px;
}
.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
}

.nav-menu {
  flex: 1; /* 占据剩余空间，把底部菜单往下挤 */
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  text-decoration: none;
  color: #4a5568;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-item:hover {
  background-color: #f7fafc;
}

/* 高亮选中状态 */
.nav-item.active {
  background-color: #e6f0fd; /* 浅蓝色背景 */
  color: #2b6cb0; /* 深蓝色文字 */
  font-weight: 600;
}

.bottom-menu {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.divider {
  height: 1px;
  background-color: #edf2f7;
  margin: 16px 12px;
}
</style>
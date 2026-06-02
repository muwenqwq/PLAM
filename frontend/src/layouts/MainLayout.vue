<template>
  <div class="layout-wrapper">
    <Sidebar />
    
    <div class="right-container">
      <Navbar />
      
      <main class="main-content">
        <div class="business-placeholder">
          <router-view />
          </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from '@/components/navigation/Sidebar.vue';
import Navbar from '@/components/navigation/Navbar.vue';
</script>

<style scoped>
.layout-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: #f8f9fc; /* 原型主体的浅灰背景色 */
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.right-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止子元素撑破 Flex 布局 */
}

.main-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto; /* 内容超出时滚动 */
}
</style>
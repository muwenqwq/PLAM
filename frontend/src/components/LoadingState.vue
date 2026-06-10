<template>
  <div class="loading">
    <el-icon class="spin" size="26"><Loading /></el-icon>
    <span>{{ text || '正在处理，请稍候' }}</span>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
defineProps<{ text?: string }>()
</script>

<style scoped>
.loading {
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--muted);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

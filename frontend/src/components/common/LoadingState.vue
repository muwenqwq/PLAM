<template>
  <div class="loading-state">
    <div class="loading-spinner">
      <el-icon :size="size" class="is-loading"><Loading /></el-icon>
    </div>
    <p v-if="text" class="loading-text">{{ text }}</p>
  </div>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'

defineProps({
  text: { type: String, default: '加载中...' },
  size: { type: Number, default: 32 }
})
</script>

<style scoped>
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #718096;
}
.loading-spinner {
  color: #3182ce;
}
.loading-text {
  margin-top: 12px;
  font-size: 14px;
}
</style>

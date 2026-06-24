<template>
  <div class="empty">
    <span class="empty-icon"><el-icon size="30"><Box /></el-icon></span>
    <strong>{{ title || '暂无数据' }}</strong>
    <p>{{ description || '完成前面的操作后，这里会展示结果。' }}</p>
  </div>
</template>
<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  description?: string
}>()
</script>
<style scoped>
.empty { min-height: 180px; display: grid; place-items: center; align-content: center; gap: 8px; padding: 24px; color: var(--muted); text-align: center; border: 1px dashed var(--line-strong); border-radius: var(--radius-md); background: var(--surface-soft); }
.empty-icon { width: 56px; height: 56px; display: grid; place-items: center; color: var(--primary); background: var(--primary-soft); border-radius: 16px; }
strong { color: var(--text-strong); }
p { max-width: 480px; margin: 0; line-height: 1.7; }
</style>
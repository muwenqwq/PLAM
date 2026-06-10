<template>
  <div class="empty">
    <el-icon size="38"><Box /></el-icon>
    <strong>{{ title || '暂无数据' }}</strong>
    <p>{{ description || '完成左侧操作后，这里会展示结果。' }}</p>
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'
defineProps<{ title?: string; description?: string }>()
</script>

<style scoped>
.empty {
  min-height: 160px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--muted);
  border: 1px dashed var(--line);
  border-radius: 8px;
  background: var(--surface-soft);
}

strong {
  color: var(--text);
}

p {
  margin: 0;
}
</style>

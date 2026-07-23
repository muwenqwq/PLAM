<template>
  <div v-if="$slots.default" class="page-actions"><slot /></div>
</template>
<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>
<style scoped>
.page-actions { display: flex; justify-content: flex-end; gap: 10px; align-items: center; flex-wrap: wrap; }
@media (max-width: 720px) { .page-actions :deep(.el-button) { flex: 1; } }
</style>

<template>
  <div class="page-header">
    <div class="heading-copy">
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="actions"><slot /></div>
  </div>
</template>
<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>
<style scoped>
.page-header { display: flex; justify-content: space-between; gap: 20px; align-items: center; padding: 22px 24px; background: var(--surface-glass); border: 1px solid var(--line); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); backdrop-filter: blur(18px); }
.heading-copy { min-width: 0; }
h2 { margin: 0; color: var(--text-strong); font-size: 24px; line-height: 1.35; }
p { margin: 6px 0 0; color: var(--muted); line-height: 1.7; }
.actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
@media (max-width: 720px) { .page-header { display: grid; padding: 18px; } h2 { font-size: 21px; } .actions :deep(.el-button) { flex: 1; } }
</style>
<template>
  <div class="page-header">
    <div>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <div class="actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 18px;
  background: linear-gradient(135deg, #ffffff, #f0f7ff);
  border: 1px solid var(--line);
  border-radius: 8px;
}

h2 {
  margin: 0 0 6px;
  font-size: 22px;
}

p {
  margin: 0;
  color: var(--muted);
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>

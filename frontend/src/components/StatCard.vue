<template>
  <div class="stat-card">
    <div class="icon"><el-icon><component :is="icon || 'DataLine'" /></el-icon></div>
    <div>
      <span>{{ label }}</span>
      <strong>{{ value ?? '-' }}</strong>
      <small>{{ hint }}</small>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value?: string | number; hint?: string; icon?: string }>()
</script>

<style scoped>
.stat-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow);
}

.icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: var(--primary);
  background: var(--primary-soft);
}

span,
small {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

strong {
  display: block;
  margin: 4px 0;
  font-size: 24px;
}
</style>

<template>
  <div class="stat-card">
    <div class="icon"><el-icon><component :is="icon || 'DataLine'" /></el-icon></div>
    <div class="stat-copy"><span>{{ label }}</span><strong>{{ value ?? '-' }}</strong><small>{{ hint }}</small></div>
  </div>
</template>
<script setup lang="ts">
defineProps<{
  label: string
  value?: string | number
  hint?: string
  icon?: string
}>()
</script>
<style scoped>
.stat-card { min-height: 112px; display: flex; align-items: center; gap: 14px; padding: 18px; border: 1px solid var(--line); border-radius: var(--radius-lg); background: var(--surface-glass); box-shadow: var(--shadow-sm); transition: transform .2s ease, border-color .2s ease; }
.stat-card:hover { transform: translateY(-2px); border-color: var(--primary-border); }
.icon { width: 46px; height: 46px; flex: 0 0 46px; display: grid; place-items: center; border-radius: 13px; color: var(--primary); background: var(--primary-soft); border: 1px solid var(--primary-border); font-size: 21px; }
.stat-copy { min-width: 0; }
span, small { display: block; color: var(--muted); font-size: 12px; }
strong { display: block; margin: 2px 0; color: var(--text-strong); font-size: 25px; font-variant-numeric: tabular-nums; }
</style>
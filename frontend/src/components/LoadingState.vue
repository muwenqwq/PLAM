<template><div class="loading"><span class="loading-icon"><el-icon class="spin" size="24"><Loading /></el-icon></span><span>{{ text || '正在处理，请稍候' }}</span></div></template>
<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

defineProps<{
  text?: string
}>()
</script>
<style scoped>
.loading { min-height: 150px; display: flex; align-items: center; justify-content: center; gap: 12px; color: var(--muted); }
.loading-icon { width: 44px; height: 44px; display: grid; place-items: center; color: var(--primary); background: var(--primary-soft); border-radius: 13px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
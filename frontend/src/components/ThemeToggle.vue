<template>
  <el-tooltip :content="isDark ? '切换到白色主题' : '切换到夜间主题'" placement="bottom">
    <el-button
      class="theme-toggle"
      circle
      :aria-label="isDark ? '切换到白色主题' : '切换到夜间主题'"
      @click="app.toggleTheme()"
    >
      <el-icon :size="18">
        <Sunny v-if="isDark" />
        <Moon v-else />
      </el-icon>
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'

const app = useAppStore()
const isDark = computed(() => app.theme === 'dark')

onMounted(() => app.initializeTheme())
</script>

<style scoped>
.theme-toggle {
  width: 42px;
  height: 42px;
  color: var(--text);
  background: var(--surface-glass);
  border-color: var(--line);
  box-shadow: var(--shadow-sm);
}

.theme-toggle:hover,
.theme-toggle:focus-visible {
  color: var(--primary);
  background: var(--primary-soft);
  border-color: var(--primary-border);
}
</style>
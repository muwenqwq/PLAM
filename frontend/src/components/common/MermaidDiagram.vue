<template>
  <div class="mermaid-wrapper">
    <div v-if="loading" class="mermaid-loading">渲染中...</div>
    <div v-else-if="error" class="mermaid-error">{{ error }}</div>
    <div v-else ref="container" class="mermaid-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import mermaid from 'mermaid'

const props = defineProps({
  code: { type: String, required: true },
  id: { type: String, default: () => `mermaid-${Math.random().toString(36).slice(2, 8)}` }
})

const container = ref(null)
const loading = ref(false)
const error = ref('')

mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose' })

async function render() {
  if (!props.code || !container.value) return
  loading.value = true
  error.value = ''
  try {
    // 清空容器
    container.value.innerHTML = ''
    const { svg } = await mermaid.render(props.id, props.code)
    container.value.innerHTML = svg
  } catch (e) {
    error.value = '图表渲染失败，请检查语法'
    console.error('Mermaid render error:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => { nextTick(render) })
watch(() => props.code, () => { nextTick(render) })
</script>

<style scoped>
.mermaid-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}
.mermaid-container {
  max-width: 100%;
  overflow-x: auto;
}
.mermaid-container :deep(svg) {
  max-width: 100%;
  height: auto;
}
.mermaid-loading,
.mermaid-error {
  padding: 24px;
  text-align: center;
  color: var(--text-subtle);
  font-size: 14px;
}
.mermaid-error {
  color: var(--color-error);
  background: var(--color-error-bg);
  border-radius: 8px;
}
</style>

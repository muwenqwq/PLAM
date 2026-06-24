<template>
  <div class="mermaid-wrap">
    <div v-if="error" class="mermaid-error">{{ error }}</div>
    <div v-else ref="container" class="mermaid-canvas" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import mermaid from 'mermaid'
import { useAppStore } from '@/stores/app'

const props = defineProps<{ code?: string }>()
const app = useAppStore()
const container = ref<HTMLElement>()
const error = ref('')

function configureMermaid() {
  mermaid.initialize({
    startOnLoad: false,
    theme: app.theme === 'dark' ? 'dark' : 'default',
    securityLevel: 'strict',
    suppressErrorRendering: true,
    logLevel: 'fatal'
  })
}

function normalizeCode(raw?: string) {
  const fallback = 'graph TD\n  A["暂无图谱"] --> B["请重新生成"]'
  if (!raw?.trim()) return fallback
  let code = raw.trim()
  code = code.replace(/^```mermaid/i, '').replace(/^```/, '').replace(/```$/, '').trim()
  const lines = code.split(/\r?\n/)
  const start = lines.findIndex((line) => /^(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie|mindmap|timeline)\b/.test(line.trim()))
  if (start > 0) code = lines.slice(start).join('\n').trim()
  if (!/^(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie|mindmap|timeline)\b/.test(code)) {
    return fallback
  }
  return code
}

async function render() {
  await nextTick()
  if (!container.value) return
  container.value.innerHTML = ''
  error.value = ''
  if (!props.code?.trim()) return
  try {
    configureMermaid()
    const code = normalizeCode(props.code)
    const valid = await mermaid.parse(code, { suppressErrors: true })
    if (!valid) {
      throw new Error('Mermaid 源码格式不正确，请重新生成图谱。')
    }
    const id = `mermaid-${Date.now()}-${Math.floor(Math.random() * 1000)}`
    const result = await mermaid.render(id, code)
    container.value.innerHTML = result.svg
  } catch (e: any) {
    if (container.value) container.value.innerHTML = ''
    error.value = e?.message || 'Mermaid 渲染失败，请重新生成图谱。'
  }
}

onMounted(render)
watch(() => props.code, render)
watch(() => app.theme, render)
</script>

<style scoped>
.mermaid-wrap {
  width: 100%;
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.mermaid-canvas {
  min-height: 220px;
}

.mermaid-error {
  color: var(--danger);
}
</style>

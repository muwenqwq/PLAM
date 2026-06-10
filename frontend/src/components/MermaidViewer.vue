<template>
  <div class="mermaid-wrap">
    <div v-if="error" class="mermaid-error">{{ error }}</div>
    <div v-else ref="container" class="mermaid-canvas" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import mermaid from 'mermaid'

const props = defineProps<{ code?: string }>()
const container = ref<HTMLElement>()
const error = ref('')

mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'strict' })

async function render() {
  await nextTick()
  if (!container.value || !props.code) return
  try {
    error.value = ''
    const id = `mermaid-${Date.now()}-${Math.floor(Math.random() * 1000)}`
    const result = await mermaid.render(id, props.code)
    container.value.innerHTML = result.svg
  } catch (e: any) {
    error.value = e?.message || 'Mermaid 渲染失败'
  }
}

onMounted(render)
watch(() => props.code, render)
</script>

<style scoped>
.mermaid-wrap {
  width: 100%;
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.mermaid-canvas {
  min-height: 220px;
}

.mermaid-error {
  color: var(--danger);
}
</style>

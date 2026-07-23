<template>
  <div class="mermaid-viewer" :class="{ fullscreen }">
    <div v-if="props.code?.trim() && !error" class="viewer-toolbar">
      <span class="zoom-label">{{ Math.round(scale * 100) }}%</span>
      <div class="viewer-actions">
        <el-tooltip content="缩小" placement="top">
          <el-button circle size="small" :disabled="scale <= MIN_SCALE" aria-label="缩小图谱" @click="zoomOut">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="放大" placement="top">
          <el-button circle size="small" :disabled="scale >= MAX_SCALE" aria-label="放大图谱" @click="zoomIn">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="恢复原始大小" placement="top">
          <el-button circle size="small" aria-label="恢复原始大小" @click="resetZoom">
            <el-icon><RefreshLeft /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="fullscreen ? '退出全屏' : '全屏查看'" placement="top">
          <el-button circle size="small" aria-label="切换全屏查看" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="下载 PNG 图片" placement="top">
          <el-button circle size="small" type="primary" aria-label="下载图谱图片" @click="downloadPng">
            <el-icon><Download /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
    <div class="mermaid-wrap" @wheel.ctrl.prevent="handleWheel">
      <div v-if="error" class="mermaid-error">{{ error }}</div>
      <div
        v-else
        ref="container"
        class="mermaid-canvas"
        :style="{ transform: `scale(${scale})` }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { Download, FullScreen, RefreshLeft, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import mermaid from 'mermaid'
import { useAppStore } from '@/stores/app'

const MIN_SCALE = 0.5
const MAX_SCALE = 2.5
const SCALE_STEP = 0.2

const props = defineProps<{ code?: string; filename?: string }>()
const app = useAppStore()
const container = ref<HTMLElement>()
const error = ref('')
const scale = ref(1)
const fullscreen = ref(false)

function configureMermaid() {
  mermaid.initialize({
    startOnLoad: false,
    theme: app.theme === 'dark' ? 'dark' : 'default',
    securityLevel: 'strict',
    suppressErrorRendering: true,
    logLevel: 'fatal',
    flowchart: {
      htmlLabels: false,
      useMaxWidth: false
    },
    themeVariables: {
      fontFamily: 'Arial, Microsoft YaHei, sans-serif'
    }
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
  scale.value = 1
  if (!props.code?.trim()) return
  try {
    configureMermaid()
    const code = normalizeCode(props.code)
    const valid = await mermaid.parse(code, { suppressErrors: true })
    if (!valid) throw new Error('图谱格式不正确，请重新生成。')
    const id = `mermaid-${Date.now()}-${Math.floor(Math.random() * 1000)}`
    const result = await mermaid.render(id, code)
    container.value.innerHTML = result.svg
    const svg = container.value.querySelector('svg')
    if (svg) {
      svg.removeAttribute('height')
      svg.style.maxWidth = 'none'
      svg.style.height = 'auto'
    }
  } catch (e: any) {
    if (container.value) container.value.innerHTML = ''
    error.value = e?.message || '图谱渲染失败，请重新生成。'
  }
}

function setScale(value: number) {
  scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, Number(value.toFixed(2))))
}

function zoomIn() {
  setScale(scale.value + SCALE_STEP)
}

function zoomOut() {
  setScale(scale.value - SCALE_STEP)
}

function resetZoom() {
  scale.value = 1
}

function handleWheel(event: WheelEvent) {
  setScale(scale.value + (event.deltaY < 0 ? SCALE_STEP : -SCALE_STEP))
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
}

async function downloadPng() {
  const svg = container.value?.querySelector('svg')
  if (!svg) return
  const viewBox = svg.viewBox?.baseVal
  const bounds = svg.getBoundingClientRect()
  const width = Math.max(1, Math.round(viewBox?.width || bounds.width || 1200))
  const height = Math.max(1, Math.round(viewBox?.height || bounds.height || 800))
  const clone = prepareExportSvg(svg, width, height)
  const source = new XMLSerializer().serializeToString(clone)
  const svgBlob = new Blob([source], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  try {
    const image = new Image()
    image.crossOrigin = 'anonymous'
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve()
      image.onerror = () => reject(new Error('图谱图片转换失败'))
      image.src = url
    })
    const pixelRatio = Math.max(0.5, Math.min(2, 8000 / width, 8000 / height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(width * pixelRatio))
    canvas.height = Math.max(1, Math.round(height * pixelRatio))
    const context = canvas.getContext('2d')
    if (!context) throw new Error('浏览器不支持图片导出')
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    const pngBlob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
    if (!pngBlob) throw new Error('图谱图片生成失败')
    const pngUrl = URL.createObjectURL(pngBlob)
    const link = document.createElement('a')
    link.href = pngUrl
    link.download = `${safeFilename(props.filename || '知识图谱')}.png`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(pngUrl)
    ElMessage.success('知识图谱图片已开始下载')
  } catch (e: any) {
    ElMessage.error(e?.message || '知识图谱图片下载失败')
  } finally {
    URL.revokeObjectURL(url)
  }
}

function prepareExportSvg(svg: SVGSVGElement, width: number, height: number) {
  const clone = svg.cloneNode(true) as SVGSVGElement
  clone.setAttribute('width', String(width))
  clone.setAttribute('height', String(height))
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  clone.setAttribute('xmlns:xlink', 'http://www.w3.org/1999/xlink')
  clone.querySelectorAll('foreignObject').forEach((node) => {
    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text')
    const x = Number(node.getAttribute('x') || 0)
    const y = Number(node.getAttribute('y') || 0)
    const nodeHeight = Number(node.getAttribute('height') || 20)
    text.setAttribute('x', String(x))
    text.setAttribute('y', String(y + Math.max(14, nodeHeight / 2)))
    text.setAttribute('dominant-baseline', 'middle')
    text.setAttribute('font-family', 'Arial, Microsoft YaHei, sans-serif')
    text.setAttribute('font-size', '14')
    text.setAttribute('fill', '#1f2937')
    text.textContent = node.textContent?.replace(/\s+/g, ' ').trim() || ''
    node.replaceWith(text)
  })
  clone.querySelectorAll('image').forEach((node) => {
    const href = node.getAttribute('href') || node.getAttribute('xlink:href') || ''
    if (href && !href.startsWith('data:') && !href.startsWith('blob:')) node.remove()
  })
  clone.querySelectorAll('style').forEach((node) => {
    node.textContent = (node.textContent || '')
      .replace(/@import[^;]+;/gi, '')
      .replace(/@font-face\s*\{[^}]*\}/gi, '')
  })
  return clone
}

function safeFilename(value: string) {
  return value.replace(/[\\/:*?"<>|]+/g, '_').trim() || '知识图谱'
}

onMounted(render)
watch(() => props.code, render)
watch(() => app.theme, render)
</script>

<style scoped>
.mermaid-viewer {
  position: relative;
  min-width: 0;
}

.viewer-toolbar {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-bottom: 0;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  background: var(--surface);
}

.viewer-actions {
  display: flex;
  gap: 6px;
}

.zoom-label {
  min-width: 48px;
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

.mermaid-wrap {
  width: 100%;
  min-height: 380px;
  max-height: 620px;
  overflow: auto;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  background: var(--surface-soft);
}

.mermaid-canvas {
  min-width: 720px;
  min-height: 320px;
  transform-origin: top left;
  transition: transform .16s ease;
}

.mermaid-canvas :deep(svg) {
  display: block;
  margin: 0 auto;
}

.mermaid-error {
  color: var(--danger);
}

.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  grid-template-rows: auto 1fr;
  padding: 18px;
  background: var(--page-bg);
}

.fullscreen .mermaid-wrap {
  max-height: none;
  min-height: 0;
}

@media (max-width: 720px) {
  .mermaid-wrap {
    min-height: 300px;
    padding: 12px;
  }

  .mermaid-canvas {
    min-width: 620px;
  }
}
</style>

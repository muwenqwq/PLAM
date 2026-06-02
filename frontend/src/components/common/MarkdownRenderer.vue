<template>
  <div class="markdown-body" v-html="renderedHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps({
  content: { type: String, default: '' }
})

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch { /* fallback */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedHtml = computed(() => {
  return props.content ? md.render(props.content) : ''
})
</script>

<style scoped>
.markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #2d3748;
  word-wrap: break-word;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 1.2em;
  margin-bottom: 0.6em;
  font-weight: 600;
  color: #1a202c;
}
.markdown-body :deep(h1) { font-size: 1.5em; }
.markdown-body :deep(h2) { font-size: 1.3em; }
.markdown-body :deep(h3) { font-size: 1.1em; }
.markdown-body :deep(p) { margin: 0.6em 0; }
.markdown-body :deep(code) {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #e53e3e;
}
.markdown-body :deep(pre.hljs) {
  background: #f8f9fa;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 12px 0;
}
.markdown-body :deep(pre.hljs code) {
  background: none;
  padding: 0;
  color: inherit;
  font-size: 14px;
}
.markdown-body :deep(blockquote) {
  border-left: 4px solid #3182ce;
  padding-left: 16px;
  color: #718096;
  margin: 12px 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.5em;
  margin: 0.6em 0;
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f7fafc;
  font-weight: 600;
}
.markdown-body :deep(a) {
  color: #3182ce;
  text-decoration: none;
}
.markdown-body :deep(a:hover) {
  text-decoration: underline;
}
.markdown-body :deep(strong) {
  font-weight: 600;
}
</style>

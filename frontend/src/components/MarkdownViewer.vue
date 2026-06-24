<template>
  <article class="markdown" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import texmath from 'markdown-it-texmath'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const props = defineProps<{ content?: string }>()
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
md.use(texmath, {
  engine: katex,
  delimiters: ['dollars', 'brackets', 'doxygen', 'gitlab'],
  katexOptions: {
    macros: { '\\RR': '\\mathbb{R}' },
    throwOnError: false
  }
})
const html = computed(() => md.render(props.content || ''))
</script>

<style scoped>
.markdown {
  line-height: 1.75;
  color: var(--text);
}

.markdown :deep(h1),
.markdown :deep(h2),
.markdown :deep(h3) {
  margin: 12px 0 8px;
}

.markdown :deep(pre) {
  padding: 12px;
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: var(--radius-md);
}

.markdown :deep(code) {
  font-family: "JetBrains Mono", Consolas, monospace;
}
</style>

<template>
  <el-card shadow="never" class="resource-card">
    <template #header><div class="header"><strong>{{ resource.title }}</strong><el-tag size="small" effect="plain">{{ typeName(resource.resourceType) }}</el-tag></div></template>
    <p class="summary">{{ resource.outputSummary || '暂无摘要' }}</p>
    <div class="meta"><span>{{ resource.subject || '通用学科' }}</span><span>质量分 {{ resource.qualityScore || '-' }}</span></div>
    <slot />
  </el-card>
</template>
<script setup lang="ts">
defineProps<{ resource: any }>()
const names: Record<string, string> = { plan: '学习计划', lecture_note: '课程笔记', summary: '知识点总结', review_outline: '复习提纲', mistake_review: '错题整理', quiz_set: '练习题', case_task: '案例任务', knowledge_graph: '思维导图' }
function typeName(type: string) { return names[type] || type }
</script>
<style scoped>
.resource-card { border-radius: var(--radius-lg); transition: transform .2s ease, border-color .2s ease; }
.resource-card:hover { transform: translateY(-2px); border-color: var(--primary-border); }
.header, .meta { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.header strong { color: var(--text-strong); overflow-wrap: anywhere; }
.summary { min-height: 44px; color: var(--muted); line-height: 1.7; }
.meta { color: var(--muted); font-size: 12px; }
</style>

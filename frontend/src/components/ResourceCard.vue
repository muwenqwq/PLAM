<template>
  <el-card shadow="never" class="resource-card">
    <template #header>
      <div class="header">
        <strong>{{ resource.title }}</strong>
        <el-tag size="small">{{ typeName(resource.resourceType) }}</el-tag>
      </div>
    </template>
    <p class="summary">{{ resource.outputSummary || '暂无摘要' }}</p>
    <div class="meta">
      <span>{{ resource.subject || '通用学科' }}</span>
      <span>质量分 {{ resource.qualityScore || '-' }}</span>
    </div>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ resource: any }>()

const names: Record<string, string> = {
  plan: '学习计划',
  lecture_note: '讲义',
  review_outline: '复习提纲',
  quiz_set: '习题集',
  case_task: '案例任务',
  knowledge_graph: '知识图谱',
  ppt_outline: 'PPT 大纲'
}

function typeName(type: string) {
  return names[type] || type
}
</script>

<style scoped>
.resource-card {
  border-radius: 8px;
}

.header,
.meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.summary {
  min-height: 44px;
  color: var(--muted);
}

.meta {
  color: var(--muted);
  font-size: 12px;
}
</style>

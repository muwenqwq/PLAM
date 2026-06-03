<template>
  <el-card class="resource-card" shadow="hover" @click="$emit('click', resource)">
    <div class="card-header">
      <el-tag :type="tagType" size="small">{{ typeLabel }}</el-tag>
      <span class="score" v-if="resource.qualityScore">
        ⭐ {{ resource.qualityScore }}
      </span>
    </div>
    <h4 class="card-title">{{ resource.title }}</h4>
    <div class="card-footer">
      <span class="time">{{ formatTime(resource.createdAt) }}</span>
      <el-tag :type="statusType" size="small" effect="plain">{{ statusLabel }}</el-tag>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  resource: { type: Object, required: true }
})

defineEmits(['click'])

const typeMap = {
  explanation_doc: { label: '📄 讲义', type: '' },
  mindmap: { label: '🧠 导图', type: 'success' },
  quiz: { label: '📝 练习', type: 'warning' },
  reading_material: { label: '📖 阅读', type: 'info' },
  code_lab: { label: '💻 实验', type: 'danger' }
}

const statusMap = {
  completed: { label: '已完成', type: 'success' },
  generating: { label: '生成中', type: 'warning' },
  failed: { label: '失败', type: 'danger' }
}

const typeLabel = computed(() => typeMap[props.resource.resourceType]?.label || props.resource.resourceType)
const tagType = computed(() => typeMap[props.resource.resourceType]?.type || '')
const statusLabel = computed(() => statusMap[props.resource.status]?.label || props.resource.status)
const statusType = computed(() => statusMap[props.resource.status]?.type || 'info')

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.resource-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.resource-card:hover {
  transform: translateY(-2px);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.score {
  font-size: 13px;
  color: var(--color-warning);
}
.card-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-heading);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.time {
  font-size: 12px;
  color: var(--text-placeholder);
}
</style>

<template>
  <div class="timeline-node" :class="node.status">
    <div class="node-indicator">
      <div class="indicator-dot">
        <span v-if="node.status === 'completed'">✓</span>
        <span v-else-if="node.status === 'in_progress'">▶</span>
        <span v-else>{{ node.order }}</span>
      </div>
      <div v-if="!isLast" class="indicator-line"></div>
    </div>

    <div class="node-content">
      <div class="node-header">
        <h4 class="node-title">{{ node.knowledgePoint }}</h4>
        <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
      </div>
      <p class="node-reason">{{ node.reason }}</p>
      <div class="node-meta">
        <span class="meta-item">⏱️ {{ node.estimatedDuration }}</span>
        <span class="meta-item">✅ {{ node.completionCriteria }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  isLast: { type: Boolean, default: false }
})

const statusMap = {
  completed: { label: '已完成', type: 'success' },
  in_progress: { label: '进行中', type: 'warning' },
  not_started: { label: '未开始', type: 'info' }
}

const statusLabel = computed(() => statusMap[props.node.status]?.label || props.node.status)
const statusType = computed(() => statusMap[props.node.status]?.type || 'info')
</script>

<style scoped>
.timeline-node {
  display: flex;
  gap: 16px;
}
.node-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}
.indicator-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-subtle);
  flex-shrink: 0;
  transition: all 0.3s;
}
.timeline-node.completed .indicator-dot {
  background: var(--color-success);
  color: white;
}
.timeline-node.in_progress .indicator-dot {
  background: var(--color-warning);
  color: white;
}
.indicator-line {
  width: 2px;
  flex: 1;
  min-height: 24px;
  background: var(--border-default);
  margin: 4px 0;
}
.timeline-node.completed .indicator-line {
  background: var(--color-success);
}
.node-content {
  flex: 1;
  padding-bottom: 24px;
  min-width: 0;
}
.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.node-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-heading);
}
.node-reason {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-subtle);
  line-height: 1.5;
}
.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.meta-item {
  font-size: 12px;
  color: var(--text-placeholder);
}
</style>

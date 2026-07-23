<template>
  <el-timeline>
    <el-timeline-item
      v-for="step in steps"
      :key="step.id || step.stepOrder"
      :timestamp="step.agentName"
      placement="top"
      type="primary"
    >
      <el-card shadow="never">
        <div class="step-head">
          <strong>第 {{ step.stepOrder }} 步 · {{ stepTypeName(step.stepType) }}</strong>
          <el-tag size="small" :type="step.executionStatus === 'failed' ? 'danger' : 'success'">{{ statusName(step.executionStatus) }}</el-tag>
        </div>
        <p>{{ step.outputSummary }}</p>
        <el-collapse v-if="step.inputJson || step.resultJson" class="step-detail">
          <el-collapse-item v-if="step.inputJson" title="输入" :name="`input-${step.id || step.stepOrder}`">
            <pre>{{ formatJson(step.inputJson) }}</pre>
          </el-collapse-item>
          <el-collapse-item v-if="step.resultJson" title="输出" :name="`result-${step.id || step.stepOrder}`">
            <pre>{{ formatJson(step.resultJson) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup lang="ts">
defineProps<{ steps: any[] }>()

function formatJson(value: any) {
  if (!value) return ''
  if (typeof value === 'string') {
    try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value }
  }
  return JSON.stringify(value, null, 2)
}

function stepTypeName(value?: string) {
  return ({ planning: '目标规划', grounding: '资料与知识整理', generation: '内容生成', review: '质量复核' } as Record<string, string>)[value || ''] || value || '执行'
}

function statusName(value?: string) {
  return ({ succeeded: '已完成', running: '进行中', failed: '失败', pending: '等待中' } as Record<string, string>)[value || ''] || value || '已完成'
}
</script>

<style scoped>
.step-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

p {
  margin: 8px 0 0;
  color: var(--muted);
}

.step-detail {
  margin-top: 10px;
  border-top: 0;
  border-bottom: 0;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
  color: var(--text);
}
</style>

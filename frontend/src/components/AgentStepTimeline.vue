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
          <strong>第 {{ step.stepOrder }} 步 · {{ step.stepType }}</strong>
          <el-tag size="small">{{ step.executionStatus || 'succeeded' }}</el-tag>
        </div>
        <p>{{ step.outputSummary }}</p>
      </el-card>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup lang="ts">
defineProps<{ steps: any[] }>()
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
</style>

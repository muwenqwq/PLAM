<template>
  <div class="agent-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>🤖 Agent 运行记录</h2>
      <p>查看多智能体协作的运行轨迹，包括各 Agent 的输入、输出和耗时</p>
    </div>

    <!-- 任务 ID 输入 -->
    <el-card class="search-card" shadow="never">
      <div class="search-form">
        <el-input
          v-model="taskId"
          placeholder="输入任务 ID（例如：task_mock_001）"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>🔍</template>
        </el-input>
        <el-button type="primary" :loading="agentRunStore.loading" @click="handleSearch">
          查询
        </el-button>
      </div>
    </el-card>

    <!-- 运行记录 -->
    <LoadingState v-if="agentRunStore.loading" text="加载运行记录中..." />
    <EmptyState
      v-else-if="!agentRunStore.runs.length && searched"
      icon="🤖"
      title="暂无运行记录"
      description="输入任务 ID 查询 Agent 运行轨迹"
    />

    <!-- 记录列表 -->
    <div v-else-if="agentRunStore.runs.length" class="runs-list">
      <!-- 时间线视图 -->
      <el-card class="timeline-card" shadow="never">
        <template #header>
          <div class="section-header">
            <span>📋 任务 {{ agentRunStore.runs[0]?.taskId }} · 共 {{ agentRunStore.runs.length }} 个 Agent</span>
            <el-tag type="success" size="small">完成</el-tag>
          </div>
        </template>

        <div class="agent-timeline">
          <div
            v-for="(run, index) in agentRunStore.runs"
            :key="run.id"
            class="agent-item"
            :class="run.status"
          >
            <!-- 左侧时间轴 -->
            <div class="agent-indicator">
              <div class="indicator-dot">
                <span v-if="run.status === 'success'">✓</span>
                <span v-else>✗</span>
              </div>
              <div v-if="index < agentRunStore.runs.length - 1" class="indicator-line"></div>
            </div>

            <!-- 右侧内容 -->
            <div class="agent-content">
              <div class="agent-header">
                <el-tag :type="run.status === 'success' ? 'success' : 'danger'" size="small">
                  {{ run.agentName }}
                </el-tag>
                <span class="agent-time">{{ formatTime(run.createdAt) }}</span>
                <span class="agent-latency">⏱️ {{ run.latencyMs }}ms</span>
              </div>

              <div class="agent-body">
                <div class="agent-section">
                  <span class="section-label">输入：</span>
                  <span class="section-text">{{ run.inputSummary }}</span>
                </div>
                <div class="agent-section">
                  <span class="section-label">输出：</span>
                  <span class="section-text">{{ run.outputSummary }}</span>
                </div>
                <div v-if="run.errorMessage" class="agent-section error">
                  <span class="section-label">错误：</span>
                  <span class="section-text">{{ run.errorMessage }}</span>
                </div>
              </div>

              <div class="agent-footer">
                <el-tag type="info" size="small" effect="plain">模型：{{ run.modelName }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAgentRunStore } from '@/stores/agentRun'
import { LoadingState, EmptyState } from '@/components/common'

const agentRunStore = useAgentRunStore()

const taskId = ref('task_mock_001')
const searched = ref(false)

async function handleSearch() {
  if (!taskId.value.trim()) {
    ElMessage.warning('请输入任务 ID')
    return
  }
  searched.value = true
  try {
    await agentRunStore.fetchRuns(taskId.value.trim())
  } catch {
    ElMessage.error('查询失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.agent-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header h2 { margin: 0 0 4px; font-size: 20px; color: #1a202c; }
.page-header p { margin: 0; font-size: 14px; color: #718096; }
.search-card { border-radius: 12px; }
.search-form {
  display: flex;
  gap: 12px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.timeline-card { border-radius: 12px; }

/* Agent 时间线 */
.agent-timeline {
  display: flex;
  flex-direction: column;
}
.agent-item {
  display: flex;
  gap: 16px;
}
.agent-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}
.indicator-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.agent-item.success .indicator-dot {
  background: #67c23a;
  color: white;
}
.agent-item.failed .indicator-dot {
  background: #f56c6c;
  color: white;
}
.indicator-line {
  width: 2px;
  flex: 1;
  min-height: 16px;
  background: #e2e8f0;
  margin: 4px 0;
}
.agent-content {
  flex: 1;
  padding-bottom: 20px;
  min-width: 0;
}
.agent-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.agent-time {
  font-size: 12px;
  color: #a0aec0;
}
.agent-latency {
  font-size: 12px;
  color: #718096;
}
.agent-body {
  background: #f7fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
}
.agent-section {
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.6;
}
.agent-section:last-child { margin-bottom: 0; }
.agent-section.error { color: #e53e3e; }
.section-label {
  font-weight: 600;
  color: #4a5568;
}
.agent-section.error .section-label {
  color: #e53e3e;
}
.section-text {
  color: #2d3748;
}
.agent-footer {
  display: flex;
  gap: 8px;
}
</style>

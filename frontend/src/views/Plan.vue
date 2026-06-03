<template>
  <div class="plan-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>📅 学习路径</h2>
        <p>根据知识点依赖、薄弱点和可用时间规划学习顺序</p>
      </div>
      <el-button
        type="primary"
        :loading="studyPlanStore.loading"
        @click="handleGenerate"
      >
        {{ studyPlanStore.nodes.length ? '🔄 重新规划' : '✨ 生成路径' }}
      </el-button>
    </div>

    <!-- 路径统计 -->
    <div v-if="studyPlanStore.nodes.length" class="stats-bar">
      <el-tag type="info">共 {{ studyPlanStore.nodes.length }} 个节点</el-tag>
      <el-tag type="success">已完成 {{ completedCount }}</el-tag>
      <el-tag type="warning">进行中 {{ inProgressCount }}</el-tag>
      <el-tag>未开始 {{ notStartedCount }}</el-tag>
    </div>

    <!-- 学习路径时间线 -->
    <LoadingState v-if="studyPlanStore.loading" text="生成学习路径中..." />
    <EmptyState
      v-else-if="!studyPlanStore.nodes.length"
      icon="📅"
      title="暂无学习路径"
      description="点击「生成路径」，系统将为你规划个性化学习顺序"
    />
    <el-card v-else class="timeline-card" shadow="never">
      <TimelineNode
        v-for="(node, index) in studyPlanStore.nodes"
        :key="node.id"
        :node="node"
        :is-last="index === studyPlanStore.nodes.length - 1"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useStudyPlanStore } from '@/stores/studyPlan'
import { TimelineNode, LoadingState, EmptyState } from '@/components/common'

const userStore = useUserStore()
const studyPlanStore = useStudyPlanStore()

const completedCount = computed(() =>
  studyPlanStore.nodes.filter(n => n.status === 'completed').length
)
const inProgressCount = computed(() =>
  studyPlanStore.nodes.filter(n => n.status === 'in_progress').length
)
const notStartedCount = computed(() =>
  studyPlanStore.nodes.filter(n => n.status === 'not_started').length
)

async function handleGenerate() {
  try {
    await studyPlanStore.generate({
      studentId: userStore.studentId || 1,
      courseId: userStore.courseId || 1
    })
    ElMessage.success('学习路径生成成功')
  } catch {
    ElMessage.error('路径生成失败')
  }
}

onMounted(() => {
  if (userStore.studentId) {
    studyPlanStore.fetchPlan(userStore.studentId)
  }
})
</script>

<style scoped>
.plan-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
/* .page-header 样式已移至 style.css 全局定义 */
.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.timeline-card {
  border-radius: 12px;
  padding: 8px 0;
}
</style>

<template>
  <div class="profile-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>📋 学习画像</h2>
      <p>通过自然语言描述你的学习情况，系统将自动提取结构化画像</p>
    </div>

    <!-- 输入区域 -->
    <el-card class="input-card" shadow="never">
      <template #header>
        <span>💬 描述你的学习情况</span>
      </template>
      <el-input
        v-model="studentMessage"
        type="textarea"
        :rows="4"
        placeholder="例如：我是计算机大二学生，想两周内学会 A* 搜索算法。我 Python 一般，喜欢图解和代码例子。每天大概能学 45 分钟。"
        :disabled="profileStore.loading"
      />
      <div class="input-actions">
        <el-button
          type="primary"
          :loading="profileStore.loading"
          :disabled="!studentMessage.trim()"
          @click="handleGenerate"
        >
          {{ profileStore.loading ? '分析中...' : '🧠 生成画像' }}
        </el-button>
        <span class="input-hint">输入越详细，画像越准确（支持 6 个维度）</span>
      </div>
    </el-card>

    <!-- 画像展示区 -->
    <el-card v-if="profileStore.profile" class="result-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span>📊 当前画像</span>
          <el-tag type="success" size="small">最新版本</el-tag>
        </div>
      </template>
      <ProfileCard :profile="profileStore.profile" />
    </el-card>

    <!-- 历史版本 -->
    <el-card class="history-card" shadow="never">
      <template #header>
        <div class="section-header">
          <span>🕐 历史版本</span>
          <el-button
            text
            type="primary"
            size="small"
            :loading="loadingHistory"
            @click="fetchHistory"
          >
            刷新
          </el-button>
        </div>
      </template>
      <LoadingState v-if="loadingHistory" text="加载历史中..." />
      <EmptyState
        v-else-if="!profileStore.history.length"
        icon="🕐"
        title="暂无历史"
        description="生成画像后，历史版本将在此显示"
      />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in profileStore.history"
          :key="item.id"
          :timestamp="formatTime(item.createdAt)"
          placement="top"
          :type="item.id === profileStore.profile?.id ? 'primary' : ''"
        >
          <el-card shadow="hover" class="history-item">
            <div class="history-item-header">
              <el-tag size="small" :type="item.source === 'conversation' ? '' : 'info'">
                {{ item.source === 'conversation' ? '对话生成' : item.source }}
              </el-tag>
              <el-button
                v-if="item.id !== profileStore.profile?.id"
                text
                type="primary"
                size="small"
                @click="viewHistory(item)"
              >
                查看
              </el-button>
              <el-tag v-else size="small" type="success">当前</el-tag>
            </div>
            <p class="history-summary">
              目标：{{ item.profile?.goal || '未设置' }}
            </p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useProfileStore } from '@/stores/profile'
import { ProfileCard, LoadingState, EmptyState } from '@/components/common'

const userStore = useUserStore()
const profileStore = useProfileStore()

const studentMessage = ref('')
const loadingHistory = ref(false)

// 生成画像
async function handleGenerate() {
  if (!studentMessage.value.trim()) return
  try {
    await profileStore.generate({
      studentId: userStore.studentId || 1,
      courseId: userStore.courseId || 1,
      studentMessage: studentMessage.value.trim()
    })
    ElMessage.success('画像生成成功')
    // 生成后刷新历史
    fetchHistory()
  } catch {
    ElMessage.error('画像生成失败，请稍后重试')
  }
}

// 获取历史版本
async function fetchHistory() {
  loadingHistory.value = true
  try {
    await profileStore.fetchHistory(userStore.studentId || 1)
  } finally {
    loadingHistory.value = false
  }
}

// 查看历史版本
function viewHistory(item) {
  profileStore.profile = item.profile || item
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  if (userStore.studentId) {
    profileStore.fetchLatest(userStore.studentId)
    fetchHistory()
  }
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
/* .page-header 样式已移至 style.css 全局定义 */
.input-card {
  border-radius: 12px;
}
.input-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
.input-hint {
  font-size: 12px;
  color: var(--text-placeholder);
}
.result-card,
.history-card {
  border-radius: 12px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.history-item {
  border-radius: 8px;
}
.history-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.history-summary {
  margin: 0;
  font-size: 13px;
  color: var(--text-subtle);
}
</style>

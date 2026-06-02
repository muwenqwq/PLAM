<template>
  <div class="dashboard">
    <!-- 顶部欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>欢迎回来，{{ userStore.name }} 👋</h2>
          <p class="goal">🎯 当前目标：{{ userStore.target }}</p>
          <p class="hint">基于大模型的个性化学习助手，为你量身定制学习资源</p>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" @click="$router.push('/profile')">
            📋 构建画像
          </el-button>
          <el-button @click="$router.push('/resources')">
            📚 生成资源
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 功能入口网格 -->
    <div class="feature-grid">
      <el-card
        v-for="feature in features"
        :key="feature.path"
        class="feature-card"
        shadow="hover"
        @click="$router.push(feature.path)"
      >
        <div class="feature-icon">{{ feature.icon }}</div>
        <h3 class="feature-title">{{ feature.title }}</h3>
        <p class="feature-desc">{{ feature.desc }}</p>
      </el-card>
    </div>

    <!-- 下方区域：画像预览 + 最近资源 -->
    <div class="bottom-section">
      <!-- 画像预览 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <span>📋 学习画像</span>
            <el-button text type="primary" @click="$router.push('/profile')">
              查看详情 →
            </el-button>
          </div>
        </template>
        <LoadingState v-if="profileStore.loading" text="加载画像中..." />
        <ProfileCard v-else-if="profileStore.profile" :profile="profileStore.profile" />
        <EmptyState v-else icon="📋" title="暂无画像" description="请先构建你的学习画像">
          <el-button type="primary" size="small" @click="$router.push('/profile')">
            去构建
          </el-button>
        </EmptyState>
      </el-card>

      <!-- 最近资源 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <span>📚 最近资源</span>
            <el-button text type="primary" @click="$router.push('/resources')">
              查看全部 →
            </el-button>
          </div>
        </template>
        <LoadingState v-if="resourceStore.loading" text="加载资源中..." />
        <div v-else-if="recentResources.length" class="resource-list">
          <ResourceCard
            v-for="r in recentResources"
            :key="r.id"
            :resource="r"
            @click="$router.push('/resources')"
          />
        </div>
        <EmptyState v-else icon="📚" title="暂无资源" description="生成资源后将在此显示">
          <el-button type="primary" size="small" @click="$router.push('/resources')">
            去生成
          </el-button>
        </EmptyState>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useProfileStore } from '@/stores/profile'
import { useResourceStore } from '@/stores/resource'
import { ProfileCard, ResourceCard, LoadingState, EmptyState } from '@/components/common'

const userStore = useUserStore()
const profileStore = useProfileStore()
const resourceStore = useResourceStore()

// 功能入口配置
const features = [
  { icon: '📋', title: '学习画像', desc: '描述你的学习情况，系统自动提取画像', path: '/profile' },
  { icon: '📚', title: '学习资源', desc: '根据画像生成讲义、导图、题目等 5 类资源', path: '/resources' },
  { icon: '📅', title: '学习路径', desc: '按知识点依赖规划学习顺序和时间', path: '/study-plan' },
  { icon: '👨‍🏫', title: '智能辅导', desc: '基于课程知识库的即时问答', path: '/tutor' },
  { icon: '📊', title: '数据统计', desc: '查看练习结果、掌握度和薄弱点', path: '/analytics' },
  { icon: '🤖', title: 'Agent 记录', desc: '查看多智能体运行轨迹', path: '/agent-runs' }
]

// 最多展示 3 条最近资源
const recentResources = computed(() => {
  return (resourceStore.resources || []).slice(0, 3)
})

onMounted(() => {
  // 加载画像和资源列表（如果已登录）
  if (userStore.studentId) {
    profileStore.fetchLatest(userStore.studentId)
    resourceStore.fetchList(userStore.studentId)
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 欢迎卡片 */
.welcome-card {
  background: linear-gradient(135deg, #3182ce 0%, #2b6cb0 100%);
  border: none;
  border-radius: 12px;
}
.welcome-card :deep(.el-card__body) {
  padding: 32px;
}
.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}
.welcome-text h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #ffffff;
}
.welcome-text .goal {
  margin: 0 0 4px;
  font-size: 14px;
  color: rgba(255,255,255,0.9);
}
.welcome-text .hint {
  margin: 0;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}
.welcome-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.welcome-actions .el-button {
  border-radius: 8px;
}

/* 功能入口 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.feature-card {
  cursor: pointer;
  border-radius: 12px;
  transition: transform 0.2s;
  text-align: center;
}
.feature-card:hover {
  transform: translateY(-2px);
}
.feature-card :deep(.el-card__body) {
  padding: 24px 16px;
}
.feature-icon {
  font-size: 32px;
  margin-bottom: 12px;
}
.feature-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #1a202c;
}
.feature-desc {
  margin: 0;
  font-size: 12px;
  color: #a0aec0;
  line-height: 1.5;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .bottom-section {
    grid-template-columns: 1fr;
  }
  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

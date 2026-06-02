<template>
  <div class="statistics-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>📊 数据统计</h2>
      <p>查看练习结果、知识点掌握度和薄弱点分析</p>
    </div>

    <LoadingState v-if="analyticsStore.loading" text="加载统计数据中..." />

    <template v-else-if="analyticsStore.analytics">
      <!-- 概览卡片 -->
      <div class="overview-cards">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ analyticsStore.analytics.totalQuizzes || 0 }}</div>
          <div class="stat-label">完成测验</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value score">{{ analyticsStore.analytics.averageScore || 0 }}</div>
          <div class="stat-label">平均分</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value weak">{{ analyticsStore.analytics.weakPoints?.length || 0 }}</div>
          <div class="stat-label">薄弱知识点</div>
        </el-card>
      </div>

      <!-- 掌握度图表 + 薄弱点 -->
      <div class="detail-row">
        <!-- 掌握度雷达图 -->
        <el-card shadow="never" class="chart-card">
          <template #header><span>📈 知识点掌握度</span></template>
          <div ref="radarChart" class="chart-container"></div>
        </el-card>

        <!-- 薄弱点列表 -->
        <el-card shadow="never" class="weak-card">
          <template #header><span>⚠️ 薄弱点</span></template>
          <EmptyState
            v-if="!analyticsStore.analytics.weakPoints?.length"
            icon="✅"
            title="暂无薄弱点"
            description="继续保持！"
          />
          <div v-else class="weak-list">
            <div
              v-for="wp in analyticsStore.analytics.weakPoints"
              :key="wp"
              class="weak-item"
            >
              <span class="weak-name">{{ wp }}</span>
              <el-progress
                :percentage="getMasteryPercent(wp)"
                :color="getProgressColor(getMasteryPercent(wp))"
                :stroke-width="8"
              />
            </div>
          </div>
        </el-card>
      </div>

      <!-- 最近测验结果 -->
      <el-card shadow="never" class="recent-card">
        <template #header><span>📝 最近测验</span></template>
        <EmptyState
          v-if="!analyticsStore.analytics.recentResults?.length"
          icon="📝"
          title="暂无测验记录"
        />
        <el-table v-else :data="analyticsStore.analytics.recentResults" stripe>
          <el-table-column prop="quizTitle" label="测验名称" />
          <el-table-column label="得分" width="120">
            <template #default="{ row }">
              <span :class="{ 'score-pass': row.score >= 80, 'score-fail': row.score < 60 }">
                {{ row.score }} / {{ row.totalScore }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="正确率" width="120">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.round(row.score / row.totalScore * 100)"
                :color="getProgressColor(row.score / row.totalScore)"
                :stroke-width="6"
              />
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.submittedAt) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <EmptyState
      v-else
      icon="📊"
      title="暂无统计数据"
      description="完成测验后，统计数据将在此显示"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import { useAnalyticsStore } from '@/stores/analytics'
import { LoadingState, EmptyState } from '@/components/common'

const userStore = useUserStore()
const analyticsStore = useAnalyticsStore()

const radarChart = ref(null)
let chartInstance = null

// 获取掌握度百分比
function getMasteryPercent(key) {
  const map = analyticsStore.analytics?.masteryMap
  return map ? Math.round((map[key] || 0) * 100) : 0
}

function getProgressColor(val) {
  const percent = typeof val === 'number' && val <= 1 ? val * 100 : val
  if (percent >= 70) return '#67c23a'
  if (percent >= 40) return '#e6a23c'
  return '#f56c6c'
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

// 渲染雷达图
function renderChart() {
  const map = analyticsStore.analytics?.masteryMap
  if (!map || !radarChart.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(radarChart.value)
  }

  const keys = Object.keys(map)
  const values = Object.values(map).map(v => Math.round(v * 100))

  chartInstance.setOption({
    radar: {
      indicator: keys.map(k => ({ name: k, max: 100 })),
      shape: 'polygon',
      splitArea: { areaStyle: { color: ['#f7fafc', '#edf2f7', '#e2e8f0'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '掌握度',
        areaStyle: { color: 'rgba(49,130,206,0.2)' },
        lineStyle: { color: '#3182ce', width: 2 },
        itemStyle: { color: '#3182ce' }
      }]
    }]
  })
}

// 窗口大小变化时重绘
function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  if (userStore.studentId) {
    await analyticsStore.fetchAnalytics(userStore.studentId)
  }
  nextTick(renderChart)
  window.addEventListener('resize', handleResize)
})

// 数据变化时重绘
watch(() => analyticsStore.analytics, () => { nextTick(renderChart) }, { deep: true })
</script>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header h2 { margin: 0 0 4px; font-size: 20px; color: #1a202c; }
.page-header p { margin: 0; font-size: 14px; color: #718096; }

/* 概览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.stat-card {
  border-radius: 12px;
  text-align: center;
}
.stat-card :deep(.el-card__body) { padding: 24px; }
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #3182ce;
}
.stat-value.score { color: #e6a23c; }
.stat-value.weak { color: #f56c6c; }
.stat-label {
  margin-top: 8px;
  font-size: 13px;
  color: #718096;
}

/* 详情行 */
.detail-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.chart-card, .weak-card { border-radius: 12px; }
.chart-container {
  width: 100%;
  height: 280px;
}
.weak-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.weak-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.weak-name {
  width: 100px;
  flex-shrink: 0;
  font-size: 14px;
  color: #4a5568;
}
.weak-item :deep(.el-progress) { flex: 1; }

/* 测验表格 */
.recent-card { border-radius: 12px; }
.score-pass { color: #67c23a; font-weight: 600; }
.score-fail { color: #f56c6c; font-weight: 600; }

@media (max-width: 900px) {
  .overview-cards { grid-template-columns: 1fr; }
  .detail-row { grid-template-columns: 1fr; }
}
</style>

<template>
  <div class="page">
    <PageHeader title="学习驾驶舱" description="汇总学习空间、资源、测验、路径和掌握度，作为演示入口。" />
    <div class="grid three">
      <StatCard label="学习空间" :value="overview.spaceCount" icon="FolderOpened" hint="按用户隔离" />
      <StatCard label="生成资源" :value="overview.resourceCount" icon="Document" hint="Markdown / Mermaid" />
      <StatCard label="测验数量" :value="overview.quizCount" icon="EditPen" hint="自动评分" />
      <StatCard label="学习路径" :value="overview.pathCount" icon="Guide" hint="今日任务可追踪" />
      <StatCard label="平均得分" :value="overview.averageScore || 0" icon="TrendCharts" hint="测验结果" />
      <StatCard label="平均掌握度" :value="overview.averageMastery || 0" icon="PieChart" hint="知识点掌握" />
    </div>
    <div class="grid two">
      <el-card>
        <template #header>闭环指标图</template>
        <div ref="chartRef" class="chart" />
      </el-card>
      <el-card>
        <template #header>快捷入口</template>
        <div class="quick">
          <RouterLink v-for="item in quick" :key="item.path" :to="item.path" class="route-card">
            <el-icon><component :is="item.icon" /></el-icon>
            <strong>{{ item.title }}</strong>
            <span>{{ item.text }}</span>
          </RouterLink>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { getOverview } from '@/api/report'

const chartRef = ref<HTMLElement>()
const overview = reactive<any>({
  spaceCount: 0,
  resourceCount: 0,
  quizCount: 0,
  pathCount: 0,
  averageScore: 0,
  averageMastery: 0
})
const quick = [
  { path: '/models', title: '模型配置', text: '创建 Mock 模型并测试', icon: 'Cpu' },
  { path: '/chat', title: '智能对话', text: '发送学习问题', icon: 'ChatDotRound' },
  { path: '/resources', title: '资源生成', text: '生成讲义、图谱、PPT', icon: 'DocumentAdd' },
  { path: '/paths', title: '学习路径', text: '生成并跟踪路径', icon: 'Guide' },
  { path: '/quiz', title: '测验评估', text: '生成、答题、评分', icon: 'EditPen' },
  { path: '/reports', title: '学习报告', text: '生成报告与图表', icon: 'PieChart' }
]

async function load() {
  try {
    Object.assign(overview, await getOverview())
  } catch {
    Object.assign(overview, { spaceCount: 1, resourceCount: 3, quizCount: 1, pathCount: 1, averageScore: 86, averageMastery: 78 })
  }
  await nextTick()
  const chart = echarts.init(chartRef.value!)
  chart.setOption({
    tooltip: {},
    grid: { left: 36, right: 18, bottom: 32, top: 24 },
    xAxis: { type: 'category', data: ['空间', '资源', '测验', '路径'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [overview.spaceCount, overview.resourceCount, overview.quizCount, overview.pathCount], itemStyle: { color: '#2563eb', borderRadius: [6, 6, 0, 0] } }]
  })
}

onMounted(load)
</script>

<style scoped>
.quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick a {
  display: grid;
  gap: 6px;
}

.quick span {
  color: var(--muted);
  font-size: 13px;
}
</style>

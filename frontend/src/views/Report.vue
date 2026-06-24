<template>
  <div class="page">
    <PageHeader title="学习报告" description="汇总资源、路径、测验和掌握度，生成可导出的学习报告。" />
    <div class="grid three">
      <StatCard label="空间" :value="overview.spaceCount" />
      <StatCard label="资源" :value="overview.resourceCount" />
      <StatCard label="测验" :value="overview.quizCount" />
      <StatCard label="路径" :value="overview.pathCount" />
      <StatCard label="平均得分" :value="overview.averageScore" />
      <StatCard label="平均掌握度" :value="overview.averageMastery" />
    </div>
    <div class="grid two">
      <el-card>
        <template #header>报告图表</template>
        <div ref="chartRef" class="chart" />
      </el-card>
      <el-card>
        <template #header>生成报告</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间" @change="loadReports">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 服务">
            <el-select v-model="form.modelProviderId" placeholder="选择 AI 服务">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="报告标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="报告类型">
            <el-select v-model="form.reportType">
              <el-option label="空间周报" value="space_weekly" />
              <el-option label="阶段总结" value="stage_summary" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成报告</el-button>
      </el-card>
    </div>
    <el-card>
      <template #header>报告列表</template>
      <el-table :data="reports" empty-text="暂无报告" highlight-current-row @row-click="openReport">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="reportType" label="类型" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="openReport(row)">查看</el-button>
            <el-button size="small" @click.stop="exportMd(row.id)">导出</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="report">
      <template #header>
        <div class="toolbar"><span>{{ report.title }}</span><el-button size="small" @click="exportMd(report.id)">导出 Markdown</el-button></div>
      </template>
      <h3>摘要</h3>
      <p>{{ report.summary }}</p>
      <h3>AI 学习建议</h3>
      <p>{{ report.suggestionText }}</p>
      <template v-if="reportData">
        <h3>薄弱知识点</h3>
        <el-tag v-for="point in reportData.weak_points" :key="point" type="warning" style="margin: 4px">{{ point }}</el-tag>
        <EmptyState v-if="!reportData.weak_points?.length" title="暂无薄弱点" />
        <h3>优势领域</h3>
        <el-tag v-for="s in reportData.strengths" :key="s" type="success" style="margin: 4px">{{ s }}</el-tag>
        <EmptyState v-if="!reportData.strengths?.length" title="暂无优势记录" />
        <h3>下一步计划</h3>
        <ul>
          <li v-for="action in reportData.next_actions" :key="action">{{ action }}</li>
        </ul>
        <EmptyState v-if="!reportData.next_actions?.length" title="暂无后续计划" />
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { exportReport, generateReport, getOverview, getReport, listReportsBySpace } from '@/api/report'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import EmptyState from '@/components/EmptyState.vue'
import { useAppStore } from '@/stores/app'

const app = useAppStore()
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const reports = ref<any[]>([])
const overview = reactive<any>({ spaceCount: 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, reportType: 'space_weekly', title: '数据库学习周报' })
const report = ref<any>()
const generating = ref(false)

const reportData = computed(() => {
  const raw = report.value?.reportJson
  if (!raw) return null
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) } catch { return null }
  }
  return raw
})

async function loadDefaults() {
  const [space, provider] = await Promise.all([
    getDefaultSpace().catch(() => null),
    getDefaultProvider().catch(() => null)
  ])
  if (space?.id) form.spaceId = space.id
  if (provider?.id) form.modelProviderId = provider.id
}

async function loadSpacesAndProviders() {
  const [spaceData, providerData] = await Promise.all([
    listSpaces({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] })),
    listProviders({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] }))
  ])
  spaces.value = spaceData.records || []
  providers.value = providerData.records || []
}

async function loadReports() {
  if (!form.spaceId) return
  reports.value = await listReportsBySpace(form.spaceId).catch(() => [])
}

async function openReport(row: any) {
  report.value = await getReport(row.id)
}

async function renderChart() {
  await nextTick()
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const isDark = app.theme === 'dark'
  chart.setOption({
    color: ['#8b6ee8', '#4f82d8', '#2b8a6e'],
    backgroundColor: 'transparent',
    tooltip: { backgroundColor: isDark ? '#151a27' : '#ffffff', borderColor: isDark ? 'rgba(255,255,255,.12)' : '#e5e4ec', textStyle: { color: isDark ? '#e9eaf0' : '#1f1d2b' } },
    legend: { bottom: 0, textStyle: { color: isDark ? '#a4a6b2' : '#6e6a7c' } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      itemStyle: { borderColor: isDark ? '#10141f' : '#ffffff', borderWidth: 3 },
      label: { color: isDark ? '#e9eaf0' : '#1f1d2b' },
      data: [
        { name: '资源', value: overview.resourceCount },
        { name: '测验', value: overview.quizCount },
        { name: '路径', value: overview.pathCount }
      ]
    }]
  }, true)
}

async function load() {
  Object.assign(overview, await getOverview().catch(() => ({ spaceCount: 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })))
  await renderChart()
}

async function generate() {
  generating.value = true
  try {
    report.value = await generateReport(form)
    ElMessage.success('报告已生成')
    loadReports()
  } finally {
    generating.value = false
  }
}

async function copyText(content: string) {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(content)
      return
    } catch {
      // Fall back for HTTP and restricted clipboard contexts.
    }
  }
  const textarea = document.createElement('textarea')
  textarea.value = content
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  textarea.remove()
  if (!copied) throw new Error('浏览器未允许复制，请手动复制报告内容。')
}

async function exportMd(id: number) {
  try {
    const content = await exportReport(id)
    await copyText(content)
    ElMessage.success('报告 Markdown 已复制')
  } catch (error: any) {
    ElMessage.error(error?.message || '报告导出失败')
  }
}

watch(() => app.theme, renderChart)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults()])
  await load()
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartRef.value)
  }
  await loadReports()
})
</script>

<style scoped>
.report-data {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

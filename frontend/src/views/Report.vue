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
            <el-select v-model="form.spaceId" placeholder="选择学习空间" @change="handleSpaceChange">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 服务">
            <el-select v-model="form.modelProviderId" placeholder="选择 AI 服务">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="报告标题"><el-input v-model="form.title" placeholder="填写这份学习报告的标题" /></el-form-item>
          <el-form-item label="报告类型">
            <el-select v-model="form.reportType">
              <el-option label="空间周报" value="space_weekly" />
              <el-option label="阶段总结" value="stage_summary" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 角色">
            <div class="role-setting">
              <el-checkbox v-model="form.rolePlayEnabled">使用默认 AI 角色风格生成建议</el-checkbox>
              <el-select v-model="form.companionRoleId" placeholder="选择陪伴角色" clearable :disabled="!form.rolePlayEnabled">
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
              <el-alert v-if="form.rolePlayEnabled" :title="roleStyleText" type="info" :closable="false" show-icon />
            </div>
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
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="openReport(row)">查看</el-button>
            <el-dropdown @command="(format) => downloadReport(row, format)" @click.stop>
              <el-button size="small">导出<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="docx">Word 文档</el-dropdown-item>
                  <el-dropdown-item command="pdf">PDF 文档</el-dropdown-item>
                  <el-dropdown-item command="png">长图 PNG</el-dropdown-item>
                  <el-dropdown-item command="md" divided>Markdown 原文</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" plain @click.stop="removeReport(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="report">
      <template #header>
        <div class="toolbar"><span>{{ report.title }}</span><div class="inline-actions"><el-dropdown @command="(format) => downloadReport(report, format)"><el-button size="small" type="primary">导出报告<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="docx">Word 文档</el-dropdown-item><el-dropdown-item command="pdf">PDF 文档</el-dropdown-item><el-dropdown-item command="png">长图 PNG</el-dropdown-item><el-dropdown-item command="md" divided>Markdown 原文</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button size="small" @click="exportMd(report.id)">复制 Markdown</el-button><el-button size="small" type="danger" plain @click="removeReport(report)">删除</el-button></div></div>
      </template>
      <h3>摘要</h3>
      <p>{{ report.summary }}</p>
      <template v-if="reportData">
        <section v-if="reportData.learner_profile" class="report-section">
          <h3>本阶段学习画像</h3>
          <p class="profile-snapshot">{{ reportData.learner_profile.adaptive_summary || reportData.learner_profile.profile_narrative || '画像仍在持续形成。' }}</p>
        </section>
        <section v-if="reportData.activity_summary" class="report-section">
          <h3>本空间学习足迹</h3>
          <div class="evidence-grid">
            <div><strong>{{ reportData.activity_summary.message_count || 0 }}</strong><span>对话消息</span></div>
            <div><strong>{{ reportData.activity_summary.knowledge_file_count || 0 }}</strong><span>知识库资料</span></div>
            <div><strong>{{ reportData.activity_summary.knowledge_chunk_count || 0 }}</strong><span>资料片段</span></div>
            <div><strong>{{ reportData.activity_summary.resource_count || 0 }}</strong><span>生成资源</span></div>
            <div><strong>{{ reportData.activity_summary.submitted_quiz_count || 0 }}</strong><span>已完成测验</span></div>
            <div><strong>{{ reportData.activity_summary.path_count || 0 }}</strong><span>学习路径</span></div>
          </div>
        </section>
        <section v-if="reportData.resource_breakdown" class="report-section">
          <h3>资源沉淀</h3>
          <div class="report-data">
            <el-tag v-for="([type, count]) in Object.entries(reportData.resource_breakdown)" :key="type" type="info">
              {{ resourceTypeLabel(type) }} {{ count }} 份
            </el-tag>
          </div>
        </section>
        <section v-if="reportData.path_progress" class="report-section split-section">
          <div>
            <h3>学习路径进度</h3>
            <el-progress :percentage="Number(reportData.path_progress.average_progress || 0)" :stroke-width="12" />
            <p>已完成 {{ reportData.path_progress.completed_item_count || 0 }} / {{ reportData.path_progress.item_count || 0 }} 个任务，待完成 {{ reportData.path_progress.pending_item_count || 0 }} 个。</p>
          </div>
          <div>
            <h3>下一批任务</h3>
            <ul><li v-for="item in reportData.path_progress.next_items" :key="item.title">{{ item.title }}<span v-if="item.estimated_minutes"> · {{ item.estimated_minutes }} 分钟</span></li></ul>
            <p v-if="!reportData.path_progress.next_items?.length" class="muted">当前没有待完成路径任务。</p>
          </div>
        </section>
        <section v-if="reportData.quiz_performance" class="report-section split-section">
          <div>
            <h3>测验表现</h3>
            <p class="score-line"><strong>{{ reportData.quiz_performance.average_score || 0 }}%</strong> 平均得分率</p>
            <p>共作答 {{ reportData.quiz_performance.answer_count || 0 }} 题，正确 {{ reportData.quiz_performance.correct_answer_count || 0 }} 题，需复盘 {{ reportData.quiz_performance.wrong_answer_count || 0 }} 题。</p>
          </div>
          <div>
            <h3>最近测验</h3>
            <ul><li v-for="item in reportData.quiz_performance.recent_quizzes" :key="item.title">{{ item.title }} · {{ item.score_rate }}%</li></ul>
            <p v-if="!reportData.quiz_performance.recent_quizzes?.length" class="muted">完成测验后会在这里形成趋势。</p>
          </div>
        </section>
        <section v-if="reportData.learning_observations?.length" class="report-section">
          <h3>阶段观察</h3>
          <ul><li v-for="item in reportData.learning_observations" :key="item">{{ item }}</li></ul>
        </section>
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
        <section v-if="reportData.mastery_records?.length" class="report-section">
          <h3>知识点掌握明细</h3>
          <el-table :data="reportData.mastery_records" size="small">
            <el-table-column prop="knowledge_point" label="知识点" min-width="160" />
            <el-table-column prop="mastery_level" label="掌握度" width="100" />
            <el-table-column prop="review_count" label="练习次数" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </section>
      </template>
      <h3>AI 学习建议</h3>
      <p>{{ report.suggestionText }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { use as useECharts, init as initChart, type ECharts } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { deleteReport, downloadReportFile, exportReport, generateReport, getOverview, getReport, listReportsBySpace } from '@/api/report'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import EmptyState from '@/components/EmptyState.vue'
import { useAppStore } from '@/stores/app'
import { getActiveCompanionRole, listCompanionRoles } from '@/api/companionRole'

const app = useAppStore()
const chartRef = ref<HTMLElement>()
useECharts([PieChart, LegendComponent, TooltipComponent, CanvasRenderer])

let chart: ECharts | undefined
let resizeObserver: ResizeObserver | undefined
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const roles = ref<any[]>([])
const reports = ref<any[]>([])
const overview = reactive<any>({ spaceCount: 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, reportType: 'space_weekly', title: '', rolePlayEnabled: false, companionRoleId: null as number | null })
const report = ref<any>()
const generating = ref(false)
const selectedRole = computed(() => roles.value.find((role) => role.id === form.companionRoleId))
const roleStyleText = computed(() => selectedRole.value ? `${selectedRole.value.roleName} · ${selectedRole.value.speakingStyle || '按角色卡风格生成建议'}` : '使用当前默认学习陪伴角色')

const reportData = computed(() => {
  const raw = report.value?.reportJson
  if (!raw) return null
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) } catch { return null }
  }
  return raw
})


async function loadRoles() {
  const [listData, activeRole] = await Promise.all([
    listCompanionRoles({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
    getActiveCompanionRole().catch(() => null)
  ])
  roles.value = listData.records || []
  if (!form.companionRoleId && activeRole?.id) form.companionRoleId = activeRole.id
}

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
  if (!report.value && reports.value.length) {
    report.value = await getReport(reports.value[0].id).catch(() => undefined)
  }
}

async function handleSpaceChange() {
  report.value = undefined
  await Promise.all([loadReports(), load()])
}

async function openReport(row: any) {
  report.value = await getReport(row.id)
}

async function removeReport(item: any) {
  await ElMessageBox.confirm(`确认删除报告“${item.title}”吗？`, '删除报告', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteReport(item.id)
  if (report.value?.id === item.id) report.value = undefined
  await loadReports()
  ElMessage.success('报告已删除')
}

function resourceTypeLabel(type: string) {
  const labels: Record<string, string> = { plan: '学习计划', lecture: '课程讲义', outline: '复习提纲', quiz: '练习资料', case: '案例材料', graph: '知识图谱', report: '阶段报告', knowledge_graph: '知识图谱' }
  return labels[type] || type
}

async function renderChart() {
  await nextTick()
  if (!chartRef.value) return
  if (!chart) chart = initChart(chartRef.value)
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
  Object.assign(overview, await getOverview(form.spaceId).catch(() => ({ spaceCount: form.spaceId ? 1 : 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })))
  await renderChart()
}

async function generate() {
  if (!form.spaceId) return ElMessage.warning('请先选择学习空间')
  if (!form.title?.trim()) return ElMessage.warning('请填写报告标题')
  generating.value = true
  try {
    report.value = await generateReport(form)
    ElMessage.success('报告已生成')
    loadReports()
  } finally {
    generating.value = false
  }
}


function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

type ExportFormat = 'docx' | 'pdf' | 'png' | 'md'

function reportFilename(item: any, format: ExportFormat) {
  const title = (item?.title || `report-${item?.id || Date.now()}`).replace(/[\\/:*?"<>|]+/g, '_').trim()
  return `${title || 'report'}.${format}`
}

async function downloadReport(item: any, format: ExportFormat = 'docx') {
  const blob = await downloadReportFile(item.id, format)
  saveBlob(blob, reportFilename(item, format))
  const labels: Record<ExportFormat, string> = { docx: 'Word', pdf: 'PDF', png: '长图', md: 'Markdown' }
  ElMessage.success(`报告 ${labels[format]} 文件已开始下载`)
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
    ElMessage.success('报告 Markdown 已复制到剪贴板')
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
  await Promise.all([loadSpacesAndProviders(), loadRoles(), loadDefaults()])
  await load()
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartRef.value)
  }
  await loadReports()
})
</script>

<style scoped>
.role-setting { display: grid; gap: 10px; }
.helper-text { margin: 0; color: var(--muted); font-size: 13px; }
.report-data {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.report-section { margin-top: 24px; padding-top: 4px; border-top: 1px solid var(--line); }
.profile-snapshot { padding: 12px 14px; border-left: 3px solid var(--primary); background: var(--primary-soft); line-height: 1.75; }
.evidence-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; }
.evidence-grid > div { min-width: 0; padding: 12px; background: var(--surface-soft); }
.evidence-grid strong { display: block; color: var(--text-strong); font-size: 24px; }
.evidence-grid span, .muted { color: var(--muted); font-size: 13px; }
.split-section { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 28px; }
.score-line strong { margin-right: 8px; color: var(--primary); font-size: 28px; }
@media (max-width: 1100px) {
  .evidence-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .split-section { grid-template-columns: 1fr; }
}
</style>

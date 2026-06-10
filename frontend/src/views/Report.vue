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
          <el-form-item label="空间 ID"><el-input-number v-model="form.spaceId" :min="1" /></el-form-item>
          <el-form-item label="模型 ID"><el-input-number v-model="form.modelProviderId" :min="1" /></el-form-item>
          <el-form-item label="报告标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="报告类型"><el-select v-model="form.reportType"><el-option label="空间周报" value="space_weekly" /><el-option label="阶段总结" value="stage_summary" /></el-select></el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成报告</el-button>
      </el-card>
    </div>
    <el-card v-if="report">
      <template #header>
        <div class="toolbar"><span>{{ report.title }}</span><el-button size="small" @click="exportMd">导出 Markdown</el-button></div>
      </template>
      <h3>摘要</h3>
      <p>{{ report.summary }}</p>
      <h3>AI 学习建议</h3>
      <p>{{ report.suggestionText }}</p>
      <pre>{{ report.reportJson }}</pre>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { exportReport, generateReport, getOverview } from '@/api/report'

const chartRef = ref<HTMLElement>()
const overview = reactive<any>({ spaceCount: 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })
const form = reactive<any>({ spaceId: 1, modelProviderId: 1, reportType: 'space_weekly', title: '数据库学习周报' })
const report = ref<any>()
const generating = ref(false)

async function load() {
  Object.assign(overview, await getOverview())
  await nextTick()
  const chart = echarts.init(chartRef.value!)
  chart.setOption({
    tooltip: {},
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      data: [
        { name: '资源', value: overview.resourceCount },
        { name: '测验', value: overview.quizCount },
        { name: '路径', value: overview.pathCount }
      ]
    }]
  })
}

async function generate() {
  generating.value = true
  try {
    report.value = await generateReport(form)
    ElMessage.success('报告已生成')
  } finally {
    generating.value = false
  }
}

async function exportMd() {
  const content = await exportReport(report.value.id)
  await navigator.clipboard?.writeText(content)
  ElMessage.success('报告 Markdown 已复制')
}

onMounted(load)
</script>

<style scoped>
pre {
  white-space: pre-wrap;
  padding: 12px;
  background: var(--surface-soft);
  border-radius: 8px;
}
</style>

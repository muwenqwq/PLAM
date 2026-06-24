<template>
  <div class="page">
    <PageHeader title="多智能体工作台" description="创建 Agent 任务，展示 Planner、Knowledge、Exercise、Review 等执行步骤。" />
    <div class="grid two">
      <el-card>
        <template #header>创建任务</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型配置">
            <el-select v-model="form.providerId" placeholder="选择模型配置">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="资源类型">
            <el-select v-model="form.resourceType">
              <el-option label="学习计划" value="plan" />
              <el-option label="习题集" value="quiz_set" />
              <el-option label="知识图谱" value="knowledge_graph" />
              <el-option label="PPT 大纲" value="ppt_outline" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" placeholder="逗号分隔" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="running" @click="run">运行 Agent</el-button>
      </el-card>
      <el-card>
        <template #header>执行步骤</template>
        <el-alert v-if="taskSummary" :title="taskSummary" type="success" show-icon :closable="false" class="task-summary" />
        <AgentStepTimeline v-if="steps.length" :steps="steps" />
        <EmptyState v-else title="尚未运行任务" description="运行后会展示多智能体执行时间线。" />
      </el-card>
    </div>
    <el-card>
      <template #header>生成资源</template>
      <div v-if="resources.length" class="grid two">
        <ResourceCard v-for="item in resources" :key="item.id || item.title" :resource="item">
          <MarkdownViewer :content="item.contentMarkdown" />
        </ResourceCard>
      </div>
      <EmptyState v-else title="暂无资源" description="运行 Agent 任务或从下方任务列表查看历史资源。" />
    </el-card>
    <el-card>
      <template #header>任务列表</template>
      <el-table :data="tasks" empty-text="暂无 Agent 任务" highlight-current-row @row-click="openTask">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="taskType" label="类型" width="140" />
        <el-table-column prop="executionStatus" label="执行状态" width="110" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="openTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import AgentStepTimeline from '@/components/AgentStepTimeline.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { createAgentTask, getAgentSteps, getAgentTask, listAgentTasks } from '@/api/agent'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { parseKnowledgePoints } from '@/utils/knowledge'

const running = ref(false)
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const tasks = ref<any[]>([])
const knowledgeText = ref('索引, 范式, SQL 查询')
const steps = ref<any[]>([])
const resources = ref<any[]>([])
const taskSummary = ref('')
const form = reactive<any>({ spaceId: null as number | null, providerId: null as number | null, title: '数据库期末复习资源', subject: '数据库', taskType: 'resource_generation', resourceType: 'plan' })

async function loadDefaults() {
  const [space, provider] = await Promise.all([
    getDefaultSpace().catch(() => null),
    getDefaultProvider().catch(() => null)
  ])
  if (space?.id) form.spaceId = space.id
  if (provider?.id) form.providerId = provider.id
}

async function loadSpacesAndProviders() {
  const [spaceData, providerData] = await Promise.all([
    listSpaces({ pageNum: 1, pageSize: 50 }),
    listProviders({ pageNum: 1, pageSize: 50 })
  ])
  spaces.value = spaceData.records || []
  providers.value = providerData.records || []
}

async function loadTasks() {
  const data = await listAgentTasks({ pageNum: 1, pageSize: 50 })
  tasks.value = data.records || []
}

async function openTask(row: any) {
  steps.value = []
  resources.value = []
  taskSummary.value = ''
  const task = await getAgentTask(row.id)
  taskSummary.value = task.outputSummary || ''
  steps.value = await getAgentSteps(row.id)
  resources.value = task.resources || []
}

async function run() {
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!form.title?.trim() || !form.subject?.trim()) {
    ElMessage.warning('请填写任务标题和学科')
    return
  }
  if (!knowledgePoints.length) {
    ElMessage.warning('请至少填写一个知识点')
    return
  }
  running.value = true
  steps.value = []
  resources.value = []
  taskSummary.value = ''
  try {
    const data = await createAgentTask({
      ...form,
      inputParams: {
        knowledge_points: knowledgePoints,
        subject: form.subject,
        resource_type: form.resourceType
      }
    })
    steps.value = data.steps || []
    resources.value = data.resources || []
    taskSummary.value = data.task?.outputSummary || data.outputSummary || ''
    if (!steps.value.length && data.task?.id) {
      steps.value = await getAgentSteps(data.task.id)
    }
    ElMessage.success('Agent 任务执行完成')
    loadTasks()
  } finally {
    running.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults(), loadTasks()])
})
</script>

<style scoped>
.task-summary {
  margin-bottom: 12px;
}
</style>

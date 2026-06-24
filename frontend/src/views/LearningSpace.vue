<template>
  <div class="page">
    <PageHeader title="学习空间" description="按学科或课程组织知识库、资源、路径、测验和报告。">
      <el-button type="primary" @click="openCreate">新建空间</el-button>
    </PageHeader>
    <el-card>
      <el-table :data="spaces" v-loading="loading" empty-text="暂无学习空间" highlight-current-row @row-click="selectSpace">
        <el-table-column prop="spaceName" label="空间名称" min-width="160" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="资源" width="80"><template #default="{ row }"><el-tag size="small">{{ row.resourceCount || 0 }}</el-tag></template></el-table-column>
        <el-table-column label="任务" width="80"><template #default="{ row }"><el-tag size="small" type="warning">{{ row.taskCount || 0 }}</el-tag></template></el-table-column>
        <el-table-column label="默认" width="90"><template #default="{ row }"><el-tag v-if="row.defaultSpace || row.isDefault" type="success">默认</el-tag></template></el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="selectSpace(row)">详情</el-button>
            <el-button size="small" @click.stop="edit(row)">编辑</el-button>
            <el-button size="small" @click.stop="setDefault(row.id)">设默认</el-button>
            <el-button size="small" type="danger" @click.stop="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <template v-if="selected">
      <h2 style="margin: 20px 0 12px">{{ selected.spaceName }} <el-tag>{{ selected.subject }}</el-tag></h2>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="概览" name="overview">
          <div class="grid three">
            <StatCard label="生成资源" :value="summary.generatedResourceCount || summary.resourceCount || 0" hint="Markdown / Mermaid" />
            <StatCard label="Agent 任务" :value="summary.activeTaskCount || summary.taskCount || 0" hint="已完成 / 运行中" />
            <StatCard label="用户画像" :value="summary.profileCount || 0" hint="学习中" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="资源" name="resources">
          <div class="grid two" v-if="spaceResources.length">
            <ResourceCard v-for="item in spaceResources" :key="item.id" :resource="item" />
          </div>
          <EmptyState v-else title="暂无资源" description="前往资源生成中心创建" />
        </el-tab-pane>
        <el-tab-pane label="测验" name="quizzes">
          <el-table :data="spaceQuizzes" empty-text="暂无测验">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="difficulty" label="难度" width="80" />
            <el-table-column prop="questionCount" label="题目数" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="学习路径" name="paths">
          <el-table :data="spacePaths" empty-text="暂无路径">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="progressRate" label="进度" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="学习资料" name="knowledge">
          <el-table :data="spaceKnowledge" empty-text="暂无知识库文件">
            <el-table-column prop="originalName" label="文件名" min-width="180" />
            <el-table-column prop="fileType" label="类型" width="80" />
            <el-table-column prop="parserStatus" label="处理状态" width="100" />
            <el-table-column prop="chunkCount" label="片段数" width="80" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="报告" name="reports">
          <el-table :data="spaceReports" empty-text="暂无报告">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="reportType" label="类型" width="120" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="dialog" :title="form.id ? '编辑学习空间' : '新建学习空间'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="空间名称" required><el-input v-model="form.spaceName" /></el-form-item>
        <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import { createSpace, deleteSpace, getSpaceSummary, listSpaces, setDefaultSpace, updateSpace } from '@/api/learningSpace'
import { listResources } from '@/api/resource'
import { listQuizzes } from '@/api/quiz'
import { listLearningPaths } from '@/api/learningPath'
import { listKnowledgeFiles } from '@/api/knowledge'
import { listReportsBySpace } from '@/api/report'

const spaces = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const selected = ref<any>()
const activeTab = ref('overview')
const loadedTabs = ref<Set<string>>(new Set())
const summary = reactive<any>({ resourceCount: 0, taskCount: 0, generatedResourceCount: 0, activeTaskCount: 0, profileCount: 0 })
const spaceResources = ref<any[]>([])
const spaceQuizzes = ref<any[]>([])
const spacePaths = ref<any[]>([])
const spaceKnowledge = ref<any[]>([])
const spaceReports = ref<any[]>([])
const form = reactive<any>({ id: null, spaceName: '', subject: '', description: '' })

async function load() {
  loading.value = true
  try {
    const data = await listSpaces({ pageNum: 1, pageSize: 50 })
    spaces.value = data.records || []
  } finally {
    loading.value = false
  }
}

const tabLoaders: Record<string, () => Promise<void>> = {
  overview: async () => {
    if (!selected.value) return
    Object.assign(summary, await getSpaceSummary(selected.value.id))
  },
  resources: async () => {
    const data = await listResources({ spaceId: selected.value!.id, pageNum: 1, pageSize: 50 })
    spaceResources.value = data.records || []
  },
  quizzes: async () => {
    const data = await listQuizzes({ spaceId: selected.value!.id, pageNum: 1, pageSize: 50 })
    spaceQuizzes.value = data.records || []
  },
  paths: async () => {
    const data = await listLearningPaths({ spaceId: selected.value!.id, pageNum: 1, pageSize: 50 })
    spacePaths.value = data.records || []
  },
  knowledge: async () => {
    const data = await listKnowledgeFiles({ spaceId: selected.value!.id, pageNum: 1, pageSize: 50 })
    spaceKnowledge.value = data.records || []
  },
  reports: async () => {
    spaceReports.value = await listReportsBySpace(selected.value!.id)
  }
}

function selectSpace(row: any) {
  selected.value = row
  activeTab.value = 'overview'
  loadedTabs.value = new Set()
  onTabChange('overview')
}

async function onTabChange(name: string) {
  if (loadedTabs.value.has(name)) return
  loadedTabs.value.add(name)
  const loader = tabLoaders[name]
  if (loader) {
    try { await loader() } catch { /* ignore */ }
  }
}

function openCreate() {
  Object.assign(form, { id: null, spaceName: '', subject: '', description: '' })
  dialog.value = true
}

function edit(row: any) {
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.spaceName) return ElMessage.warning('请输入空间名称')
  saving.value = true
  try {
    if (form.id) await updateSpace(form.id, form)
    else await createSpace(form)
    ElMessage.success('保存成功')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该学习空间？', '提示')
  await deleteSpace(id)
  ElMessage.success('已删除')
  if (selected.value?.id === id) selected.value = null
  load()
}

async function setDefault(id: number) {
  await setDefaultSpace(id)
  ElMessage.success('已设置默认空间')
  load()
}

onMounted(load)
</script>

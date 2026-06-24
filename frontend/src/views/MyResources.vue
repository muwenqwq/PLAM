<template>
  <div class="page my-resources-page">
    <PageHeader
      title="我的资源"
      description="统一管理已经生成的课程笔记、练习题、复习提纲和学习计划。"
    >
      <el-button type="primary" @click="router.push('/resource-generation')">生成新资源</el-button>
    </PageHeader>

    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <div>
            <h2 class="section-title">资源列表</h2>
            <p class="section-subtitle">生成后的资源会保存在这里，可以继续修改或删除。</p>
          </div>
          <div class="inline-actions">
            <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 160px">
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div v-if="filteredResources.length" class="resource-grid">
        <article v-for="item in filteredResources" :key="item.id" class="resource-item">
          <div class="resource-top">
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.outputSummary || '暂无描述，点击修改可以补充。' }}</p>
            </div>
            <el-tag effect="plain">{{ typeName(item.resourceType) }}</el-tag>
          </div>
          <div class="resource-meta">
            <span>来源：{{ sourceName(item) }}</span>
            <span>{{ item.taskId ? '多 Agent 生成' : '普通生成' }}</span>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <div class="resource-actions">
            <el-button size="small" @click="viewResource(item.id)">查看</el-button>
            <el-button size="small" type="primary" plain @click="openEdit(item)">修改</el-button>
            <el-button size="small" type="danger" plain @click="removeResource(item)">删除</el-button>
          </div>
        </article>
      </div>
      <EmptyState
        v-else
        title="还没有生成资源"
        description="先去资源生成页面，选择资料和类型后生成你的第一份学习资源。"
      />
    </el-card>

    <el-dialog v-model="viewVisible" :title="current?.title || '资源详情'" width="860px">
      <template v-if="current">
        <div class="detail-meta">
          <el-tag effect="plain">{{ typeName(current.resourceType) }}</el-tag>
          <el-tag :type="current.taskId ? 'success' : 'info'" effect="plain">
            {{ current.taskId ? '多 Agent 生成' : '普通生成' }}
          </el-tag>
          <span>{{ formatDateTime(current.createdAt) }}</span>
        </div>
        <p class="detail-summary">{{ current.outputSummary || '暂无描述' }}</p>
        <MermaidViewer v-if="current.resourceType === 'knowledge_graph' || current.resourceType === 'mind_map'" :code="extractMermaid(current)" />
        <MarkdownViewer :content="current.contentMarkdown || ''" />
      </template>
      <template #footer>
        <el-button v-if="current" @click="openEdit(current)">修改</el-button>
        <el-button type="primary" @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="修改资源" width="760px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="editForm.resourceType" class="full-width">
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="学科 / 主题">
            <el-input v-model="editForm.subject" />
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="editForm.outputSummary" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.contentMarkdown" type="textarea" :rows="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import MermaidViewer from '@/components/MermaidViewer.vue'
import { deleteResource, getResource, listResources, updateResource } from '@/api/resource'
import { listSpaces } from '@/api/learningSpace'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const resources = ref<any[]>([])
const spaces = ref<any[]>([])
const filterType = ref('')
const loading = ref(false)
const saving = ref(false)
const current = ref<any>()
const viewVisible = ref(false)
const editVisible = ref(false)
const editForm = reactive<any>({
  id: null,
  title: '',
  resourceType: 'lecture_note',
  subject: '',
  outputSummary: '',
  contentMarkdown: '',
  contentJson: null,
  status: 'active'
})

const resourceTypes = [
  { label: '课程笔记', value: 'lecture_note' },
  { label: '知识点总结', value: 'summary' },
  { label: '思维导图', value: 'knowledge_graph' },
  { label: '练习题', value: 'quiz_set' },
  { label: '复习提纲', value: 'review_outline' },
  { label: '错题整理', value: 'mistake_review' },
  { label: '学习计划', value: 'plan' },
  { label: '其他资源', value: 'case_task' },
  { label: '演示提纲', value: 'ppt_outline' }
]

const filteredResources = computed(() => {
  if (!filterType.value) return resources.value
  return resources.value.filter((item) => item.resourceType === filterType.value)
})

const spaceMap = computed(() => new Map(spaces.value.map((s) => [s.id, s.spaceName])))

async function load() {
  loading.value = true
  try {
    const [resourceData, spaceData] = await Promise.all([
      listResources({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
      listSpaces({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] }))
    ])
    resources.value = resourceData.records || []
    spaces.value = spaceData.records || []
  } finally {
    loading.value = false
  }
}

async function viewResource(id: number) {
  current.value = await getResource(id)
  viewVisible.value = true
}

async function openEdit(item: any) {
  const detail = item.contentMarkdown === undefined ? await getResource(item.id) : item
  Object.assign(editForm, {
    id: detail.id,
    title: detail.title,
    resourceType: detail.resourceType || 'lecture_note',
    subject: detail.subject || '',
    outputSummary: detail.outputSummary || '',
    contentMarkdown: detail.contentMarkdown || '',
    contentJson: detail.contentJson || null,
    status: detail.status || 'active'
  })
  editVisible.value = true
}

async function saveEdit() {
  if (!editForm.title?.trim()) {
    ElMessage.warning('请填写资源标题')
    return
  }
  saving.value = true
  try {
    const updated = await updateResource(editForm.id, {
      title: editForm.title,
      resourceType: editForm.resourceType,
      subject: editForm.subject,
      outputSummary: editForm.outputSummary,
      contentMarkdown: editForm.contentMarkdown,
      contentJson: editForm.contentJson,
      status: editForm.status
    })
    ElMessage.success('资源已更新')
    editVisible.value = false
    current.value = updated
    await load()
  } finally {
    saving.value = false
  }
}

async function removeResource(item: any) {
  await ElMessageBox.confirm(`确定删除“${item.title}”吗？删除后不可恢复。`, '删除资源', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteResource(item.id)
  ElMessage.success('资源已删除')
  if (current.value?.id === item.id) {
    current.value = undefined
    viewVisible.value = false
  }
  await load()
}

function typeName(type: string) {
  return resourceTypes.find((item) => item.value === type)?.label || '学习资源'
}

function sourceName(item: any) {
  return spaceMap.value.get(item.spaceId) || item.subject || '当前学习空间'
}

function extractMermaid(item: any) {
  return item?.contentJson?.mermaid || (item?.contentMarkdown || '').match(/```mermaid([\s\S]*?)```/)?.[1]?.trim() || 'graph TD\nA[暂无图谱] --> B[请重新生成]'
}

async function openRouteResource() {
  const editId = Number(route.query.edit)
  const viewId = Number(route.query.view)
  if (Number.isFinite(editId) && editId > 0) {
    await openEdit({ id: editId })
  } else if (Number.isFinite(viewId) && viewId > 0) {
    await viewResource(viewId)
  } else {
    return
  }
  await router.replace({ path: '/resources' })
}

onMounted(async () => {
  await load()
  await openRouteResource()
})
</script>

<style scoped>
.resource-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.resource-item {
  min-height: 220px;
  display: grid;
  gap: 14px;
  align-content: space-between;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
  transition: transform .2s ease, border-color .2s ease;
}

.resource-item:hover {
  transform: translateY(-2px);
  border-color: var(--primary-border);
}

.resource-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.resource-top h3 {
  margin: 0;
  font-size: 17px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.resource-top p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.resource-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: var(--muted);
  font-size: 12px;
}

.resource-actions,
.detail-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.detail-summary {
  margin: 12px 0;
  color: var(--muted);
  line-height: 1.7;
}

.full-width {
  width: 100%;
}

@media (max-width: 1120px) {
  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .resource-grid {
    grid-template-columns: 1fr;
  }

  .resource-top {
    display: grid;
  }
}
</style>

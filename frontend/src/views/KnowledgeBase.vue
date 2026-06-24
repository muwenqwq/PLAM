<template>
  <div class="page knowledge-page">
    <PageHeader
      title="我的知识库"
      description="上传课程资料后，系统可以帮你进行资料问答、总结和资源生成。"
    >
      <el-button type="primary" :loading="creating" @click="uploadFile">上传资料</el-button>
    </PageHeader>

    <div class="grid two">
      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">上传学习资料</h2>
            <p class="section-subtitle">支持 txt、md、docx、pptx 等课程资料。</p>
          </div>
        </template>
        <el-form :model="form" label-position="top">
          <el-form-item label="所属学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间" class="full-width" @change="handleSpaceChange">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="资料文件">
            <el-upload
              class="upload-control"
              drag
              action="#"
              :auto-upload="false"
              :limit="1"
              accept=".txt,.md,.markdown,.csv,.tsv,.json,.xml,.html,.htm,.java,.py,.js,.ts,.vue,.css,.sql,.yaml,.yml,.properties,.log,.docx,.pptx"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <div class="upload-title">{{ selectedFile?.name || '拖拽或选择资料文件' }}</div>
              <div class="upload-subtitle">上传后会自动整理成可问答的学习片段</div>
            </el-upload>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="uploadHint"
          :title="uploadHint"
          type="success"
          show-icon
          :closable="false"
          class="hint-alert"
        />
        <div class="inline-actions">
          <el-button type="primary" :loading="creating" @click="uploadFile">上传资料并整理</el-button>
          <el-button @click="load">刷新列表</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">问问资料</h2>
            <p class="section-subtitle">选择资料后，直接用自己的话提问。</p>
          </div>
        </template>
        <el-input
          v-model="query"
          type="textarea"
          :rows="3"
          placeholder="例如：这份资料里数据库索引应该怎么复习？"
        />
        <div class="actions">
          <el-button :loading="searching" @click="search">查找相关内容</el-button>
          <el-button type="primary" :loading="searching" @click="qa">根据资料回答</el-button>
        </div>
        <div class="result-box">
          <LoadingState v-if="searching" text="正在阅读你的资料" />
          <template v-else>
            <MarkdownViewer v-if="answer" :content="answer" />
            <div v-for="item in results" :key="`${item.source}-${item.chunkIndex}`" class="hit">
              <strong>{{ item.source }}</strong>
              <p>{{ item.chunkText }}</p>
              <el-tag size="small" type="info">相关度 {{ item.score }}</el-tag>
            </div>
            <EmptyState
              v-if="!answer && !results.length"
              title="还没有问答结果"
              description="上传资料后，输入问题即可根据资料查找答案。"
            />
          </template>
        </div>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <div>
            <h2 class="section-title">资料列表</h2>
            <p class="section-subtitle">这里管理你上传过的学习资料。</p>
          </div>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="files" v-loading="loading" empty-text="暂无学习资料">
        <el-table-column prop="originalName" label="文件名" min-width="220" />
        <el-table-column prop="fileType" label="文件类型" width="110" />
        <el-table-column label="所属学习空间" min-width="140">
          <template #default="{ row }">{{ spaceName(row.spaceId) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.parserStatus)" effect="plain">{{ statusText(row.parserStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="片段数量" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewFile(row.id)">查看</el-button>
            <el-button size="small" type="danger" plain @click="removeFile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState
        v-if="!loading && !files.length"
        title="你还没有上传学习资料"
        description="先上传课程资料，之后就可以问答、总结和生成学习资源。"
      />
    </el-card>

    <el-dialog v-model="detailVisible" :title="fileDetail?.originalName || '资料详情'" width="760px">
      <template v-if="fileDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ fileDetail.originalName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ fileDetail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="所属空间">{{ spaceName(fileDetail.spaceId) }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="statusType(fileDetail.parserStatus)" effect="plain">{{ statusText(fileDetail.parserStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="片段数量">{{ fileDetail.chunkCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatDateTime(fileDetail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="fileDetail.errorMessage"
          :title="fileDetail.errorMessage"
          type="error"
          show-icon
          class="hint-alert"
        />
        <div v-if="chunks.length" class="chunk-list">
          <div v-for="c in chunks" :key="c.chunkIndex || c.id" class="chunk-card">
            <strong>片段 {{ c.chunkIndex }}</strong>
            <p>{{ c.contentText }}</p>
          </div>
        </div>
        <EmptyState v-else title="暂无可查看片段" description="资料处理完成后会在这里展示片段预览。" />
      </template>
      <template #footer>
        <el-button v-if="fileDetail" @click="reprocess(fileDetail)">重新整理资料</el-button>
        <el-button type="primary" @click="detailVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { deleteKnowledgeFile, getKnowledgeFile, indexKnowledgeFile, listKnowledgeFiles, qaKnowledge, searchKnowledge, uploadKnowledgeFile } from '@/api/knowledge'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { formatDateTime } from '@/utils/format'

const files = ref<any[]>([])
const results = ref<any[]>([])
const answer = ref('')
const fileDetail = ref<any>()
const chunks = ref<any[]>([])
const spaces = ref<any[]>([])
const query = ref('')
const selectedFile = ref<File | null>(null)
const creating = ref(false)
const loading = ref(false)
const searching = ref(false)
const detailVisible = ref(false)
const uploadHint = ref('')
const form = reactive<any>({ spaceId: null as number | null })

const spaceMap = computed(() => new Map(spaces.value.map((s) => [s.id, s.spaceName])))

async function loadDefaults() {
  const space = await getDefaultSpace().catch(() => null)
  if (space?.id) form.spaceId = space.id
}

async function loadSpaces() {
  const data = await listSpaces({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] }))
  spaces.value = data.records || []
}

async function load() {
  loading.value = true
  try {
    if (!form.spaceId) {
      files.value = []
      return
    }
    const data = await listKnowledgeFiles({ pageNum: 1, pageSize: 50, spaceId: form.spaceId }).catch(() => ({ records: [] }))
    files.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function handleSpaceChange() {
  answer.value = ''
  results.value = []
  fileDetail.value = undefined
  chunks.value = []
  detailVisible.value = false
  await load()
}

function handleFileChange(uploadFile: UploadFile) {
  selectedFile.value = uploadFile.raw || null
  uploadHint.value = ''
}

function handleFileRemove() {
  selectedFile.value = null
  uploadHint.value = ''
}

async function uploadFile() {
  if (!form.spaceId) {
    ElMessage.warning('请先选择学习空间')
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的资料文件')
    return
  }
  creating.value = true
  uploadHint.value = '资料正在上传并整理，请稍候。'
  try {
    const file = await uploadKnowledgeFile(form.spaceId, selectedFile.value)
    uploadHint.value = `资料已整理完成，共生成 ${file.chunkCount || 0} 个学习片段。`
    ElMessage.success('资料已加入知识库')
    selectedFile.value = null
    await load()
    await viewFile(file.id)
  } catch {
    uploadHint.value = ''
  } finally {
    creating.value = false
  }
}

async function viewFile(id: number) {
  fileDetail.value = await getKnowledgeFile(id)
  chunks.value = fileDetail.value?.chunks || []
  detailVisible.value = true
}

async function reprocess(row: any) {
  const file = await indexKnowledgeFile(row.id, {})
  ElMessage.success('资料已重新整理')
  await load()
  fileDetail.value = file
  chunks.value = file?.chunks || []
}

async function removeFile(row: any) {
  await ElMessageBox.confirm(`确定删除“${row.originalName}”吗？删除后它将不能用于问答和资源生成。`, '删除资料', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteKnowledgeFile(row.id)
  ElMessage.success('资料已删除')
  if (fileDetail.value?.id === row.id) {
    detailVisible.value = false
    fileDetail.value = undefined
    chunks.value = []
  }
  await load()
}

async function search() {
  if (!form.spaceId) return ElMessage.warning('请先选择学习空间')
  if (!query.value.trim()) return ElMessage.warning('请先输入你的问题')
  searching.value = true
  answer.value = ''
  try {
    const data = await searchKnowledge({ query: query.value, spaceId: form.spaceId, fileIds: currentSpaceFileIds(), topK: 5 })
    results.value = data.results || []
  } finally {
    searching.value = false
  }
}

async function qa() {
  if (!form.spaceId) return ElMessage.warning('请先选择学习空间')
  if (!query.value.trim()) return ElMessage.warning('请先输入你的问题')
  searching.value = true
  try {
    const data = await qaKnowledge({ query: query.value, spaceId: form.spaceId, fileIds: currentSpaceFileIds(), topK: 5 })
    answer.value = data.answerMarkdown
    results.value = data.results || []
  } finally {
    searching.value = false
  }
}

function currentSpaceFileIds() {
  return files.value.filter((file) => file.spaceId === form.spaceId).map((file) => file.id)
}

function statusText(status?: string) {
  if (!status || status === 'pending' || status === 'processing') return '处理中'
  if (status === 'failed') return '处理失败'
  return '已完成'
}

function statusType(status?: string) {
  if (!status || status === 'pending' || status === 'processing') return 'warning'
  if (status === 'failed') return 'danger'
  return 'success'
}

function spaceName(spaceId?: number) {
  return spaceMap.value.get(spaceId) || (spaceId ? `学习空间 #${spaceId}` : '未分配')
}

onMounted(async () => {
  await Promise.all([loadSpaces(), loadDefaults()])
  await load()
})
</script>

<style scoped>
.actions {
  margin: 12px 0;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.full-width,
.upload-control {
  width: 100%;
}

.upload-control :deep(.el-upload) {
  width: 100%;
}

.upload-control :deep(.el-upload-dragger) {
  width: 100%;
  padding: 34px 18px;
  background: var(--surface-soft);
  border-color: var(--line-strong);
  border-radius: var(--radius-md);
}

.upload-title {
  color: var(--text);
  font-weight: 700;
}

.upload-subtitle {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.hint-alert {
  margin: 14px 0;
}

.hit {
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.hit p {
  margin: 6px 0;
  color: var(--muted);
  line-height: 1.7;
}

.chunk-list {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.chunk-card {
  padding: 12px;
  background: var(--surface-soft);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
}

.chunk-card p {
  margin: 8px 0 0;
  color: var(--text);
  line-height: 1.7;
}
</style>

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
            <p class="section-subtitle">支持文字版 PDF、txt、md、docx、pptx 等课程资料。</p>
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
              ref="uploadRef"
              class="upload-control"
              drag
              multiple
              action="#"
              :auto-upload="false"
              :limit="20"
              accept=".pdf,.txt,.md,.markdown,.csv,.tsv,.json,.xml,.html,.htm,.java,.py,.js,.ts,.vue,.css,.sql,.yaml,.yml,.properties,.log,.docx,.pptx"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :on-exceed="handleFileExceed"
            >
              <div class="upload-title">{{ selectedFiles.length ? `已选择 ${selectedFiles.length} 个文件` : '拖拽或选择多个资料文件' }}</div>
              <div class="upload-subtitle">一次最多选择 20 个文件，上传后会逐个整理成可问答的学习片段</div>
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
          <div class="knowledge-chat-head">
            <div>
              <h2 class="section-title">知识库 AI 对话</h2>
              <p class="section-subtitle">AI 只参考当前学习空间的资料回答，并标出实际引用。</p>
            </div>
            <div class="inline-actions">
              <el-select v-model="modelProviderId" placeholder="选择 AI 服务" style="width: 160px">
                <el-option v-for="provider in providers" :key="provider.id" :label="provider.providerName" :value="provider.id" />
              </el-select>
              <el-tooltip content="清空当前对话" placement="top">
                <el-button circle :icon="Delete" aria-label="清空知识库对话" @click="clearChat" />
              </el-tooltip>
            </div>
          </div>
        </template>
        <div ref="chatScrollRef" class="knowledge-chat-body">
          <EmptyState
            v-if="!messages.length && !searching"
            title="从当前资料开始提问"
            description="可以追问概念、比较观点、整理复习重点，回答会附上引用来源。"
          />
          <article v-for="message in messages" :key="message.id" class="knowledge-message" :class="message.role">
            <div class="message-role">{{ message.role === 'user' ? '我' : '知识库 AI' }}</div>
            <div class="message-bubble">
              <p v-if="message.role === 'user'" class="user-message">{{ message.content }}</p>
              <MarkdownViewer v-else :content="message.content" />
              <el-collapse v-if="message.role === 'assistant' && message.citations?.length" class="evidence-collapse">
                <el-collapse-item :title="`引用资料（${message.citations.length} 个片段）`" :name="`evidence-${message.id}`">
                  <div v-for="item in message.citations" :key="`${message.id}-${sourceName(item)}-${item.chunkIndex}`" class="hit">
                    <div class="hit-head">
                      <div>
                        <strong>{{ sourceName(item) }}</strong>
                        <p class="hit-meta">片段 {{ item.chunkIndex ?? '-' }} · {{ retrievalModeLabel(item.retrievalMode) }}</p>
                      </div>
                      <el-tag size="small" type="success">{{ scorePercent(item.score) }}</el-tag>
                    </div>
                    <p class="hit-snippet">{{ item.chunkText }}</p>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </article>
          <LoadingState v-if="searching" text="正在阅读资料并组织回答" />
        </div>
        <div class="knowledge-composer">
          <el-input
            v-model="query"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入问题，按 Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="qa"
          />
          <el-button type="primary" :icon="Promotion" :loading="searching" @click="qa">发送</el-button>
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { Delete, Promotion } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { deleteKnowledgeFile, getKnowledgeFile, indexKnowledgeFile, listKnowledgeFiles, qaKnowledge, uploadKnowledgeFile } from '@/api/knowledge'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { formatDateTime } from '@/utils/format'

const files = ref<any[]>([])
const messages = ref<any[]>([])
const fileDetail = ref<any>()
const chunks = ref<any[]>([])
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const modelProviderId = ref<number | null>(null)
const query = ref('')
const chatScrollRef = ref<HTMLElement>()
const uploadRef = ref<UploadInstance>()
const selectedFiles = ref<File[]>([])
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

async function loadProviders() {
  const [data, defaultProvider] = await Promise.all([
    listProviders({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] })),
    getDefaultProvider().catch(() => null)
  ])
  providers.value = data.records || []
  modelProviderId.value = defaultProvider?.id || providers.value[0]?.id || null
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
  loadChat()
  fileDetail.value = undefined
  chunks.value = []
  detailVisible.value = false
  await load()
}

function syncSelectedFiles(uploadFiles: UploadFile[]) {
  selectedFiles.value = uploadFiles.flatMap((item) => item.raw ? [item.raw] : [])
}

function handleFileChange(_uploadFile: UploadFile, uploadFiles: UploadFile[]) {
  syncSelectedFiles(uploadFiles)
  uploadHint.value = ''
}

function handleFileRemove(_uploadFile: UploadFile, uploadFiles: UploadFile[]) {
  syncSelectedFiles(uploadFiles)
  uploadHint.value = ''
}

function handleFileExceed() {
  ElMessage.warning('一次最多上传 20 个资料文件')
}

async function uploadFile() {
  if (!form.spaceId) {
    ElMessage.warning('请先选择学习空间')
    return
  }
  if (!selectedFiles.value.length) {
    ElMessage.warning('请至少选择一个要上传的资料文件')
    return
  }
  creating.value = true
  const pendingFiles = [...selectedFiles.value]
  uploadHint.value = `正在上传并整理 ${pendingFiles.length} 个资料文件，请稍候。`
  try {
    const uploaded: any[] = []
    const failed: string[] = []
    for (const sourceFile of pendingFiles) {
      try {
        uploaded.push(await uploadKnowledgeFile(form.spaceId, sourceFile))
      } catch {
        failed.push(sourceFile.name)
      }
    }
    const chunkCount = uploaded.reduce((total, item) => total + Number(item.chunkCount || 0), 0)
    uploadHint.value = failed.length
      ? `已整理 ${uploaded.length} 个文件、生成 ${chunkCount} 个学习片段；${failed.length} 个文件上传失败。`
      : `已整理 ${uploaded.length} 个文件，共生成 ${chunkCount} 个学习片段。`
    if (uploaded.length) ElMessage.success(`${uploaded.length} 个资料文件已加入知识库`)
    if (failed.length) ElMessage.warning(`未成功上传：${failed.join('、')}`)
    selectedFiles.value = []
    uploadRef.value?.clearFiles()
    await load()
    if (uploaded.length === 1) await viewFile(uploaded[0].id)
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

async function qa() {
  if (!form.spaceId) return ElMessage.warning('请先选择学习空间')
  if (!query.value.trim()) return ElMessage.warning('请先输入你的问题')
  const question = query.value.trim()
  const history = messages.value.slice(-12).map((item) => ({ role: item.role, content: item.content }))
  messages.value.push({ id: messageId(), role: 'user', content: question, citations: [] })
  query.value = ''
  saveChat()
  await scrollChat()
  searching.value = true
  try {
    const data = await qaKnowledge({
      query: question,
      spaceId: form.spaceId,
      modelProviderId: modelProviderId.value,
      fileIds: currentSpaceFileIds(),
      topK: 5,
      history
    })
    messages.value.push({
      id: messageId(),
      role: 'assistant',
      content: data.answerMarkdown || '当前资料不足以回答这个问题。',
      citations: data.results || []
    })
    saveChat()
  } catch {
    messages.value.push({
      id: messageId(),
      role: 'assistant',
      content: '本次资料问答没有完成，请检查 AI 服务配置后重试。',
      citations: []
    })
  } finally {
    searching.value = false
    await scrollChat()
  }
}

function messageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function chatKey() {
  return form.spaceId ? `knowledge-chat-${form.spaceId}` : ''
}

function loadChat() {
  const key = chatKey()
  if (!key) {
    messages.value = []
    return
  }
  try {
    const stored = JSON.parse(sessionStorage.getItem(key) || '[]')
    messages.value = Array.isArray(stored) ? stored.slice(-30) : []
  } catch {
    messages.value = []
  }
  scrollChat()
}

function saveChat() {
  const key = chatKey()
  if (key) sessionStorage.setItem(key, JSON.stringify(messages.value.slice(-30)))
}

function clearChat() {
  messages.value = []
  const key = chatKey()
  if (key) sessionStorage.removeItem(key)
  ElMessage.success('当前知识库对话已清空')
}

async function scrollChat() {
  await nextTick()
  if (chatScrollRef.value) chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
}


function sourceName(item: any) {
  return item.sourceFileName || item.source || '知识片段'
}

function scorePercent(value: any) {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) return '相关度 -'
  return `相关度 ${Math.round(Math.min(1, Math.max(0, num)) * 100)}%`
}

function retrievalModeLabel(mode?: string) {
  return ({ chroma: 'Chroma 向量检索', local_vector: '本地向量检索', mysql_fallback: 'MySQL 兜底检索', mysql: 'MySQL 检索' } as Record<string, string>)[mode || ''] || '知识库检索'
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
  await Promise.all([loadSpaces(), loadProviders(), loadDefaults()])
  await load()
  loadChat()
})
</script>

<style scoped>
.knowledge-chat-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }

.knowledge-chat-body {
  height: 470px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 4px 14px;
}

.knowledge-message { display: flex; gap: 10px; align-items: flex-start; }
.knowledge-message.user { flex-direction: row-reverse; }
.message-role { flex: 0 0 auto; padding-top: 9px; color: var(--muted); font-size: 12px; font-weight: 700; }
.message-bubble { max-width: 86%; padding: 12px 14px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface-soft); }
.knowledge-message.user .message-bubble { background: var(--primary-soft); border-color: var(--primary-border); }
.user-message { margin: 0; white-space: pre-wrap; line-height: 1.75; }
.knowledge-composer { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; align-items: end; padding-top: 12px; border-top: 1px solid var(--line); }

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

.evidence-collapse {
  margin-top: 16px;
}


.hit-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.hit-meta {
  margin: 3px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.hit-snippet {
  margin: 8px 0;
  color: var(--text);
  line-height: 1.7;
}

.hit-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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

@media (max-width: 760px) {
  .knowledge-chat-head { align-items: flex-start; flex-direction: column; }
  .knowledge-chat-body { height: 420px; }
  .message-bubble { max-width: 92%; }
  .knowledge-composer { grid-template-columns: 1fr; }
}
</style>

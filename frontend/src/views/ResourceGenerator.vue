<template>
  <div class="page resource-generation-page">
    <PageHeader
      title="资源生成"
      description="选择学习资料和资源类型，系统会生成并自动保存到“我的资源”。"
    >
      <el-button @click="router.push('/resources')">查看我的资源</el-button>
    </PageHeader>

    <div class="grid two">
      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">生成设置</h2>
            <p class="section-subtitle">按步骤确认资料、类型和生成方式。</p>
          </div>
        </template>
        <el-steps :active="stepActive" finish-status="success" simple class="steps">
          <el-step title="资料" />
          <el-step title="类型" />
          <el-step title="方式" />
        </el-steps>

        <el-form :model="form" label-position="top" class="generation-form">
          <el-form-item label="选择学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间" class="full-width" @change="loadFiles">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="选择资料来源">
            <el-select
              v-model="selectedFileIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="从我的知识库选择资料"
              class="full-width"
            >
              <el-option v-for="f in knowledgeFiles" :key="f.id" :label="f.originalName" :value="f.id" />
            </el-select>
            <p class="helper-text">如果暂时不选择具体文件，系统会按当前学习空间和关键词生成。</p>
          </el-form-item>

          <div class="type-grid">
            <button
              v-for="item in resourceTypes"
              :key="item.value"
              type="button"
              class="type-card"
              :class="{ active: form.resourceType === item.value }"
              @click="selectType(item.value)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <strong>{{ item.label }}</strong>
              <span>{{ item.text }}</span>
            </button>
          </div>

          <div class="form-row">
            <el-form-item label="资源标题">
              <el-input v-model="form.title" placeholder="例如：数据库索引复习笔记" />
            </el-form-item>
            <el-form-item label="学科">
              <el-input v-model="form.subject" placeholder="例如：数据库" />
            </el-form-item>
          </div>
          <el-form-item label="重点内容">
            <el-input v-model="knowledgeText" placeholder="用逗号分隔，例如：索引, 范式, SQL 查询" />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" class="full-width">
                <el-option label="基础" value="easy" />
                <el-option label="中等" value="medium" />
                <el-option label="提高" value="hard" />
              </el-select>
            </el-form-item>
            <el-form-item label="内容长度">
              <el-select v-model="form.outputLength" class="full-width">
                <el-option label="简短" value="short" />
                <el-option label="中等" value="medium" />
                <el-option label="详细" value="long" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="生成方式">
            <el-segmented v-model="generationMode" :options="generationModes" />
          </el-form-item>
          <div v-if="generationMode === 'multi_agent'" class="soft-panel">
            <div class="toolbar">
              <div>
                <strong>多 Agent 协作生成</strong>
                <p class="helper-text">适合需要更完整结构、练习题或复习计划的场景。</p>
              </div>
              <el-tag type="success" effect="plain">已启用</el-tag>
            </div>
            <el-checkbox-group v-model="selectedAgents" class="agent-options">
              <el-checkbox-button v-for="agent in agentOptions" :key="agent.value" :label="agent.value">
                {{ agent.label }}
              </el-checkbox-button>
            </el-checkbox-group>
            <el-form-item label="生成深度" class="depth-select">
              <el-select v-model="agentDepth" class="full-width">
                <el-option label="快速生成" value="quick" />
                <el-option label="标准生成" value="standard" />
                <el-option label="深入生成" value="deep" />
              </el-select>
            </el-form-item>
          </div>

          <el-collapse class="advanced">
            <el-collapse-item title="高级选项" name="advanced">
              <el-form-item label="AI 服务">
                <el-select v-model="form.modelProviderId" placeholder="默认服务" class="full-width">
                  <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
                </el-select>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">生成前确认</h2>
            <p class="section-subtitle">确认后开始生成，完成后会自动保存。</p>
          </div>
        </template>
        <div class="confirm-list">
          <div><span>资源类型</span><strong>{{ typeName(form.resourceType) }}</strong></div>
          <div><span>资料来源</span><strong>{{ selectedFileNames || '当前学习空间 / 关键词' }}</strong></div>
          <div><span>生成方式</span><strong>{{ generationMode === 'multi_agent' ? '多 Agent 协作生成' : '普通生成' }}</strong></div>
          <div><span>难度与长度</span><strong>{{ difficultyName(form.difficulty) }} / {{ lengthName(form.outputLength) }}</strong></div>
        </div>

        <el-alert
          v-if="generationMode === 'multi_agent'"
          title="多 Agent 模式会调用多智能体任务接口，生成结果同样保存到我的资源。"
          type="success"
          show-icon
          :closable="false"
          class="hint-alert"
        />
        <el-alert
          v-else
          title="普通生成会使用当前资料和关键词快速生成一个资源。"
          type="info"
          show-icon
          :closable="false"
          class="hint-alert"
        />

        <el-button type="primary" size="large" :loading="generating" class="generate-button" @click="generate">
          {{ generating ? '正在生成' : '开始生成' }}
        </el-button>

        <div class="result-panel">
          <LoadingState v-if="generating" text="正在生成学习资源，请稍候" />
          <template v-else-if="current">
            <div class="toolbar result-header">
              <div>
                <h3>{{ current.title }}</h3>
                <p>{{ current.outputSummary || '资源已生成并保存。' }}</p>
              </div>
              <el-tag type="success" effect="plain">已保存</el-tag>
            </div>
            <MermaidViewer v-if="current.resourceType === 'knowledge_graph' || current.resourceType === 'mind_map'" :code="extractMermaid(current)" />
            <MarkdownViewer :content="current.contentMarkdown || ''" />
            <div class="inline-actions">
              <el-button type="primary" @click="openGeneratedResource('view')">查看资源</el-button>
              <el-button @click="openGeneratedResource('edit')">修改资源</el-button>
              <el-button @click="resetCurrent">继续生成</el-button>
            </div>
          </template>
          <EmptyState
            v-else
            title="等待生成"
            description="完成左侧设置后，点击开始生成。"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import MermaidViewer from '@/components/MermaidViewer.vue'
import { generateResource } from '@/api/resource'
import { createAgentTask } from '@/api/agent'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { listKnowledgeFiles } from '@/api/knowledge'
import { parseKnowledgePoints } from '@/utils/knowledge'

const router = useRouter()
const generating = ref(false)
const current = ref<any>()
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const knowledgeFiles = ref<any[]>([])
const selectedFileIds = ref<number[]>([])
const knowledgeText = ref('索引, 范式, SQL 查询')
const generationMode = ref<'normal' | 'multi_agent'>('normal')
const selectedAgents = ref(['planner', 'knowledge', 'content', 'review'])
const agentDepth = ref('standard')
const form = reactive<any>({
  spaceId: null as number | null,
  modelProviderId: null as number | null,
  title: '数据库索引复习笔记',
  subject: '数据库',
  resourceType: 'lecture_note',
  difficulty: 'medium',
  outputLength: 'medium',
  useKnowledgeBase: true
})

const generationModes = [
  { label: '普通生成', value: 'normal' },
  { label: '多 Agent 协作生成', value: 'multi_agent' }
]

const agentOptions = [
  { label: '规划', value: 'planner' },
  { label: '知识整理', value: 'knowledge' },
  { label: '内容生成', value: 'content' },
  { label: '练习设计', value: 'exercise' },
  { label: '复核优化', value: 'review' }
]

const resourceTypes = [
  { label: '课程笔记', value: 'lecture_note', icon: 'Notebook', text: '整理成可直接复习的笔记' },
  { label: '知识点总结', value: 'summary', icon: 'Collection', text: '提炼关键概念和易错点' },
  { label: '思维导图', value: 'knowledge_graph', icon: 'Share', text: '用结构图串联知识点' },
  { label: '练习题', value: 'quiz_set', icon: 'EditPen', text: '生成题目和参考答案' },
  { label: '复习提纲', value: 'review_outline', icon: 'Tickets', text: '按章节生成复习框架' },
  { label: '错题整理', value: 'mistake_review', icon: 'Warning', text: '归纳薄弱点和订正建议' },
  { label: '学习计划', value: 'plan', icon: 'Calendar', text: '安排分阶段学习任务' },
  { label: '其他资源', value: 'case_task', icon: 'MagicStick', text: '生成案例、任务或扩展材料' }
]

const stepActive = computed(() => {
  if (generationMode.value === 'multi_agent') return 3
  if (form.resourceType) return 2
  if (form.spaceId || selectedFileIds.value.length) return 1
  return 0
})

const selectedFileNames = computed(() => knowledgeFiles.value
  .filter((f) => selectedFileIds.value.includes(f.id))
  .map((f) => f.originalName)
  .join('、'))

async function loadDefaults() {
  const [space, provider] = await Promise.all([
    getDefaultSpace().catch(() => null),
    getDefaultProvider().catch(() => null)
  ])
  if (space?.id) {
    form.spaceId = space.id
    await loadFiles()
  }
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

async function loadFiles() {
  selectedFileIds.value = []
  const params: any = { pageNum: 1, pageSize: 100 }
  if (form.spaceId) params.spaceId = form.spaceId
  const data = await listKnowledgeFiles(params).catch(() => ({ records: [] }))
  knowledgeFiles.value = data.records || []
}

function selectType(type: string) {
  form.resourceType = type
  if (!form.title || resourceTypes.some((item) => form.title.includes(item.label))) {
    form.title = `${form.subject || '课程'}${typeName(type)}`
  }
}

function typeName(type: string) {
  return resourceTypes.find((item) => item.value === type)?.label || '学习资源'
}

function difficultyName(value: string) {
  return ({ easy: '基础', medium: '中等', hard: '提高' } as Record<string, string>)[value] || value
}

function lengthName(value: string) {
  return ({ short: '简短', medium: '中等', long: '详细' } as Record<string, string>)[value] || value
}

function buildInputParams(knowledgePoints: string[]) {
  return {
    knowledge_points: knowledgePoints,
    difficulty: form.difficulty,
    output_length: form.outputLength,
    use_knowledge_base: form.useKnowledgeBase,
    source_file_ids: selectedFileIds.value,
    source_file_names: selectedFileNames.value ? selectedFileNames.value.split('、') : [],
    generation_depth: agentDepth.value,
    enabled_agents: selectedAgents.value
  }
}

async function generate() {
  if (!form.spaceId) return ElMessage.warning('请先选择学习空间')
  if (!form.title?.trim()) return ElMessage.warning('请填写资源标题')
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!knowledgePoints.length && !selectedFileIds.value.length) {
    return ElMessage.warning('请填写重点内容或选择资料来源')
  }
  generating.value = true
  current.value = undefined
  try {
    if (generationMode.value === 'multi_agent') {
      const data = await createAgentTask({
        spaceId: form.spaceId,
        providerId: form.modelProviderId,
        taskType: 'resource_generation',
        title: form.title,
        subject: form.subject,
        resourceType: form.resourceType,
        inputParams: buildInputParams(knowledgePoints)
      })
      current.value = data.resources?.[0]
      if (!current.value) throw new Error('多 Agent 未返回可保存资源')
      ElMessage.success('多 Agent 资源已生成并保存')
    } else {
      const data = await generateResource({
        ...form,
        knowledgePoints,
        sourceFileIds: selectedFileIds.value,
        sourceFileNames: selectedFileNames.value ? selectedFileNames.value.split('、') : []
      })
      current.value = data.resource
      ElMessage.success('资源已生成并保存')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function extractMermaid(item: any) {
  return item?.contentJson?.mermaid || (item?.contentMarkdown || '').match(/```mermaid([\s\S]*?)```/)?.[1]?.trim() || 'graph TD\nA[暂无图谱] --> B[请重新生成]'
}

function openGeneratedResource(action: 'view' | 'edit') {
  if (!current.value?.id) return
  router.push({ path: '/resources', query: { [action]: String(current.value.id) } })
}

function resetCurrent() {
  current.value = undefined
}

onMounted(async () => {
  await loadSpacesAndProviders()
  await loadDefaults()
})
</script>

<style scoped>
.steps {
  margin-bottom: 18px;
}

.generation-form {
  display: grid;
  gap: 4px;
}

.full-width {
  width: 100%;
}

.helper-text {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 8px 0 18px;
}

.type-card {
  min-height: 118px;
  display: grid;
  align-content: start;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition: transform .2s ease, border-color .2s ease, background-color .2s ease;
}

.type-card:hover {
  transform: translateY(-2px);
  border-color: var(--primary-border);
}

.type-card.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
  background: var(--primary-soft);
}

.type-card .el-icon {
  color: var(--primary);
}

.type-card span {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.agent-options {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.depth-select {
  margin-bottom: 0;
}

.advanced {
  border-top: 0;
  border-bottom: 0;
}

.confirm-list {
  display: grid;
  gap: 10px;
}

.confirm-list > div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  border: 1px solid var(--line);
}

.confirm-list span {
  color: var(--muted);
}

.confirm-list strong {
  text-align: right;
  overflow-wrap: anywhere;
}

.hint-alert {
  margin: 14px 0;
}

.generate-button {
  width: 100%;
  min-height: 44px;
}

.result-panel {
  margin-top: 16px;
  min-height: 240px;
}

.result-header h3 {
  margin: 0 0 6px;
}

.result-header p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .type-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .type-grid {
    grid-template-columns: 1fr;
  }

  .confirm-list > div {
    display: grid;
  }

  .confirm-list strong {
    text-align: left;
  }
}
</style>

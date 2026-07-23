<template>
  <div class="page">
    <PageHeader title="学习路径" description="AI 先拆解学习目标，你可以继续编辑任务、顺序和时间安排。" />
    <div class="grid two">
      <el-card>
        <template #header>生成路径</template>
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
          <el-form-item label="学科"><el-input v-model="form.subject" placeholder="填写课程或学科名称" /></el-form-item>
          <el-form-item label="目标"><el-input v-model="form.goal" placeholder="填写希望在这段时间达到的学习目标" /></el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" placeholder="填写要覆盖的知识点，用逗号分隔" /></el-form-item>
          <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" :max="90" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成路径</el-button>
      </el-card>
      <el-card>
        <template #header>今日任务</template>
        <el-timeline v-if="today.length">
          <el-timeline-item v-for="item in today" :key="item.id" :timestamp="item.dueDate">
            <strong>{{ item.title }}</strong>
            <p class="muted task-description">{{ item.description }}</p>
          </el-timeline-item>
        </el-timeline>
        <EmptyState v-else title="今天暂无待办任务" />
      </el-card>
    </div>
    <el-card>
      <template #header>路径列表</template>
      <el-table :data="paths" empty-text="暂无学习路径" @row-click="select">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column label="进度" width="180">
          <template #default="{ row }"><el-progress :percentage="Number(row.progressRate || 0)" /></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button size="small" :icon="Edit" @click.stop="openEditor(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="current">
      <template #header>
        <div class="toolbar">
          <span>{{ current.title }}</span>
          <el-progress :percentage="Number(current.progressRate || 0)" style="width: 180px" />
        </div>
      </template>
      <el-timeline>
        <el-timeline-item v-for="item in current.items" :key="item.id" :timestamp="item.dueDate" placement="top">
          <el-card shadow="never">
            <div class="toolbar">
              <div class="task-title">
                <strong>{{ item.itemOrder }}. {{ item.title }}</strong>
                <el-tag size="small" :type="item.status === 'done' ? 'success' : 'info'">{{ item.status === 'done' ? '已完成' : '待完成' }}</el-tag>
              </div>
              <el-button size="small" type="success" :disabled="item.status === 'done'" @click="done(item)">{{ item.status === 'done' ? '已完成' : '标记完成' }}</el-button>
            </div>
            <p class="muted task-description">{{ item.description }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑学习路径" width="920px" destroy-on-close>
      <el-form :model="editForm" label-position="top">
        <div class="form-row path-meta-grid">
          <el-form-item label="路径标题" required><el-input v-model="editForm.title" /></el-form-item>
          <el-form-item label="学科" required><el-input v-model="editForm.subject" /></el-form-item>
        </div>
        <el-form-item label="学习目标" required>
          <el-input v-model="editForm.goal" type="textarea" :rows="2" />
        </el-form-item>
        <div class="form-row path-meta-grid">
          <el-form-item label="开始日期">
            <el-date-picker v-model="editForm.startDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
          <el-form-item label="目标日期">
            <el-date-picker v-model="editForm.targetDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
        </div>

        <div class="edit-section-head">
          <div>
            <h3>阶段任务</h3>
            <p>在 AI 生成结果上直接修改，也可以增删任务并调整顺序。</p>
          </div>
          <el-button :icon="Plus" @click="addEditItem">添加任务</el-button>
        </div>
        <div class="edit-task-list">
          <section v-for="(item, index) in editForm.items" :key="item.localKey" class="edit-task">
            <div class="edit-task-head">
              <strong>任务 {{ index + 1 }}</strong>
              <div class="edit-task-actions">
                <el-tooltip content="上移" placement="top">
                  <el-button circle size="small" :icon="ArrowUp" :disabled="index === 0" @click="moveEditItem(index, -1)" />
                </el-tooltip>
                <el-tooltip content="下移" placement="top">
                  <el-button circle size="small" :icon="ArrowDown" :disabled="index === editForm.items.length - 1" @click="moveEditItem(index, 1)" />
                </el-tooltip>
                <el-tooltip content="删除任务" placement="top">
                  <el-button circle size="small" type="danger" plain :icon="Delete" :disabled="editForm.items.length === 1" @click="removeEditItem(index)" />
                </el-tooltip>
              </div>
            </div>
            <div class="form-row path-meta-grid">
              <el-form-item label="任务标题" required><el-input v-model="item.title" /></el-form-item>
              <el-form-item label="知识点"><el-input v-model="item.knowledgePointsText" placeholder="多个知识点用逗号分隔" /></el-form-item>
            </div>
            <el-form-item label="任务说明"><el-input v-model="item.description" type="textarea" :rows="2" /></el-form-item>
            <div class="edit-task-fields">
              <el-form-item label="计划日期"><el-date-picker v-model="item.dueDate" type="date" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
              <el-form-item label="预计时长（分钟）"><el-input-number v-model="item.estimatedMinutes" :min="1" :max="1440" /></el-form-item>
              <el-form-item label="状态">
                <el-select v-model="item.status" class="full-width">
                  <el-option label="待完成" value="todo" />
                  <el-option label="进行中" value="doing" />
                  <el-option label="已完成" value="done" />
                </el-select>
              </el-form-item>
            </div>
          </section>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="saveEdit">保存路径</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, Delete, Edit, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { generateLearningPath, getLearningPath, listLearningPaths, todayTasks, updateLearningPath, updatePathItemStatus } from '@/api/learningPath'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { parseKnowledgePoints } from '@/utils/knowledge'

const paths = ref<any[]>([])
const today = ref<any[]>([])
const current = ref<any>()
const generating = ref(false)
const editVisible = ref(false)
const savingEdit = ref(false)
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const knowledgeText = ref('')
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, subject: '', goal: '', days: 7 })
const editForm = reactive<any>({ id: null, title: '', subject: '', goal: '', startDate: '', targetDate: '', items: [] })

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
    listSpaces({ pageNum: 1, pageSize: 50 }),
    listProviders({ pageNum: 1, pageSize: 50 })
  ])
  spaces.value = spaceData.records || []
  providers.value = providerData.records || []
}

async function load() {
  if (!form.spaceId) {
    paths.value = []
    today.value = []
    return
  }
  const data = await listLearningPaths({ spaceId: form.spaceId, pageNum: 1, pageSize: 30 })
  paths.value = data.records || []
  today.value = await todayTasks(form.spaceId)
}

async function handleSpaceChange() {
  current.value = undefined
  await load()
}

async function generate() {
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!form.subject?.trim() || !form.goal?.trim()) {
    ElMessage.warning('请填写学科和学习目标')
    return
  }
  if (!knowledgePoints.length) {
    ElMessage.warning('请至少填写一个知识点')
    return
  }
  generating.value = true
  current.value = undefined
  try {
    current.value = await generateLearningPath({ ...form, knowledgePoints })
    ElMessage.success('学习路径已生成')
    await load()
  } finally {
    generating.value = false
  }
}

async function select(row: any) {
  current.value = await getLearningPath(row.id)
}

async function done(item: any) {
  await updatePathItemStatus(item.id, 'done')
  ElMessage.success('已标记完成')
  if (current.value) await select(current.value)
  await load()
}

function knowledgePointsText(value: any) {
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.join(', ')
    } catch {}
    return value
  }
  return ''
}

function editableItem(item: any = {}) {
  return {
    id: item.id || null,
    localKey: item.id ? `item-${item.id}` : `new-${Date.now()}-${Math.random()}`,
    title: item.title || '',
    description: item.description || '',
    knowledgePointsText: knowledgePointsText(item.knowledgePoints),
    estimatedMinutes: Number(item.estimatedMinutes || 30),
    dueDate: item.dueDate || editForm.startDate || '',
    status: item.status || 'todo'
  }
}

async function openEditor(row: any) {
  const detail = await getLearningPath(row.id)
  current.value = detail
  Object.assign(editForm, {
    id: detail.id,
    title: detail.title || '',
    subject: detail.subject || '',
    goal: detail.goal || '',
    startDate: detail.startDate || '',
    targetDate: detail.targetDate || '',
    items: (detail.items || []).map(editableItem)
  })
  if (!editForm.items.length) editForm.items = [editableItem()]
  editVisible.value = true
}

function addEditItem() {
  editForm.items.push(editableItem({ dueDate: editForm.targetDate || editForm.startDate }))
}

function removeEditItem(index: number) {
  if (editForm.items.length > 1) editForm.items.splice(index, 1)
}

function moveEditItem(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= editForm.items.length) return
  const [item] = editForm.items.splice(index, 1)
  editForm.items.splice(target, 0, item)
}

async function saveEdit() {
  if (!editForm.title.trim() || !editForm.subject.trim() || !editForm.goal.trim()) {
    return ElMessage.warning('请完整填写路径标题、学科和学习目标')
  }
  if (!editForm.items.length || editForm.items.some((item: any) => !item.title.trim())) {
    return ElMessage.warning('每个阶段任务都需要填写标题')
  }
  savingEdit.value = true
  try {
    current.value = await updateLearningPath(editForm.id, {
      title: editForm.title.trim(),
      subject: editForm.subject.trim(),
      goal: editForm.goal.trim(),
      startDate: editForm.startDate || null,
      targetDate: editForm.targetDate || null,
      items: editForm.items.map((item: any) => ({
        id: item.id || null,
        title: item.title.trim(),
        description: item.description?.trim() || '',
        knowledgePoints: parseKnowledgePoints(item.knowledgePointsText),
        estimatedMinutes: Number(item.estimatedMinutes || 30),
        dueDate: item.dueDate || null,
        status: item.status || 'todo'
      }))
    })
    editVisible.value = false
    ElMessage.success('学习路径已保存')
    await load()
  } finally {
    savingEdit.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults()])
  await load()
})
</script>

<style scoped>
.task-title { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.task-description { white-space: pre-line; line-height: 1.75; }
.path-meta-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.edit-section-head, .edit-task-head, .edit-task-actions { display: flex; align-items: center; }
.edit-section-head { justify-content: space-between; gap: 16px; margin: 6px 0 12px; }
.edit-section-head h3 { margin: 0; }
.edit-section-head p { margin: 5px 0 0; color: var(--muted); font-size: 13px; }
.edit-task-list { display: grid; gap: 12px; max-height: 50vh; overflow: auto; padding-right: 4px; }
.edit-task { padding: 14px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface-soft); }
.edit-task-head { justify-content: space-between; margin-bottom: 10px; }
.edit-task-actions { gap: 6px; }
.edit-task-fields { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.full-width { width: 100%; }
@media (max-width: 760px) {
  .path-meta-grid, .edit-task-fields { grid-template-columns: 1fr; }
}
</style>

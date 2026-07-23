<template>
  <div class="page">
    <PageHeader title="学习测验" description="生成小测验，检查自己对资料和知识点的掌握情况。" />
    <div class="grid two">
      <el-card>
        <template #header>生成测验</template>
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
          <el-form-item label="标题"><el-input v-model="form.title" placeholder="填写这份测验的标题" /></el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" placeholder="填写要考查的知识点，用逗号分隔" /></el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty">
              <el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型"><el-tag type="info">单项选择题（四选一）</el-tag></el-form-item>
          <el-form-item label="题目数"><el-input-number v-model="form.questionCount" :min="1" :max="30" /></el-form-item>
          <el-form-item label="AI 角色">
            <div class="role-setting">
              <el-checkbox v-model="form.rolePlayEnabled">使用默认 AI 角色风格生成题目和讲解</el-checkbox>
              <el-select v-model="form.companionRoleId" placeholder="选择陪伴角色" clearable :disabled="!form.rolePlayEnabled">
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
              <el-alert v-if="form.rolePlayEnabled" :title="roleStyleText()" type="info" :closable="false" show-icon />
            </div>
          </el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成测验</el-button>
      </el-card>
      <el-card>
        <template #header>掌握度</template>
        <el-table :data="mastery" empty-text="暂无掌握度记录">
          <el-table-column prop="knowledgePoint" label="知识点" />
          <el-table-column prop="masteryLevel" label="掌握度" width="120" />
          <el-table-column label="状态" width="100"><template #default="{ row }">{{ masteryStatus(row.status) }}</template></el-table-column>
        </el-table>
      </el-card>
    </div>
    <el-card v-if="quiz">
      <template #header>
        <div class="toolbar"><span>{{ quiz.title }}</span><div class="inline-actions"><el-tag>{{ quiz.status === 'submitted' ? '已提交' : '待作答' }}</el-tag><el-button size="small" type="danger" plain @click="removeQuiz(quiz)">删除测验</el-button></div></div>
      </template>
      <div v-for="q in quiz.questions" :key="q.id" class="question">
        <div class="toolbar">
          <strong>{{ q.questionOrder }}. {{ q.stem }} <el-tag size="small" type="info">单项选择题</el-tag></strong>
          <div class="inline-actions"><el-button size="small" @click="copyQuestion(q)">复制题目</el-button><el-button size="small" type="primary" plain @click="askAi(q)">问 AI</el-button></div>
        </div>
        <el-radio-group v-model="answers[q.id]" :disabled="quiz.status === 'submitted'">
          <el-radio v-for="(opt, idx) in options(q)" :key="idx" :value="optionValue(opt, idx)">{{ opt }}</el-radio>
        </el-radio-group>
      </div>
      <el-button v-if="quiz.status !== 'submitted'" type="primary" :loading="submitting" @click="submit">提交测验</el-button>
    </el-card>
    <el-card v-if="result">
      <template #header>测验结果</template>
      <div class="grid three">
        <StatCard label="得分" :value="result.score" />
        <StatCard label="总分" :value="result.totalScore" />
        <StatCard label="正确率" :value="Number(result.accuracyRate || 0) * 100" />
      </div>
      <MarkdownViewer :content="result.analysisMarkdown" />
      <div v-if="result.questionFeedbacks?.length" class="feedback-list">
        <article v-for="item in result.questionFeedbacks" :key="item.questionId" class="feedback-item">
          <div class="toolbar">
            <strong>第 {{ item.questionOrder }} 题</strong>
            <el-tag :type="item.correct ? 'success' : 'warning'">{{ item.score }} / {{ item.fullScore }}</el-tag>
          </div>
          <p>{{ item.feedback }}</p>
          <div class="option-review-list">
            <div
              v-for="(opt, idx) in item.options || []"
              :key="opt"
              class="option-review"
              :class="{ correct: optionValue(opt, idx) === item.correctAnswer, selected: optionValue(opt, idx) === item.answerText }"
            >
              <div class="option-review-head">
                <strong>{{ opt }}</strong>
                <div class="inline-actions">
                  <el-tag v-if="optionValue(opt, idx) === item.correctAnswer" size="small" type="success">正确答案</el-tag>
                  <el-tag v-if="optionValue(opt, idx) === item.answerText" size="small" :type="item.correct ? 'success' : 'warning'">你的选择</el-tag>
                </div>
              </div>
              <p>{{ item.optionExplanations?.[optionValue(opt, idx)] || '请结合题干条件判断该选项。' }}</p>
            </div>
          </div>
          <small>你的答案：{{ item.answerText || '未作答' }}；参考答案：{{ item.correctAnswer }}</small>
          <div class="inline-actions feedback-actions"><el-button size="small" @click="copyQuestion(item)">复制题目</el-button><el-button size="small" type="primary" plain @click="askAi(item, true)">让 AI 讲解</el-button></div>
        </article>
      </div>
      <el-tag v-for="point in result.weakPoints || []" :key="point" type="warning">{{ point }}</el-tag>
    </el-card>
    <el-card class="quiz-history">
      <template #header>
        <div class="toolbar">
          <div>
            <strong>当前空间的历史测验</strong>
            <p class="history-subtitle">{{ currentSpaceName }} · 新生成的测验也会同步保存到“我的资源”。</p>
          </div>
          <el-tag effect="plain">{{ quizzes.length }} 份</el-tag>
        </div>
      </template>
      <el-table :data="quizzes" max-height="420" empty-text="当前学习空间暂无测验" highlight-current-row @row-click="openQuiz">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column label="难度" width="90"><template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template></el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column label="状态" width="100"><template #default="{ row }">{{ quizStatus(row.status) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="openQuiz(row)">查看</el-button>
            <el-button size="small" type="danger" plain @click.stop="removeQuiz(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { deleteQuiz, generateQuiz, getMastery, getQuiz, getQuizResult, listQuizzes, submitQuiz } from '@/api/quiz'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { parseKnowledgePoints } from '@/utils/knowledge'
import { getActiveCompanionRole, listCompanionRoles } from '@/api/companionRole'

const generating = ref(false)
const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const quizzes = ref<any[]>([])
const quiz = ref<any>()
const result = ref<any>()
const mastery = ref<any[]>([])
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const roles = ref<any[]>([])
const answers = reactive<Record<string, string>>({})
const knowledgeText = ref('')
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, subject: '', title: '', knowledgePoints: [], difficulty: 'medium', questionCount: 5, questionType: 'single_choice', rolePlayEnabled: false, companionRoleId: null as number | null })
const currentSpaceName = computed(() => spaces.value.find((space) => space.id === form.spaceId)?.spaceName || '未选择学习空间')

function selectedRole() { return roles.value.find((role) => role.id === form.companionRoleId) }
function roleStyleText() { const role = selectedRole(); return role ? `${role.roleName} · ${role.speakingStyle || '按角色卡风格讲解'}` : '使用当前默认学习陪伴角色' }
function quizStatus(status?: string) { return status === 'submitted' ? '已提交' : '待作答' }
function masteryStatus(status?: string) { return ({ mastered: '已掌握', learning: '学习中', weak: '需复习' } as Record<string, string>)[status || ''] || '待评估' }
function difficultyLabel(value?: string) { return ({ easy: '基础', medium: '中等', hard: '提高' } as Record<string, string>)[value || ''] || value || '-' }

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

function options(q: any) {
  return Array.isArray(q.options) ? q.options : []
}

function optionValue(option: string, index: number) {
  const match = String(option || '').trim().match(/^([A-D])[\.、:：\)）]/i)
  return match?.[1]?.toUpperCase() || ['A', 'B', 'C', 'D'][index] || String(index)
}

async function loadQuizzes() {
  if (!form.spaceId) return (quizzes.value = [])
  const data = await listQuizzes({ spaceId: form.spaceId, pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] }))
  quizzes.value = data.records || []
}

async function loadMastery() {
  mastery.value = form.spaceId ? await getMastery(form.spaceId).catch(() => []) : []
}

async function handleSpaceChange() {
  quiz.value = undefined
  result.value = undefined
  await Promise.all([loadQuizzes(), loadMastery()])
}

function questionText(item: any, includeAnswers = false) {
  const lines = [`题目：${item.stem}`, ...options(item)]
  if (includeAnswers) {
    lines.push(`我的答案：${item.answerText || '未作答'}`, `正确答案：${item.correctAnswer || '请先引导我分析'}`)
  }
  return lines.join('\n')
}

async function copyText(content: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(content)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = content
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  textarea.remove()
}

async function copyQuestion(item: any) {
  await copyText(questionText(item, Boolean(item.correctAnswer)))
  ElMessage.success('题目和选项已复制')
}

async function askAi(item: any, includeAnswers = false) {
  const prompt = `我对下面这道选择题有疑问，请结合当前学习空间的学习画像讲解。\n\n${questionText(item, includeAnswers)}\n\n请逐项说明每个选项为什么对或错，再给我一道同类题检查是否掌握。`
  await copyText(questionText(item, includeAnswers)).catch(() => undefined)
  await router.push({ path: '/chat', query: { spaceId: form.spaceId, prompt, new: '1' } })
}

async function openQuiz(row: any) {
  result.value = undefined
  const data = await getQuiz(row.id)
  quiz.value = data
  Object.keys(answers).forEach((key) => delete answers[key])
  if (data.status === 'submitted') {
    try {
      result.value = await getQuizResult(data.id)
    } catch { /* 获取已保存结果 */ }
  }
}

async function removeQuiz(item: any) {
  await ElMessageBox.confirm(`确认删除测验“${item.title}”及其作答记录吗？`, '删除测验', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteQuiz(item.id)
  if (quiz.value?.id === item.id) {
    quiz.value = undefined
    result.value = undefined
    Object.keys(answers).forEach((key) => delete answers[key])
  }
  await Promise.all([loadQuizzes(), loadMastery()])
  ElMessage.success('测验及其空间资源已删除')
}

async function generate() {
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!form.spaceId) {
    ElMessage.warning('请先选择学习空间')
    return
  }
  if (!form.subject?.trim()) {
    ElMessage.warning('请先填写学科')
    return
  }
  if (!knowledgePoints.length) {
    ElMessage.warning('请至少填写一个知识点')
    return
  }
  if (!form.title?.trim()) {
    ElMessage.warning('请填写测验标题')
    return
  }
  generating.value = true
  quiz.value = undefined
  result.value = undefined
  try {
    quiz.value = await generateQuiz({ ...form, knowledgePoints })
    Object.keys(answers).forEach((key) => delete answers[key])
    ElMessage.success('测验已生成，并保存到当前学习空间资源')
    await loadQuizzes()
  } finally {
    generating.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    const payload = (quiz.value.questions || []).map((q: any) => ({ questionId: q.id, answerText: answers[q.id] || '' }))
    result.value = await submitQuiz(quiz.value.id, payload, { rolePlayEnabled: form.rolePlayEnabled, companionRoleId: form.companionRoleId })
    ElMessage.success('测验已提交')
    loadMastery()
    loadQuizzes()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadRoles(), loadDefaults()])
  const routeSpaceId = Number(route.query.spaceId)
  if (Number.isFinite(routeSpaceId) && spaces.value.some((space) => space.id === routeSpaceId)) {
    form.spaceId = routeSpaceId
  }
  await Promise.all([loadQuizzes(), loadMastery()])
  const routeQuizId = Number(route.query.quizId)
  if (Number.isFinite(routeQuizId) && routeQuizId > 0) {
    await openQuiz({ id: routeQuizId })
    await router.replace({ path: '/quiz' })
  }
})
</script>

<style scoped>
.role-setting { display: grid; gap: 10px; }
.helper-text { margin: 0; color: var(--muted); font-size: 13px; }
.feedback-list {
  display: grid;
  gap: 10px;
  margin: 14px 0;
}

.feedback-item {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.feedback-item p {
  margin: 8px 0;
}

.feedback-item small {
  color: var(--muted);
}
.feedback-actions { margin-top: 10px; }
.option-review-list { display: grid; gap: 8px; margin: 12px 0; }
.option-review { padding: 10px 12px; border-left: 3px solid var(--line); background: var(--surface); }
.option-review.correct { border-left-color: var(--success); background: var(--success-soft); }
.option-review.selected:not(.correct) { border-left-color: var(--warning); background: var(--warning-soft); }
.option-review-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.option-review p { margin: 6px 0 0; color: var(--muted); font-size: 13px; line-height: 1.6; }
.question {
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
  display: grid;
  gap: 10px;
}
.quiz-history { margin-top: 16px; }
.history-subtitle { margin: 4px 0 0; color: var(--muted); font-size: 13px; }
</style>

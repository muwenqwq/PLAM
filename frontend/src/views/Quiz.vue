<template>
  <div class="page">
    <PageHeader title="学习测验" description="生成小测验，检查自己对资料和知识点的掌握情况。" />
    <el-card>
      <template #header>测验列表</template>
      <el-table :data="quizzes" empty-text="暂无测验" highlight-current-row @row-click="openQuiz">
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="subject" label="学科" width="100" />
        <el-table-column prop="difficulty" label="难度" width="80" />
        <el-table-column prop="questionCount" label="题目数" width="80" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click.stop="openQuiz(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <div class="grid two">
      <el-card>
        <template #header>生成测验</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="AI 服务">
            <el-select v-model="form.modelProviderId" placeholder="选择 AI 服务">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" /></el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty">
              <el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="form.questionType">
              <el-option label="仅选择题" value="single_choice" />
              <el-option label="仅判断题" value="judge" />
              <el-option label="混合题型（含简答）" value="mixed" />
            </el-select>
          </el-form-item>
          <el-form-item label="题目数"><el-input-number v-model="form.questionCount" :min="1" :max="30" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成测验</el-button>
      </el-card>
      <el-card>
        <template #header>掌握度</template>
        <el-table :data="mastery" empty-text="暂无掌握度记录">
          <el-table-column prop="knowledgePoint" label="知识点" />
          <el-table-column prop="masteryLevel" label="掌握度" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
        </el-table>
      </el-card>
    </div>
    <el-card v-if="quiz">
      <template #header>
        <div class="toolbar"><span>{{ quiz.title }}</span><el-tag>{{ quiz.status === 'submitted' ? '已提交' : '待作答' }}</el-tag></div>
      </template>
      <div v-for="q in quiz.questions" :key="q.id" class="question">
        <strong>{{ q.questionOrder }}. {{ q.stem }} <el-tag size="small" type="info">{{ typeLabel(q.questionType) }}</el-tag></strong>
        <el-radio-group v-if="q.questionType === 'single_choice'" v-model="answers[q.id]" :disabled="quiz.status === 'submitted'">
          <el-radio v-for="(opt, idx) in options(q)" :key="idx" :value="opt.startsWith('A.') ? 'A' : opt.startsWith('B.') ? 'B' : opt.startsWith('C.') ? 'C' : opt.startsWith('D.') ? 'D' : opt">{{ opt }}</el-radio>
        </el-radio-group>
        <el-radio-group v-else-if="q.questionType === 'judge'" v-model="answers[q.id]" :disabled="quiz.status === 'submitted'">
          <el-radio label="正确">正确</el-radio>
          <el-radio label="错误">错误</el-radio>
        </el-radio-group>
        <el-input v-else v-model="answers[q.id]" type="textarea" :rows="2" placeholder="请输入答案" :disabled="quiz.status === 'submitted'" />
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
      <el-tag v-for="point in result.weakPoints || []" :key="point" type="warning">{{ point }}</el-tag>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { generateQuiz, getMastery, getQuiz, listQuizzes, submitQuiz } from '@/api/quiz'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { parseKnowledgePoints } from '@/utils/knowledge'

const generating = ref(false)
const submitting = ref(false)
const quizzes = ref<any[]>([])
const quiz = ref<any>()
const result = ref<any>()
const mastery = ref<any[]>([])
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const answers = reactive<Record<string, string>>({})
const knowledgeText = ref('索引, 范式, SQL 查询')
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, subject: '数据库', title: '数据库阶段测验', knowledgePoints: [], difficulty: 'medium', questionCount: 5, questionType: 'mixed' })

const questionTypeLabels: Record<string, string> = { single_choice: '选择题', judge: '判断题', short_answer: '简答题' }
function typeLabel(type: string) { return questionTypeLabels[type] || type }

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

async function loadQuizzes() {
  const data = await listQuizzes({ pageNum: 1, pageSize: 50 }).catch(() => ({ records: [] }))
  quizzes.value = data.records || []
}

async function loadMastery() {
  mastery.value = await getMastery().catch(() => [])
}

async function openQuiz(row: any) {
  result.value = undefined
  const data = await getQuiz(row.id)
  quiz.value = data
  Object.keys(answers).forEach((key) => delete answers[key])
  if (data.status === 'submitted') {
    try {
      result.value = await submitQuiz(data.id, [])
    } catch { /* 获取已保存结果 */ }
  }
}

async function generate() {
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!form.subject?.trim()) {
    ElMessage.warning('请先填写学科')
    return
  }
  if (!knowledgePoints.length) {
    ElMessage.warning('请至少填写一个知识点')
    return
  }
  generating.value = true
  quiz.value = undefined
  result.value = undefined
  try {
    quiz.value = await generateQuiz({ ...form, knowledgePoints })
    Object.keys(answers).forEach((key) => delete answers[key])
    ElMessage.success('测验已生成')
    loadQuizzes()
  } finally {
    generating.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    const payload = (quiz.value.questions || []).map((q: any) => ({ questionId: q.id, answerText: answers[q.id] || '' }))
    result.value = await submitQuiz(quiz.value.id, payload)
    ElMessage.success('测验已提交')
    loadMastery()
    loadQuizzes()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults(), loadQuizzes()])
  await loadMastery()
})
</script>

<style scoped>
.question {
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
  display: grid;
  gap: 10px;
}
</style>

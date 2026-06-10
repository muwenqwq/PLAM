<template>
  <div class="page">
    <PageHeader title="测验评估" description="生成测验、答题、自动评分，并同步更新知识点掌握度。" />
    <div class="grid two">
      <el-card>
        <template #header>生成测验</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="空间 ID"><el-input-number v-model="form.spaceId" :min="1" /></el-form-item>
          <el-form-item label="模型 ID"><el-input-number v-model="form.modelProviderId" :min="1" /></el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" /></el-form-item>
          <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" /></el-select></el-form-item>
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
      <template #header>{{ quiz.title }}</template>
      <div v-for="q in quiz.questions" :key="q.id" class="question">
        <strong>{{ q.questionOrder }}. {{ q.stem }}</strong>
        <el-radio-group v-if="q.questionType !== 'short_answer'" v-model="answers[q.id]">
          <el-radio v-for="option in options(q)" :key="option" :label="option.startsWith('A.') ? 'A' : option.startsWith('B.') ? 'B' : option.startsWith('C.') ? 'C' : option.startsWith('D.') ? 'D' : option">{{ option }}</el-radio>
        </el-radio-group>
        <el-input v-else v-model="answers[q.id]" type="textarea" :rows="2" placeholder="请输入简答题答案" />
      </div>
      <el-button type="primary" :loading="submitting" @click="submit">提交测验</el-button>
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
import { generateQuiz, getMastery, submitQuiz } from '@/api/quiz'

const generating = ref(false)
const submitting = ref(false)
const quiz = ref<any>()
const result = ref<any>()
const mastery = ref<any[]>([])
const answers = reactive<Record<string, string>>({})
const knowledgeText = ref('索引, 范式, SQL 查询')
const form = reactive<any>({ spaceId: 1, modelProviderId: 1, subject: '数据库', title: '数据库阶段测验', knowledgePoints: [], difficulty: 'medium', questionCount: 5 })

function options(q: any) {
  return Array.isArray(q.options) ? q.options : []
}

async function loadMastery() {
  mastery.value = await getMastery()
}

async function generate() {
  generating.value = true
  try {
    quiz.value = await generateQuiz({ ...form, knowledgePoints: knowledgeText.value.split(',').map((v) => v.trim()) })
    Object.keys(answers).forEach((key) => delete answers[key])
    ElMessage.success('测验已生成')
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
  } finally {
    submitting.value = false
  }
}

onMounted(loadMastery)
</script>

<style scoped>
.question {
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
  display: grid;
  gap: 10px;
}
</style>

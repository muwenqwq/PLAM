<template>
  <div class="page">
    <PageHeader title="学习画像" description="每个学习空间都有独立画像，AI 会据此调整对话、资源、路径、测验和报告。">
      <el-select v-model="selectedSpaceId" placeholder="选择学习空间" style="width: 220px" @change="loadSpaceProfile">
        <el-option v-for="space in spaces" :key="space.id" :label="space.spaceName" :value="space.id" />
      </el-select>
    </PageHeader>
    <el-alert title="AI 会从学习对话和测验表现中提炼稳定特征；问候、闲聊和原始提问不会直接写入画像。" type="info" show-icon :closable="false" style="margin-bottom: 16px" />
    <el-card class="portrait-card">
      <template #header>
        <div class="toolbar">
          <span>AI 当前理解</span>
          <el-button text type="primary" :loading="analyzingProfile" :disabled="!selectedSpaceId" @click="reanalyzeProfile">重新分析</el-button>
        </div>
      </template>
      <p class="portrait-summary">{{ profile.adaptiveSummary || '画像正在形成中，先补充你的学习情况，后续学习行为会持续更新这里。' }}</p>
      <div class="portrait-meta">
        <el-tag size="small" type="info">{{ sourceLabel(profile.lastActivitySource || profile.profileSource) }}</el-tag>
        <span v-if="profile.lastActivityAt">最近更新：{{ formatTime(profile.lastActivityAt) }}</span>
      </div>
      <el-alert
        v-if="profile.lastActivitySummary"
        :title="profile.lastActivitySummary"
        type="success"
        :closable="false"
        show-icon
        style="margin-top: 12px"
      />
    </el-card>
    <div class="grid two">
      <el-card>
        <template #header>
          <div class="toolbar">
            <span>{{ selectedSpaceName }} · 学习情况</span>
            <el-tag type="success" effect="plain">AI 分析，可手动修正</el-tag>
          </div>
        </template>
        <el-form :model="profile" label-width="110px">
          <el-form-item label="情况描述">
            <el-input
              v-model="profile.profileNarrative"
              type="textarea"
              :rows="5"
              maxlength="4000"
              show-word-limit
              placeholder="可以直接用一段话描述：我现在的基础、学习目标、最困难的地方、可投入时间，以及希望 AI 怎样讲解和督促我。"
            />
          </el-form-item>
          <el-form-item label="学习目标"><el-input v-model="profile.learningGoal" /></el-form-item>
          <el-form-item label="学科方向"><el-input v-model="profile.subjectDirection" /></el-form-item>
          <el-form-item label="基础水平">
            <el-select v-model="profile.foundationLevel"><el-option label="入门" value="beginner" /><el-option label="中等" value="intermediate" /><el-option label="高级" value="advanced" /></el-select>
          </el-form-item>
          <el-form-item label="薄弱点"><el-input v-model="profile.weakPointsText" placeholder="用逗号分隔" /></el-form-item>
          <el-form-item label="兴趣 / 擅长"><el-input v-model="profile.interestTagsText" placeholder="用逗号分隔" /></el-form-item>
          <el-form-item label="每周时间"><el-input-number v-model="profile.weeklyAvailableHours" :min="0" :max="100" /> 小时</el-form-item>
          <el-form-item label="可用时间"><el-input v-model="profile.availableTimeText" placeholder="如 工作日晚 20:00-22:00" /></el-form-item>
          <el-form-item label="输出风格"><el-input v-model="profile.outputStyle" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="savingProfile" :disabled="!selectedSpaceId" @click="saveProfileInfo">保存空间画像</el-button>
      </el-card>
      <el-card>
        <template #header>通用内容偏好</template>
        <el-form :model="preference" label-width="120px">
          <el-form-item label="资源类型"><el-input v-model="preference.resourceTypesText" placeholder="讲义, 知识图谱, 测验" /></el-form-item>
          <el-form-item label="内容长度"><el-select v-model="preference.contentLengthPreference"><el-option label="简短" value="short" /><el-option label="中等" value="medium" /><el-option label="详细" value="long" /></el-select></el-form-item>
          <el-form-item label="难度偏好"><el-select v-model="preference.difficultyPreference"><el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" /></el-select></el-form-item>
          <el-form-item label="知识图谱"><el-switch v-model="preference.knowledgeGraphEnabled" /></el-form-item>
          <el-form-item label="测验"><el-switch v-model="preference.quizEnabled" /></el-form-item>
          <el-form-item label="复习计划"><el-switch v-model="preference.reviewPlanEnabled" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="savingPreference" @click="savePreferenceInfo">保存偏好</el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { analyzeSpaceProfile, getPreference, getSpaceProfile, savePreference, saveSpaceProfile } from '@/api/profile'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'

const savingProfile = ref(false)
const savingPreference = ref(false)
const analyzingProfile = ref(false)
const route = useRoute()
const spaces = ref<any[]>([])
const selectedSpaceId = ref<number | null>(null)
const selectedSpaceName = computed(() => spaces.value.find((space) => space.id === selectedSpaceId.value)?.spaceName || '请选择学习空间')
const profile = reactive<any>({ profileNarrative: '', adaptiveSummary: '', learningGoal: '', subjectDirection: '', foundationLevel: 'intermediate', weakPointsText: '', interestTagsText: '', weeklyAvailableHours: 0, availableTimeText: '', outputStyle: '', lastActivitySource: '', lastActivitySummary: '', lastActivityAt: '' })
const preference = reactive<any>({ resourceTypesText: '', contentLengthPreference: 'medium', difficultyPreference: 'medium', languagePreference: 'zh-CN', knowledgeGraphEnabled: true, quizEnabled: true, reviewPlanEnabled: true })

function resetProfile() {
  Object.assign(profile, { profileNarrative: '', adaptiveSummary: '', learningGoal: '', subjectDirection: '', foundationLevel: 'intermediate', weakPointsText: '', interestTagsText: '', weeklyAvailableHours: 0, availableTimeText: '', outputStyle: '', lastActivitySource: '', lastActivitySummary: '', lastActivityAt: '' })
}

function applyProfileData(data: any) {
  Object.assign(profile, data || {})
  profile.weakPointsText = (profile.weakPoints || []).join(', ')
  profile.interestTagsText = (profile.interestTags || []).join(', ')
  profile.availableTimeText = (profile.availableTimeSlots || []).join(', ')
}

async function loadSpaceProfile() {
  resetProfile()
  if (!selectedSpaceId.value) return
  try {
    applyProfileData(await getSpaceProfile(selectedSpaceId.value))
  } catch {}
}

async function reanalyzeProfile() {
  if (!selectedSpaceId.value) return ElMessage.warning('请先选择学习空间')
  analyzingProfile.value = true
  try {
    applyProfileData(await analyzeSpaceProfile(selectedSpaceId.value))
    ElMessage.success('学习画像已重新分析')
  } finally {
    analyzingProfile.value = false
  }
}

async function loadPreferenceInfo() {
  try {
    Object.assign(preference, await getPreference())
    preference.resourceTypesText = (preference.preferredResourceTypes || []).join(', ')
  } catch {}
}

async function saveProfileInfo() {
  if (!selectedSpaceId.value) return ElMessage.warning('请先选择学习空间')
  savingProfile.value = true
  try {
    const saved = await saveSpaceProfile(selectedSpaceId.value, {
      ...profile,
      profileSource: 'manual',
      weakPoints: profile.weakPointsText.split(',').map((v: string) => v.trim()).filter(Boolean),
      interestTags: profile.interestTagsText.split(',').map((v: string) => v.trim()).filter(Boolean),
      availableTimeSlots: profile.availableTimeText.split(',').map((v: string) => v.trim()).filter(Boolean)
    })
    applyProfileData(saved)
    ElMessage.success('画像已保存')
  } finally {
    savingProfile.value = false
  }
}

function sourceLabel(source?: string) {
  const labels: Record<string, string> = {
    manual: '手动补充',
    chat: 'AI 对话更新',
    assessment: '测验结果更新',
    quiz_generation: '测验生成更新',
    resource_generation: '资源生成更新',
    agent_resource_generation: '多 Agent 更新',
    learning_path: '学习路径更新',
    knowledge_qa: '知识库问答更新',
    profile_refresh: 'AI 重新分析',
    ai_adaptive: 'AI 自适应更新',
    space_creation: '空间创建'
  }
  return labels[source || ''] || '持续学习更新'
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

async function savePreferenceInfo() {
  savingPreference.value = true
  try {
    await savePreference({
      ...preference,
      languagePreference: 'zh-CN',
      preferredResourceTypes: preference.resourceTypesText.split(',').map((v: string) => v.trim()).filter(Boolean)
    })
    ElMessage.success('偏好已保存')
  } finally {
    savingPreference.value = false
  }
}

onMounted(async () => {
  const [spaceData, defaultSpace] = await Promise.all([
    listSpaces({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
    getDefaultSpace().catch(() => null)
  ])
  spaces.value = spaceData.records || []
  const querySpaceId = Number(Array.isArray(route.query.spaceId) ? route.query.spaceId[0] : route.query.spaceId)
  selectedSpaceId.value = Number.isFinite(querySpaceId) && spaces.value.some((space) => space.id === querySpaceId)
    ? querySpaceId
    : defaultSpace?.id || spaces.value[0]?.id || null
  await Promise.all([loadSpaceProfile(), loadPreferenceInfo()])
})
</script>

<style scoped>
.portrait-card { margin-bottom: 16px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.portrait-summary { margin: 0; color: var(--text-strong); font-size: 15px; line-height: 1.8; }
.portrait-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-top: 12px; color: var(--muted); font-size: 13px; }
</style>

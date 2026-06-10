<template>
  <div class="page">
    <PageHeader title="用户画像与学习偏好" description="画像用于路径、资源和测验生成；偏好用于控制输出风格。" />
    <div class="grid two">
      <el-card>
        <template #header>学习画像</template>
        <el-form :model="profile" label-width="110px">
          <el-form-item label="学习目标"><el-input v-model="profile.learningGoal" /></el-form-item>
          <el-form-item label="学科方向"><el-input v-model="profile.subjectDirection" /></el-form-item>
          <el-form-item label="基础水平">
            <el-select v-model="profile.foundationLevel"><el-option label="薄弱" value="weak" /><el-option label="一般" value="normal" /><el-option label="较好" value="good" /></el-select>
          </el-form-item>
          <el-form-item label="薄弱点"><el-input v-model="profile.weakPointsText" placeholder="用逗号分隔" /></el-form-item>
          <el-form-item label="可用时间"><el-input v-model="profile.availableTimeText" placeholder="如 工作日晚 20:00-22:00" /></el-form-item>
          <el-form-item label="输出风格"><el-input v-model="profile.outputStyle" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="savingProfile" @click="saveProfileInfo">保存画像</el-button>
      </el-card>
      <el-card>
        <template #header>学习偏好</template>
        <el-form :model="preference" label-width="120px">
          <el-form-item label="资源类型"><el-input v-model="preference.resourceTypesText" placeholder="讲义, 知识图谱, 测验" /></el-form-item>
          <el-form-item label="内容长度"><el-select v-model="preference.contentLengthPreference"><el-option label="简短" value="short" /><el-option label="中等" value="medium" /><el-option label="详细" value="long" /></el-select></el-form-item>
          <el-form-item label="难度偏好"><el-select v-model="preference.difficultyPreference"><el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" /></el-select></el-form-item>
          <el-form-item label="语言"><el-select v-model="preference.languagePreference"><el-option label="中文" value="zh-CN" /></el-select></el-form-item>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getPreference, getProfile, savePreference, saveProfile } from '@/api/profile'

const savingProfile = ref(false)
const savingPreference = ref(false)
const profile = reactive<any>({ learningGoal: '', subjectDirection: '', foundationLevel: 'normal', weakPointsText: '', availableTimeText: '', outputStyle: '结构化 Markdown' })
const preference = reactive<any>({ resourceTypesText: '讲义, 知识图谱, 测验', contentLengthPreference: 'medium', difficultyPreference: 'medium', languagePreference: 'zh-CN', knowledgeGraphEnabled: true, quizEnabled: true, reviewPlanEnabled: true })

async function load() {
  try {
    Object.assign(profile, await getProfile())
    profile.weakPointsText = (profile.weakPoints || []).join(', ')
    profile.availableTimeText = (profile.availableTimeSlots || []).join(', ')
  } catch {}
  try {
    Object.assign(preference, await getPreference())
    preference.resourceTypesText = (preference.preferredResourceTypes || []).join(', ')
  } catch {}
}

async function saveProfileInfo() {
  savingProfile.value = true
  try {
    await saveProfile({
      ...profile,
      weakPoints: profile.weakPointsText.split(',').map((v: string) => v.trim()).filter(Boolean),
      availableTimeSlots: profile.availableTimeText.split(',').map((v: string) => v.trim()).filter(Boolean)
    })
    ElMessage.success('画像已保存')
  } finally {
    savingProfile.value = false
  }
}

async function savePreferenceInfo() {
  savingPreference.value = true
  try {
    await savePreference({
      ...preference,
      preferredResourceTypes: preference.resourceTypesText.split(',').map((v: string) => v.trim()).filter(Boolean)
    })
    ElMessage.success('偏好已保存')
  } finally {
    savingPreference.value = false
  }
}

onMounted(load)
</script>

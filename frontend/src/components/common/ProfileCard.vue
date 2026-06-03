<template>
  <el-card class="profile-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span>📋 学习画像</span>
        <el-tag size="small" type="info">{{ profile.course || '未设置课程' }}</el-tag>
      </div>
    </template>

    <div class="profile-grid">
      <div class="profile-item" v-for="item in displayItems" :key="item.label">
        <span class="item-icon">{{ item.icon }}</span>
        <div class="item-content">
          <span class="item-label">{{ item.label }}</span>
          <span class="item-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <div v-if="profile.weakness?.length" class="section">
      <h4 class="section-title">⚠️ 薄弱点</h4>
      <div class="tag-list">
        <el-tag v-for="w in profile.weakness" :key="w" type="warning" size="small">{{ w }}</el-tag>
      </div>
    </div>

    <div v-if="profile.preference?.length" class="section">
      <h4 class="section-title">💡 学习偏好</h4>
      <div class="tag-list">
        <el-tag v-for="p in profile.preference" :key="p" type="success" size="small">{{ p }}</el-tag>
      </div>
    </div>

    <div v-if="masteryEntries.length" class="section">
      <h4 class="section-title">📊 掌握度</h4>
      <div class="mastery-list">
        <div v-for="[key, val] in masteryEntries" :key="key" class="mastery-item">
          <span class="mastery-label">{{ key }}</span>
          <el-progress :percentage="Math.round(val * 100)" :color="getProgressColor(val)" :stroke-width="8" />
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  profile: { type: Object, required: true }
})

const displayItems = computed(() => {
  const p = props.profile
  return [
    { icon: '🎓', label: '专业', value: p.major },
    { icon: '📚', label: '年级', value: p.grade },
    { icon: '🎯', label: '目标', value: p.goal },
    { icon: '🏗️', label: '基础', value: p.foundation },
    { icon: '⏰', label: '时间', value: p.timeBudget }
  ].filter(item => item.value)
})

const masteryEntries = computed(() => {
  const map = props.profile.masteryMap
  if (!map || typeof map !== 'object') return []
  return Object.entries(map)
})

function getProgressColor(val) {
  if (val >= 0.7) return '#67c23a'
  if (val >= 0.4) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.profile-card {
  max-width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.profile-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.item-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}
.item-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.item-label {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-bottom: 2px;
}
.item-value {
  font-size: 14px;
  color: var(--text-body);
  font-weight: 500;
  word-break: break-all;
}
.section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-divider);
}
.section-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted);
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.mastery-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mastery-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mastery-label {
  width: 120px;
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-muted);
}
.mastery-item :deep(.el-progress) {
  flex: 1;
}
</style>

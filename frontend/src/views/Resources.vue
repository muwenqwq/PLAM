<template>
  <div class="resources-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>📚 学习资源</h2>
      <p>根据学习画像和课程知识点，生成 5 类个性化资源</p>
    </div>

    <!-- 生成控制栏 -->
    <el-card class="generate-card" shadow="never">
      <div class="generate-form">
        <el-select v-model="selectedTypes" multiple collapse-tags placeholder="选择资源类型" style="flex:1">
          <el-option
            v-for="t in resourceTypes"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
        <el-button
          type="primary"
          :loading="resourceStore.generating"
          :disabled="!selectedTypes.length"
          @click="handleGenerate"
        >
          {{ resourceStore.generating ? '生成中...' : '✨ 生成资源' }}
        </el-button>
      </div>
    </el-card>

    <!-- 资源类型筛选标签 -->
    <div class="filter-tags">
      <el-check-tag
        :checked="activeFilter === ''"
        @change="activeFilter = ''"
      >
        全部
      </el-check-tag>
      <el-check-tag
        v-for="t in resourceTypes"
        :key="t.value"
        :checked="activeFilter === t.value"
        @change="activeFilter = t.value"
      >
        {{ t.label }}
      </el-check-tag>
    </div>

    <!-- 资源列表 -->
    <LoadingState v-if="resourceStore.loading" text="加载资源中..." />
    <EmptyState
      v-else-if="!filteredResources.length"
      icon="📚"
      title="暂无资源"
      description="选择资源类型并点击生成，资源将在此显示"
    />
    <div v-else class="resource-grid">
      <ResourceCard
        v-for="r in filteredResources"
        :key="r.id"
        :resource="r"
        @click="openDetail(r)"
      />
    </div>

    <!-- 资源详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentResource?.title || '资源详情'"
      direction="rtl"
      size="50%"
    >
      <template v-if="currentResource">
        <div class="drawer-meta">
          <el-tag :type="getTypeTag(currentResource.resourceType)" size="small">
            {{ getTypeLabel(currentResource.resourceType) }}
          </el-tag>
          <span class="meta-score">⭐ {{ currentResource.qualityScore }}</span>
          <span class="meta-time">{{ formatTime(currentResource.createdAt) }}</span>
        </div>

        <!-- Mermaid 图表单独渲染 -->
        <MermaidDiagram
          v-if="currentResource.format === 'mermaid'"
          :code="currentResource.content"
        />
        <!-- 其他 Markdown 内容 -->
        <MarkdownRenderer
          v-else
          :content="currentResource.content"
        />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useResourceStore } from '@/stores/resource'
import { ResourceCard, MarkdownRenderer, MermaidDiagram, LoadingState, EmptyState } from '@/components/common'

const userStore = useUserStore()
const resourceStore = useResourceStore()

const selectedTypes = ref(['explanation_doc', 'mindmap', 'quiz', 'reading_material', 'code_lab'])
const activeFilter = ref('')
const drawerVisible = ref(false)
const currentResource = ref(null)

// SRS §4.3 定义的 5 类资源
const resourceTypes = [
  { value: 'explanation_doc', label: '📄 讲义' },
  { value: 'mindmap', label: '🧠 导图' },
  { value: 'quiz', label: '📝 练习' },
  { value: 'reading_material', label: '📖 阅读' },
  { value: 'code_lab', label: '💻 实验' }
]

// 按类型筛选
const filteredResources = computed(() => {
  const list = resourceStore.resources || []
  if (!activeFilter.value) return list
  return list.filter(r => r.resourceType === activeFilter.value)
})

// 生成资源
async function handleGenerate() {
  try {
    await resourceStore.generate({
      studentId: userStore.studentId || 1,
      courseId: userStore.courseId || 1,
      knowledgePointId: 12, // TODO: 让用户选择知识点
      resourceTypes: selectedTypes.value
    })
    ElMessage.success('资源生成成功')
    // 生成后刷新列表
    resourceStore.fetchList(userStore.studentId || 1)
  } catch {
    ElMessage.error('资源生成失败')
  }
}

// 打开资源详情
function openDetail(resource) {
  currentResource.value = resource
  drawerVisible.value = true
}

function getTypeLabel(type) {
  return resourceTypes.find(t => t.value === type)?.label || type
}
function getTypeTag(type) {
  const map = { explanation_doc: '', mindmap: 'success', quiz: 'warning', reading_material: 'info', code_lab: 'danger' }
  return map[type] || ''
}
function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  if (userStore.studentId) {
    resourceStore.fetchList(userStore.studentId)
  }
})
</script>

<style scoped>
.resources-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header h2 { margin: 0 0 4px; font-size: 20px; color: #1a202c; }
.page-header p { margin: 0; font-size: 14px; color: #718096; }
.generate-card { border-radius: 12px; }
.generate-form {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.drawer-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.meta-score { font-size: 13px; color: #e6a23c; }
.meta-time { font-size: 12px; color: #a0aec0; }
</style>

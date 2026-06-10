<template>
  <div class="page">
    <PageHeader title="资源生成中心" description="生成讲义、提纲、习题集、知识图谱和 PPT 大纲，并保存到 MySQL。" />
    <div class="grid two">
      <el-card>
        <template #header>生成参数</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="空间 ID"><el-input-number v-model="form.spaceId" :min="1" /></el-form-item>
          <el-form-item label="模型 ID"><el-input-number v-model="form.modelProviderId" :min="1" /></el-form-item>
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="资源类型">
            <el-select v-model="form.resourceType">
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" /></el-form-item>
          <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="基础" value="easy" /><el-option label="中等" value="medium" /><el-option label="提高" value="hard" /></el-select></el-form-item>
          <el-form-item label="输出长度"><el-select v-model="form.outputLength"><el-option label="简短" value="short" /><el-option label="中等" value="medium" /><el-option label="详细" value="long" /></el-select></el-form-item>
          <el-form-item label="使用知识库"><el-switch v-model="form.useKnowledgeBase" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成资源</el-button>
      </el-card>
      <el-card>
        <template #header>生成结果</template>
        <LoadingState v-if="generating" />
        <template v-else-if="current">
          <MermaidViewer v-if="current.resourceType === 'knowledge_graph'" :code="extractMermaid(current)" />
          <MarkdownViewer :content="current.contentMarkdown" />
        </template>
        <EmptyState v-else />
      </el-card>
    </div>
    <el-card>
      <template #header>资源列表</template>
      <div class="grid three" v-if="resources.length">
        <ResourceCard v-for="item in resources" :key="item.id" :resource="item">
          <el-button size="small" @click="exportMd(item.id)">导出 Markdown</el-button>
        </ResourceCard>
      </div>
      <EmptyState v-else />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingState from '@/components/LoadingState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import MermaidViewer from '@/components/MermaidViewer.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import { exportMarkdown, generateResource, listResources } from '@/api/resource'

const generating = ref(false)
const resources = ref<any[]>([])
const current = ref<any>()
const knowledgeText = ref('范式, SQL 查询, 索引')
const form = reactive<any>({ spaceId: 1, modelProviderId: 1, title: '数据库索引知识图谱', subject: '数据库', resourceType: 'knowledge_graph', difficulty: 'medium', outputLength: 'medium', useKnowledgeBase: true })
const resourceTypes = [
  { label: '学习计划', value: 'plan' },
  { label: '讲义', value: 'lecture_note' },
  { label: '复习提纲', value: 'review_outline' },
  { label: '习题集', value: 'quiz_set' },
  { label: '案例任务', value: 'case_task' },
  { label: '知识图谱', value: 'knowledge_graph' },
  { label: 'PPT 大纲', value: 'ppt_outline' }
]

async function load() {
  const data = await listResources({ pageNum: 1, pageSize: 30 })
  resources.value = data.records || []
}

async function generate() {
  generating.value = true
  try {
    const data = await generateResource({ ...form, knowledgePoints: knowledgeText.value.split(',').map((v) => v.trim()) })
    current.value = data.resource
    ElMessage.success('资源已生成')
    load()
  } finally {
    generating.value = false
  }
}

function extractMermaid(item: any) {
  return item?.contentJson?.mermaid || (item?.contentMarkdown || '').match(/```mermaid([\s\S]*?)```/)?.[1]?.trim() || 'graph TD\nA[暂无图谱] --> B[请重新生成]'
}

async function exportMd(id: number) {
  const content = await exportMarkdown(id)
  await navigator.clipboard?.writeText(content)
  ElMessage.success('Markdown 已复制到剪贴板')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="知识图谱" description="读取 knowledge_graph 类型资源并渲染 Mermaid，可复制源码用于答辩展示。">
      <RouterLink to="/resources"><el-button type="primary">重新生成</el-button></RouterLink>
    </PageHeader>
    <div class="grid two">
      <el-card>
        <template #header>图谱资源</template>
        <el-table :data="graphs" empty-text="暂无知识图谱资源" @row-click="current = $event">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="subject" label="学科" width="120" />
        </el-table>
      </el-card>
      <el-card>
        <template #header>
          <div class="toolbar"><span>{{ current?.title || '图谱预览' }}</span><el-button size="small" @click="copy">复制源码</el-button></div>
        </template>
        <MermaidViewer v-if="mermaidCode" :code="mermaidCode" />
        <EmptyState v-else title="请选择图谱资源" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MermaidViewer from '@/components/MermaidViewer.vue'
import { listResources } from '@/api/resource'

const graphs = ref<any[]>([])
const current = ref<any>()
const mermaidCode = computed(() => current.value?.contentJson?.mermaid || current.value?.contentMarkdown?.match(/```mermaid([\s\S]*?)```/)?.[1]?.trim() || '')

async function load() {
  const data = await listResources({ resourceType: 'knowledge_graph', pageNum: 1, pageSize: 50 })
  graphs.value = data.records || []
  current.value = graphs.value[0]
}

async function copy() {
  if (!mermaidCode.value) return
  await navigator.clipboard?.writeText(mermaidCode.value)
  ElMessage.success('Mermaid 源码已复制')
}

onMounted(load)
</script>

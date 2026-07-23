<template>
  <div class="page">
    <PageHeader title="知识图谱" description="选择当前学习空间的图谱，缩放查看并直接下载为图片。">
      <RouterLink to="/resources"><el-button type="primary">从资源生成图谱</el-button></RouterLink>
    </PageHeader>
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-select v-model="spaceId" placeholder="选择学习空间" style="width: 240px" @change="load">
        <el-option v-for="space in spaces" :key="space.id" :label="space.spaceName" :value="space.id" />
      </el-select>
    </el-card>
    <div class="grid two">
      <el-card>
        <template #header>图谱资源</template>
        <el-table :data="graphs" empty-text="暂无知识图谱资源" highlight-current-row @row-click="current = $event">
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="subject" label="学科" width="120" />
        </el-table>
      </el-card>
      <el-card>
        <template #header>
          <div class="toolbar">
            <span>{{ current?.title || '图谱预览' }}</span>
          </div>
        </template>
        <MermaidViewer v-if="mermaidCode" :code="mermaidCode" :filename="current?.title" />
        <EmptyState v-else title="请选择图谱资源" description="可以在资源详情里点击生成知识图谱。" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MermaidViewer from '@/components/MermaidViewer.vue'
import { listResources } from '@/api/resource'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'

const graphs = ref<any[]>([])
const current = ref<any>()
const spaces = ref<any[]>([])
const spaceId = ref<number | null>(null)
const mermaidCode = computed(() => current.value?.contentJson?.mermaid || current.value?.contentMarkdown?.match(/```mermaid([\s\S]*?)```/)?.[1]?.trim() || '')

async function load() {
  if (!spaceId.value) {
    graphs.value = []
    current.value = undefined
    return
  }
  const data = await listResources({ spaceId: spaceId.value, resourceType: 'knowledge_graph', pageNum: 1, pageSize: 50 })
  graphs.value = data.records || []
  current.value = graphs.value[0]
}

onMounted(async () => {
  const [spaceData, defaultSpace] = await Promise.all([
    listSpaces({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
    getDefaultSpace().catch(() => null)
  ])
  spaces.value = spaceData.records || []
  spaceId.value = defaultSpace?.id || spaces.value[0]?.id || null
  await load()
})
</script>

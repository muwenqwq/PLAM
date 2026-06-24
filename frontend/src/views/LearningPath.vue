<template>
  <div class="page">
    <PageHeader title="学习路径" description="根据目标生成个性化路径，支持今日任务和节点完成状态更新。" />
    <div class="grid two">
      <el-card>
        <template #header>生成路径</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="学习空间">
            <el-select v-model="form.spaceId" placeholder="选择学习空间">
              <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型配置">
            <el-select v-model="form.modelProviderId" placeholder="选择模型配置">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="目标"><el-input v-model="form.goal" /></el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" /></el-form-item>
          <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" :max="90" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="generating" @click="generate">生成路径</el-button>
      </el-card>
      <el-card>
        <template #header>今日任务</template>
        <el-timeline v-if="today.length">
          <el-timeline-item v-for="item in today" :key="item.id" :timestamp="item.dueDate">
            <strong>{{ item.title }}</strong>
            <p class="muted">{{ item.description }}</p>
          </el-timeline-item>
        </el-timeline>
        <EmptyState v-else />
      </el-card>
    </div>
    <el-card>
      <template #header>路径列表</template>
      <el-table :data="paths" empty-text="暂无学习路径" @row-click="select">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column prop="progressRate" label="进度" width="140" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button size="small" @click.stop="adjust(row.id)">调整</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-card v-if="current">
      <template #header>{{ current.title }}</template>
      <el-timeline>
        <el-timeline-item v-for="item in current.items" :key="item.id" :timestamp="item.dueDate" placement="top">
          <el-card shadow="never">
            <div class="toolbar">
              <strong>{{ item.itemOrder }}. {{ item.title }}</strong>
              <el-button size="small" type="success" @click="done(item)">标记完成</el-button>
            </div>
            <p class="muted">{{ item.description }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { adjustLearningPath, generateLearningPath, getLearningPath, listLearningPaths, todayTasks, updatePathItemStatus } from '@/api/learningPath'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { parseKnowledgePoints } from '@/utils/knowledge'

const paths = ref<any[]>([])
const today = ref<any[]>([])
const current = ref<any>()
const generating = ref(false)
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const knowledgeText = ref('索引, 范式, SQL 查询')
const form = reactive<any>({ spaceId: null as number | null, modelProviderId: null as number | null, subject: '数据库', goal: '一周内完成期末复习', days: 7 })

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
    listSpaces({ pageNum: 1, pageSize: 50 }),
    listProviders({ pageNum: 1, pageSize: 50 })
  ])
  spaces.value = spaceData.records || []
  providers.value = providerData.records || []
}

async function load() {
  const data = await listLearningPaths({ pageNum: 1, pageSize: 30 })
  paths.value = data.records || []
  today.value = await todayTasks()
}

async function generate() {
  const knowledgePoints = parseKnowledgePoints(knowledgeText.value)
  if (!form.subject?.trim() || !form.goal?.trim()) {
    ElMessage.warning('请填写学科和学习目标')
    return
  }
  if (!knowledgePoints.length) {
    ElMessage.warning('请至少填写一个知识点')
    return
  }
  generating.value = true
  current.value = undefined
  try {
    current.value = await generateLearningPath({ ...form, knowledgePoints })
    ElMessage.success('学习路径已生成')
    load()
  } finally {
    generating.value = false
  }
}

async function select(row: any) {
  current.value = await getLearningPath(row.id)
}

async function done(item: any) {
  await updatePathItemStatus(item.id, 'done')
  ElMessage.success('已标记完成')
  if (current.value) select(current.value)
  load()
}

async function adjust(id: number) {
  current.value = await adjustLearningPath(id)
  ElMessage.success('路径已调整')
  load()
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults()])
  await load()
})
</script>

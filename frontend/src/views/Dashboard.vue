<template>
  <div class="page dashboard-page">
    <PageHeader
      :title="`你好，${displayName}`"
      description="今天想先整理资料、生成复习内容，还是开始一次测验？"
    >
      <el-button type="primary" @click="router.push('/knowledge')">上传资料</el-button>
    </PageHeader>

    <section class="today-grid">
      <RouterLink v-for="item in todayActions" :key="item.path" :to="item.path" class="study-action">
        <el-icon size="24"><component :is="item.icon" /></el-icon>
        <strong>{{ item.title }}</strong>
        <span>{{ item.text }}</span>
      </RouterLink>
    </section>

    <div class="grid three">
      <StatCard label="学习空间" :value="overview.spaceCount" icon="FolderOpened" hint="按课程整理" />
      <StatCard label="学习资料" :value="knowledgeFiles.length" icon="Collection" hint="可用于问答和生成" />
      <StatCard label="生成资源" :value="overview.resourceCount || resources.length" icon="Files" hint="笔记、题目、提纲" />
      <StatCard label="测验数量" :value="overview.quizCount" icon="EditPen" hint="阶段练习" />
      <StatCard label="平均得分" :value="overview.averageScore || 0" icon="TrendCharts" hint="最近测验" />
      <StatCard label="平均掌握度" :value="overview.averageMastery || 0" icon="PieChart" hint="知识点掌握" />
    </div>

    <div class="grid two">
      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">最近学习资料</h2>
            <p class="section-subtitle">上传后的资料可以用来提问、总结和生成复习内容。</p>
          </div>
        </template>
        <div v-if="knowledgeFiles.length" class="card-list">
          <div v-for="file in knowledgeFiles.slice(0, 5)" :key="file.id" class="list-item">
            <div>
              <strong>{{ file.originalName }}</strong>
              <p>{{ statusText(file.parserStatus) }} · {{ file.chunkCount || 0 }} 个学习片段</p>
            </div>
            <el-button size="small" @click="router.push('/knowledge')">查看</el-button>
          </div>
        </div>
        <EmptyState
          v-else
          title="你还没有上传学习资料"
          description="上传后可以进行问答、总结和资源生成。"
        />
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div>
            <h2 class="section-title">最近生成资源</h2>
            <p class="section-subtitle">系统会把生成结果自动保存到“我的资源”。</p>
          </div>
        </template>
        <div v-if="resources.length" class="card-list">
          <div v-for="item in resources.slice(0, 5)" :key="item.id" class="list-item">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ resourceTypeName(item.resourceType) }} · {{ item.outputSummary || '暂无描述' }}</p>
            </div>
            <el-button size="small" @click="router.push('/resources')">管理</el-button>
          </div>
        </div>
        <EmptyState
          v-else
          title="还没有生成学习资源"
          description="选择资料后，可以生成课程笔记、练习题、复习提纲或学习计划。"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getOverview } from '@/api/report'
import { listKnowledgeFiles } from '@/api/knowledge'
import { listResources } from '@/api/resource'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.user?.nickname || auth.user?.username || 'Demo Student')
const overview = reactive<any>({ spaceCount: 0, resourceCount: 0, quizCount: 0, pathCount: 0, averageScore: 0, averageMastery: 0 })
const knowledgeFiles = ref<any[]>([])
const resources = ref<any[]>([])

const todayActions = [
  { path: '/knowledge', title: '上传资料', text: '把课件、笔记或文档放进知识库', icon: 'UploadFilled' },
  { path: '/knowledge', title: '基于资料提问', text: '围绕已上传资料获得解释和答案', icon: 'ChatDotRound' },
  { path: '/resource-generation', title: '生成学习资源', text: '生成笔记、练习题、提纲或计划', icon: 'DocumentAdd' },
  { path: '/quiz', title: '开始测验', text: '用小测验检查今天的掌握情况', icon: 'EditPen' }
]

const resourceTypeMap: Record<string, string> = {
  plan: '学习计划',
  lecture_note: '课程笔记',
  summary: '知识点总结',
  mind_map: '思维导图',
  review_outline: '复习提纲',
  quiz_set: '练习题',
  mistake_review: '错题整理',
  case_task: '案例任务',
  knowledge_graph: '思维导图',
  ppt_outline: '演示提纲'
}

function resourceTypeName(type: string) {
  return resourceTypeMap[type] || '学习资源'
}

function statusText(status?: string) {
  if (!status || status === 'pending' || status === 'processing') return '处理中'
  if (status === 'failed') return '处理失败'
  return '已完成'
}

async function load() {
  try {
    Object.assign(overview, await getOverview())
  } catch {
    Object.assign(overview, { spaceCount: 0, resourceCount: 0, quizCount: 0, averageScore: 0, averageMastery: 0 })
  }
  try {
    const data = await listKnowledgeFiles({ pageNum: 1, pageSize: 5 })
    knowledgeFiles.value = data.records || []
  } catch {
    knowledgeFiles.value = []
  }
  try {
    const data = await listResources({ pageNum: 1, pageSize: 5 })
    resources.value = data.records || []
  } catch {
    resources.value = []
  }
}

onMounted(load)
</script>

<style scoped>
.today-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.study-action {
  min-height: 148px;
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  color: var(--text);
  box-shadow: var(--shadow-sm);
  transition: transform .2s, border-color .2s;
}

.study-action:hover {
  transform: translateY(-2px);
  border-color: var(--primary-border);
}

.study-action .el-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: var(--primary);
  background: var(--primary-soft);
}

.study-action strong {
  font-size: 17px;
}

.study-action span {
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .today-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .today-grid {
    grid-template-columns: 1fr;
  }
}
</style>

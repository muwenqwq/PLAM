<template>
  <div class="page">
    <PageHeader title="知识库与 Mock RAG" description="创建资料元数据、调用索引接口，并进行知识库检索和问答。" />
    <div class="grid two">
      <el-card>
        <template #header>创建资料</template>
        <el-form :model="form" label-width="90px">
          <el-form-item label="空间 ID"><el-input-number v-model="form.spaceId" :min="1" /></el-form-item>
          <el-form-item label="文件名"><el-input v-model="form.originalName" /></el-form-item>
          <el-form-item label="类型"><el-input v-model="form.fileType" /></el-form-item>
          <el-form-item label="来源说明"><el-input v-model="sourceText" type="textarea" :rows="4" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="creating" @click="createFile">创建元数据</el-button>
      </el-card>
      <el-card>
        <template #header>检索 / 问答</template>
        <el-input v-model="query" placeholder="输入检索问题" />
        <div class="actions">
          <el-button :loading="searching" @click="search">检索</el-button>
          <el-button type="primary" :loading="searching" @click="qa">知识库问答</el-button>
        </div>
        <div class="result-box">
          <MarkdownViewer v-if="answer" :content="answer" />
          <div v-for="item in results" :key="item.chunkIndex" class="hit">
            <strong>{{ item.source }}</strong>
            <p>{{ item.chunkText }}</p>
            <el-tag size="small">score {{ item.score }}</el-tag>
          </div>
          <EmptyState v-if="!answer && !results.length" />
        </div>
      </el-card>
    </div>
    <el-card>
      <template #header>资料列表</template>
      <el-table :data="files" empty-text="暂无知识库资料">
        <el-table-column prop="originalName" label="文件名" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column prop="parserStatus" label="索引状态" width="120" />
        <el-table-column prop="chunkCount" label="切片数" width="100" />
        <el-table-column label="操作" width="140"><template #default="{ row }"><el-button size="small" type="primary" @click="index(row)">索引</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { createKnowledgeFile, indexKnowledgeFile, listKnowledgeFiles, qaKnowledge, searchKnowledge } from '@/api/knowledge'

const files = ref<any[]>([])
const results = ref<any[]>([])
const answer = ref('')
const query = ref('数据库索引如何复习')
const sourceText = ref('数据库索引可以提升查询效率，但需要结合选择性、覆盖索引和执行计划理解。')
const creating = ref(false)
const searching = ref(false)
const form = reactive<any>({ spaceId: 1, originalName: '数据库复习资料.md', fileType: 'md', fileSize: 0 })

async function load() {
  const data = await listKnowledgeFiles({ pageNum: 1, pageSize: 50 })
  files.value = data.records || []
}

async function createFile() {
  creating.value = true
  try {
    const file = await createKnowledgeFile(form)
    ElMessage.success('元数据已创建')
    await indexKnowledgeFile(file.id, { sourceText: sourceText.value })
    ElMessage.success('Mock 索引完成')
    load()
  } finally {
    creating.value = false
  }
}

async function index(row: any) {
  await indexKnowledgeFile(row.id, { sourceText: sourceText.value })
  ElMessage.success('索引完成')
  load()
}

async function search() {
  searching.value = true
  answer.value = ''
  try {
    const data = await searchKnowledge({ query: query.value, fileIds: files.value.map((f) => f.id), topK: 5 })
    results.value = data.results || []
  } finally {
    searching.value = false
  }
}

async function qa() {
  searching.value = true
  try {
    const data = await qaKnowledge({ query: query.value, fileIds: files.value.map((f) => f.id), topK: 5 })
    answer.value = data.answerMarkdown
    results.value = data.results || []
  } finally {
    searching.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.actions {
  margin: 12px 0;
  display: flex;
  gap: 10px;
}

.hit {
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.hit p {
  margin: 6px 0;
  color: var(--muted);
}
</style>

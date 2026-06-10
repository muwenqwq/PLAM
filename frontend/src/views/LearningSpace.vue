<template>
  <div class="page">
    <PageHeader title="学习空间" description="按学科或课程组织知识库、资源、路径、测验和报告。">
      <el-button type="primary" @click="openCreate">新建空间</el-button>
    </PageHeader>
    <el-card>
      <el-table :data="spaces" v-loading="loading" empty-text="暂无学习空间">
        <el-table-column prop="spaceName" label="空间名称" min-width="160" />
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column label="默认" width="90">
          <template #default="{ row }"><el-tag v-if="row.defaultSpace || row.isDefault" type="success">默认</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showSummary(row)">Summary</el-button>
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="setDefault(row.id)">设默认</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" :title="form.id ? '编辑学习空间' : '新建学习空间'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="空间名称" required><el-input v-model="form.spaceName" /></el-form-item>
        <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="summaryVisible" title="空间摘要" width="560px">
      <pre class="summary">{{ summary }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createSpace, deleteSpace, getSpaceSummary, listSpaces, setDefaultSpace, updateSpace } from '@/api/learningSpace'

const spaces = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const summaryVisible = ref(false)
const summary = ref('')
const form = reactive<any>({ id: null, spaceName: '', subject: '', description: '' })

async function load() {
  loading.value = true
  try {
    const data = await listSpaces({ pageNum: 1, pageSize: 50 })
    spaces.value = data.records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, spaceName: '', subject: '', description: '' })
  dialog.value = true
}

function edit(row: any) {
  Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.spaceName) return ElMessage.warning('请输入空间名称')
  saving.value = true
  try {
    if (form.id) await updateSpace(form.id, form)
    else await createSpace(form)
    ElMessage.success('保存成功')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该学习空间？', '提示')
  await deleteSpace(id)
  ElMessage.success('已删除')
  load()
}

async function setDefault(id: number) {
  await setDefaultSpace(id)
  ElMessage.success('已设置默认空间')
  load()
}

async function showSummary(row: any) {
  try {
    summary.value = JSON.stringify(await getSpaceSummary(row.id), null, 2)
  } catch {
    summary.value = '当前后端不可用，启动 Java 后端后可查看真实 summary。'
  }
  summaryVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.summary {
  white-space: pre-wrap;
  background: var(--surface-soft);
  padding: 12px;
  border-radius: 8px;
}
</style>

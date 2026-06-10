<template>
  <div class="page">
    <PageHeader title="AI 模型配置" description="答辩重点：API Key 只脱敏展示，Mock 模型可快速创建并测试。">
      <el-button @click="quickMock">快速创建 Mock</el-button>
      <el-button type="primary" @click="openCreate">新增配置</el-button>
    </PageHeader>
    <el-card>
      <el-table :data="providers" v-loading="loading" empty-text="暂无模型配置">
        <el-table-column prop="providerName" label="名称" min-width="150" />
        <el-table-column prop="providerType" label="类型" width="150" />
        <el-table-column prop="modelName" label="模型" width="160" />
        <el-table-column prop="apiKeyMasked" label="API Key 脱敏" width="160" />
        <el-table-column label="默认" width="80"><template #default="{ row }"><el-tag v-if="row.defaultProvider || row.isDefault" type="success">默认</el-tag></template></el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="test(row.id)">测试</el-button>
            <el-button size="small" type="primary" @click="setDefault(row.id)">默认</el-button>
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="模型配置" width="620px">
      <ModelProviderForm v-model="form" />
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ModelProviderForm from '@/components/ModelProviderForm.vue'
import { createProvider, deleteProvider, listProviders, setDefaultProvider, testProvider, updateProvider } from '@/api/modelProvider'

const providers = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const form = reactive<any>({})

function defaults() {
  return { id: null, providerName: 'Mock 学习模型', providerType: 'mock', baseUrl: 'http://localhost:8000', apiKey: 'MOCK-ONLY', modelName: 'mock-chat-v1', embeddingModel: 'mock-embedding-v1', temperature: 0.7, maxTokens: 2048, streamEnabled: false }
}

async function load() {
  loading.value = true
  try {
    const data = await listProviders({ pageNum: 1, pageSize: 50 })
    providers.value = data.records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, defaults())
  dialog.value = true
}

function edit(row: any) {
  Object.assign(form, defaults(), row, { apiKey: '' })
  dialog.value = true
}

async function quickMock() {
  await createProvider(defaults())
  ElMessage.success('Mock 模型已创建')
  load()
}

async function save() {
  saving.value = true
  try {
    if (form.id) await updateProvider(form.id, form)
    else await createProvider(form)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function test(id: number) {
  const data = await testProvider(id)
  ElMessage.success(data.message || '连接测试成功')
}

async function setDefault(id: number) {
  await setDefaultProvider(id)
  ElMessage.success('已设置默认模型')
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该模型配置？', '提示')
  await deleteProvider(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page model-page">
    <PageHeader title="AI 模型" description="接入你自己的 AI 服务，资料分片、问答和资源生成会优先使用默认模型。">
      <el-button @click="quickMock">添加演示模型</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">接入 AI 模型</el-button>
    </PageHeader>

    <el-alert
      title="API Key 由后端保存，页面只显示脱敏结果。建议先测试连接，再设为默认模型。"
      type="info"
      show-icon
      :closable="false"
    />

    <div v-if="providers.length" v-loading="loading" class="provider-grid">
      <article v-for="item in providers" :key="item.id" class="provider-card">
        <div class="provider-head">
          <div class="provider-icon"><el-icon><Connection /></el-icon></div>
          <div class="provider-title">
            <div class="title-line">
              <h2>{{ item.providerName }}</h2>
              <el-tag v-if="item.defaultProvider || item.isDefault" type="success" effect="plain">默认使用</el-tag>
            </div>
            <p>{{ providerTypeName(item.providerType) }}</p>
          </div>
        </div>
        <dl class="provider-info">
          <div><dt>模型</dt><dd>{{ item.modelName || '未填写' }}</dd></div>
          <div><dt>API 地址</dt><dd>{{ item.baseUrl || '本地服务' }}</dd></div>
          <div><dt>API Key</dt><dd>{{ item.apiKeyMasked || '未配置 / 本地调用' }}</dd></div>
        </dl>
        <div class="provider-actions">
          <el-button :loading="testingId === item.id" @click="test(item.id)">测试连接</el-button>
          <el-button v-if="!(item.defaultProvider || item.isDefault)" type="primary" plain @click="setDefault(item.id)">设为默认</el-button>
          <el-button @click="edit(item)">编辑</el-button>
          <el-button type="danger" plain @click="remove(item)">删除</el-button>
        </div>
      </article>
    </div>
    <EmptyState v-else-if="!loading" title="还没有接入 AI 模型" description="点击“接入 AI 模型”，填写 API 地址、密钥和模型名称后即可使用。">
      <el-button type="primary" @click="openCreate">接入第一个模型</el-button>
    </EmptyState>

    <el-dialog v-model="dialog" :title="form.id ? '编辑 AI 模型' : '接入 AI 模型'" width="720px" destroy-on-close>
      <ModelProviderForm v-model="form" />
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Connection, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import ModelProviderForm from '@/components/ModelProviderForm.vue'
import { createProvider, deleteProvider, listProviders, setDefaultProvider, testProvider, updateProvider } from '@/api/modelProvider'

const providers = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const dialog = ref(false)
const form = reactive<any>({})

function defaults() {
  return { id: null, providerName: '我的 DeepSeek', providerType: 'deepseek', baseUrl: 'https://api.deepseek.com/v1', apiKey: '', modelName: 'deepseek-chat', embeddingModel: '', temperature: 0.7, maxTokens: 4096, streamEnabled: false, defaultProvider: false, remark: '' }
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
  await createProvider({ ...defaults(), providerName: '本地演示模型', providerType: 'mock', baseUrl: 'mock://local/llm', apiKey: 'MOCK-ONLY', modelName: 'mock-chat-v1', embeddingModel: 'mock-embedding-v1' })
  ElMessage.success('演示模型已添加')
  await load()
}

async function save() {
  if (!form.providerName?.trim()) return ElMessage.warning('请填写配置名称')
  if (!form.modelName?.trim()) return ElMessage.warning('请填写模型名称')
  if (form.providerType !== 'mock' && !form.baseUrl?.trim()) return ElMessage.warning('请填写 API 地址')
  saving.value = true
  try {
    if (form.id) await updateProvider(form.id, form)
    else await createProvider(form)
    ElMessage.success('AI 模型配置已保存')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function test(id: number) {
  testingId.value = id
  try {
    const data = await testProvider(id)
    if (data.success === false) ElMessage.error(data.message || '连接测试失败')
    else ElMessage.success(data.message || '连接测试成功')
  } finally {
    testingId.value = null
  }
}

async function setDefault(id: number) {
  await setDefaultProvider(id)
  ElMessage.success('已设为默认模型')
  await load()
}

async function remove(item: any) {
  await ElMessageBox.confirm(`确定删除“${item.providerName}”吗？`, '删除 AI 模型', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteProvider(item.id)
  ElMessage.success('模型配置已删除')
  await load()
}

function providerTypeName(type: string) {
  return ({ deepseek: 'DeepSeek', qwen: '通义千问', openai_compatible: 'OpenAI 兼容接口', ollama: 'Ollama 本地模型', custom: '自定义接口', mock: '本地演示模型' } as Record<string, string>)[type] || type
}

onMounted(load)
</script>

<style scoped>
.provider-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.provider-card { display: grid; gap: 18px; padding: 20px; border: 1px solid var(--line); border-radius: var(--radius-lg); background: var(--surface-glass); box-shadow: var(--shadow-sm); }
.provider-head { display: flex; align-items: center; gap: 13px; }
.provider-icon { width: 46px; height: 46px; flex: 0 0 46px; display: grid; place-items: center; color: var(--primary); background: var(--primary-soft); border: 1px solid var(--primary-border); border-radius: 12px; font-size: 20px; }
.provider-title { min-width: 0; flex: 1; }
.title-line { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.provider-title h2 { margin: 0; color: var(--text-strong); font-size: 18px; overflow-wrap: anywhere; }
.provider-title p { margin: 3px 0 0; color: var(--muted); font-size: 13px; }
.provider-info { display: grid; gap: 9px; margin: 0; }
.provider-info > div { display: grid; grid-template-columns: 76px minmax(0, 1fr); gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--line); }
.provider-info dt { color: var(--muted); }
.provider-info dd { margin: 0; color: var(--text); overflow-wrap: anywhere; }
.provider-actions { display: flex; flex-wrap: wrap; gap: 8px; }
@media (max-width: 900px) { .provider-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .provider-info > div { grid-template-columns: 1fr; gap: 2px; } .title-line { align-items: flex-start; flex-direction: column; } }
</style>
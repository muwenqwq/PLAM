<template>
  <el-form :model="model" label-position="top" class="model-form">
    <div class="form-row">
      <el-form-item label="配置名称" required>
        <el-input v-model="model.providerName" placeholder="例如：我的 DeepSeek" />
      </el-form-item>
      <el-form-item label="服务类型" required>
        <el-select v-model="model.providerType" class="full-width" @change="applyPreset">
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="通义千问" value="qwen" />
          <el-option label="OpenAI 兼容接口" value="openai_compatible" />
          <el-option label="Ollama 本地模型" value="ollama" />
          <el-option label="其他自定义接口" value="custom" />
          <el-option label="本地演示模型" value="mock" />
        </el-select>
      </el-form-item>
    </div>

    <el-form-item label="API 地址" required>
      <el-input v-model="model.baseUrl" :placeholder="preset.baseUrl" />
      <span class="field-help">填写兼容 OpenAI Chat Completions 的服务地址，通常以 /v1 结尾。</span>
    </el-form-item>

    <el-form-item label="API Key" :required="model.providerType !== 'mock' && model.providerType !== 'ollama'">
      <el-input v-model="model.apiKey" type="password" show-password :placeholder="model.id ? '留空表示不修改当前密钥' : '请输入 API Key'" autocomplete="off" />
      <span class="field-help">密钥只会脱敏显示，编辑时留空不会覆盖原密钥。</span>
    </el-form-item>

    <div class="form-row">
      <el-form-item label="对话模型名称" required>
        <el-input v-model="model.modelName" :placeholder="preset.modelName" />
      </el-form-item>
      <el-form-item label="向量模型名称">
        <el-input v-model="model.embeddingModel" :placeholder="preset.embeddingModel" />
      </el-form-item>
    </div>

    <div class="form-row">
      <el-form-item label="生成温度">
        <el-slider v-model="model.temperature" :min="0" :max="2" :step="0.1" show-input />
      </el-form-item>
      <el-form-item label="最大输出 Token">
        <el-input-number v-model="model.maxTokens" :min="256" :max="200000" :step="256" class="full-width" />
      </el-form-item>
    </div>

    <div class="switch-row">
      <div><strong>设为默认模型</strong><span>资源生成、资料分片和问答将优先使用该模型。</span></div>
      <el-switch v-model="model.defaultProvider" />
    </div>
    <div class="switch-row">
      <div><strong>启用流式输出</strong><span>接口支持时可逐步返回内容。</span></div>
      <el-switch v-model="model.streamEnabled" />
    </div>

    <el-form-item label="备注">
      <el-input v-model="model.remark" type="textarea" :rows="2" placeholder="选填，例如：用于课程资料问答" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const model = defineModel<any>({ required: true })

const presets: Record<string, any> = {
  deepseek: { baseUrl: 'https://api.deepseek.com/v1', modelName: 'deepseek-chat', embeddingModel: '' },
  qwen: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', modelName: 'qwen-plus', embeddingModel: 'text-embedding-v3' },
  openai_compatible: { baseUrl: 'https://api.example.com/v1', modelName: 'model-name', embeddingModel: '' },
  ollama: { baseUrl: 'http://127.0.0.1:11434/v1', modelName: 'qwen2.5:7b', embeddingModel: 'nomic-embed-text' },
  custom: { baseUrl: 'https://api.example.com/v1', modelName: 'model-name', embeddingModel: '' },
  mock: { baseUrl: 'mock://local/llm', modelName: 'mock-chat-v1', embeddingModel: 'mock-embedding-v1' }
}

const preset = computed(() => presets[model.value.providerType] || presets.openai_compatible)

function applyPreset(type: string) {
  const next = presets[type]
  if (!next) return
  if (!model.value.baseUrl || Object.values(presets).some((item: any) => item.baseUrl === model.value.baseUrl)) model.value.baseUrl = next.baseUrl
  if (!model.value.modelName || Object.values(presets).some((item: any) => item.modelName === model.value.modelName)) model.value.modelName = next.modelName
  if (!model.value.embeddingModel || Object.values(presets).some((item: any) => item.embeddingModel === model.value.embeddingModel)) model.value.embeddingModel = next.embeddingModel
}
</script>

<style scoped>
.model-form { display: grid; gap: 2px; }
.full-width { width: 100%; }
.field-help { display: block; margin-top: 6px; color: var(--muted); font-size: 12px; line-height: 1.6; }
.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 16px; padding: 13px 14px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface-soft); }
.switch-row strong, .switch-row span { display: block; }
.switch-row strong { color: var(--text-strong); font-size: 14px; }
.switch-row span { margin-top: 2px; color: var(--muted); font-size: 12px; }
</style>
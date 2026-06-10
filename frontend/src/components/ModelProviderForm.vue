<template>
  <el-form :model="model" label-width="104px">
    <el-form-item label="配置名称" required>
      <el-input v-model="model.providerName" placeholder="Mock 学习模型" />
    </el-form-item>
    <el-form-item label="提供商类型" required>
      <el-select v-model="model.providerType">
        <el-option label="Mock 演示模型" value="mock" />
        <el-option label="OpenAI Compatible" value="openai_compatible" />
      </el-select>
    </el-form-item>
    <el-form-item label="Base URL">
      <el-input v-model="model.baseUrl" placeholder="http://localhost:8000" />
    </el-form-item>
    <el-form-item label="API Key">
      <el-input v-model="model.apiKey" type="password" show-password placeholder="演示可留空或输入 MOCK-ONLY" />
    </el-form-item>
    <el-form-item label="模型名称">
      <el-input v-model="model.modelName" placeholder="mock-chat-v1" />
    </el-form-item>
    <el-form-item label="Embedding">
      <el-input v-model="model.embeddingModel" placeholder="mock-embedding-v1" />
    </el-form-item>
    <div class="form-row">
      <el-form-item label="温度">
        <el-input-number v-model="model.temperature" :min="0" :max="2" :step="0.1" />
      </el-form-item>
      <el-form-item label="最大 token">
        <el-input-number v-model="model.maxTokens" :min="256" :max="8192" :step="256" />
      </el-form-item>
    </div>
    <el-form-item label="流式输出">
      <el-switch v-model="model.streamEnabled" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
const model = defineModel<any>({ required: true })
</script>

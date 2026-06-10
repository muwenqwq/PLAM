<template>
  <div class="page">
    <PageHeader title="多智能体工作台" description="创建 Agent 任务，展示 Planner、Knowledge、Exercise、Review 等执行步骤。" />
    <div class="grid two">
      <el-card>
        <template #header>创建任务</template>
        <el-form :model="form" label-width="100px">
          <el-form-item label="空间 ID"><el-input-number v-model="form.spaceId" :min="1" /></el-form-item>
          <el-form-item label="模型 ID"><el-input-number v-model="form.providerId" :min="1" /></el-form-item>
          <el-form-item label="任务标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="资源类型">
            <el-select v-model="form.resourceType">
              <el-option label="学习计划" value="plan" />
              <el-option label="习题集" value="quiz_set" />
              <el-option label="知识图谱" value="knowledge_graph" />
              <el-option label="PPT 大纲" value="ppt_outline" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="knowledgeText" placeholder="逗号分隔" /></el-form-item>
        </el-form>
        <el-button type="primary" :loading="running" @click="run">运行 Agent</el-button>
      </el-card>
      <el-card>
        <template #header>执行步骤</template>
        <AgentStepTimeline v-if="steps.length" :steps="steps" />
        <EmptyState v-else title="尚未运行任务" description="运行后会展示多智能体执行时间线。" />
      </el-card>
    </div>
    <el-card>
      <template #header>生成资源</template>
      <div v-if="resources.length" class="grid two">
        <ResourceCard v-for="item in resources" :key="item.id || item.title" :resource="item">
          <MarkdownViewer :content="item.contentMarkdown" />
        </ResourceCard>
      </div>
      <EmptyState v-else />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import AgentStepTimeline from '@/components/AgentStepTimeline.vue'
import ResourceCard from '@/components/ResourceCard.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { createAgentTask } from '@/api/agent'

const running = ref(false)
const knowledgeText = ref('索引, 范式, SQL 查询')
const steps = ref<any[]>([])
const resources = ref<any[]>([])
const form = reactive<any>({ spaceId: 1, providerId: 1, title: '数据库期末复习资源', subject: '数据库', taskType: 'resource_generation', resourceType: 'plan' })

async function run() {
  running.value = true
  try {
    const data = await createAgentTask({ ...form, inputParams: { knowledgePoints: knowledgeText.value.split(',').map((v) => v.trim()) } })
    steps.value = data.steps || []
    resources.value = data.resources || []
    ElMessage.success('Agent 任务执行完成')
  } finally {
    running.value = false
  }
}
</script>

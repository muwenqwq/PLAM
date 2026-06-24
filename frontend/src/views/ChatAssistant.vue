<template>
  <div class="page chat-page">
    <PageHeader title="智能对话" description="选择学习空间和模型配置，向 AI 学习助手发送问题。" />
    <div class="chat-layout">
      <el-card class="conversation-list">
        <template #header>
          <div class="toolbar"><span>会话列表</span><el-button size="small" type="primary" @click="newConversation">新建</el-button></div>
        </template>
        <el-scrollbar class="conversation-scroll">
          <div v-for="item in conversations" :key="item.id" class="conversation" :class="{ active: item.id === currentId }" @click="select(item)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subject || '通用学习' }}</span>
          </div>
          <EmptyState v-if="!conversations.length" title="暂无会话" description="新建一个学习会话开始演示。" />
        </el-scrollbar>
      </el-card>
      <el-card class="chat-window">
        <template #header>
          <div class="toolbar">
            <span>{{ current?.title || '未选择会话' }}</span>
            <div class="selectors">
              <el-select v-model="spaceId" placeholder="空间" size="small" style="width: 140px">
                <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
              </el-select>
              <el-select v-model="providerId" placeholder="模型" size="small" style="width: 140px">
                <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
              </el-select>
            </div>
          </div>
        </template>
        <el-scrollbar class="messages">
          <div v-for="msg in messages" :key="msg.id || msg.createdAt" class="message" :class="normalizeRole(msg)">
            <strong>{{ msg.role === 'user' ? '我' : 'AI 助手' }}</strong>
            <MarkdownViewer :content="normalizeContent(msg)" />
          </div>
          <EmptyState v-if="!messages.length" title="等待提问" description="可以点击示例问题快速填充。" />
        </el-scrollbar>
        <div class="examples">
          <el-button v-for="item in examples" :key="item" size="small" @click="input = item">{{ item }}</el-button>
        </div>
        <div class="sender">
          <el-input v-model="input" type="textarea" :rows="3" placeholder="请输入学习问题" />
          <el-button type="primary" :loading="sending" @click="send">发送</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { createConversation, getMessages, listConversations, sendMessage } from '@/api/chat'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'

const conversations = ref<any[]>([])
const messages = ref<any[]>([])
const currentId = ref<number>()
const input = ref('')
const sending = ref(false)
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const spaceId = ref<number | null>(null)
const providerId = ref<number | null>(null)
const current = computed(() => conversations.value.find((item) => item.id === currentId.value))
const examples = [
  '我两天后要考数据库，帮我复习范式、SQL 查询和索引。',
  '帮我用简单例子解释 A* 搜索算法。',
  '根据我的基础生成一份 Java 后端复习计划。'
]

function normalizeRole(message: any) {
  return message.role || message.messageRole || 'assistant'
}

function normalizeContent(message: any) {
  return message.content || message.contentMd || message.replyMarkdown || ''
}

function normalizeMessage(message: any) {
  return {
    ...message,
    role: normalizeRole(message),
    content: normalizeContent(message)
  }
}

async function load() {
  const data = await listConversations({ pageNum: 1, pageSize: 30 })
  conversations.value = data.records || []
  if (!currentId.value && conversations.value[0]) select(conversations.value[0])
}

async function loadDefaults() {
  const [space, provider] = await Promise.all([
    getDefaultSpace().catch(() => null),
    getDefaultProvider().catch(() => null)
  ])
  if (space?.id) spaceId.value = space.id
  if (provider?.id) providerId.value = provider.id
}

async function loadSpacesAndProviders() {
  const [spaceData, providerData] = await Promise.all([
    listSpaces({ pageNum: 1, pageSize: 50 }),
    listProviders({ pageNum: 1, pageSize: 50 })
  ])
  spaces.value = spaceData.records || []
  providers.value = providerData.records || []
}

async function newConversation() {
  const created = await createConversation({ spaceId: spaceId.value, providerId: providerId.value, title: '新的学习会话', subject: '综合学习' })
  conversations.value.unshift(created)
  select(created)
}

async function select(item: any) {
  currentId.value = item.id
  const data = await getMessages(item.id)
  messages.value = (data || []).map(normalizeMessage)
}

async function send() {
  if (!input.value.trim()) return ElMessage.warning('请输入问题')
  if (!currentId.value) await newConversation()
  sending.value = true
  const question = input.value
  messages.value.push({ role: 'user', content: question })
  input.value = ''
  try {
    const response = await sendMessage(currentId.value!, { message: question, subject: current.value?.subject, modelProviderId: providerId.value })
    messages.value.push(normalizeMessage(response.assistantMessage || response))
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults()])
  await load()
})
</script>

<style scoped>
.chat-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}

.chat-page {
  height: calc(100dvh - 112px);
  min-height: 0;
  overflow: hidden;
}

.conversation-list,
.chat-window {
  min-height: 0;
  overflow: hidden;
}

.conversation-list :deep(.el-card__body),
.chat-window :deep(.el-card__body) {
  min-height: 0;
}

.conversation-list :deep(.el-card__body) {
  height: calc(100% - 57px);
}

.conversation-scroll {
  height: 100%;
}

.chat-window :deep(.el-card__body) {
  height: calc(100% - 57px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.conversation {
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.conversation.active,
.conversation:hover {
  background: var(--primary-soft);
}

.conversation span {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
}

.message {
  max-width: 78%;
  margin: 0 0 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.messages {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
}

.message.user {
  margin-left: auto;
  background: var(--info-soft);
}

.examples,
.sender,
.selectors {
  display: flex;
  gap: 10px;
  align-items: center;
}

.examples {
  flex: 0 0 auto;
  margin: 10px 0;
  flex-wrap: wrap;
}

.sender {
  flex: 0 0 auto;
  align-items: stretch;
}

.sender .el-textarea {
  flex: 1;
}

.sender :deep(.el-textarea__inner) {
  min-height: 76px !important;
  resize: none;
}

.sender .el-button {
  align-self: stretch;
}

@media (max-width: 1100px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .conversation-list {
    display: none;
  }
}
</style>

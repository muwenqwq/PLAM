<template>
  <div class="page chat-page">
    <PageHeader title="AI 对话" description="选择学习空间、模型和 AI 角色，让学习问答带上陪伴感。" />

    <div class="chat-layout">
      <el-card class="conversation-list">
        <template #header>
          <div class="toolbar">
            <span>会话列表</span>
            <el-button size="small" type="primary" @click="newConversation">新建</el-button>
          </div>
        </template>

        <el-scrollbar class="conversation-scroll">
          <div
            v-for="item in conversations"
            :key="item.id"
            class="conversation"
            :class="{ active: item.id === currentId }"
            @click="select(item)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.rolePlayEnabled ? roleNameFor(item.roleId) : '通用学习问答' }}</span>
          </div>
          <EmptyState
            v-if="!conversations.length"
            title="暂无会话"
            description="新建一个学习会话，选择角色后开始提问。"
          />
        </el-scrollbar>
      </el-card>

      <el-card class="chat-window">
        <template #header>
          <div class="chat-head">
            <div class="title-block">
              <span>{{ current?.title || '未选择会话' }}</span>
              <em v-if="rolePlayEnabled && selectedRole">{{ selectedRole.roleName }} 正在陪你学习</em>
            </div>
            <div class="selectors">
              <el-select v-model="spaceId" placeholder="空间" size="small" style="width: 138px" @change="handleSpaceChange">
                <el-option v-for="s in spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
              </el-select>
              <el-select v-model="providerId" placeholder="模型" size="small" style="width: 138px">
                <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
              </el-select>
              <el-select
                v-model="selectedRoleId"
                clearable
                placeholder="AI 角色"
                size="small"
                style="width: 170px"
                @change="applyRoleSetting"
              >
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
              <el-switch v-model="rolePlayEnabled" size="small" active-text="角色" @change="applyRoleSetting" />
            </div>
          </div>
        </template>

        <div
          v-if="rolePlayEnabled && selectedRole"
          class="role-banner"
          :style="{ borderColor: selectedRole.themeColor || '#409EFF' }"
        >
          <span class="role-avatar" :style="{ background: selectedRole.themeColor || '#409EFF' }">
            {{ selectedRole.roleName?.slice(0, 1) }}
          </span>
          <div>
            <strong>{{ selectedRole.roleName }} · {{ selectedRole.roleIdentity || '学习陪伴角色' }}</strong>
            <p>{{ selectedRole.speakingStyle || '会按照你的角色卡风格陪你学习。' }}</p>
          </div>
        </div>

        <el-scrollbar class="messages">
          <div v-for="msg in messages" :key="msg.id || msg.createdAt || msg.content" class="message" :class="normalizeRole(msg)">
            <strong>{{ normalizeRole(msg) === 'user' ? '我' : assistantName }}</strong>
            <MarkdownViewer :content="normalizeContent(msg)" />
          </div>
          <EmptyState
            v-if="!messages.length"
            title="等待提问"
            description="输入学习问题开始对话，也可以先选择一个 AI 角色。"
          />
        </el-scrollbar>

        <div class="sender">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="请输入学习问题，例如：这道题我看不懂，换一种方式讲。"
            @keydown.ctrl.enter="send"
          />
          <el-button type="primary" :loading="sending" @click="send">发送</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownViewer from '@/components/MarkdownViewer.vue'
import { applyConversationRole, createConversation, getMessages, listConversations, streamMessage } from '@/api/chat'
import { getDefaultProvider, listProviders } from '@/api/modelProvider'
import { getDefaultSpace, listSpaces } from '@/api/learningSpace'
import { CompanionRole, getActiveCompanionRole, listCompanionRoles } from '@/api/companionRole'

const route = useRoute()
const conversations = ref<any[]>([])
const messages = ref<any[]>([])
const currentId = ref<number>()
const input = ref('')
const sending = ref(false)
const spaces = ref<any[]>([])
const providers = ref<any[]>([])
const roles = ref<CompanionRole[]>([])
const spaceId = ref<number | null>(null)
const providerId = ref<number | null>(null)
const selectedRoleId = ref<number | null>(null)
const rolePlayEnabled = ref(false)

const current = computed(() => conversations.value.find((item) => item.id === currentId.value))
const selectedRole = computed(() => roles.value.find((item) => item.id === selectedRoleId.value))
const assistantName = computed(() => (rolePlayEnabled.value && selectedRole.value ? selectedRole.value.roleName : 'AI 助手'))
const selectedSpaceSubject = computed(() => spaces.value.find((item) => item.id === spaceId.value)?.subject)

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

function roleNameFor(roleId?: number) {
  return roles.value.find((role) => role.id === roleId)?.roleName || 'AI 角色陪伴'
}

async function load() {
  if (!spaceId.value) {
    conversations.value = []
    messages.value = []
    currentId.value = undefined
    return
  }
  const data = await listConversations({ spaceId: spaceId.value, pageNum: 1, pageSize: 30 })
  conversations.value = data.records || []
  if (!currentId.value && conversations.value[0]) await select(conversations.value[0])
}

async function handleSpaceChange() {
  currentId.value = undefined
  messages.value = []
  await load()
}

async function loadRoles() {
  const [listData, activeRole] = await Promise.all([
    listCompanionRoles({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
    getActiveCompanionRole().catch(() => null)
  ])
  roles.value = listData.records || []
  if (!selectedRoleId.value && activeRole?.id) selectedRoleId.value = activeRole.id
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
  const created = await createConversation({
    spaceId: spaceId.value,
    title: selectedRole.value ? `${selectedRole.value.roleName}陪学会话` : '新的学习会话',
    intentType: 'learning_guidance',
    roleId: selectedRoleId.value,
    rolePlayEnabled: rolePlayEnabled.value && Boolean(selectedRoleId.value)
  })
  conversations.value.unshift(created)
  await select(created)
}

async function select(item: any) {
  currentId.value = item.id
  if (item.spaceId) spaceId.value = item.spaceId
  selectedRoleId.value = item.roleId || selectedRoleId.value
  rolePlayEnabled.value = Boolean(item.rolePlayEnabled)
  const data = await getMessages(item.id)
  messages.value = (data || []).map(normalizeMessage)
}

async function applyRoleSetting() {
  if (rolePlayEnabled.value && !selectedRoleId.value) {
    rolePlayEnabled.value = false
    return ElMessage.warning('请先选择一个 AI 角色')
  }
  if (!currentId.value) return
  const updated = await applyConversationRole(currentId.value, {
    roleId: selectedRoleId.value,
    rolePlayEnabled: rolePlayEnabled.value
  })
  const index = conversations.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) conversations.value[index] = updated
  ElMessage.success(rolePlayEnabled.value ? '角色陪伴已启用' : '角色陪伴已关闭')
}

async function applyRouteContext() {
  const rawSpaceId = Array.isArray(route.query.spaceId) ? route.query.spaceId[0] : route.query.spaceId
  const routeSpaceId = rawSpaceId ? Number(rawSpaceId) : NaN
  if (Number.isFinite(routeSpaceId) && spaces.value.some((space) => space.id === routeSpaceId)) {
    spaceId.value = routeSpaceId
  }
  const rawPrompt = Array.isArray(route.query.prompt) ? route.query.prompt[0] : route.query.prompt
  if (rawPrompt) input.value = rawPrompt

  const rawRoleId = Array.isArray(route.query.roleId) ? route.query.roleId[0] : route.query.roleId
  const roleId = rawRoleId ? Number(rawRoleId) : NaN
  if (Number.isFinite(roleId)) {
    selectedRoleId.value = roleId
    rolePlayEnabled.value = route.query.rolePlay === '1'
  }

  if (route.query.new === '1') {
    await newConversation()
    return true
  }

  if (Number.isFinite(roleId) && currentId.value) await applyRoleSetting()
  return false
}

async function send() {
  if (!input.value.trim()) return ElMessage.warning('请输入问题')
  if (!currentId.value) await newConversation()
  sending.value = true
  const question = input.value
  const optimisticUser = { id: `user-${Date.now()}`, role: 'user', content: question }
  const streamingMessage = reactive({ id: `stream-${Date.now()}`, role: 'assistant', content: '' })
  messages.value.push(optimisticUser)
  messages.value.push(streamingMessage)
  input.value = ''
  try {
    await streamMessage(currentId.value!, {
      message: question,
      subject: selectedSpaceSubject.value,
      modelProviderId: providerId.value
    }, (event) => {
      if (event.type === 'delta') {
        streamingMessage.content += event.content || ''
      } else if (event.type === 'done') {
        const response = event.response || {}
        const assistant = normalizeMessage(response.assistantMessage || streamingMessage)
        Object.assign(streamingMessage, assistant)
        if (response.conversation) {
          const index = conversations.value.findIndex((item) => item.id === response.conversation.id)
          if (index >= 0) conversations.value[index] = response.conversation
        }
      }
    })
  } catch (error: any) {
    messages.value = messages.value.filter((item) => item !== optimisticUser && item !== streamingMessage)
    if (!input.value) input.value = question
    ElMessage.error(error?.message || '发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSpacesAndProviders(), loadDefaults(), loadRoles()])
  const routeCreatedConversation = await applyRouteContext()
  if (!routeCreatedConversation) await load()
})
</script>

<style scoped>
.chat-page { height: calc(100dvh - 124px); min-height: 0; overflow: hidden; }
.chat-layout { flex: 1; min-height: 0; display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 14px; }
.conversation-list, .chat-window { min-height: 0; overflow: hidden; }
.conversation-list :deep(.el-card__body), .chat-window :deep(.el-card__body) { min-height: 0; }
.conversation-list :deep(.el-card__body) { height: calc(100% - 57px); }
.conversation-scroll { height: 100%; }
.chat-window :deep(.el-card__body) { height: calc(100% - 57px); display: flex; flex-direction: column; min-height: 0; }
.toolbar, .chat-head, .selectors { display: flex; gap: 10px; align-items: center; }
.chat-head { justify-content: space-between; flex-wrap: wrap; }
.title-block { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title-block span { font-weight: 700; color: var(--text-strong); }
.title-block em { color: var(--muted); font-size: 12px; font-style: normal; }
.conversation { padding: 12px; border-radius: var(--radius-md); cursor: pointer; }
.conversation.active, .conversation:hover { background: var(--primary-soft); }
.conversation span { display: block; margin-top: 4px; color: var(--muted); font-size: 12px; }
.role-banner { flex: 0 0 auto; display: flex; align-items: center; gap: 10px; margin-bottom: 12px; padding: 10px 12px; border: 1px solid var(--primary); border-radius: var(--radius-md); background: var(--surface-soft); }
.role-avatar { width: 34px; height: 34px; flex: 0 0 34px; display: grid; place-items: center; color: #fff; border-radius: 50%; font-weight: 800; }
.role-banner strong { color: var(--text-strong); font-size: 13px; }
.role-banner p { margin: 3px 0 0; color: var(--muted); font-size: 12px; }
.message { max-width: 84%; margin: 0 0 12px; padding: 12px; border-radius: var(--radius-md); background: var(--surface-soft); }
.message.user { margin-left: auto; background: var(--info-soft); }
.messages { flex: 1 1 0; height: 0; min-height: 0; }
.sender { display: flex; flex: 0 0 auto; gap: 10px; align-items: stretch; padding-top: 10px; }
.sender .el-textarea { flex: 1; }
.sender :deep(.el-textarea__inner) { min-height: 76px !important; resize: none; }
.sender .el-button { align-self: stretch; }
@media (max-width: 1100px) {
  .chat-layout { grid-template-columns: 1fr; }
  .conversation-list { display: none; }
  .selectors { width: 100%; flex-wrap: wrap; }
}
</style>

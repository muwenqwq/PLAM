<template>
  <div class="tutor-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>👨‍🏫 智能辅导</h2>
      <p>基于课程知识库的即时问答，回答包含引用来源</p>
    </div>

    <el-card class="chat-card" shadow="never">
      <!-- 对话消息区域 -->
      <div ref="chatContainer" class="chat-messages">
        <!-- 空状态引导 -->
        <EmptyState
          v-if="!tutorStore.messages.length"
          icon="💬"
          title="开始提问吧"
          description="输入课程相关问题，AI 将结合课程知识库为你解答"
        />
        <!-- 消息列表 -->
        <ChatMessage
          v-for="(msg, index) in tutorStore.messages"
          :key="index"
          :msg="msg"
        />
        <!-- 加载指示器 -->
        <div v-if="tutorStore.loading" class="typing-indicator">
          <div class="msg-avatar">🤖</div>
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input">
        <el-input
          v-model="inputText"
          :placeholder="tutorStore.loading ? 'AI 思考中...' : '输入课程问题，例如：A* 中 g(n) 和 h(n) 有什么区别？'"
          :disabled="tutorStore.loading"
          @keyup.enter="handleSend"
        >
          <template #append>
            <el-button
              type="primary"
              :loading="tutorStore.loading"
              :disabled="!inputText.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </template>
        </el-input>
        <div class="input-hint">
          <span>💡 提问越具体，回答越精准 · 回答基于课程知识库，来源可查</span>
          <el-button text type="danger" size="small" @click="handleClear">清空对话</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useTutorStore } from '@/stores/tutor'
import { ChatMessage, EmptyState } from '@/components/common'

const userStore = useUserStore()
const tutorStore = useTutorStore()

const inputText = ref('')
const chatContainer = ref(null)

// 发送消息
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || tutorStore.loading) return
  inputText.value = ''

  try {
    await tutorStore.sendMessage(text, {
      studentId: userStore.studentId || 1,
      courseId: userStore.courseId || 1
    })
  } catch {
    // 错误已在 store 中处理
  }
  // 滚动到底部
  scrollToBottom()
}

// 清空对话
function handleClear() {
  tutorStore.clearMessages()
}

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 新消息到来时自动滚动
watch(() => tutorStore.messages.length, scrollToBottom)
</script>

<style scoped>
.tutor-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - 140px);
}
/* .page-header 样式已移至 style.css 全局定义 */
.chat-card {
  border-radius: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-right: auto;
}
.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: var(--bg-hover);
  border-radius: 12px;
  border: 1px solid var(--border-default);
}
.typing-dots span {
  width: 8px;
  height: 8px;
  background: var(--text-placeholder);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

/* 输入区域 */
.chat-input {
  padding: 16px 24px;
  border-top: 1px solid var(--border-divider);
}
.chat-input :deep(.el-input-group__append) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.chat-input :deep(.el-input-group__append .el-button) {
  color: white;
}
.input-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-placeholder);
}
</style>

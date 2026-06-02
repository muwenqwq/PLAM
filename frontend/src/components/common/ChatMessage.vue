<template>
  <div class="chat-msg" :class="[msg.role, { error: msg.error }]">
    <div class="msg-avatar">
      {{ msg.role === 'user' ? '👤' : '🤖' }}
    </div>
    <div class="msg-body">
      <div class="msg-content">
        <MarkdownRenderer v-if="msg.role === 'assistant'" :content="msg.content" />
        <span v-else>{{ msg.content }}</span>
      </div>

      <div v-if="msg.sources?.length" class="msg-sources">
        <span class="sources-label">📚 来源：</span>
        <el-tag v-for="(s, i) in msg.sources" :key="i" size="small" type="info" effect="plain">
          {{ s.file }} {{ s.section }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import MarkdownRenderer from './MarkdownRenderer.vue'

defineProps({
  msg: { type: Object, required: true }
})
</script>

<style scoped>
.chat-msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 80%;
}
.chat-msg.user {
  flex-direction: row-reverse;
  margin-left: auto;
}
.chat-msg.assistant {
  margin-right: auto;
}
.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.msg-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msg-content {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
}
.chat-msg.user .msg-content {
  background: #3182ce;
  color: white;
  border-bottom-right-radius: 4px;
}
.chat-msg.assistant .msg-content {
  background: #f7fafc;
  color: #2d3748;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 4px;
}
.chat-msg.error .msg-content {
  background: #fff5f5;
  border-color: #fed7d7;
  color: #e53e3e;
}
.msg-sources {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
}
.sources-label {
  font-size: 12px;
  color: #a0aec0;
}
</style>

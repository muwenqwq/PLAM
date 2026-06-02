import { defineStore } from 'pinia'
import { ref } from 'vue'
import { askTutor } from '@/api/tutor'

export const useTutorStore = defineStore('tutor', () => {
  const messages = ref([])
  const loading = ref(false)

  async function sendMessage(content, context = {}) {
    // 添加用户消息
    messages.value.push({
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    })

    loading.value = true
    try {
      const res = await askTutor({ question: content, ...context })
      // 添加 AI 回复
      messages.value.push({
        role: 'assistant',
        content: res.answer,
        sources: res.sources || [],
        suggestedResources: res.suggestedResources || [],
        agentTrace: res.agentTrace || [],
        timestamp: new Date().toISOString()
      })
      return res
    } catch (error) {
      messages.value.push({
        role: 'assistant',
        content: '抱歉，回答出现问题，请稍后重试。',
        error: true,
        timestamp: new Date().toISOString()
      })
      throw error
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
  }

  return { messages, loading, sendMessage, clearMessages }
})

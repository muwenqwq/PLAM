import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAgentRuns } from '@/api/agentRun'

export const useAgentRunStore = defineStore('agentRun', () => {
  const runs = ref([])
  const loading = ref(false)

  async function fetchRuns(taskId) {
    loading.value = true
    try {
      const res = await getAgentRuns(taskId)
      runs.value = Array.isArray(res) ? res : res.data || []
      return runs.value
    } finally {
      loading.value = false
    }
  }

  return { runs, loading, fetchRuns }
})

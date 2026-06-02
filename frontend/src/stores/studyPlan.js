import { defineStore } from 'pinia'
import { ref } from 'vue'
import { generateStudyPlan, getStudyPlan } from '@/api/studyPlan'

export const useStudyPlanStore = defineStore('studyPlan', () => {
  const plan = ref(null)
  const nodes = ref([])
  const loading = ref(false)

  async function fetchPlan(studentId) {
    loading.value = true
    try {
      const res = await getStudyPlan(studentId)
      plan.value = res
      nodes.value = res.nodes || []
      return res
    } finally {
      loading.value = false
    }
  }

  async function generate(data) {
    loading.value = true
    try {
      const res = await generateStudyPlan(data)
      plan.value = res
      nodes.value = res.nodes || []
      return res
    } finally {
      loading.value = false
    }
  }

  function updateNodeStatus(nodeId, status) {
    const node = nodes.value.find(n => n.id === nodeId)
    if (node) node.status = status
  }

  return { plan, nodes, loading, fetchPlan, generate, updateNodeStatus }
})

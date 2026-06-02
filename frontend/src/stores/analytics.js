import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAnalytics, submitAssessment } from '@/api/analytics'

export const useAnalyticsStore = defineStore('analytics', () => {
  const analytics = ref(null)
  const loading = ref(false)
  const submitting = ref(false)

  async function fetchAnalytics(studentId) {
    loading.value = true
    try {
      const res = await getAnalytics(studentId)
      analytics.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  async function submitQuiz(data) {
    submitting.value = true
    try {
      const res = await submitAssessment(data)
      return res
    } finally {
      submitting.value = false
    }
  }

  return { analytics, loading, submitting, fetchAnalytics, submitQuiz }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { extractProfile, getLatestProfile, getProfileHistory } from '@/api/profile'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref(null)
  const history = ref([])
  const loading = ref(false)

  async function fetchLatest(studentId) {
    loading.value = true
    try {
      const res = await getLatestProfile(studentId)
      profile.value = res.profile || res
      return profile.value
    } finally {
      loading.value = false
    }
  }

  async function fetchHistory(studentId) {
    try {
      const res = await getProfileHistory(studentId)
      history.value = Array.isArray(res) ? res : res.data || []
      return history.value
    } catch {
      history.value = []
    }
  }

  async function generate(data) {
    loading.value = true
    try {
      const res = await extractProfile(data)
      profile.value = res.profile || res
      return res
    } finally {
      loading.value = false
    }
  }

  return { profile, history, loading, fetchLatest, fetchHistory, generate }
})

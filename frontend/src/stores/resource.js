import { defineStore } from 'pinia'
import { ref } from 'vue'
import { generateResources, getResourceList, getResourceDetail } from '@/api/resource'

export const useResourceStore = defineStore('resource', () => {
  const resources = ref([])
  const currentResource = ref(null)
  const loading = ref(false)
  const generating = ref(false)

  async function fetchList(studentId) {
    loading.value = true
    try {
      const res = await getResourceList(studentId)
      resources.value = Array.isArray(res) ? res : res.data || []
      return resources.value
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(resourceId) {
    loading.value = true
    try {
      const res = await getResourceDetail(resourceId)
      currentResource.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  async function generate(data) {
    generating.value = true
    try {
      const res = await generateResources(data)
      return res
    } finally {
      generating.value = false
    }
  }

  return { resources, currentResource, loading, generating, fetchList, fetchDetail, generate }
})

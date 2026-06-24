import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/storage'

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface ApiResult<T> {
  code: string
  message: string
  data: T
  timestamp: string
  success: boolean
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && Object.prototype.hasOwnProperty.call(body, 'success')) {
      if (!body.success) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return response.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearToken()
      ElMessage.warning('登录状态已过期，请重新登录')
      if (location.pathname !== '/login') location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default request

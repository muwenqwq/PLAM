import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000  // 2 分钟，AI 操作可能较慢
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 读取 user store 的持久化数据
    try {
      const userData = JSON.parse(localStorage.getItem('user') || '{}')
      if (userData.token) {
        config.headers.Authorization = `Bearer ${userData.token}`
      }
    } catch {
      // 忽略解析错误
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const { data } = response
    // 如果后端返回了 code 字段，按约定处理
    if (data.code !== undefined && data.code !== 0 && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    // 剥掉业务层 {code, data} 包装，直接返回 payload，Store 无需关心包装
    if (data.data !== undefined) {
      return data.data
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      // 可以在这里触发跳转登录页
    } else if (status === 403) {
      ElMessage.error('没有权限访问')
    } else if (status === 500) {
      ElMessage.error('服务器内部错误')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request

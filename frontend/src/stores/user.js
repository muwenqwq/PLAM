import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 将主题应用到 DOM（同步 Element Plus 暗色变量）
 * 在 store 外定义，避免循环依赖
 */
function applyTheme(val) {
  const html = document.documentElement
  if (val === 'dark') {
    html.dataset.theme = 'dark'
    html.classList.add('dark')
  } else {
    delete html.dataset.theme
    html.classList.remove('dark')
  }
}

export const useUserStore = defineStore('user', () => {
  const name = ref('访客')
  const target = ref('请先登录')
  const imgSrc = ref('https://api.dicebear.com/7.x/notionists/svg?seed=Felix')
  const studentId = ref(null)
  const courseId = ref(null)
  const token = ref('')
  const theme = ref('light') // 'light' | 'dark'

  const setUserInfo = (info) => {
    name.value = info.name
    target.value = info.target
    imgSrc.value = info.imgSrc
    if (info.studentId !== undefined) studentId.value = info.studentId
    if (info.courseId !== undefined) courseId.value = info.courseId
    if (info.token) token.value = info.token
  }

  const setTheme = (val) => {
    theme.value = val
    applyTheme(val)
  }

  const clearUserInfo = () => {
    name.value = '访客'
    target.value = '请先登录'
    imgSrc.value = 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix'
    studentId.value = null
    courseId.value = null
    token.value = ''
    // 主题不随清空重置
  }

  return {
    name,
    target,
    imgSrc,
    studentId,
    courseId,
    token,
    theme,
    setUserInfo,
    setTheme,
    clearUserInfo
  }
}, {
  persist: true
})
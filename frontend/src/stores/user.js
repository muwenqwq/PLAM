import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const name = ref('访客')
  const target = ref('请先登录')
  const imgSrc = ref('https://api.dicebear.com/7.x/notionists/svg?seed=Felix')
  const studentId = ref(null)
  const courseId = ref(null)
  const token = ref('')

  const setUserInfo = (info) => {
    name.value = info.name
    target.value = info.target
    imgSrc.value = info.imgSrc
    if (info.studentId !== undefined) studentId.value = info.studentId
    if (info.courseId !== undefined) courseId.value = info.courseId
    if (info.token) token.value = info.token
  }

  const clearUserInfo = () => {
    name.value = '访客'
    target.value = '请先登录'
    imgSrc.value = 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix'
    studentId.value = null
    courseId.value = null
    token.value = ''
  }

  return {
    name,
    target,
    imgSrc,
    studentId,
    courseId,
    token,
    setUserInfo,
    clearUserInfo
  }
}, {
  persist: true
})
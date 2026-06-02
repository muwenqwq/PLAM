import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 1. 定义状态 (State) - 相当于组件里的 ref
  const name = ref('访客')
  const target = ref('请先登录')
  const imgSrc = ref('https://api.dicebear.com/7.x/notionists/svg?seed=Felix')

  // 2. 定义动作 (Actions) - 用于修改数据的函数
  const setUserInfo = (info) => {
    name.value = info.name
    target.value = info.target
    imgSrc.value = info.imgSrc
  }

  // 退出登录时清空数据
  const clearUserInfo = () => {
    name.value = '访客'
    target.value = '请先登录'
    imgSrc.value = 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix'
  }

  // 3. 必须 return 出去，组件里才能用到
  return {
    name,
    target,
    imgSrc,
    setUserInfo,
    clearUserInfo
  }
}, {
  persist: true 
})
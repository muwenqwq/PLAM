import { defineStore } from 'pinia'
import { currentUser, login, logoutApi } from '@/api/auth'
import { clearToken, getStoredUser, getToken, setStoredUser, setToken } from '@/utils/storage'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getStoredUser<any>() || null
  }),
  getters: {
    isLogin: (state) => Boolean(state.token)
  },
  actions: {
    async loginByPassword(username: string, password: string) {
      const data = await login({ username, password })
      this.token = data.accessToken
      this.user = data.user
      setToken(data.accessToken)
      setStoredUser(data.user)
      return data
    },
    async loadUser() {
      if (!this.token) return null
      const user = await currentUser()
      this.user = user
      setStoredUser(user)
      return user
    },
    async logout() {
      try {
        if (this.token) await logoutApi()
      } finally {
        this.token = ''
        this.user = null
        clearToken()
      }
    }
  }
})

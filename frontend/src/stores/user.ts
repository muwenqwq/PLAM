import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    spaces: [] as any[],
    providers: [] as any[]
  }),
  actions: {
    setSpaces(spaces: any[]) {
      this.spaces = spaces
    },
    setProviders(providers: any[]) {
      this.providers = providers
    }
  }
})

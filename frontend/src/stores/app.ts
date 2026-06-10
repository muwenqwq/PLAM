import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    currentSpaceId: Number(localStorage.getItem('eduagent_space_id') || 1)
  }),
  actions: {
    setSpace(id: number) {
      this.currentSpaceId = id
      localStorage.setItem('eduagent_space_id', String(id))
    },
    toggleMenu() {
      this.collapsed = !this.collapsed
    }
  }
})

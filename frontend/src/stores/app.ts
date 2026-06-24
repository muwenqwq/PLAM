import { defineStore } from 'pinia'
import { getStorageItem, setStorageItem } from '@/utils/storage'

export type AppTheme = 'light' | 'dark'

export function readStoredTheme(): AppTheme {
  return getStorageItem('eduagent_theme') === 'dark' ? 'dark' : 'light'
}

export function applyTheme(theme: AppTheme) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = theme
  document.documentElement.style.colorScheme = theme
}

function readStoredSpaceId() {
  const value = Number(getStorageItem('eduagent_space_id'))
  return Number.isFinite(value) && value > 0 ? value : 1
}

export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: false,
    mobileMenuOpen: false,
    theme: readStoredTheme(),
    currentSpaceId: readStoredSpaceId()
  }),
  actions: {
    initializeTheme() {
      applyTheme(this.theme)
    },
    setTheme(theme: AppTheme) {
      this.theme = theme
      setStorageItem('eduagent_theme', theme)
      applyTheme(theme)
    },
    toggleTheme() {
      this.setTheme(this.theme === 'light' ? 'dark' : 'light')
    },
    setSpace(id: number) {
      this.currentSpaceId = id
      setStorageItem('eduagent_space_id', String(id))
    },
    toggleMenu() {
      this.collapsed = !this.collapsed
    },
    toggleMobileMenu() {
      this.mobileMenuOpen = !this.mobileMenuOpen
    },
    closeMobileMenu() {
      this.mobileMenuOpen = false
    }
  }
})
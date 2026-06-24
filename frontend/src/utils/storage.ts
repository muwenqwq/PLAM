const TOKEN_KEY = 'eduagent_token'
const USER_KEY = 'eduagent_user'

export function getStorageItem(key: string) {
  if (typeof window === 'undefined') return ''
  try {
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

export function setStorageItem(key: string, value: string) {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(key, value)
  } catch {
    // Restricted storage should not prevent the app from running.
  }
}

export function removeStorageItem(key: string) {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(key)
  } catch {
    // Restricted storage should not prevent logout or startup.
  }
}

export function getToken() {
  return getStorageItem(TOKEN_KEY)
}

export function setToken(token: string) {
  setStorageItem(TOKEN_KEY, token)
}

export function clearToken() {
  removeStorageItem(TOKEN_KEY)
  removeStorageItem(USER_KEY)
}

export function setStoredUser(user: unknown) {
  setStorageItem(USER_KEY, JSON.stringify(user || {}))
}

export function getStoredUser<T>() {
  const raw = getStorageItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    removeStorageItem(USER_KEY)
    return null
  }
}
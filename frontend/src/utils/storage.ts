const TOKEN_KEY = 'eduagent_token'
const USER_KEY = 'eduagent_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function setStoredUser(user: unknown) {
  localStorage.setItem(USER_KEY, JSON.stringify(user || {}))
}

export function getStoredUser<T>() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) as T : null
}

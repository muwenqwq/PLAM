import request from './request'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  nickname: string
  email?: string
  password: string
}

export function login(data: LoginPayload) {
  return request.post<any, any>('/auth/login', data)
}

export function register(data: RegisterPayload) {
  return request.post<any, any>('/auth/register', data)
}

export function currentUser() {
  return request.get<any, any>('/auth/me')
}

export function logoutApi() {
  return request.post<any, any>('/auth/logout')
}

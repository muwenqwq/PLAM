import request from './request'

export function getProfile() {
  return request.get<any, any>('/profiles/me')
}

export function saveProfile(data: any) {
  return request.put<any, any>('/profiles/me', data)
}

export function getPreference() {
  return request.get<any, any>('/preferences/me')
}

export function savePreference(data: any) {
  return request.put<any, any>('/preferences/me', data)
}

import request from './request'

export function getProfile() {
  return request.get<any, any>('/profiles/me')
}

export function saveProfile(data: any) {
  return request.put<any, any>('/profiles/me', data)
}

export function getSpaceProfile(spaceId: number) {
  return request.get<any, any>(`/profiles/space/${spaceId}`)
}

export function saveSpaceProfile(spaceId: number, data: any) {
  return request.put<any, any>(`/profiles/space/${spaceId}`, data)
}

export function analyzeSpaceProfile(spaceId: number) {
  return request.post<any, any>(`/profiles/space/${spaceId}/analyze`)
}

export function getPreference() {
  return request.get<any, any>('/preferences/me')
}

export function savePreference(data: any) {
  return request.put<any, any>('/preferences/me', data)
}

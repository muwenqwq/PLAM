import request, { PageResult } from './request'

export function listSpaces(params: any = {}) {
  return request.get<any, PageResult<any>>('/learning-spaces', { params })
}

export function createSpace(data: any) {
  return request.post<any, any>('/learning-spaces', data)
}

export function updateSpace(id: number, data: any) {
  return request.put<any, any>(`/learning-spaces/${id}`, data)
}

export function deleteSpace(id: number) {
  return request.delete<any, void>(`/learning-spaces/${id}`)
}

export function setDefaultSpace(id: number) {
  return request.post<any, any>(`/learning-spaces/${id}/default`)
}

export function getDefaultSpace() {
  return request.get<any, any>('/learning-spaces/default')
}

export function getSpaceSummary(id: number) {
  return request.get<any, any>(`/learning-spaces/${id}/summary`)
}

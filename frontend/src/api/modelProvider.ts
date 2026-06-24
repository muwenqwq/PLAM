import request, { PageResult } from './request'

export function listProviders(params: any = {}) {
  return request.get<any, PageResult<any>>('/model-providers', { params })
}

export function createProvider(data: any) {
  return request.post<any, any>('/model-providers', data)
}

export function updateProvider(id: number, data: any) {
  return request.put<any, any>(`/model-providers/${id}`, data)
}

export function deleteProvider(id: number) {
  return request.delete<any, void>(`/model-providers/${id}`)
}

export function setDefaultProvider(id: number) {
  return request.post<any, any>(`/model-providers/${id}/default`)
}

export function testProvider(id: number) {
  return request.post<any, any>(`/model-providers/${id}/test`, {})
}

export function getDefaultProvider() {
  return request.get<any, any>('/model-providers/default')
}

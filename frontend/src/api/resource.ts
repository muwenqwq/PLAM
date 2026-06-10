import request, { PageResult } from './request'

export function generateResource(data: any) {
  return request.post<any, any>('/resources/generate', data)
}

export function listResources(params: any = {}) {
  return request.get<any, PageResult<any>>('/resources', { params })
}

export function updateResource(id: number, data: any) {
  return request.put<any, any>(`/resources/${id}`, data)
}

export function deleteResource(id: number) {
  return request.delete<any, void>(`/resources/${id}`)
}

export function exportMarkdown(id: number) {
  return request.post<any, string>(`/resources/${id}/export/markdown`)
}

export function generateGraphFromResource(id: number) {
  return request.post<any, any>(`/resources/${id}/graph`)
}

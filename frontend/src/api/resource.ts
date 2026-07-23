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

export function downloadResourceFile(id: number, format: 'docx' | 'pdf' | 'png' | 'md' = 'docx') {
  return request.get<any, Blob>(`/resources/${id}/download`, {
    params: { format },
    responseType: 'blob'
  })
}

export function downloadResourceMarkdown(id: number) {
  return downloadResourceFile(id, 'md')
}

export function getResource(id: number) {
  return request.get<any, any>(`/resources/${id}`)
}

export function generateGraphFromResource(id: number) {
  return request.post<any, any>(`/resources/${id}/graph`)
}

import request, { PageResult } from './request'

export function createKnowledgeFile(data: any) {
  return request.post<any, any>('/knowledge/files', data)
}

export function uploadKnowledgeFile(spaceId: number, file: File) {
  const formData = new FormData()
  formData.append('spaceId', String(spaceId))
  formData.append('file', file)
  return request.post<any, any>('/knowledge/files/upload', formData)
}

export function listKnowledgeFiles(params: any = {}) {
  return request.get<any, PageResult<any>>('/knowledge/files', { params })
}

export function indexKnowledgeFile(id: number, data: any) {
  return request.post<any, any>(`/knowledge/files/${id}/index`, data)
}

export function searchKnowledge(data: any) {
  return request.post<any, any>('/knowledge/search', data)
}

export function getKnowledgeFile(id: number) {
  return request.get<any, any>(`/knowledge/files/${id}`)
}

export function deleteKnowledgeFile(id: number) {
  return request.delete<any, void>(`/knowledge/files/${id}`)
}

export function qaKnowledge(data: any) {
  return request.post<any, any>('/knowledge/qa', data)
}

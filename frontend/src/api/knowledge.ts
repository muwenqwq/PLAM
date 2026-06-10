import request, { PageResult } from './request'

export function createKnowledgeFile(data: any) {
  return request.post<any, any>('/knowledge/files', data)
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

export function qaKnowledge(data: any) {
  return request.post<any, any>('/knowledge/qa', data)
}

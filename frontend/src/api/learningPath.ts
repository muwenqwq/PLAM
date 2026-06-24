import request, { PageResult } from './request'

export function generateLearningPath(data: any) {
  return request.post<any, any>('/learning-paths/generate', data)
}

export function listLearningPaths(params: any = {}) {
  return request.get<any, PageResult<any>>('/learning-paths', { params })
}

export function getLearningPath(id: number) {
  return request.get<any, any>(`/learning-paths/${id}`)
}

export function updatePathItemStatus(id: number, status: string) {
  return request.put<any, any>(`/learning-path-items/${id}/status`, { status })
}

export function todayTasks() {
  return request.get<any, any[]>('/learning-paths/today')
}

export function adjustLearningPath(id: number) {
  return request.post<any, any>(`/learning-paths/${id}/adjust`)
}

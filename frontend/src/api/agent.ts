import request, { PageResult } from './request'

export function createAgentTask(data: any) {
  return request.post<any, any>('/agent-tasks', data)
}

export function listAgentTasks(params: any = {}) {
  return request.get<any, PageResult<any>>('/agent-tasks', { params })
}

export function getAgentSteps(id: number) {
  return request.get<any, any[]>(`/agent-tasks/${id}/steps`)
}

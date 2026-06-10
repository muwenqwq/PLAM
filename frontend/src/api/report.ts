import request from './request'

export function getOverview() {
  return request.get<any, any>('/reports/overview')
}

export function listReportsBySpace(spaceId: number) {
  return request.get<any, any[]>(`/reports/space/${spaceId}`)
}

export function generateReport(data: any) {
  return request.post<any, any>('/reports/generate', data)
}

export function getReport(id: number) {
  return request.get<any, any>(`/reports/${id}`)
}

export function exportReport(id: number) {
  return request.post<any, string>(`/reports/${id}/export`)
}

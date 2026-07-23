import request from './request'

export function getOverview(spaceId?: number | null) {
  return request.get<any, any>('/reports/overview', { params: { spaceId } })
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

export function deleteReport(id: number) {
  return request.delete<any, void>(`/reports/${id}`)
}

export function exportReport(id: number) {
  return request.post<any, string>(`/reports/${id}/export`)
}

export function downloadReportFile(id: number, format: 'docx' | 'pdf' | 'png' | 'md' = 'docx') {
  return request.get<any, Blob>(`/reports/${id}/download`, {
    params: { format },
    responseType: 'blob'
  })
}

export function downloadReportMarkdown(id: number) {
  return downloadReportFile(id, 'md')
}

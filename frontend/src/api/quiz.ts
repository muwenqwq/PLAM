import request, { PageResult } from './request'

export function generateQuiz(data: any) {
  return request.post<any, any>('/quizzes/generate', data)
}

export function listQuizzes(params: any = {}) {
  return request.get<any, PageResult<any>>('/quizzes', { params })
}

export function getQuiz(id: number) {
  return request.get<any, any>(`/quizzes/${id}`)
}

export function deleteQuiz(id: number) {
  return request.delete<any, void>(`/quizzes/${id}`)
}

export function submitQuiz(id: number, answers: any[], options: any = {}) {
  return request.post<any, any>(`/quizzes/${id}/submit`, { answers, ...options })
}

export function getQuizResult(id: number) {
  return request.get<any, any>(`/quizzes/${id}/result`)
}

export function getMastery(spaceId?: number | null) {
  return request.get<any, any[]>('/mastery/me', { params: { spaceId } })
}

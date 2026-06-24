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

export function submitQuiz(id: number, answers: any[]) {
  return request.post<any, any>(`/quizzes/${id}/submit`, { answers })
}

export function getMastery() {
  return request.get<any, any[]>('/mastery/me')
}

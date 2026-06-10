import request, { PageResult } from './request'

export function listConversations(params: any = {}) {
  return request.get<any, PageResult<any>>('/chat/conversations', { params })
}

export function createConversation(data: any) {
  return request.post<any, any>('/chat/conversations', data)
}

export function getMessages(id: number) {
  return request.get<any, any[]>(`/chat/conversations/${id}/messages`)
}

export function sendMessage(id: number, data: any) {
  return request.post<any, any>(`/chat/conversations/${id}/messages`, data)
}

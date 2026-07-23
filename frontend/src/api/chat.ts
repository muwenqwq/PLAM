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

export async function streamMessage(id: number, data: any, onEvent: (event: any) => void) {
  const { getToken } = await import('@/utils/storage')
  const token = getToken()
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${baseUrl}/chat/conversations/${id}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  })
  if (!response.ok || !response.body) {
    let message = `流式请求失败（${response.status}）`
    try {
      const errorBody = await response.json()
      message = errorBody?.message || message
    } catch {
      // Keep the status-based message when the response is not JSON.
    }
    throw new Error(message)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop() || ''
    for (const frame of frames) {
      const line = frame.split('\n').find((item) => item.startsWith('data:'))
      if (!line) continue
      const event = JSON.parse(line.slice(5).trim())
      if (event.type === 'error') {
        throw new Error(event.message || 'AI 流式生成失败')
      }
      onEvent(event)
    }
  }
}


export function applyConversationRole(id: number, data: any) {
  return request.post<any, any>(`/chat/conversations/${id}/role`, data)
}

import request from './request'

/**
 * 智能辅导提问
 * POST /api/tutor/ask
 */
export function askTutor(data) {
  return request.post('/tutor/ask', data)
}

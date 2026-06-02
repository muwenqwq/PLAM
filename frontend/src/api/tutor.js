import request from './request'
import { mockTutorResponse } from './mock'

/**
 * 智能辅导提问
 * POST /api/tutor/ask
 */
export async function askTutor(data) {
  try {
    return await request.post('/tutor/ask', data)
  } catch {
    await new Promise(r => setTimeout(r, 2000))
    return mockTutorResponse
  }
}

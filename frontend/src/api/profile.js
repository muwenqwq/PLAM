import request from './request'
import { mockProfile, mockProfileHistory } from './mock'

/**
 * 生成学生画像
 * POST /api/profile/extract
 */
export async function extractProfile(data) {
  try {
    return await request.post('/profile/extract', data)
  } catch {
    // 后端未就绪时返回 mock 数据
    await new Promise(r => setTimeout(r, 1000))
    return mockProfile
  }
}

/**
 * 查询学生最新画像
 * GET /api/profile/:studentId
 */
export async function getLatestProfile(studentId) {
  try {
    return await request.get(`/profile/${studentId}`)
  } catch {
    return mockProfile
  }
}

/**
 * 查询画像历史版本
 * GET /api/profile/:studentId/history
 */
export async function getProfileHistory(studentId) {
  try {
    return await request.get(`/profile/${studentId}/history`)
  } catch {
    return mockProfileHistory
  }
}

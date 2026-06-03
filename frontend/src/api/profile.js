import request from './request'

/**
 * 生成学生画像
 * POST /api/profile/extract
 */
export function extractProfile(data) {
  return request.post('/profile/extract', data)
}

/**
 * 查询学生最新画像
 * GET /api/profile/:studentId
 */
export function getLatestProfile(studentId) {
  return request.get(`/profile/${studentId}`)
}

/**
 * 查询画像历史版本
 * GET /api/profile/:studentId/history
 */
export function getProfileHistory(studentId) {
  return request.get(`/profile/${studentId}/history`)
}

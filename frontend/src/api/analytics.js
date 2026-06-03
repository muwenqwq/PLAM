import request from './request'

/**
 * 提交练习结果
 * POST /api/assessment/submit
 */
export function submitAssessment(data) {
  return request.post('/assessment/submit', data)
}

/**
 * 查询学习评估数据
 * GET /api/analytics/:studentId
 */
export function getAnalytics(studentId) {
  return request.get(`/analytics/${studentId}`)
}

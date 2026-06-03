import request from './request'

/**
 * 生成学习路径
 * POST /api/study-plan/generate
 */
export function generateStudyPlan(data) {
  return request.post('/study-plan/generate', data)
}

/**
 * 查询学习路径
 * GET /api/study-plan/:studentId
 */
export function getStudyPlan(studentId) {
  return request.get(`/study-plan/${studentId}`)
}

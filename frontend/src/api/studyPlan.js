import request from './request'
import { mockStudyPlan } from './mock'

/**
 * 生成学习路径
 * POST /api/study-plan/generate
 */
export async function generateStudyPlan(data) {
  try {
    return await request.post('/study-plan/generate', data)
  } catch {
    await new Promise(r => setTimeout(r, 1200))
    return mockStudyPlan
  }
}

/**
 * 查询学习路径
 * GET /api/study-plan/:studentId
 */
export async function getStudyPlan(studentId) {
  try {
    return await request.get(`/study-plan/${studentId}`)
  } catch {
    return mockStudyPlan
  }
}

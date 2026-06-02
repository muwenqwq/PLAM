import request from './request'
import { mockAnalytics } from './mock'

/**
 * 提交练习结果
 * POST /api/assessment/submit
 */
export async function submitAssessment(data) {
  try {
    return await request.post('/assessment/submit', data)
  } catch {
    await new Promise(r => setTimeout(r, 800))
    return { score: 80, totalScore: 100, masteryUpdate: { 'A*算法': 0.5 } }
  }
}

/**
 * 查询学习评估数据
 * GET /api/analytics/:studentId
 */
export async function getAnalytics(studentId) {
  try {
    return await request.get(`/analytics/${studentId}`)
  } catch {
    return mockAnalytics
  }
}

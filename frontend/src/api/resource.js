import request from './request'
import { mockResources } from './mock'

/**
 * 生成个性化资源
 * POST /api/resources/generate
 */
export async function generateResources(data) {
  try {
    return await request.post('/resources/generate', data)
  } catch {
    await new Promise(r => setTimeout(r, 1500))
    return { taskId: 'task_mock_001', resourceIds: mockResources.map(r => r.id), status: 'success' }
  }
}

/**
 * 查询学生资源列表
 * GET /api/resources/:studentId
 */
export async function getResourceList(studentId) {
  try {
    return await request.get(`/resources/${studentId}`)
  } catch {
    return mockResources
  }
}

/**
 * 查询资源详情
 * GET /api/resources/detail/:resourceId
 */
export async function getResourceDetail(resourceId) {
  try {
    return await request.get(`/resources/detail/${resourceId}`)
  } catch {
    return mockResources.find(r => r.id === resourceId) || mockResources[0]
  }
}

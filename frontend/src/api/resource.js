import request from './request'

/**
 * 生成个性化资源
 * POST /api/resources/generate
 */
export function generateResources(data) {
  return request.post('/resources/generate', data)
}

/**
 * 查询学生资源列表
 * GET /api/resources/:studentId
 */
export function getResourceList(studentId) {
  return request.get(`/resources/${studentId}`)
}

/**
 * 查询资源详情
 * GET /api/resources/detail/:resourceId
 */
export function getResourceDetail(resourceId) {
  return request.get(`/resources/detail/${resourceId}`)
}

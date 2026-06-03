import request from './request'

/**
 * 查询 Agent 运行轨迹
 * GET /api/agent-runs/:taskId
 */
export function getAgentRuns(taskId) {
  return request.get(`/agent-runs/${taskId}`)
}

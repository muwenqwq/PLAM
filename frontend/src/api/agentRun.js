import request from './request'
import { mockAgentRuns } from './mock'

/**
 * 查询 Agent 运行轨迹
 * GET /api/agent-runs/:taskId
 */
export async function getAgentRuns(taskId) {
  try {
    return await request.get(`/agent-runs/${taskId}`)
  } catch {
    return mockAgentRuns
  }
}

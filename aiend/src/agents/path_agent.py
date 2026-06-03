"""PathAgent——规划学习路径"""

import json
import time
from src.llm_gateway.client import chat_json

SYSTEM_PROMPT = """你是学习路径规划专家。根据学生画像、知识点依赖和可用资源，生成学习路径节点。返回 JSON：
{
  "nodes": [
    {
      "order": 1,
      "knowledgePointId": 1,
      "knowledgePoint": "知识点名",
      "recommendedResourceIds": [1, 2],
      "estimatedMinutes": 30,
      "reason": "推荐理由",
      "completionCriteria": "完成标准"
    }
  ]
}
每个学生至少生成 3 个节点。"""


def run(task_id: str, profile: dict, knowledge_points: list,
        dependencies: list, resources: list) -> dict:
    start = time.time()

    user_msg = f"""学生画像：{json.dumps(profile, ensure_ascii=False)[:1000]}
知识点列表：{json.dumps(knowledge_points, ensure_ascii=False)[:2000]}
依赖关系：{json.dumps(dependencies, ensure_ascii=False)[:1000]}
可用资源：{json.dumps(resources, ensure_ascii=False)[:1000]}"""

    result_text = chat_json(SYSTEM_PROMPT, user_msg)

    try:
        result = json.loads(result_text)
    except json.JSONDecodeError:
        result = {
            "nodes": [
                {"order": 1, "knowledgePointId": 1, "knowledgePoint": "基础知识",
                 "recommendedResourceIds": [], "estimatedMinutes": 30,
                 "reason": "先建立基础", "completionCriteria": "理解概念"}
            ]
        }

    trace = {
        "agentName": "PathAgent",
        "status": "success",
        "outputSummary": f"生成 {len(result.get('nodes', []))} 个学习节点",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }
    return {"nodes": result["nodes"], "agentTrace": [trace]}

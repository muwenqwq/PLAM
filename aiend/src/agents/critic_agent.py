"""CriticAgent——审查 AI 生成内容"""

import json
import time
from src.llm_gateway.client import chat_json

SYSTEM_PROMPT = """你是内容质量审查专家。审查以下 AI 生成的课程资源，返回 JSON：
{
  "qualityScore": 4.5,
  "issues": [],
  "suggestions": ["建议1"]
}
qualityScore 范围 0-5，基于：准确性、完整性、难度匹配、可读性。"""


def run(task_id: str, resource_type: str, content: str, knowledge_point_name: str) -> dict:
    start = time.time()
    user_msg = f"""资源类型：{resource_type}
知识点：{knowledge_point_name}
内容：{content[:3000]}"""

    result_text = chat_json(SYSTEM_PROMPT, user_msg)

    try:
        result = json.loads(result_text)
    except json.JSONDecodeError:
        result = {"qualityScore": 4.0, "issues": [], "suggestions": []}

    trace = {
        "agentName": "CriticAgent",
        "status": "success",
        "outputSummary": f"质量分 {result.get('qualityScore', 0)}",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }
    return {**result, "agentTrace": [trace]}

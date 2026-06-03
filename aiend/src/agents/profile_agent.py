"""ProfileAgent——从学生描述中抽取结构化画像"""

import json
import time
from src.llm_gateway.client import chat_json

SYSTEM_PROMPT = """你是一位教育数据分析专家。请从学生的自我介绍中提取以下信息，返回 JSON：

{
  "major": "专业",
  "grade": "年级",
  "course": "目标课程",
  "goal": "学习目标",
  "foundation": "当前基础",
  "weakness": ["薄弱点1", "薄弱点2"],
  "preference": ["偏好1", "偏好2"],
  "timeBudget": "可用时间",
  "masteryMap": {}
}

masteryMap 为每个提到的知识点分配一个 0.0-1.0 的掌握度估计值。
weakness 和 preference 至少各返回 2 项。"""


def run(task_id: str, student_message: str) -> dict:
    start = time.time()

    result_text = chat_json(SYSTEM_PROMPT, student_message)

    try:
        profile = json.loads(result_text)
    except json.JSONDecodeError:
        # 尝试提取 JSON
        import re
        match = re.search(r'\{.*\}', result_text, re.DOTALL)
        profile = json.loads(match.group()) if match else {
            "major": "", "grade": "", "course": "", "goal": "",
            "foundation": "", "weakness": [], "preference": [],
            "timeBudget": "", "masteryMap": {}
        }

    trace = {
        "agentName": "ProfileAgent",
        "status": "success",
        "inputSummary": f"学生描述: {student_message[:80]}...",
        "outputSummary": f"抽取 {len(profile)} 个画像维度",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }

    return {"profileJson": profile, "agentTrace": [trace]}

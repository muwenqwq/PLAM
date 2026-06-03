"""QuizAgent——生成练习题 / 分析练习结果"""

import json
import time
from src.llm_gateway.client import chat_json

QUIZ_SYSTEM = """你是课程练习出题专家。请生成分层练习题，返回 JSON：
{
  "questions": [
    {"id": 1, "type": "single_choice", "question": "题目", "options": ["A","B","C","D"], "answer": "B", "explanation": "解析"}
  ]
}"""

ANALYZE_SYSTEM = """你是学习评估专家。根据学生答案分析掌握度变化，返回 JSON：
{
  "score": 80,
  "totalScore": 100,
  "masteryDelta": {"知识点A": 0.15},
  "weakPointsIdentified": ["薄弱点"],
  "details": [{"questionId": 1, "correct": true, "explanation": "..."}]
}"""


def run_quiz(task_id: str, knowledge_point_name: str, profile: dict) -> dict:
    start = time.time()
    user_msg = f"知识点：{knowledge_point_name}\n难度：适合掌握度 0.3-0.7 的学生\n生成 5 道题。"
    result_text = chat_json(QUIZ_SYSTEM, user_msg)

    try:
        quiz = json.loads(result_text)
    except json.JSONDecodeError:
        quiz = {"questions": []}

    trace = {
        "agentName": "QuizAgent",
        "status": "success",
        "outputSummary": f"生成 {len(quiz.get('questions', []))} 道题",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }
    return {"content": json.dumps(quiz, ensure_ascii=False), "agentTrace": [trace]}


def run_analyze(task_id: str, answers: list, current_mastery: dict) -> dict:
    start = time.time()
    user_msg = f"学生答案：{json.dumps(answers, ensure_ascii=False)}\n当前掌握度：{json.dumps(current_mastery, ensure_ascii=False)}"
    result_text = chat_json(ANALYZE_SYSTEM, user_msg)

    try:
        result = json.loads(result_text)
    except json.JSONDecodeError:
        correct = sum(1 for a in answers if a.get("correct"))
        result = {
            "score": round(correct / max(len(answers), 1) * 100, 1),
            "totalScore": 100,
            "masteryDelta": {},
            "weakPointsIdentified": [],
            "details": answers,
        }

    trace = {
        "agentName": "QuizAgent",
        "status": "success",
        "outputSummary": f"分析 {len(answers)} 道题",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }
    return {
        "score": result["score"],
        "totalScore": result["totalScore"],
        "masteryDelta": result.get("masteryDelta", {}),
        "details": result.get("details", []),
        "weakPointsIdentified": result.get("weakPointsIdentified", []),
        "agentTrace": [trace],
    }

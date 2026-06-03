"""CodeAgent——生成代码实操案例"""

import time
from src.llm_gateway.client import chat

SYSTEM_PROMPT = """你是一位编程教学专家。请根据课程知识点和学生基础，生成一个可执行的代码实验。
包含：实验目标、代码模板（留关键TODO）、运行说明。使用 Markdown 格式。"""


def run(task_id: str, knowledge_point_name: str, profile: dict, chunks: list[dict]) -> dict:
    start = time.time()
    chunk_text = "\n\n".join([c.get("document", "") for c in chunks[:2]])

    user_msg = f"""知识点：{knowledge_point_name}
学生基础：{profile.get('foundation', '')}
课程资料：{chunk_text[:2000]}
请生成一个 {knowledge_point_name} 的 Python 代码实验，难度适合该学生。"""

    content = chat(SYSTEM_PROMPT, user_msg)

    trace = {
        "agentName": "CodeAgent",
        "status": "success",
        "outputSummary": f"生成代码实验 {len(content)} 字符",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }
    return {"content": content, "agentTrace": [trace]}

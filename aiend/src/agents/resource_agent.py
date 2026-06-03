"""ResourceAgent——生成个性化讲义（explanation_doc）"""

import json
import time
from src.llm_gateway.client import chat

SYSTEM_PROMPT = """你是一位大学课程助教，擅长编写符合学生认知水平的个性化讲义。
根据提供的课程资料和学生画像，生成一份结构清晰、适合该学生当前水平的讲义。

使用 Markdown 格式，包含：
1. 标题和概述
2. 核心概念（用学生能理解的语言）
3. 关键公式和例子
4. 常见误区提醒
5. 小结和下一步学习建议"""


def run(task_id: str, resource_type: str, knowledge_point_name: str,
        profile: dict, chunks: list[dict]) -> dict:
    start = time.time()

    # 构建上下文
    chunk_text = "\n\n".join([c.get("document", "") for c in chunks[:3]])
    user_msg = f"""知识点：{knowledge_point_name}
资源类型：{resource_type}

学生画像：
- 专业：{profile.get('major', '')}
- 基础：{profile.get('foundation', '')}
- 偏好：{', '.join(profile.get('preference', []))}
- 薄弱点：{', '.join(profile.get('weakness', []))}

课程参考资料：
{chunk_text[:4000]}

请生成适合该学生的{knowledge_point_name}个性化讲义。"""

    content = chat(SYSTEM_PROMPT, user_msg)

    trace = {
        "agentName": "ResourceAgent",
        "status": "success",
        "inputSummary": f"生成 {resource_type}: {knowledge_point_name}",
        "outputSummary": f"生成内容 {len(content)} 字符",
        "modelName": "LLM",
        "latencyMs": int((time.time() - start) * 1000),
    }

    return {"content": content, "agentTrace": [trace]}

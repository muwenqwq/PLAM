from fastapi import APIRouter
from src.schemas.tutor import TutorAskRequest, TutorAskResponse
from src.agents import retriever_agent
from src.llm_gateway.client import chat
import time

router = APIRouter(prefix="/ai/tutor")

SYSTEM_PROMPT = """你是一位大学课程助教。根据提供的课程参考数据回答学生问题。

使用 Markdown 格式，包含：
1. 直接回答
2. 解释或例子
3. 如果有课程资料支持，请引用
4. 推荐下一步学习的方向

如果课程资料不足以回答，请注明"课程依据不足，建议教师确认"。"""


@router.post("/ask", response_model=TutorAskResponse)
def ask_tutor(req: TutorAskRequest):
    task_id = req.taskId
    trace_all = []

    # 1. RAG 检索
    ret_result = retriever_agent.run(task_id, req.question)
    chunks = ret_result["chunks"]
    trace_all.extend(ret_result["agentTrace"])

    # 2. 构建上下文
    chunk_text = "\n\n".join([c.get("document", "") for c in chunks[:3]])
    profile_str = f"学生专业：{req.profileJson.get('major', '')}，年级：{req.profileJson.get('grade', '')}"

    user_msg = f"""学生画像：{profile_str}
课程参考资料：{chunk_text[:3000] if chunk_text else '无相关课程资料'}
学生问题：{req.question}"""

    # 3. 生成回答
    start = time.time()
    answer = chat(SYSTEM_PROMPT, user_msg)
    latency = int((time.time() - start) * 1000)

    # 4. 来源
    sources = []
    for c in chunks[:3]:
        meta = c.get("metadata", {})
        sources.append({
            "file": meta.get("source_file", meta.get("sourceFile", "")),
            "section": meta.get("title", ""),
        })

    trace_all.append({
        "agentName": "TutorAgent",
        "status": "success",
        "outputSummary": f"生成回答 {len(answer)} 字符，引用 {len(sources)} 处来源",
        "modelName": "LLM",
        "latencyMs": latency,
    })

    return TutorAskResponse(
        answer=answer,
        sources=sources,
        suggestedResourceIds=[],
        safetyStatus="passed",
        agentTrace=trace_all,
    )

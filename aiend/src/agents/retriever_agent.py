"""RetrieverAgent——课程知识库语义检索"""

import time
from src.rag.chroma_store import search


def run(task_id: str, query: str, collection_name: str = "course_docs", top_k: int = 5) -> dict:
    start = time.time()
    chunks = search(collection_name, query, top_k)

    trace = {
        "agentName": "RetrieverAgent",
        "status": "success",
        "inputSummary": f"检索: {query[:80]}",
        "outputSummary": f"检索到 {len(chunks)} 个相关文档片段",
        "modelName": "Chroma",
        "latencyMs": int((time.time() - start) * 1000),
    }

    return {"chunks": chunks, "agentTrace": [trace]}

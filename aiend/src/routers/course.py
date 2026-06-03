from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
from src.rag.chroma_store import add_documents, delete_collection
from src.rag.chunker import chunk_markdown

router = APIRouter(prefix="/ai/course")


class IngestRequest(BaseModel):
    courseId: int
    files: List[dict]


@router.post("/ingest")
def ingest_course(req: IngestRequest):
    collection_name = f"course_{req.courseId}"

    # 清空旧数据
    delete_collection(collection_name)

    all_chunks = []
    for f in req.files:
        raw_chunks = chunk_markdown(f.get("content", ""))
        for c in raw_chunks:
            chunk_id = f"chunk_{req.courseId}_{c['index']}"
            all_chunks.append({
                "id": chunk_id,
                "document": c["content"],
                "metadata": {
                    "source_file": f.get("fileName", ""),
                    "title": f.get("fileName", ""),
                },
                "chunk_key": chunk_id,
                "sourceFile": f.get("fileName", ""),
                "knowledgePointId": None,
                "embeddingStatus": "completed",
            })

    if all_chunks:
        add_documents(
            collection_name,
            [c["id"] for c in all_chunks],
            [c["document"] for c in all_chunks],
            [c.get("metadata", {}) for c in all_chunks],
        )

    return {
        "chunksCreated": len(all_chunks),
        "chunks": [{
            "chunkKey": c["chunk_key"],
            "knowledgePointId": c["knowledgePointId"],
            "sourceFile": c["sourceFile"],
            "title": c["metadata"].get("title", ""),
            "content": c["document"][:200],
            "embeddingStatus": c["embeddingStatus"],
        } for c in all_chunks],
        "agentTrace": [],
    }

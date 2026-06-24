from app.rag.embedding import stable_hash
from app.schemas.rag import RagChunk


def chunk_text(file_id: int, file_name: str, text: str) -> list[RagChunk]:
    sentences = [item.strip() for item in text.replace("。", "。\n").splitlines() if item.strip()]
    if len(sentences) < 3:
        sentences.extend([
            "系统会根据资料自动生成可检索的知识片段。",
            "后续可以替换为真实文件解析、向量化和召回流程。",
            "每个片段都会保留来源、索引和 Mock embedding 引用。",
        ])
    chunks: list[RagChunk] = []
    for index, sentence in enumerate(sentences[:5], start=1):
        content = sentence if sentence.endswith("。") else f"{sentence}。"
        digest = stable_hash(f"{file_id}:{index}:{content}")
        chunks.append(
            RagChunk(
                chunk_index=index,
                content_text=content,
                content_hash=digest,
                token_count=max(8, len(content) // 2),
                metadata={"file_id": file_id, "file_name": file_name, "mock": True},
                embedding_ref=f"mock://embedding/{digest[:12]}",
                source=file_name,
            )
        )
    return chunks

from app.rag.chunker import chunk_text
from app.rag.embedding import similarity_score
from app.rag.parser import parse_text
from app.schemas.rag import RagChunk


def mock_search(query: str, file_ids: list[int], top_k: int) -> list[RagChunk]:
    ids = file_ids or [1, 2]
    chunks: list[RagChunk] = []
    for file_id in ids:
        text = parse_text(f"知识资料-{file_id}.md", f"{query} 的核心概念、应用场景、易错点和复习路径。")
        chunks.extend(chunk_text(file_id, f"知识资料-{file_id}.md", text))
    ranked = []
    for rank, chunk in enumerate(chunks, start=1):
        chunk.score = similarity_score(query, chunk.content_text, rank)
        ranked.append(chunk)
    return sorted(ranked, key=lambda item: item.score or 0, reverse=True)[: max(1, top_k)]

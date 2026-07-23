import json
import math
import os
import re
import threading
from pathlib import Path
from typing import Any

from app.rag.embedding import stable_hash
from app.schemas.rag import RagChunk, RagIndexRequest, RagSearchRequest

_DIMENSIONS = 128
_LOCK = threading.Lock()


def upsert_chunks(request: RagIndexRequest, chunks: list[RagChunk]) -> None:
    if not chunks:
        return
    _json_store().upsert(request, chunks)
    _chroma_store().upsert(request, chunks)


def search_chunks(request: RagSearchRequest) -> list[RagChunk]:
    chroma_results = _chroma_store().search(request)
    if chroma_results:
        return chroma_results
    return _json_store().search(request)


def delete_chunks(file_id: int) -> None:
    _json_store().delete(file_id)
    _chroma_store().delete(file_id)


class JsonVectorStore:
    def __init__(self) -> None:
        root = Path(os.getenv("RAG_VECTOR_DIR", ".rag_vector_store"))
        self.path = root / "chunks.json"

    def upsert(self, request: RagIndexRequest, chunks: list[RagChunk]) -> None:
        with _LOCK:
            records = self._load()
            records = [item for item in records if int(item.get("file_id", -1)) != int(request.file_id)]
            for chunk in chunks:
                metadata = dict(chunk.metadata or {})
                metadata.setdefault("file_id", request.file_id)
                metadata.setdefault("file_name", request.file_name)
                record = {
                    "id": f"file-{request.file_id}-chunk-{chunk.chunk_index}",
                    "file_id": int(request.file_id),
                    "file_name": request.file_name,
                    "chunk_index": int(chunk.chunk_index),
                    "content_text": chunk.content_text,
                    "content_hash": chunk.content_hash,
                    "token_count": int(chunk.token_count or 0),
                    "metadata": metadata,
                    "embedding_ref": chunk.embedding_ref,
                    "embedding": _embed(chunk.content_text),
                }
                records.append(record)
            self._save(records)

    def search(self, request: RagSearchRequest) -> list[RagChunk]:
        records = self._load()
        if not records:
            return []
        query_embedding = _embed(request.query)
        file_ids = {int(value) for value in request.file_ids or []}
        filters = request.filters or {}
        scored: list[tuple[float, dict[str, Any]]] = []
        for record in records:
            if file_ids and int(record.get("file_id", -1)) not in file_ids:
                continue
            if not _matches_filters(record.get("metadata") or {}, filters):
                continue
            score = _cosine(query_embedding, record.get("embedding") or [])
            if score <= 0:
                continue
            scored.append((score, record))
        scored.sort(key=lambda item: item[0], reverse=True)
        return [_record_to_chunk(record, score, "local_vector") for score, record in scored[: max(1, min(20, request.top_k or 5))]]

    def delete(self, file_id: int) -> None:
        with _LOCK:
            records = [item for item in self._load() if int(item.get("file_id", -1)) != int(file_id)]
            self._save(records)

    def _load(self) -> list[dict[str, Any]]:
        if not self.path.exists():
            return []
        try:
            return json.loads(self.path.read_text(encoding="utf-8"))
        except Exception:  # noqa: BLE001 - Corrupt local vector cache should not break the app.
            return []

    def _save(self, records: list[dict[str, Any]]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.path.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")


class ChromaVectorStore:
    def __init__(self) -> None:
        self.enabled = os.getenv("RAG_VECTOR_BACKEND", "").lower() == "chroma"
        self._collection: Any | None = None

    def _collection_or_none(self) -> Any | None:
        if not self.enabled:
            return None
        if self._collection is not None:
            return self._collection
        try:
            import chromadb  # type: ignore

            root = os.getenv("CHROMA_PERSIST_DIR") or os.getenv("RAG_VECTOR_DIR") or ".rag_vector_store/chroma"
            client = chromadb.PersistentClient(path=root)
            self._collection = client.get_or_create_collection("plam_knowledge_chunks")
            return self._collection
        except Exception:  # noqa: BLE001 - Chroma is optional; JSON vector store remains available.
            self.enabled = False
            return None

    def upsert(self, request: RagIndexRequest, chunks: list[RagChunk]) -> None:
        collection = self._collection_or_none()
        if collection is None or not chunks:
            return
        ids = [f"file-{request.file_id}-chunk-{chunk.chunk_index}" for chunk in chunks]
        metadatas = []
        for chunk in chunks:
            metadata = _flatten_metadata(chunk.metadata or {})
            metadata["file_id"] = int(request.file_id)
            metadata["file_name"] = request.file_name
            metadata["chunk_index"] = int(chunk.chunk_index)
            metadata["content_hash"] = chunk.content_hash
            metadata["token_count"] = int(chunk.token_count or 0)
            metadata["embedding_ref"] = chunk.embedding_ref
            metadatas.append(metadata)
        try:
            collection.upsert(
                ids=ids,
                documents=[chunk.content_text for chunk in chunks],
                embeddings=[_embed(chunk.content_text) for chunk in chunks],
                metadatas=metadatas,
            )
        except Exception:  # noqa: BLE001
            self.enabled = False

    def search(self, request: RagSearchRequest) -> list[RagChunk]:
        collection = self._collection_or_none()
        if collection is None:
            return []
        try:
            result = collection.query(
                query_embeddings=[_embed(request.query)],
                n_results=max(1, min(20, request.top_k or 5)),
                where=_chroma_where(request),
                include=["documents", "metadatas", "distances"],
            )
            documents = (result.get("documents") or [[]])[0]
            metadatas = (result.get("metadatas") or [[]])[0]
            distances = (result.get("distances") or [[]])[0]
            chunks: list[RagChunk] = []
            for index, document in enumerate(documents):
                metadata = dict(metadatas[index] or {})
                score = max(0.0, min(1.0, 1.0 - float(distances[index] if index < len(distances) else 0.0)))
                chunks.append(
                    RagChunk(
                        chunk_index=int(metadata.get("chunk_index") or index + 1),
                        content_text=document,
                        content_hash=str(metadata.get("content_hash") or stable_hash(document)),
                        token_count=int(metadata.get("token_count") or max(8, len(document) // 2)),
                        metadata=metadata,
                        embedding_ref=str(metadata.get("embedding_ref") or "chroma://embedding"),
                        score=round(score, 4),
                        source=str(metadata.get("file_name") or "知识片段"),
                        source_file_name=str(metadata.get("file_name") or "知识片段"),
                        retrieval_mode="chroma",
                    )
                )
            return chunks
        except Exception:  # noqa: BLE001
            self.enabled = False
            return []

    def delete(self, file_id: int) -> None:
        collection = self._collection_or_none()
        if collection is None:
            return
        try:
            collection.delete(where={"file_id": int(file_id)})
        except Exception:  # noqa: BLE001 - JSON fallback has already been cleaned.
            self.enabled = False


_json_instance: JsonVectorStore | None = None
_chroma_instance: ChromaVectorStore | None = None


def _json_store() -> JsonVectorStore:
    global _json_instance
    if _json_instance is None:
        _json_instance = JsonVectorStore()
    return _json_instance


def _chroma_store() -> ChromaVectorStore:
    global _chroma_instance
    if _chroma_instance is None:
        _chroma_instance = ChromaVectorStore()
    return _chroma_instance


def _record_to_chunk(record: dict[str, Any], score: float, retrieval_mode: str) -> RagChunk:
    metadata = dict(record.get("metadata") or {})
    metadata.setdefault("file_id", record.get("file_id"))
    metadata.setdefault("file_name", record.get("file_name"))
    return RagChunk(
        chunk_index=int(record.get("chunk_index") or 0),
        content_text=str(record.get("content_text") or ""),
        content_hash=str(record.get("content_hash") or stable_hash(str(record.get("content_text") or ""))),
        token_count=int(record.get("token_count") or 0),
        metadata=metadata,
        embedding_ref=str(record.get("embedding_ref") or "local://embedding"),
        score=round(score, 4),
        source=str(record.get("file_name") or "知识片段"),
        source_file_name=str(record.get("file_name") or "知识片段"),
        retrieval_mode=retrieval_mode,
    )


def _matches_filters(metadata: dict[str, Any], filters: dict[str, Any]) -> bool:
    for key, expected in filters.items():
        if expected is None or expected == "":
            continue
        actual = metadata.get(key)
        if isinstance(expected, list):
            expected_values = {str(item) for item in expected}
            if str(actual) not in expected_values:
                return False
        elif str(actual) != str(expected):
            return False
    return True


def _chroma_where(request: RagSearchRequest) -> dict[str, Any] | None:
    conditions: list[dict[str, Any]] = []
    if request.file_ids:
        conditions.append({"file_id": {"$in": [int(value) for value in request.file_ids]}})
    for key, value in (request.filters or {}).items():
        if value is None or value == "":
            continue
        conditions.append({key: value})
    if not conditions:
        return None
    if len(conditions) == 1:
        return conditions[0]
    return {"$and": conditions}


def _embed(text: str) -> list[float]:
    vector = [0.0] * _DIMENSIONS
    for token in _tokens(text):
        digest = stable_hash(token)
        bucket = int(digest[:8], 16) % _DIMENSIONS
        vector[bucket] += 1.0
    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector
    return [round(value / norm, 6) for value in vector]


def _tokens(text: str) -> list[str]:
    lower = (text or "").lower()
    tokens = re.findall(r"[a-z0-9_]{2,}", lower)
    cjk = re.sub(r"[^\u4e00-\u9fff]+", "", lower)
    tokens.extend(cjk[index : index + 2] for index in range(max(0, len(cjk) - 1)))
    if cjk:
        tokens.extend(cjk[index] for index in range(len(cjk)))
    return [token for token in tokens if token.strip()]


def _cosine(left: list[float], right: list[float]) -> float:
    if not left or not right:
        return 0.0
    return sum(a * b for a, b in zip(left, right))


def _flatten_metadata(metadata: dict[str, Any]) -> dict[str, str | int | float | bool]:
    flattened: dict[str, str | int | float | bool] = {}
    for key, value in metadata.items():
        if isinstance(value, (str, int, float, bool)):
            flattened[key] = value
        elif value is not None:
            flattened[key] = json.dumps(value, ensure_ascii=False)
    return flattened

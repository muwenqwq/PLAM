"""Chroma 向量库持久化管理"""

import chromadb
from chromadb.config import Settings as ChromaSettings
from src.config.settings import settings

# 全局持久化客户端
_client = None


def get_collection(collection_name: str = "course_docs"):
    """获取向量集合（持久化）"""
    global _client
    if _client is None:
        _client = chromadb.PersistentClient(
            path=settings.CHROMA_PERSIST_DIR,
            settings=ChromaSettings(anonymized_telemetry=False),
        )

    return _client.get_or_create_collection(
        name=collection_name,
        metadata={"hnsw:space": "cosine"},
    )


def add_documents(
    collection_name: str,
    ids: list[str],
    documents: list[str],
    metadatas: list[dict],
):
    """添加文档到向量库"""
    collection = get_collection(collection_name)
    collection.add(
        ids=ids,
        documents=documents,
        metadatas=metadatas,
    )


def search(collection_name: str, query: str, top_k: int = 5) -> list[dict]:
    """语义检索"""
    collection = get_collection(collection_name)
    results = collection.query(
        query_texts=[query],
        n_results=top_k,
    )

    chunks = []
    if results["ids"] and results["ids"][0]:
        for i, doc_id in enumerate(results["ids"][0]):
            chunks.append({
                "id": doc_id,
                "document": results["documents"][0][i] if results["documents"] and results["documents"][0] else "",
                "metadata": results["metadatas"][0][i] if results["metadatas"] and results["metadatas"][0] else {},
                "distance": results["distances"][0][i] if results["distances"] and results["distances"][0] else 0.0,
            })

    return chunks


def delete_collection(collection_name: str):
    """删除向量集合"""
    global _client
    if _client is None:
        _client = chromadb.PersistentClient(path=settings.CHROMA_PERSIST_DIR)
    try:
        _client.delete_collection(collection_name)
    except Exception:
        pass


def collection_count(collection_name: str) -> int:
    """获取集合中文档数量"""
    collection = get_collection(collection_name)
    return collection.count()

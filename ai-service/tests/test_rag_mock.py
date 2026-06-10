from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_rag_index_returns_chunks():
    response = client.post("/ai/rag/index", json={"file_id": 1, "file_name": "高等数学.md", "source_text": "函数极限是基础。导数用于描述变化率。积分用于计算累积量。"})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["chunk_count"] >= 3
    assert data["chunks"][0]["embedding_ref"].startswith("mock://embedding/")


def test_rag_search_returns_source():
    response = client.post("/ai/rag/search", json={"query": "导数", "file_ids": [1], "top_k": 3})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["results"]
    assert data["results"][0]["source"]


def test_rag_qa_returns_citations():
    response = client.post("/ai/rag/qa", json={"query": "如何复习导数？", "file_ids": [1], "top_k": 2})
    assert response.status_code == 200
    data = response.json()
    assert "参考来源" in data["answer_markdown"]
    assert len(data["citations"]) >= 1

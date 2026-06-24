from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_rag_index_returns_chunks():
    response = client.post(
        "/ai/rag/index",
        json={
            "file_id": 1,
            "file_name": "calculus.md",
            "source_text": "Limits are foundational. Derivatives describe rate of change. Integrals compute accumulation.",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["chunk_count"] >= 3
    assert data["chunks"][0]["embedding_ref"].startswith("mock://embedding/")


def test_rag_search_does_not_fabricate_sources():
    response = client.post("/ai/rag/search", json={"query": "derivative", "file_ids": [1], "top_k": 3})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["results"] == []


def test_rag_qa_uses_provided_context_only():
    context = [
        {
            "chunk_index": 1,
            "content_text": "Derivatives describe rate of change and should be reviewed with definitions and examples.",
            "content_hash": "real-context-1",
            "token_count": 20,
            "metadata": {"file_name": "derivative-review.md"},
            "embedding_ref": "test://real-context-1",
            "score": 0.9,
            "source": "derivative-review.md",
        }
    ]
    response = client.post(
        "/ai/rag/qa",
        json={"query": "How should I review derivatives?", "file_ids": [1], "top_k": 2, "context": context},
    )
    assert response.status_code == 200
    data = response.json()
    assert "derivative-review.md" in data["answer_markdown"]
    assert len(data["citations"]) == 1
    assert data["result_json"]["mock"] is False

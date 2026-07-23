from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)
FILE_ID = 9001


def test_rag_index_returns_chunks():
    response = client.post(
        "/ai/rag/index",
        json={
            "file_id": FILE_ID,
            "file_name": "calculus.md",
            "source_text": "Limits are foundational. Derivatives describe rate of change. Integrals compute accumulation.",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["chunk_count"] >= 3
    assert data["chunks"][0]["embedding_ref"].startswith("mock://embedding/")


def test_rag_search_returns_real_indexed_chunks():
    response = client.post("/ai/rag/search", json={"query": "derivatives", "file_ids": [FILE_ID], "top_k": 3})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["results"]
    top = data["results"][0]
    assert top["source_file_name"] == "calculus.md"
    assert top["chunk_index"] >= 1
    assert top["retrieval_mode"] in {"local_vector", "chroma"}
    assert "Derivatives" in top["content_text"]


def test_rag_search_does_not_fabricate_sources_for_missing_file():
    response = client.post("/ai/rag/search", json={"query": "derivatives", "file_ids": [999999], "top_k": 3})
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["results"] == []


def test_rag_delete_removes_indexed_chunks():
    indexed = client.post(
        "/ai/rag/index",
        json={"file_id": 9010, "file_name": "delete-me.md", "source_text": "A removable knowledge chunk about transactions."},
    )
    assert indexed.status_code == 200
    deleted = client.delete("/ai/rag/index/9010")
    assert deleted.status_code == 200
    searched = client.post("/ai/rag/search", json={"query": "transactions", "file_ids": [9010], "top_k": 3})
    assert searched.json()["results"] == []


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
        json={
            "query": "How should I review derivatives?",
            "file_ids": [1],
            "top_k": 2,
            "context": context,
            "history": [{"role": "user", "content": "I am reviewing calculus."}],
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert "derivative-review.md" in data["answer_markdown"]
    assert len(data["citations"]) == 1
    assert data["result_json"]["grounded"] is True
    assert data["result_json"]["citation_count"] == 1


def test_rag_qa_accepts_context_without_index_metadata():
    response = client.post(
        "/ai/rag/qa",
        json={
            "query": "Scrum Master 做什么？",
            "context": [
                {
                    "chunk_index": 0,
                    "content_text": "Scrum Master 服务团队，帮助移除障碍并促进 Scrum 实践。",
                    "source": "scrum.md",
                }
            ],
        },
    )

    assert response.status_code == 200
    assert response.json()["citations"][0]["content_hash"] == ""

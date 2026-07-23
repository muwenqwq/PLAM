from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def _model():
    return {"provider_type": "mock", "model_name": "mock-chat-v1"}


def test_learning_path_generate():
    response = client.post(
        "/ai/learning-paths/generate",
        json={"model_config": _model(), "subject": "Java", "goal": "掌握 Spring Boot", "knowledge_points": ["IoC", "MVC", "MyBatis"], "days": 5},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert len(data["items"]) >= 3


def test_learning_path_adjust():
    response = client.post(
        "/ai/learning-paths/adjust",
        json={"model_config": _model(), "path_title": "Java 路径", "current_progress": 40, "items": [{"title": "IoC", "knowledge_points": ["IoC"]}]},
    )
    assert response.status_code == 200
    assert response.json()["adjusted_items"][0]["title"].startswith("调整")


def test_learning_path_adjust_accepts_database_json_string_points():
    response = client.post(
        "/ai/learning-paths/adjust",
        json={
            "model_config": _model(),
            "path_title": "软件工程路径",
            "current_progress": 30,
            "items": [{"title": "需求分析", "knowledge_points": "[\"用例\", \"需求规格\"]"}],
        },
    )
    assert response.status_code == 200
    assert response.json()["adjusted_items"][0]["knowledge_points"] == ["用例", "需求规格"]

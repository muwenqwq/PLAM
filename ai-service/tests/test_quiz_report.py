from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def _model():
    return {"provider_type": "mock", "model_name": "mock-chat-v1"}


def test_quiz_generate_and_analyze():
    generated = client.post(
        "/ai/quizzes/generate",
        json={"model_config": _model(), "subject": "数据库", "knowledge_points": ["索引", "范式"], "question_count": 4},
    )
    assert generated.status_code == 200
    quiz = generated.json()
    assert quiz["success"] is True
    assert len(quiz["questions"]) == 4

    analyzed = client.post(
        "/ai/quizzes/analyze",
        json={"model_config": _model(), "quiz_title": quiz["title"], "score": 30, "total_score": 40, "weak_points": ["索引"]},
    )
    assert analyzed.status_code == 200
    assert "得分率" in analyzed.json()["analysis_markdown"]


def test_report_generate():
    response = client.post(
        "/ai/reports/generate",
        json={"model_config": _model(), "title": "学习周报", "overview": {"resource_count": 2, "quiz_count": 1, "path_count": 1, "average_score": 85}},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["chart_data_json"]["overview"]

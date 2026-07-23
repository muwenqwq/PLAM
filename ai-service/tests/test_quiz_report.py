from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def _model():
    return {"provider_type": "mock", "model_name": "mock-chat-v1"}


def test_quiz_generate_and_analyze():
    generated = client.post(
        "/ai/quizzes/generate",
        json={"model_config": _model(), "subject": "数据库", "knowledge_points": ["索引", "范式"], "question_count": 4, "question_type": "mixed", "profile": {"learning_goal": "期末复习", "foundation_level": "beginner"}},
    )
    assert generated.status_code == 200
    quiz = generated.json()
    assert quiz["success"] is True
    assert len(quiz["questions"]) == 4
    assert all(item["question_type"] == "single_choice" for item in quiz["questions"])
    assert all(len(item["options"]) == 4 for item in quiz["questions"])
    assert all(item["answer_text"] in {"A", "B", "C", "D"} for item in quiz["questions"])
    assert all(set(item["option_explanations"]) == {"A", "B", "C", "D"} for item in quiz["questions"])

    analyzed = client.post(
        "/ai/quizzes/analyze",
        json={"model_config": _model(), "quiz_title": quiz["title"], "score": 30, "total_score": 40, "weak_points": ["索引"], "profile": {"learning_goal": "期末复习", "foundation_level": "beginner"}},
    )
    assert analyzed.status_code == 200
    assert "得分率" in analyzed.json()["analysis_markdown"]
    assert "期末复习" in analyzed.json()["analysis_markdown"]


def test_report_generate():
    response = client.post(
        "/ai/reports/generate",
        json={"model_config": _model(), "title": "学习周报", "overview": {"resource_count": 2, "quiz_count": 1, "path_count": 1, "average_score": 85}, "learning_evidence": {"activity_summary": {"resource_count": 2, "submitted_quiz_count": 1, "message_count": 8}}, "mastery_records": [{"knowledge_point": "索引", "mastery_level": 45}, {"knowledge_point": "事务", "mastery_level": 90}], "profile": {"learning_goal": "期末复习"}},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["chart_data_json"]["overview"]
    assert data["report_json"]["weak_points"] == ["索引"]
    assert data["report_json"]["strengths"] == ["事务"]
    assert data["report_json"]["learning_evidence"]["activity_summary"]["message_count"] == 8

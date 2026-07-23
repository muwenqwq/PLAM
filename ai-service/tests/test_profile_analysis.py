from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def _model():
    return {"provider_type": "mock", "model_name": "mock-chat-v1"}


def test_social_chat_does_not_enter_learner_profile():
    response = client.post(
        "/ai/profiles/analyze",
        json={
            "model_config": _model(),
            "current_profile": {"subject_direction": "软件工程", "foundation_level": "intermediate"},
            "source": "chat",
            "subject": "软件工程",
            "evidence": {"user_message": "学姐你好香啊", "assistant_reply": "你好"},
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["should_update"] is False
    assert "学姐你好香啊" not in (data.get("adaptive_summary") or "")


def test_quiz_result_updates_level_and_weak_points():
    response = client.post(
        "/ai/profiles/analyze",
        json={
            "model_config": _model(),
            "current_profile": {"subject_direction": "数据库", "foundation_level": "intermediate"},
            "source": "assessment",
            "subject": "数据库",
            "weak_points": ["索引", "事务隔离"],
            "evidence": {"score": 20, "total_score": 50, "answered_question_count": 5},
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert data["should_update"] is True
    assert data["foundation_level"] == "beginner"
    assert data["weak_points"] == ["索引", "事务隔离"]

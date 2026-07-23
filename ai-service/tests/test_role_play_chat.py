from app.core.mock_provider import MockLLMProvider
from app.core.openai_compatible_provider import OpenAICompatibleProvider
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.chat import ChatRequest
from app.schemas.model import AiModelConfig


client = TestClient(app)


def test_mock_chat_uses_companion_role_settings():
    provider = MockLLMProvider()

    response = provider.chat(
        ChatRequest(
            model=AiModelConfig(),
            subject="英语六级",
            message="我听力总是错很多，怎么办？",
            profile={"learning_goal": "通过六级", "foundation_level": "beginner", "weak_points": ["听力"]},
            role_play_enabled=True,
            companion_role={
                "role_name": "温柔学姐小知",
                "role_identity": "高年级学姐",
                "personality": "温柔、耐心、积极反馈",
                "speaking_style": "鼓励式、伙伴式、循循善诱",
                "scenario": "晚自习、考前冲刺",
                "companion_goal": "缓解焦虑并督促打卡",
                "boundaries": "不直接包办答案，先引导拆解",
                "custom_prompt": "回答前先把任务拆成两步",
            },
        )
    )

    assert "温柔学姐小知" in response.reply_markdown
    assert "鼓励式" in response.reply_markdown
    assert "通过六级" in response.reply_markdown
    assert response.reply_json["role_play"]["enabled"] is True
    assert response.reply_json["role_play"]["role_name"] == "温柔学姐小知"


def test_chat_stream_returns_incremental_events_and_final_response():
    with client.stream(
        "POST",
        "/ai/chat/stream",
        json={
            "model_config": {"provider_type": "mock", "model_name": "mock-chat-v1"},
            "subject": "数据库系统",
            "message": "请解释数据库索引",
            "role_play_enabled": True,
            "companion_role": {
                "role_name": "索引学姐",
                "role_identity": "数据库学习搭子",
                "speaking_style": "直白、耐心",
            },
        },
    ) as response:
        body = "".join(response.iter_text())

    assert response.status_code == 200
    assert '"type": "delta"' in body
    assert '"type": "done"' in body
    assert "索引学姐" in body


def test_openai_compatible_provider_parses_streaming_deltas(monkeypatch):
    class FakeResponse:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, traceback):
            return False

        def raise_for_status(self):
            return None

        def iter_lines(self):
            return iter(
                [
                    'data: {"choices":[{"delta":{"content":"先理解 "}}]}',
                    'data: {"choices":[{"delta":{"content":"B+Tree"}}]}',
                    "data: [DONE]",
                ]
            )

    class FakeClient:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, traceback):
            return False

        def stream(self, method, endpoint, **kwargs):
            assert method == "POST"
            assert endpoint == "http://model.test/v1/chat/completions"
            assert kwargs["json"]["stream"] is True
            return FakeResponse()

    monkeypatch.setattr("app.core.openai_compatible_provider.httpx.Client", FakeClient)
    request = ChatRequest(
        model=AiModelConfig(
            provider_type="openai_compatible",
            base_url="http://model.test/v1",
            api_key="test-key",
            model_name="test-model",
        ),
        subject="数据库",
        message="解释索引",
    )

    events = list(OpenAICompatibleProvider().stream_chat(request))

    assert [event["content"] for event in events if event["type"] == "delta"] == ["先理解 ", "B+Tree"]
    assert events[-1]["type"] == "done"
    assert events[-1]["response"].reply_markdown == "先理解 B+Tree"

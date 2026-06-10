from app.core.mock_provider import MockLLMProvider
from app.schemas.agents import AgentRunRequest
from app.schemas.chat import ChatRequest
from app.schemas.model import AiModelConfig, ModelTestRequest


def test_mock_provider_model_test_success():
    provider = MockLLMProvider()
    response = provider.test_connection(ModelTestRequest(model=AiModelConfig()))
    assert response.success is True
    assert response.provider_type == "mock"
    assert "Mock" in response.message


def test_mock_provider_chat_returns_structured_content():
    provider = MockLLMProvider()
    response = provider.chat(ChatRequest(model=AiModelConfig(), subject="数据库系统", message="帮我复习索引"))
    assert "数据库系统" in response.reply_markdown
    assert response.reply_json["intent"] == "learning_guidance"


def test_mock_provider_agents_return_steps_and_resource():
    provider = MockLLMProvider()
    response = provider.run_agents(AgentRunRequest(model=AiModelConfig(), title="生成复习计划", subject="数据库系统"))
    assert response.success is True
    assert len(response.steps) >= 4
    assert response.resources[0].content_markdown.startswith("#")

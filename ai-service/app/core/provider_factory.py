from app.core.llm_provider import LLMProvider
from app.core.mock_provider import MockLLMProvider
from app.core.openai_compatible_provider import OpenAICompatibleProvider
from app.schemas.model import AiModelConfig


def create_provider(config: AiModelConfig | None) -> LLMProvider:
    if config is None:
        return MockLLMProvider()
    provider_type = (config.provider_type or "mock").lower()
    if provider_type == "mock" or not config.api_key:
        return MockLLMProvider()
    if provider_type in {"openai_compatible", "deepseek", "qwen", "custom"}:
        return OpenAICompatibleProvider()
    return MockLLMProvider()


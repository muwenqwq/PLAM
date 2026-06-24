from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class AiModelConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    provider_name: str | None = Field(default=None, alias="providerName")
    provider_type: str = Field(default="mock", alias="providerType")
    base_url: str | None = Field(default=None, alias="baseUrl")
    api_key: str | None = Field(default=None, alias="apiKey")
    model_name: str = Field(default="mock-chat-v1", alias="modelName")
    embedding_model: str | None = Field(default=None, alias="embeddingModel")
    temperature: float = 0.7
    max_tokens: int = Field(default=2048, alias="maxTokens")
    stream_enabled: bool = Field(default=False, alias="streamEnabled")
    extra: dict[str, Any] = Field(default_factory=dict)


class HealthResponse(BaseModel):
    status: str
    service_name: str
    version: str
    mock_mode: bool
    current_time: datetime


class ModelTestRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    prompt: str = "请用一句话介绍你能做什么。"


class ModelTestResponse(BaseModel):
    success: bool
    provider_type: str
    model_name: str
    latency_ms: int
    message: str
    sample_output: str | None = None

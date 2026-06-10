from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class AiModelConfig(BaseModel):
    provider_name: str | None = None
    provider_type: str = "mock"
    base_url: str | None = None
    api_key: str | None = None
    model_name: str = "mock-chat-v1"
    embedding_model: str | None = None
    temperature: float = 0.7
    max_tokens: int = 2048
    stream_enabled: bool = False
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

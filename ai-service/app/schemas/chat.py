from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class ChatHistoryMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    user_id: int | None = None
    conversation_id: int | None = None
    space_id: int | None = None
    subject: str | None = None
    message: str
    history: list[ChatHistoryMessage] = Field(default_factory=list)
    profile: dict[str, Any] | None = None
    preference: dict[str, Any] | None = None


class ChatResponse(BaseModel):
    provider_type: str
    model_name: str
    reply_markdown: str
    reply_json: dict[str, Any] = Field(default_factory=dict)
    token_count: int = 0


class ChatIntentRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    message: str
    subject: str | None = None


class ChatIntentResponse(BaseModel):
    intent_type: str
    confidence: float
    subject: str | None = None
    slots: dict[str, Any] = Field(default_factory=dict)

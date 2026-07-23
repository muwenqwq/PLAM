from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.agents import GeneratedResource
from app.schemas.model import AiModelConfig


class ResourceGenerateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    title: str
    subject: str | None = None
    resource_type: str = "plan"
    input_params: dict[str, Any] = Field(default_factory=dict)
    profile: dict[str, Any] = Field(default_factory=dict)
    role_play_enabled: bool = False
    companion_role: dict[str, Any] | None = None


class ResourceGenerateResponse(BaseModel):
    success: bool
    resources: list[GeneratedResource] = Field(default_factory=list)

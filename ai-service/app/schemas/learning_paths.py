from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class LearningPathGenerateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    subject: str
    goal: str
    knowledge_points: list[str] = Field(default_factory=list)
    days: int = 7
    preference: dict[str, Any] | None = None
    profile: dict[str, Any] = Field(default_factory=dict)


class LearningPathItem(BaseModel):
    title: str
    description: str
    knowledge_points: list[str] = Field(default_factory=list)
    estimated_minutes: int = 30
    difficulty: str = "medium"
    due_day: int = 1


class LearningPathGenerateResponse(BaseModel):
    success: bool
    title: str
    summary: str
    plan_json: dict[str, Any] = Field(default_factory=dict)
    items: list[LearningPathItem] = Field(default_factory=list)


class LearningPathAdjustRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    path_title: str
    current_progress: float = 0
    reason: str | None = None
    items: list[dict[str, Any]] = Field(default_factory=list)


class LearningPathAdjustResponse(BaseModel):
    success: bool
    summary: str
    adjusted_items: list[LearningPathItem] = Field(default_factory=list)

from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class ProfileAnalyzeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    current_profile: dict[str, Any] = Field(default_factory=dict)
    source: str = "adaptive"
    subject: str | None = None
    knowledge_points: list[str] = Field(default_factory=list)
    weak_points: list[str] = Field(default_factory=list)
    evidence: dict[str, Any] = Field(default_factory=dict)


class ProfileAnalyzeResponse(BaseModel):
    success: bool = True
    should_update: bool = False
    confidence: float = 0.0
    profile_narrative: str | None = None
    learning_goal: str | None = None
    subject_direction: str | None = None
    foundation_level: str | None = None
    interest_tags: list[str] | None = None
    weak_points: list[str] | None = None
    weekly_available_hours: float | None = None
    available_time_slots: list[str] | None = None
    output_style: str | None = None
    adaptive_summary: str | None = None
    evidence_summary: str | None = None

from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class AgentRunRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    user_id: int | None = None
    space_id: int | None = None
    provider_id: int | None = None
    task_type: str = "resource_generation"
    title: str
    subject: str | None = None
    resource_type: str | None = "plan"
    input_params: dict[str, Any] = Field(default_factory=dict)


class AgentStep(BaseModel):
    agent_name: str
    step_order: int
    step_type: str
    execution_status: str = "succeeded"
    input_json: dict[str, Any] = Field(default_factory=dict)
    output_summary: str
    result_json: dict[str, Any] = Field(default_factory=dict)
    error_message: str | None = None


class GeneratedResource(BaseModel):
    resource_type: str
    title: str
    subject: str | None = None
    knowledge_points: list[str] = Field(default_factory=list)
    content_markdown: str
    content_json: dict[str, Any] = Field(default_factory=dict)
    output_summary: str
    quality_score: float | None = None


class AgentRunResponse(BaseModel):
    success: bool
    execution_status: str
    output_summary: str
    result_json: dict[str, Any] = Field(default_factory=dict)
    steps: list[AgentStep] = Field(default_factory=list)
    resources: list[GeneratedResource] = Field(default_factory=list)
    error_message: str | None = None

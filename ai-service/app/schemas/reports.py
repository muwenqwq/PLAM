from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class ReportGenerateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    report_type: str = "space_weekly"
    title: str
    overview: dict[str, Any] = Field(default_factory=dict)
    mastery_records: list[dict[str, Any]] = Field(default_factory=list)


class ReportGenerateResponse(BaseModel):
    success: bool
    title: str
    summary: str
    suggestion_text: str
    report_json: dict[str, Any] = Field(default_factory=dict)
    chart_data_json: dict[str, Any] = Field(default_factory=dict)

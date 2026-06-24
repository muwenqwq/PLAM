from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class QuizGenerateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    subject: str
    title: str | None = None
    knowledge_points: list[str] = Field(default_factory=list)
    question_count: int = 5
    difficulty: str = "medium"
    question_type: str = "mixed"


class QuizQuestion(BaseModel):
    question_order: int
    question_type: str
    stem: str
    options: list[str] = Field(default_factory=list)
    answer_text: str
    analysis_text: str
    knowledge_points: list[str] = Field(default_factory=list)
    difficulty: str = "medium"
    score: float = 10


class QuizGenerateResponse(BaseModel):
    success: bool
    title: str
    subject: str
    difficulty: str
    total_score: float
    questions: list[QuizQuestion] = Field(default_factory=list)


class QuizAnalyzeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig = Field(alias="model_config")
    quiz_title: str
    score: float
    total_score: float
    weak_points: list[str] = Field(default_factory=list)
    answers: list[dict[str, Any]] = Field(default_factory=list)


class QuizAnalyzeResponse(BaseModel):
    success: bool
    analysis_markdown: str
    suggestions: list[str] = Field(default_factory=list)

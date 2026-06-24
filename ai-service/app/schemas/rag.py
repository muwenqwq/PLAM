from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.model import AiModelConfig


class RagIndexRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    model: AiModelConfig | None = Field(default=None, alias="model_config")
    file_id: int
    file_name: str
    file_type: str | None = None
    source_text: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class RagChunk(BaseModel):
    chunk_index: int
    content_text: str
    content_hash: str
    token_count: int
    metadata: dict[str, Any] = Field(default_factory=dict)
    embedding_ref: str
    score: float | None = None
    source: str | None = None


class RagIndexResponse(BaseModel):
    success: bool
    parser_status: str
    chunk_count: int
    chunks: list[RagChunk] = Field(default_factory=list)


class RagSearchRequest(BaseModel):
    query: str
    file_ids: list[int] = Field(default_factory=list)
    top_k: int = 5
    filters: dict[str, Any] = Field(default_factory=dict)


class RagSearchResponse(BaseModel):
    success: bool
    query: str
    results: list[RagChunk] = Field(default_factory=list)


class RagQaRequest(BaseModel):
    query: str
    file_ids: list[int] = Field(default_factory=list)
    top_k: int = 5
    context: list[RagChunk] | None = None


class RagQaResponse(BaseModel):
    success: bool
    answer_markdown: str
    citations: list[RagChunk] = Field(default_factory=list)
    result_json: dict[str, Any] = Field(default_factory=dict)

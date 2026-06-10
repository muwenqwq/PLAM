from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class RagIndexRequest(BaseModel):
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
    context: list[RagChunk] = Field(default_factory=list)


class RagQaResponse(BaseModel):
    success: bool
    answer_markdown: str
    citations: list[RagChunk] = Field(default_factory=list)
    result_json: dict[str, Any] = Field(default_factory=dict)


class AiModelConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    provider_name: str | None = None
    provider_type: str = "mock"
    base_url: str | None = None
    api_key: str | None = None
    model_name: str = "mock-chat-v1"
    embedding_model: str | None = "mock-embedding-v1"
    temperature: float | None = 0.7
    max_tokens: int | None = 2048
    stream_enabled: bool | None = False
    extra: dict[str, Any] = Field(default_factory=dict)

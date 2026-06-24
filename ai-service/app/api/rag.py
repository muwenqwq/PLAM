import json
import re
from typing import Any

from fastapi import APIRouter

from app.core.openai_compatible_provider import OpenAICompatibleProvider
from app.rag.chunker import chunk_text
from app.rag.embedding import stable_hash
from app.rag.parser import parse_text
from app.schemas.model import AiModelConfig
from app.schemas.rag import RagChunk, RagIndexRequest, RagIndexResponse, RagQaRequest, RagQaResponse, RagSearchRequest, RagSearchResponse

router = APIRouter()


@router.post("/index", response_model=RagIndexResponse)
def index_file(request: RagIndexRequest) -> RagIndexResponse:
    text = parse_text(request.file_name, request.source_text)
    chunks = _chunk_with_model(request, text)
    parser_status = "ai_indexed" if chunks else "indexed"
    chunks = chunks or chunk_text(request.file_id, request.file_name, text)
    return RagIndexResponse(success=True, parser_status=parser_status, chunk_count=len(chunks), chunks=chunks)


@router.post("/search", response_model=RagSearchResponse)
def search(request: RagSearchRequest) -> RagSearchResponse:
    results: list[RagChunk] = []
    return RagSearchResponse(success=True, query=request.query, results=results)


@router.post("/qa", response_model=RagQaResponse)
def qa(request: RagQaRequest) -> RagQaResponse:
    citations = request.context or []
    if not citations:
        return RagQaResponse(
            success=True,
            answer_markdown=f"没有收到真实资料片段，无法基于资料回答“{request.query}”。",
            citations=[],
            result_json={"mock": False, "citation_count": 0, "query": request.query},
        )
    source_names = "、".join([item.source or item.metadata.get("file_name", "资料片段") for item in citations[:3]])
    answer = (
        f"根据知识库检索结果，关于“{request.query}”可以先从定义、关键步骤和易错点三方面理解。\n\n"
        f"参考来源：{source_names}。\n\n"
        "建议先阅读得分最高的片段，再把其中的概念整理成自己的复习卡片。"
    )
    return RagQaResponse(
        success=True,
        answer_markdown=answer,
        citations=citations,
        result_json={"mock": False, "citation_count": len(citations), "query": request.query},
    )


def _chunk_with_model(request: RagIndexRequest, text: str) -> list[RagChunk] | None:
    model = request.model
    if not _can_use_model(model):
        return None
    prompt = {
        "task": "split_learning_material_into_knowledge_chunks",
        "language": "zh-CN",
        "requirements": [
            "把资料按知识点、概念边界或步骤边界拆成 3 到 12 个片段。",
            "每个片段必须能独立用于检索和复习，避免只切半句话。",
            "保留原文中的关键术语、公式、步骤和例子。",
            "不要编造资料中不存在的内容。",
            "只输出严格 JSON，不要输出 Markdown。",
        ],
        "output_schema": {
            "chunks": [
                {
                    "title": "片段标题",
                    "content": "片段正文",
                    "keywords": ["关键词"],
                }
            ]
        },
        "file_name": request.file_name,
        "file_type": request.file_type,
        "metadata": request.metadata,
        "source_text": text[:18000],
    }
    try:
        result = OpenAICompatibleProvider()._complete(
            model,
            [
                {"role": "system", "content": "你是知识库切片助手，只返回可解析的 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(max(model.max_tokens or 2048, 1500), 5000),
        )
        payload = _parse_json_object(result["content"])
        raw_chunks = payload.get("chunks")
        if not isinstance(raw_chunks, list):
            return None
        return _normalize_model_chunks(request, raw_chunks, model)
    except Exception:  # noqa: BLE001 - RAG indexing must keep the demo path available.
        return None


def _can_use_model(model: AiModelConfig | None) -> bool:
    if model is None:
        return False
    provider_type = (model.provider_type or "mock").lower()
    return provider_type != "mock" and bool(model.api_key and model.base_url and model.model_name)


def _parse_json_object(text: str) -> dict[str, Any]:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?", "", cleaned, flags=re.IGNORECASE).strip()
        cleaned = re.sub(r"```$", "", cleaned).strip()
    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start >= 0 and end >= start:
        cleaned = cleaned[start : end + 1]
    return json.loads(cleaned)


def _normalize_model_chunks(request: RagIndexRequest, raw_chunks: list[Any], model: AiModelConfig) -> list[RagChunk]:
    chunks: list[RagChunk] = []
    for index, item in enumerate(raw_chunks[:12], start=1):
        title = ""
        keywords: list[str] = []
        if isinstance(item, dict):
            content = str(item.get("content") or item.get("content_text") or item.get("text") or "").strip()
            title = str(item.get("title") or "").strip()
            raw_keywords = item.get("keywords") or item.get("tags") or []
            if isinstance(raw_keywords, list):
                keywords = [str(keyword).strip() for keyword in raw_keywords if str(keyword).strip()]
        else:
            content = str(item).strip()
        if len(content) < 8:
            continue
        digest = stable_hash(f"{request.file_id}:{index}:{content}")
        metadata = dict(request.metadata or {})
        metadata.update(
            {
                "file_id": request.file_id,
                "file_name": request.file_name,
                "ai_chunking": True,
                "provider_type": model.provider_type,
                "model_name": model.model_name,
            }
        )
        if title:
            metadata["title"] = title
        if keywords:
            metadata["keywords"] = keywords
        chunks.append(
            RagChunk(
                chunk_index=len(chunks) + 1,
                content_text=content,
                content_hash=digest,
                token_count=max(8, len(content) // 2),
                metadata=metadata,
                embedding_ref=f"ai://embedding/{digest[:12]}",
                source=request.file_name,
            )
        )
    return chunks

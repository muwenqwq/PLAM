import json
import re
from typing import Any

from fastapi import APIRouter

from app.core.openai_compatible_provider import OpenAICompatibleProvider
from app.rag.chunker import chunk_text
from app.rag.embedding import stable_hash
from app.rag.parser import parse_text
from app.rag.vector_store import delete_chunks, search_chunks, upsert_chunks
from app.schemas.model import AiModelConfig
from app.schemas.rag import RagChunk, RagIndexRequest, RagIndexResponse, RagQaRequest, RagQaResponse, RagSearchRequest, RagSearchResponse

router = APIRouter()


@router.post("/index", response_model=RagIndexResponse)
def index_file(request: RagIndexRequest) -> RagIndexResponse:
    text = parse_text(request.file_name, request.source_text)
    chunks = _chunk_with_model(request, text)
    parser_status = "ai_indexed" if chunks else "indexed"
    chunks = chunks or chunk_text(request.file_id, request.file_name, text)
    upsert_chunks(request, chunks)
    return RagIndexResponse(success=True, parser_status=parser_status, chunk_count=len(chunks), chunks=chunks)


@router.delete("/index/{file_id}")
def delete_index(file_id: int) -> dict[str, Any]:
    delete_chunks(file_id)
    return {"success": True, "file_id": file_id}


@router.post("/search", response_model=RagSearchResponse)
def search(request: RagSearchRequest) -> RagSearchResponse:
    results = search_chunks(request)
    return RagSearchResponse(success=True, query=request.query, results=results)


@router.post("/qa", response_model=RagQaResponse)
def qa(request: RagQaRequest) -> RagQaResponse:
    citations = request.context or []
    if not citations:
        return RagQaResponse(
            success=True,
            answer_markdown=f"当前学习空间里没有检索到能够支持这个问题的资料，因此无法依据知识库回答“{request.query}”。你可以换个问法，或先上传相关资料。",
            citations=[],
            result_json={"grounded": True, "citation_count": 0, "query": request.query, "provider": "none"},
        )
    provider = "grounded_fallback"
    answer = _grounded_fallback(request, citations)
    if _can_use_model(request.model):
        result = OpenAICompatibleProvider()._complete(
            request.model,
            _qa_messages(request, citations),
            max_tokens=min(max(request.model.max_tokens or 2048, 1200), 5000),
        )
        answer = result["content"].strip()
        provider = f"{request.model.provider_type}:{request.model.model_name}"
    return RagQaResponse(
        success=True,
        answer_markdown=answer,
        citations=citations,
        result_json={"grounded": True, "citation_count": len(citations), "query": request.query, "provider": provider},
    )


def _qa_messages(request: RagQaRequest, citations: list[RagChunk]) -> list[dict[str, str]]:
    context_blocks: list[str] = []
    for index, item in enumerate(citations[:8], start=1):
        source = item.source_file_name or item.source or item.metadata.get("file_name", "学习资料")
        context_blocks.append(
            f"[来源{index}] {source}（片段 {item.chunk_index}）\n{item.content_text[:1800]}"
        )
    profile = json.dumps(request.profile or {}, ensure_ascii=False)
    messages: list[dict[str, str]] = [
        {
            "role": "system",
            "content": (
                "你是智学工坊的知识库学习助手。请始终使用中文回答，只能依据本轮提供的资料片段，"
                "不得把常识或猜测写成资料事实。回答要直接解决学生的问题，并在关键结论后标注 [来源1] 这样的引用。"
                "如果资料不足以回答，要明确指出缺少什么信息。不要复述整段资料，也不要输出检索过程。\n"
                f"学生画像：{profile}"
            ),
        }
    ]
    for item in request.history[-8:]:
        if item.role in {"user", "assistant"} and item.content.strip():
            messages.append({"role": item.role, "content": item.content[:8000]})
    messages.append(
        {
            "role": "user",
            "content": "可引用资料如下：\n\n" + "\n\n".join(context_blocks) + f"\n\n本轮问题：{request.query}",
        }
    )
    return messages


def _grounded_fallback(request: RagQaRequest, citations: list[RagChunk]) -> str:
    first = citations[0]
    source = first.source_file_name or first.source or first.metadata.get("file_name", "学习资料")
    excerpt = re.sub(r"\s+", " ", first.content_text).strip()[:700]
    lines = [
        f"从当前资料可以确认：{excerpt} [来源1]",
        "",
        f"这段内容与“{request.query}”直接相关。建议先按资料中的定义或步骤复述一遍，再结合例题检查自己是否真正理解。",
        "",
        f"**主要依据**：{source}，片段 {first.chunk_index}。",
    ]
    if len(citations) > 1:
        second = citations[1]
        second_source = second.source_file_name or second.source or second.metadata.get("file_name", "学习资料")
        second_excerpt = re.sub(r"\s+", " ", second.content_text).strip()[:360]
        lines.insert(2, f"补充资料指出：{second_excerpt} [来源2]")
        lines.append(f"补充依据：{second_source}，片段 {second.chunk_index}。")
    return "\n".join(lines)


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

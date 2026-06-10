from fastapi import APIRouter

from app.rag.chunker import chunk_text
from app.rag.parser import parse_text
from app.rag.retriever import mock_search
from app.schemas.rag import RagIndexRequest, RagIndexResponse, RagQaRequest, RagQaResponse, RagSearchRequest, RagSearchResponse

router = APIRouter()


@router.post("/index", response_model=RagIndexResponse)
def index_file(request: RagIndexRequest) -> RagIndexResponse:
    text = parse_text(request.file_name, request.source_text)
    chunks = chunk_text(request.file_id, request.file_name, text)
    return RagIndexResponse(success=True, parser_status="indexed", chunk_count=len(chunks), chunks=chunks)


@router.post("/search", response_model=RagSearchResponse)
def search(request: RagSearchRequest) -> RagSearchResponse:
    results = mock_search(request.query, request.file_ids, request.top_k)
    return RagSearchResponse(success=True, query=request.query, results=results)


@router.post("/qa", response_model=RagQaResponse)
def qa(request: RagQaRequest) -> RagQaResponse:
    citations = request.context or mock_search(request.query, request.file_ids, request.top_k)
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
        result_json={"mock": True, "citation_count": len(citations), "query": request.query},
    )

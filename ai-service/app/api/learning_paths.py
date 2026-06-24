from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.learning_paths import (
    LearningPathAdjustRequest,
    LearningPathAdjustResponse,
    LearningPathGenerateRequest,
    LearningPathGenerateResponse,
    LearningPathItem,
)

router = APIRouter()


@router.post("/generate", response_model=LearningPathGenerateResponse)
def generate(request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
    provider = create_provider(request.model)
    return provider.generate_learning_path(request)


@router.post("/adjust", response_model=LearningPathAdjustResponse)
def adjust(request: LearningPathAdjustRequest) -> LearningPathAdjustResponse:
    base = request.items or [{"title": "补齐薄弱知识点", "knowledge_points": ["薄弱点"]}]
    adjusted = [
        LearningPathItem(
            title=f"调整：{item.get('title', '学习任务')}",
            description=f"根据进度 {request.current_progress:.0f}% 调整，优先处理未完成和薄弱内容。",
            knowledge_points=item.get("knowledge_points") or ["薄弱点"],
            estimated_minutes=30,
            difficulty="medium",
            due_day=index,
        )
        for index, item in enumerate(base[:3], start=1)
    ]
    return LearningPathAdjustResponse(success=True, summary="已生成 Mock 调整建议，建议降低单日负载并补充复盘。", adjusted_items=adjusted)

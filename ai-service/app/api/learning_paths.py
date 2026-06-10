from fastapi import APIRouter

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
    points = request.knowledge_points or [request.subject, "基础概念", "典型应用", "复盘提升"]
    items = [
        LearningPathItem(
            title=f"第{index}天：{point}",
            description=f"围绕“{point}”完成阅读、例题和输出总结。",
            knowledge_points=[point],
            estimated_minutes=35 + index * 5,
            difficulty="medium" if index < 3 else "hard",
            due_day=min(index, request.days),
        )
        for index, point in enumerate(points[: max(3, min(7, request.days))], start=1)
    ]
    return LearningPathGenerateResponse(
        success=True,
        title=f"{request.subject} 个性化学习路径",
        summary=f"按 {request.days} 天节奏推进目标：{request.goal}",
        plan_json={"mock": True, "days": request.days, "goal": request.goal, "knowledge_points": points},
        items=items,
    )


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

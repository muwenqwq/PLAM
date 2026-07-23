import json
import re

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


def _normalize_points(raw: object) -> list[str]:
    if isinstance(raw, list):
        return [str(point).strip() for point in raw if str(point).strip()]
    if isinstance(raw, str):
        value = raw.strip()
        if value.startswith("["):
            try:
                decoded = json.loads(value)
                if isinstance(decoded, list):
                    return [str(point).strip() for point in decoded if str(point).strip()]
            except json.JSONDecodeError:
                pass
        return [part.strip() for part in re.split(r"[,，、;；\n]+", value) if part.strip()]
    return []


@router.post("/generate", response_model=LearningPathGenerateResponse)
def generate(request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
    provider = create_provider(request.model)
    return provider.generate_learning_path(request)


@router.post("/adjust", response_model=LearningPathAdjustResponse)
def adjust(request: LearningPathAdjustRequest) -> LearningPathAdjustResponse:
    base = request.items or [{"title": "当前薄弱知识点", "knowledge_points": ["薄弱点"]}]
    adjusted: list[LearningPathItem] = []
    for index, item in enumerate(base[:2], start=1):
        title = str(item.get("title") or "学习任务")
        points = _normalize_points(item.get("knowledge_points")) or ["薄弱点"]
        point_text = "、".join(str(point) for point in points)
        adjusted.append(
            LearningPathItem(
                title=f"调整：补偿复习 · {title}",
                description=(
                    f"任务：针对 {point_text} 回看原任务与未完成原因，完成一题基础练习和一题变式练习。\n"
                    f"学习产出：{point_text} 错因卡和两道带过程的作答。\n"
                    "完成标准：能够脱离资料解释关键条件，并在变式题中正确迁移。"
                ),
                knowledge_points=points,
                estimated_minutes=45,
                difficulty="medium",
                due_day=index,
            )
        )
    adjusted.append(
        LearningPathItem(
            title="调整：阶段复测与节奏确认",
            description=(
                "任务：串联本次补偿复习内容完成一组综合选择题，并重新评估后续任务负载。\n"
                "学习产出：复测作答、错题订正和下一阶段优先级。\n"
                "完成标准：正确率达到 80%，且能说明每道错题的错误触发点。"
            ),
            knowledge_points=[point for item in base[:2] for point in _normalize_points(item.get("knowledge_points"))] or ["阶段复测"],
            estimated_minutes=60,
            difficulty="hard",
            due_day=len(adjusted) + 1,
        )
    )
    reason = request.reason or "当前完成进度需要重新平衡"
    summary = f"根据当前 {request.current_progress:.0f}% 的完成进度和“{reason}”，新增 {len(adjusted)} 个补偿与复测任务。"
    return LearningPathAdjustResponse(success=True, summary=summary, adjusted_items=adjusted)

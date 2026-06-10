from fastapi import APIRouter

from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse

router = APIRouter()


@router.post("/generate", response_model=ReportGenerateResponse)
def generate(request: ReportGenerateRequest) -> ReportGenerateResponse:
    overview = request.overview
    avg_score = overview.get("average_score", 0)
    mastery = overview.get("average_mastery", 0)
    summary = f"当前平均测验得分 {avg_score}，平均掌握度 {mastery}，学习闭环数据已形成。"
    return ReportGenerateResponse(
        success=True,
        title=request.title,
        summary=summary,
        suggestion_text="建议优先处理低掌握度知识点，并将资源生成、测验和学习路径联动使用。",
        report_json={"mock": True, "report_type": request.report_type, "overview": overview},
        chart_data_json={
            "mastery": request.mastery_records,
            "overview": [
                {"name": "资源数", "value": overview.get("resource_count", 0)},
                {"name": "测验数", "value": overview.get("quiz_count", 0)},
                {"name": "路径数", "value": overview.get("path_count", 0)},
            ],
        },
    )

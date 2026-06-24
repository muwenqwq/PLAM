from fastapi import APIRouter

from app.core.provider_factory import create_provider
from app.schemas.quizzes import QuizAnalyzeRequest, QuizAnalyzeResponse, QuizGenerateRequest, QuizGenerateResponse

router = APIRouter()


@router.post("/generate", response_model=QuizGenerateResponse)
def generate(request: QuizGenerateRequest) -> QuizGenerateResponse:
    provider = create_provider(request.model)
    return provider.generate_quiz(request)


@router.post("/analyze", response_model=QuizAnalyzeResponse)
def analyze(request: QuizAnalyzeRequest) -> QuizAnalyzeResponse:
    ratio = 0 if request.total_score <= 0 else request.score / request.total_score
    weak = "、".join(request.weak_points) if request.weak_points else "暂无明显薄弱点"
    return QuizAnalyzeResponse(
        success=True,
        analysis_markdown=f"本次得分率约为 {ratio:.0%}。薄弱知识点：{weak}。建议先订正错题，再生成针对性复习资源。",
        suggestions=["复盘错题原因", "补充 2-3 道同类练习", "将薄弱点加入明日学习路径"],
    )

from fastapi import APIRouter

from app.schemas.quizzes import QuizAnalyzeRequest, QuizAnalyzeResponse, QuizGenerateRequest, QuizGenerateResponse, QuizQuestion

router = APIRouter()


@router.post("/generate", response_model=QuizGenerateResponse)
def generate(request: QuizGenerateRequest) -> QuizGenerateResponse:
    points = request.knowledge_points or [request.subject, "核心概念", "应用能力"]
    questions: list[QuizQuestion] = []
    for index in range(1, max(1, request.question_count) + 1):
        point = points[(index - 1) % len(points)]
        if index % 3 == 1:
            questions.append(QuizQuestion(
                question_order=index,
                question_type="single_choice",
                stem=f"关于“{point}”，下列说法正确的是？",
                options=["A. 只需要记忆结论", "B. 需要理解概念、条件和应用场景", "C. 与练习无关", "D. 不需要复盘"],
                answer_text="B",
                analysis_text="应同时理解概念、适用条件和典型应用。",
                knowledge_points=[point],
                difficulty=request.difficulty,
            ))
        elif index % 3 == 2:
            questions.append(QuizQuestion(
                question_order=index,
                question_type="judge",
                stem=f"学习“{point}”时，复盘错因有助于提升掌握度。",
                options=["正确", "错误"],
                answer_text="正确",
                analysis_text="错因复盘可以帮助定位薄弱知识点。",
                knowledge_points=[point],
                difficulty=request.difficulty,
            ))
        else:
            questions.append(QuizQuestion(
                question_order=index,
                question_type="short_answer",
                stem=f"请简述“{point}”的学习要点。",
                answer_text="概念 条件 应用 复盘",
                analysis_text="回答中应包含概念、适用条件、应用场景和复盘方式。",
                knowledge_points=[point],
                difficulty=request.difficulty,
            ))
    return QuizGenerateResponse(
        success=True,
        title=request.title or f"{request.subject} 阶段测验",
        subject=request.subject,
        difficulty=request.difficulty,
        total_score=sum(item.score for item in questions),
        questions=questions,
    )


@router.post("/analyze", response_model=QuizAnalyzeResponse)
def analyze(request: QuizAnalyzeRequest) -> QuizAnalyzeResponse:
    ratio = 0 if request.total_score <= 0 else request.score / request.total_score
    weak = "、".join(request.weak_points) if request.weak_points else "暂无明显薄弱点"
    return QuizAnalyzeResponse(
        success=True,
        analysis_markdown=f"本次得分率约为 {ratio:.0%}。薄弱知识点：{weak}。建议先订正错题，再生成针对性复习资源。",
        suggestions=["复盘错题原因", "补充 2-3 道同类练习", "将薄弱点加入明日学习路径"],
    )

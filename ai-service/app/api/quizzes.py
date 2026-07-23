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
    role = request.companion_role or {}
    role_enabled = bool(request.role_play_enabled and role)
    role_name = _role_value(role, "role_name", "roleName", fallback="AI学习伙伴") if role_enabled else "AI学习助手"
    answer_lines: list[str] = []
    for index, item in enumerate(request.answers[:10], start=1):
        stem = str(item.get("stem") or f"第 {index} 题").strip()
        student_answer = str(item.get("student_answer") or "未作答").strip()
        score = item.get("score", 0)
        full_score = item.get("full_score", 0)
        rule_feedback = str(item.get("rule_feedback") or "请对照解析复盘。").strip()
        answer_lines.append(f"- **{stem[:80]}**：得分 {score}/{full_score}，你的答案：{student_answer}。{rule_feedback}")
    detail = "\n".join(answer_lines) if answer_lines else "- 暂无逐题详情。"
    role_note = f"\n\n{role_name}会继续按角色风格提醒你拆小任务、复盘错题并保持节奏。" if role_enabled else ""
    profile = request.profile or {}
    learning_goal = str(profile.get("learning_goal") or "当前学习目标").strip()
    foundation = str(profile.get("foundation_level") or "intermediate").strip()
    profile_note = f"\n\n本次建议已结合空间画像：学习目标为“{learning_goal}”，当前基础水平为 {foundation}。"
    return QuizAnalyzeResponse(
        success=True,
        analysis_markdown=(
            f"## 测验反馈\n\n本次得分率约为 {ratio:.0%}。薄弱知识点：{weak}。\n\n"
            f"## 逐题反馈\n{detail}\n\n"
            "## 下一步建议\n- 先订正低分题，写下错因。\n- 围绕薄弱点补 2-3 道同类练习。\n- 将薄弱点加入明日学习路径。"
            f"{profile_note}{role_note}"
        ),
        suggestions=["复盘错题原因", "补充 2-3 道同类练习", "将薄弱点加入明日学习路径"],
    )


def _role_value(role: dict, *keys: str, fallback: str | None = "") -> str:
    for key in keys:
        value = role.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return "" if fallback is None else fallback

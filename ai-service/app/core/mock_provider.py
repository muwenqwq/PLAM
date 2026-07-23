from datetime import datetime
from typing import Any

from app.core.llm_provider import LLMProvider
from app.schemas.agents import AgentRunRequest, AgentRunResponse, AgentStep, GeneratedResource
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.learning_paths import LearningPathGenerateRequest, LearningPathGenerateResponse, LearningPathItem
from app.schemas.model import ModelTestRequest, ModelTestResponse
from app.schemas.profiles import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.schemas.quizzes import QuizGenerateRequest, QuizGenerateResponse, QuizQuestion
from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse
from app.core.resource_templates import (
    RESOURCE_TYPE_LABELS,
    build_resource_markdown,
    resource_contract,
)


class MockLLMProvider(LLMProvider):
    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        model_name = request.model.model_name or "mock-chat-v1"
        return ModelTestResponse(
            success=True,
            provider_type="mock",
            model_name=model_name,
            latency_ms=18,
            message="Mock 模型连接成功，可用于无 API Key 的本地演示。",
            sample_output="你好，我是 EduAgent 学习助手，可以生成学习计划、知识点梳理和练习建议。",
        )

    def chat(self, request: ChatRequest) -> ChatResponse:
        subject = request.subject or "当前学习主题"
        role = request.companion_role or {}
        role_enabled = bool(request.role_play_enabled and role)
        profile_note = _profile_learning_note(request.profile, inline=True)
        if role_enabled:
            role_name = _role_value(role, "role_name", "roleName", fallback="AI学习伙伴")
            identity = _role_value(role, "role_identity", "roleIdentity", fallback="学习陪伴角色")
            style = _role_value(role, "speaking_style", "speakingStyle", fallback="清楚、耐心、鼓励式")
            goal = _role_value(role, "companion_goal", "companionGoal", fallback="陪你把问题拆小并坚持学下去")
            scenario = _role_value(role, "scenario", fallback="学习会话")
            answer = (
                f"我是{role_name}，会以“{identity}”的身份，用{style}的方式陪你学习。\n\n"
                f"这次我们围绕 **{subject}** 处理你的问题：{request.message}\n\n"
                "先别急，我们把它拆成两步：\n"
                "1. 先抓住最核心的概念或卡点，用一句话讲清楚。\n"
                "2. 再用一个小例子或练习确认你真的会用。\n\n"
                f"当前陪伴场景：{scenario}。我的目标是：{goal}。\n\n"
                "如果你觉得还难，可以继续让我换成白话解释、例题讲解、步骤拆解或表格版。"
                f"{profile_note}"
            )
        else:
            answer = (
                f"我已经收到你的问题：{request.message}\n\n"
                f"围绕 **{subject}**，建议按三个步骤推进：\n"
                "1. 先梳理核心概念和前置知识。\n"
                "2. 再用 3-5 道典型题检查薄弱点。\n"
                "3. 最后整理一页 Markdown 复习卡片，便于答辩或考试前快速回顾。\n\n"
                "如果你愿意，我可以继续生成学习计划、知识图谱或练习题。"
                f"{profile_note}"
            )
        return ChatResponse(
            provider_type="mock",
            model_name=request.model.model_name or "mock-chat-v1",
            reply_markdown=answer,
            reply_json={
                "intent": "learning_guidance",
                "subject": subject,
                "role_play": {
                    "enabled": role_enabled,
                    "role_name": _role_value(role, "role_name", "roleName", fallback=None) if role_enabled else None,
                    "speaking_style": _role_value(role, "speaking_style", "speakingStyle", fallback=None) if role_enabled else None,
                },
                "suggested_resources": ["学习计划", "知识点卡片", "练习题"],
                "next_actions": ["生成学习计划", "换一种方式讲", "生成测验题"],
            },
            token_count=max(80, len(answer) // 2),
        )

    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        message = request.message
        if "题" in message or "测试" in message or "测验" in message:
            intent = "quiz_generation"
        elif "计划" in message or "复习" in message:
            intent = "study_plan"
        elif "图谱" in message or "知识点" in message:
            intent = "knowledge_graph"
        else:
            intent = "learning_guidance"
        return ChatIntentResponse(intent_type=intent, confidence=0.86, subject=request.subject or "通用学习", slots={"message_length": len(message)})

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        subject = request.subject or request.title or "学习主题"
        resource_type = request.resource_type or "plan"
        points = _clean_points(request.input_params.get("knowledge_points"), subject)
        now = datetime.now().isoformat()
        profile_note = _profile_learning_note(request.input_params.get("learner_profile"))
        content, content_json, output_summary, quality_score = build_resource_markdown(
            resource_type=resource_type,
            title=request.title,
            subject=subject,
            points=points,
            input_params=request.input_params,
            profile_note=profile_note,
        )
        content_json.update(_profile_usage_metadata(request.input_params.get("learner_profile")))
        contract = resource_contract(resource_type)
        source_context = request.input_params.get("knowledge_context") or []
        steps = [
            AgentStep(
                agent_name="PlannerAgent",
                step_order=1,
                step_type="planning",
                input_json={"title": request.title, "subject": subject, "resource_type": resource_type, "knowledge_points": points},
                output_summary=f"已把目标拆成 {len(contract['required_sections'])} 个{contract['label']}内容区块。",
                result_json={"purpose": contract["purpose"], "outline": contract["required_sections"]},
            ),
            AgentStep(
                agent_name="KnowledgeAgent",
                step_order=2,
                step_type="grounding",
                input_json={"knowledge_points": points, "source_chunk_count": len(source_context)},
                output_summary=f"已梳理 {len(points)} 个重点，并核对 {len(source_context)} 个资料片段。",
                result_json={"knowledge_points": points, "source_files": content_json.get("source_files", [])},
            ),
            AgentStep(
                agent_name="ContentAgent",
                step_order=3,
                step_type="generation",
                input_json={"resource_contract": contract, "difficulty": request.input_params.get("difficulty"), "output_length": request.input_params.get("output_length")},
                output_summary=f"已按{contract['label']}专属结构完成正文，不与其他资源类型共用通用模板。",
                result_json={"character_count": len(content), "format": content_json.get("format"), "generated_at": now},
            ),
            AgentStep(
                agent_name="ReviewAgent",
                step_order=4,
                step_type="review",
                input_json={"required_sections": contract["required_sections"], "rules": contract["rules"]},
                output_summary="已检查资源类型、必需章节、资料来源和可执行性。",
                result_json=content_json.get("quality_checks", {}),
            ),
        ]
        resource = GeneratedResource(
            resource_type=resource_type,
            title=request.title,
            subject=subject,
            knowledge_points=points,
            content_markdown=content,
            content_json={**content_json, "generated_at": now, "agent_pipeline": [step.agent_name for step in steps]},
            output_summary=output_summary,
            quality_score=quality_score,
        )
        return AgentRunResponse(
            success=True,
            execution_status="succeeded",
            output_summary=resource.output_summary,
            result_json={"step_count": len(steps), "resource_type": resource_type, "quality_checks": content_json.get("quality_checks", {})},
            steps=steps,
            resources=[resource],
        )

    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        subject = request.subject or request.title
        points = _clean_points(request.input_params.get("knowledge_points"), subject)
        role_note = _role_learning_note(request.role_play_enabled, request.companion_role)
        profile_note = _profile_learning_note(request.profile)
        content, content_json, output_summary, quality_score = build_resource_markdown(
            resource_type=request.resource_type,
            title=request.title,
            subject=subject,
            points=points,
            input_params=request.input_params,
            profile_note=profile_note,
            role_note=role_note,
        )
        content_json.update(_profile_usage_metadata(request.profile))
        return ResourceGenerateResponse(
            success=True,
            resources=[
                GeneratedResource(
                    resource_type=request.resource_type,
                    title=request.title,
                    subject=subject,
                    knowledge_points=points,
                    content_markdown=content,
                    content_json=content_json,
                    output_summary=output_summary,
                    quality_score=quality_score,
                )
            ],
        )

    def generate_learning_path(self, request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
        points = _clean_points(request.knowledge_points, request.subject)
        days = max(1, request.days)
        items: list[LearningPathItem] = []
        daily_plan: list[dict[str, Any]] = []
        for day in range(1, days + 1):
            point = points[(max(day, 2) - 2) % len(points)]
            if days == 1:
                stage = "一日冲刺闭环"
                task = f"先用 15 分钟诊断 {points[0]} 等重点，再完成核心讲解、选择题练习和错题复盘。"
                output = "一张知识结构图、一组作答记录和一份错题订正"
                criterion = "能复述重点并在综合练习中达到 80% 正确率"
                difficulty = "hard"
                minutes = 120
            elif day == 1:
                stage = "诊断与路线确认"
                task = f"围绕目标“{request.goal}”完成入门诊断，标记已会、模糊和不会的知识点。"
                output = "诊断清单和知识点优先级"
                criterion = "每个知识点都有掌握判断和判断依据"
                difficulty = "easy"
                minutes = 45
            elif day == days:
                stage = "综合验收与复盘"
                task = f"串联 {'、'.join(points)} 完成综合练习，并根据错题回查薄弱环节。"
                output = "综合作答、错题归因和下一阶段建议"
                criterion = "正确率达到 80%，且能解释每道错题的错误触发点"
                difficulty = "hard"
                minutes = 90
            elif day == days - 1 and days >= 3:
                stage = "知识串联与迁移"
                task = f"比较 {'、'.join(points[:3])} 的联系和边界，完成跨知识点应用题。"
                output = "概念对照表和一份综合题解"
                criterion = "能从题干选择知识点，并写出选择依据"
                difficulty = "hard"
                minutes = 75
            else:
                study_round = (day - 2) // len(points)
                if study_round == 0:
                    stage = f"专题掌握：{point}"
                    task = f"学习 {point} 的定义、条件、步骤和边界，完成一题基础练习与一题变式练习。"
                    output = f"{point} 五格卡片和两道带过程的作答"
                    criterion = f"不看资料解释 {point}，并独立完成变式题"
                else:
                    stage = f"变式强化：{point}"
                    task = f"回看 {point} 的首次练习，针对错误触发点完成两道不同条件的迁移题。"
                    output = f"{point} 错因对照表和两道迁移题订正"
                    criterion = f"能解释条件变化为何影响 {point} 的判断或结果"
                difficulty = "medium"
                minutes = 60
            description = f"任务：{task}\n学习产出：{output}\n完成标准：{criterion}"
            items.append(
                LearningPathItem(
                    title=f"第{day}天 · {stage}",
                    description=description,
                    knowledge_points=points if day in {1, days} else [point],
                    estimated_minutes=minutes,
                    difficulty=difficulty,
                    due_day=day,
                )
            )
            daily_plan.append({"day": day, "stage": stage, "task": task, "output": output, "completion_criteria": criterion})
        return LearningPathGenerateResponse(
            success=True,
            title=f"{request.subject} 个性化学习路径",
            summary=f"按 {days} 天完成诊断、专题学习、迁移练习和最终验收，目标：{request.goal}",
            plan_json={"days": days, "goal": request.goal, "knowledge_points": points, "learner_profile": request.profile, "daily_plan": daily_plan},
            items=items,
        )

    def generate_quiz(self, request: QuizGenerateRequest) -> QuizGenerateResponse:
        points = _clean_points(request.knowledge_points, request.subject)
        role_note = _role_learning_note(request.role_play_enabled, request.companion_role, inline=True)
        profile_note = _profile_learning_note(request.profile, inline=True)
        questions: list[QuizQuestion] = []
        for index in range(1, max(1, request.question_count) + 1):
            point = points[(index - 1) % len(points)]
            questions.append(
                QuizQuestion(
                    question_order=index,
                    question_type="single_choice",
                    stem=f"学习 {point} 时，最能说明已经掌握的是哪一项？",
                    options=["A. 能解释并完成一个应用例子", "B. 只背结论", "C. 跳过练习", "D. 只看答案"],
                    answer_text="A",
                    analysis_text="真正掌握需要能解释概念、应用到例子并复盘错误。" + role_note + profile_note,
                    option_explanations={
                        "A": f"正确。能解释 {point} 并完成应用例子，说明已经理解概念且能迁移使用。",
                        "B": "错误。只背结论无法说明理解了适用条件，也不能应对题目变式。",
                        "C": "错误。跳过练习缺少检验和纠错环节，不能确认是否真正掌握。",
                        "D": "错误。只看答案没有独立推理过程，容易形成会看不会做的错觉。",
                    },
                    knowledge_points=[point],
                    difficulty=request.difficulty,
                    score=10,
                )
            )
        return QuizGenerateResponse(
            success=True,
            title=request.title or f"{request.subject} 阶段测验",
            subject=request.subject,
            difficulty=request.difficulty,
            total_score=sum(item.score for item in questions),
            questions=questions,
        )

    def generate_report(self, request: ReportGenerateRequest) -> ReportGenerateResponse:
        overview = request.overview
        evidence = request.learning_evidence
        avg_score = overview.get("average_score", 0)
        role_note = _role_learning_note(request.role_play_enabled, request.companion_role, inline=True)
        profile_note = _profile_learning_note(request.profile, inline=True)
        weak_points = [str(item.get("knowledge_point")) for item in request.mastery_records if float(item.get("mastery_level") or 0) < 60]
        strengths = [str(item.get("knowledge_point")) for item in request.mastery_records if float(item.get("mastery_level") or 0) >= 80]
        next_actions = [f"优先复习 {point} 并完成一组针对性选择题" for point in weak_points[:3]]
        if not next_actions:
            next_actions = ["沿当前学习路径继续推进，完成下一次测验后重新生成报告"]
        activity = evidence.get("activity_summary") or {}
        summary = (
            f"本空间已沉淀 {activity.get('resource_count', overview.get('resource_count', 0))} 份资源、"
            f"{activity.get('submitted_quiz_count', 0)} 次已完成测验和 "
            f"{activity.get('message_count', 0)} 条对话消息，当前平均测验得分 {avg_score}。"
            + role_note + profile_note
        )
        return ReportGenerateResponse(
            success=True,
            title=request.title,
            summary=summary,
            suggestion_text="优先处理低掌握度知识点，并把资源生成、测验和学习路径联动使用。" + role_note,
            report_json={"report_type": request.report_type, "overview": overview, "learning_evidence": evidence, "weak_points": weak_points, "strengths": strengths, "next_actions": next_actions, "learner_profile": request.profile},
            chart_data_json={
                "overview": [
                    {"name": "资源数", "value": overview.get("resource_count", 0)},
                    {"name": "测验数", "value": overview.get("quiz_count", 0)},
                    {"name": "路径数", "value": overview.get("path_count", 0)},
                ]
            },
        )

    def analyze_profile(self, request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
        current = request.current_profile or {}
        evidence_text = " ".join(
            str(value) for value in request.evidence.values()
            if isinstance(value, (str, int, float)) and str(value).strip()
        )
        source = (request.source or "adaptive").lower()
        subject = (request.subject or current.get("subject_direction") or "").strip()
        learning_markers = (
            "学习", "复习", "考试", "测验", "题", "知识点", "概念", "不会", "不懂",
            "难", "讲解", "例子", "步骤", "计划", "掌握", "基础", "课程", "作业",
        )
        social_only_markers = ("你好", "早上好", "晚上好", "喜欢你", "想你", "香啊", "可爱", "学姐好")
        is_assessment = source == "assessment"
        is_learning_chat = source == "chat" and any(marker in evidence_text for marker in learning_markers)
        is_social_only = source == "chat" and any(marker in evidence_text for marker in social_only_markers) and not is_learning_chat
        is_learning_event = source in {"learning_path", "resource_generation", "agent_resource_generation"}
        should_update = is_assessment or is_learning_chat or (is_learning_event and bool(subject))
        if is_social_only:
            should_update = False

        current_weak = _profile_list(current.get("weak_points"))
        current_interests = _profile_list(current.get("interest_tags"))
        weak_points = _merge_profile_items(current_weak, request.weak_points) if is_assessment else current_weak
        foundation = str(current.get("foundation_level") or "intermediate")
        score_rate = _score_rate(request.evidence)
        if is_assessment and score_rate is not None:
            foundation = "advanced" if score_rate >= 0.85 else "intermediate" if score_rate >= 0.6 else "beginner"

        output_style = str(current.get("output_style") or "结构化 Markdown")
        if is_learning_chat:
            if "白话" in evidence_text or "通俗" in evidence_text:
                output_style = "白话解释，配合具体例子"
            elif "步骤" in evidence_text or "一步一步" in evidence_text:
                output_style = "分步骤讲解，先结论后过程"
            elif "表格" in evidence_text:
                output_style = "结构化表格与对比总结"

        goal = _optional_text(current.get("learning_goal"))
        profile_subject = subject or _optional_text(current.get("subject_direction"))
        narrative = _profile_narrative(goal, profile_subject, foundation, weak_points, current_interests, output_style)
        summary = _profile_summary(goal, profile_subject, foundation, weak_points, output_style)
        if not should_update:
            return ProfileAnalyzeResponse(
                should_update=False,
                confidence=0.95 if is_social_only else 0.55,
                adaptive_summary=summary,
                evidence_summary="本次互动未形成新的稳定学习特征，学习画像保持不变。",
            )

        if is_assessment:
            percent = round((score_rate or 0) * 100)
            evidence_summary = f"根据本次测验 {percent}% 的得分表现，已更新基础水平"
            if request.weak_points:
                evidence_summary += f"，并将{'、'.join(request.weak_points[:4])}列为待巩固内容"
            evidence_summary += "。"
        elif source == "chat":
            evidence_summary = f"从本次学习对话中识别出对{profile_subject or '当前主题'}的学习需求，并据此调整后续讲解方式。"
        else:
            evidence_summary = f"根据本次{_source_label(source)}，更新了{profile_subject or '当前空间'}的学习方向。"

        return ProfileAnalyzeResponse(
            should_update=True,
            confidence=0.9 if is_assessment else 0.78,
            profile_narrative=narrative,
            learning_goal=goal,
            subject_direction=profile_subject,
            foundation_level=foundation,
            interest_tags=current_interests,
            weak_points=weak_points,
            weekly_available_hours=_optional_number(current.get("weekly_available_hours")),
            available_time_slots=_profile_list(current.get("available_time_slots")),
            output_style=output_style,
            adaptive_summary=summary,
            evidence_summary=evidence_summary,
        )



def _role_learning_note(enabled: bool, role: dict[str, Any] | None, inline: bool = False) -> str:
    role = role or {}
    if not enabled or not role:
        return ""
    role_name = _role_value(role, "role_name", "roleName", fallback="AI学习伙伴")
    identity = _role_value(role, "role_identity", "roleIdentity", fallback="学习陪伴角色")
    style = _role_value(role, "speaking_style", "speakingStyle", fallback="清楚、耐心、鼓励式")
    goal = _role_value(role, "companion_goal", "companionGoal", fallback="陪你坚持学下去")
    if inline:
        return f" 讲解时会以{role_name}（{identity}）的身份，用{style}方式提醒你：{goal}。"
    return f"\n\n## AI 角色陪伴风格\n- 角色：{role_name}（{identity}）\n- 说话风格：{style}\n- 陪伴目标：{goal}"


def _profile_learning_note(profile: dict[str, Any] | None, inline: bool = False) -> str:
    profile = profile or {}
    if not profile:
        return ""
    goal = str(profile.get("learning_goal") or "当前学习目标").strip()
    level = str(profile.get("foundation_level") or "intermediate").strip()
    style = str(profile.get("output_style") or "结构化表达").strip()
    weak_points = profile.get("weak_points") or []
    weak_text = "、".join(str(item) for item in weak_points[:3]) if isinstance(weak_points, list) else str(weak_points)
    if inline:
        return f" 我会按你的学习画像调整：目标是{goal}，基础水平为{level}，采用{style}" + (f"，重点照顾{weak_text}" if weak_text else "") + "。"
    return f"\n\n## 画像适配\n- 学习目标：{goal}\n- 基础水平：{level}\n- 输出风格：{style}" + (f"\n- 当前薄弱点：{weak_text}" if weak_text else "")


def _profile_usage_metadata(profile: dict[str, Any] | None) -> dict[str, Any]:
    active_fields = sorted(
        str(key)
        for key, value in (profile or {}).items()
        if value not in (None, "", [], {})
    )
    return {
        "profile_adapted": bool(active_fields),
        "profile_fields": active_fields,
    }


def _role_value(role: dict[str, Any], *keys: str, fallback: str | None = "") -> str:
    for key in keys:
        value = role.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return "" if fallback is None else fallback


def _clean_points(raw_points: Any, fallback: str) -> list[str]:
    if isinstance(raw_points, list):
        points = [str(point).strip() for point in raw_points if str(point).strip()]
    elif isinstance(raw_points, str):
        points = [part.strip() for part in raw_points.replace("；", ",").split(",") if part.strip()]
    else:
        points = []
    return points or [fallback or "综合学习"]


def _profile_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if isinstance(value, str):
        return [part.strip() for part in value.replace("；", ",").replace("、", ",").split(",") if part.strip()]
    return []


def _merge_profile_items(existing: list[str], additions: list[str]) -> list[str]:
    return list(dict.fromkeys([*existing, *[item.strip() for item in additions if item.strip()]]))[:20]


def _optional_text(value: Any) -> str | None:
    text = str(value).strip() if value is not None else ""
    return text or None


def _optional_number(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _score_rate(evidence: dict[str, Any]) -> float | None:
    try:
        score = float(evidence.get("score"))
        total = float(evidence.get("total_score"))
        return score / total if total > 0 else None
    except (TypeError, ValueError):
        return None


def _profile_narrative(
    goal: str | None,
    subject: str | None,
    foundation: str,
    weak_points: list[str],
    interests: list[str],
    output_style: str,
) -> str:
    level = {"beginner": "入门", "intermediate": "中等", "advanced": "进阶"}.get(foundation, "中等")
    parts = [f"当前主要学习方向是{subject}" if subject else "当前学习方向仍在逐步明确", f"基础水平约为{level}"]
    if goal:
        parts.append(f"阶段目标是{goal}")
    if weak_points:
        parts.append(f"需要优先巩固{'、'.join(weak_points[:5])}")
    if interests:
        parts.append(f"较容易投入的内容包括{'、'.join(interests[:4])}")
    parts.append(f"更适合采用{output_style}的学习材料")
    return "；".join(parts) + "。"


def _profile_summary(goal: str | None, subject: str | None, foundation: str, weak_points: list[str], output_style: str) -> str:
    level = {"beginner": "入门", "intermediate": "中等", "advanced": "进阶"}.get(foundation, "中等")
    summary = f"当前以{subject or '本学习空间'}为主要方向，基础水平为{level}"
    if goal:
        summary += f"，阶段目标是{goal}"
    if weak_points:
        summary += f"，现阶段重点补强{'、'.join(weak_points[:5])}"
    return summary + f"；后续内容将采用{output_style}并通过练习结果持续校准。"


def _source_label(source: str) -> str:
    return {
        "learning_path": "学习路径规划",
        "resource_generation": "资源生成",
        "agent_resource_generation": "多智能体资源生成",
    }.get(source, "学习活动")


def _context_markdown(input_params: dict[str, Any] | None) -> str:
    context = (input_params or {}).get("knowledge_context") or []
    if not isinstance(context, list) or not context:
        return ""
    sections: list[str] = []
    sources: list[str] = []
    for item in context[:8]:
        if not isinstance(item, dict):
            continue
        source = str(item.get("source") or "学习资料").strip()
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        if source not in sources:
            sources.append(source)
        sections.append(f"> **{source}**：{content[:700]}")
    if not sections:
        return ""
    return "\n\n## 资料依据\n\n" + "\n\n".join(sections) + "\n\n来源：" + "、".join(sources)

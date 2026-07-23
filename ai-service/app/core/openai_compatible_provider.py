import json
import re
import time
from collections.abc import Iterator
from typing import Any

import httpx

from app.core.mock_provider import MockLLMProvider
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.learning_paths import LearningPathGenerateRequest, LearningPathGenerateResponse, LearningPathItem
from app.schemas.model import AiModelConfig, ModelTestRequest, ModelTestResponse
from app.schemas.profiles import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.schemas.quizzes import QuizGenerateRequest, QuizGenerateResponse, QuizQuestion
from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse
from app.schemas.agents import AgentRunRequest, AgentRunResponse, GeneratedResource
from app.core.resource_templates import build_resource_markdown, resource_contract, validate_resource_markdown


class OpenAICompatibleProvider(MockLLMProvider):
    """OpenAI-compatible provider used by DeepSeek, Qwen and custom gateways."""

    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        config = request.model
        if not config.api_key:
            return ModelTestResponse(success=False, provider_type=config.provider_type, model_name=config.model_name, latency_ms=0, message="OpenAI-compatible 模型缺少 API Key。")
        started_at = time.perf_counter()
        try:
            sample = self._complete(config, [
                {"role": "system", "content": "You are EduAgent Studio. Reply in concise Simplified Chinese."},
                {"role": "user", "content": request.prompt or "请用一句话介绍你能做什么。"},
            ], max_tokens=min(config.max_tokens or 256, 256))
        except Exception as exc:  # noqa: BLE001
            return ModelTestResponse(success=False, provider_type=config.provider_type, model_name=config.model_name, latency_ms=int((time.perf_counter() - started_at) * 1000), message=f"OpenAI-compatible 连接失败：{str(exc)[:300]}")
        return ModelTestResponse(success=True, provider_type=config.provider_type, model_name=config.model_name, latency_ms=int((time.perf_counter() - started_at) * 1000), message="OpenAI-compatible 模型连接成功。", sample_output=sample["content"])

    def chat(self, request: ChatRequest) -> ChatResponse:
        subject = request.subject or "通用学习"
        result = self._complete(request.model, self._chat_messages(request))
        return self._chat_response(request, subject, result["content"], result["token_count"])

    def stream_chat(self, request: ChatRequest) -> Iterator[dict[str, Any]]:
        subject = request.subject or "通用学习"
        chunks: list[str] = []
        for chunk in self._stream_complete(request.model, self._chat_messages(request)):
            chunks.append(chunk)
            yield {"type": "delta", "content": chunk}
        content = "".join(chunks)
        if not content:
            raise RuntimeError("流式响应中没有生成内容")
        yield {
            "type": "done",
            "response": self._chat_response(
                request,
                subject,
                content,
                max(1, len(content) // 2),
            ),
        }

    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        prompt = {
            "message": request.message,
            "subject": request.subject,
            "allowed_intents": ["learning_guidance", "study_plan", "resource_generation", "quiz_generation", "knowledge_graph"],
        }
        try:
            result = self._complete(request.model, [
                {"role": "system", "content": "只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ], max_tokens=300)
            payload = self._parse_json_object(result["content"])
            return ChatIntentResponse(intent_type=payload.get("intent_type") or "learning_guidance", confidence=float(payload.get("confidence") or 0.75), subject=payload.get("subject") or request.subject, slots=payload.get("slots") or {})
        except Exception:  # noqa: BLE001
            return super().detect_intent(request)

    def analyze_profile(self, request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
        prompt = {
            "task": "analyze_durable_learner_profile_delta",
            "current_profile": request.current_profile,
            "activity_source": request.source,
            "subject": request.subject,
            "knowledge_points": request.knowledge_points,
            "known_weak_points": request.weak_points,
            "activity_evidence": request.evidence,
            "rules": [
                "只提炼可长期用于个性化学习的特征，不要复述用户原话",
                "问候、情感表达、角色扮演闲聊、一次性寒暄不能写入学习画像",
                "没有充分证据的字段返回 null，不能猜测学校、专业、目标、水平或时间安排",
                "测验结果可用于更新基础水平和薄弱点；学习对话可用于识别讲解偏好和真实学习方向",
                "interest_tags 只表示明确的学科兴趣或已表现出的擅长内容，不能把所有提问主题都当作兴趣",
                "profile_narrative 必须是分析后的第三人称或客观学习描述，不能包含对话原句",
            ],
            "output_contract": {
                "should_update": "boolean",
                "confidence": "0-1",
                "profile_narrative": "string|null",
                "learning_goal": "string|null",
                "subject_direction": "string|null",
                "foundation_level": "beginner|intermediate|advanced|null",
                "interest_tags": "string[]|null",
                "weak_points": "string[]|null",
                "weekly_available_hours": "number|null",
                "available_time_slots": "string[]|null",
                "output_style": "string|null",
                "adaptive_summary": "string|null",
                "evidence_summary": "一句不含原话的更新依据",
            },
        }
        try:
            result = self._complete(
                request.model,
                [
                    {"role": "system", "content": "你是学习分析师。谨慎更新学生画像，只输出严格 JSON。"},
                    {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
                ],
                max_tokens=min(request.model.max_tokens or 1800, 2600),
            )
            payload = self._parse_json_object(result["content"])
            foundation = payload.get("foundation_level")
            if foundation not in {None, "beginner", "intermediate", "advanced"}:
                foundation = None
            return ProfileAnalyzeResponse(
                should_update=bool(payload.get("should_update")),
                confidence=max(0.0, min(1.0, float(payload.get("confidence") or 0))),
                profile_narrative=_nullable_text(payload.get("profile_narrative")),
                learning_goal=_nullable_text(payload.get("learning_goal")),
                subject_direction=_nullable_text(payload.get("subject_direction")),
                foundation_level=foundation,
                interest_tags=_nullable_list(payload.get("interest_tags")),
                weak_points=_nullable_list(payload.get("weak_points")),
                weekly_available_hours=_nullable_float(payload.get("weekly_available_hours")),
                available_time_slots=_nullable_list(payload.get("available_time_slots")),
                output_style=_nullable_text(payload.get("output_style")),
                adaptive_summary=_nullable_text(payload.get("adaptive_summary")),
                evidence_summary=_nullable_text(payload.get("evidence_summary")),
            )
        except Exception:  # noqa: BLE001
            return super().analyze_profile(request)

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        orchestration = super().run_agents(request)
        generated = self.generate_resource(
            ResourceGenerateRequest(
                model_config=request.model,
                title=request.title,
                subject=request.subject,
                resource_type=request.resource_type or "plan",
                input_params=request.input_params,
                profile=request.input_params.get("learner_profile") or {},
                role_play_enabled=bool(request.input_params.get("role_play_enabled")),
                companion_role=request.input_params.get("companion_role"),
            )
        )
        if generated.resources:
            content_json = generated.resources[0].content_json or {}
            used_real_provider = bool(content_json.get("real_provider"))
            orchestration.resources = generated.resources
            orchestration.output_summary = generated.resources[0].output_summary
            orchestration.result_json = {
                **orchestration.result_json,
                "real_provider": used_real_provider,
                "resource_type": generated.resources[0].resource_type,
            }
            for step in orchestration.steps:
                if step.agent_name == "ContentAgent":
                    step.output_summary = (
                        "已调用当前 AI 服务按资源类型合同生成正文。"
                        if used_real_provider
                        else generated.resources[0].output_summary
                    )
                    step.result_json = {
                        **step.result_json,
                        "real_provider": used_real_provider,
                        "character_count": len(generated.resources[0].content_markdown),
                    }
        return orchestration

    def generate_quiz(self, request: QuizGenerateRequest) -> QuizGenerateResponse:
        try:
            return self._generate_quiz_with_model(request)
        except Exception:  # noqa: BLE001
            return super().generate_quiz(request)

    def _generate_quiz_with_model(self, request: QuizGenerateRequest) -> QuizGenerateResponse:
        prompt = {"task": "generate_quiz", "subject": request.subject, "knowledge_points": request.knowledge_points, "question_count": request.question_count, "difficulty": request.difficulty, "question_type": "single_choice", "profile": request.profile, "role_play_enabled": request.role_play_enabled, "companion_role": request.companion_role, "output_contract": {"questions": [{"question_type": "single_choice", "stem": "题干", "options": ["A. 选项", "B. 选项", "C. 选项", "D. 选项"], "answer_text": "A", "analysis_text": "本题核心知识说明", "option_explanations": {"A": "为什么正确或错误", "B": "为什么正确或错误", "C": "为什么正确或错误", "D": "为什么正确或错误"}, "knowledge_points": ["知识点"], "score": 10}]}}
        result = self._complete(request.model, [{"role": "system", "content": "你是学习测验命题老师。只能生成四选一单项选择题，每题必须有 A/B/C/D 四个选项，答案只能是 A、B、C 或 D。结合学习画像调整难度，只输出严格 JSON。"}, {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)}], max_tokens=min(request.model.max_tokens or 1800, 4000))
        payload = self._parse_json_object(result["content"])
        raw_questions = payload.get("questions") if isinstance(payload.get("questions"), list) else []
        questions: list[QuizQuestion] = []
        fallback_points = _clean_points(request.knowledge_points, request.subject)
        for index, item in enumerate(raw_questions[: max(1, request.question_count)], start=1):
            if not isinstance(item, dict):
                continue
            options = _normalize_choice_options(item.get("options"))
            stem = _safe_text(item.get("stem") or item.get("question"), "")
            answer = _normalize_choice_answer(item.get("answer_text") or item.get("answer"))
            if not stem or not answer or len(options) != 4:
                continue
            raw_explanations = item.get("option_explanations") if isinstance(item.get("option_explanations"), dict) else {}
            explanations = {
                label: _safe_text(raw_explanations.get(label), f"{label} 选项需要结合题干条件判断。")
                for label in ("A", "B", "C", "D")
            }
            questions.append(QuizQuestion(question_order=index, question_type="single_choice", stem=stem, options=options, answer_text=answer, analysis_text=_safe_text(item.get("analysis_text") or item.get("analysis"), "请逐项分析选项，并复盘题干知识点。"), option_explanations=explanations, knowledge_points=_clean_points(item.get("knowledge_points"), fallback_points[0]), difficulty=str(item.get("difficulty") or request.difficulty), score=float(item.get("score") or 10)))
        if not questions:
            return super().generate_quiz(request)
        return QuizGenerateResponse(success=True, title=_safe_text(payload.get("title"), request.title or f"{request.subject} 阶段测验"), subject=_safe_text(payload.get("subject"), request.subject), difficulty=str(payload.get("difficulty") or request.difficulty), total_score=sum(item.score for item in questions), questions=questions)

    def generate_learning_path(self, request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
        try:
            return self._generate_learning_path_with_model(request)
        except Exception as exc:  # noqa: BLE001
            fallback = super().generate_learning_path(request)
            fallback.plan_json = {
                **fallback.plan_json,
                "real_provider": False,
                "provider_fallback": True,
                "fallback_reason": str(exc)[:240],
            }
            fallback.summary += " 当前 AI 服务返回格式不完整，系统已按可执行路径规则完成兜底规划。"
            return fallback

    def _generate_learning_path_with_model(self, request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
        days = max(1, request.days)
        prompt = {
            "task": "generate_learning_path",
            "subject": request.subject,
            "goal": request.goal,
            "knowledge_points": request.knowledge_points,
            "days": days,
            "preference": request.preference,
            "profile": request.profile,
            "requirements": [
                "生成每天不同的阶段目标，禁止复制同一段描述后只替换知识点",
                "路径必须覆盖诊断、知识学习、练习、错题复盘、综合迁移和最终验收",
                "每项 description 都必须明确写出：任务、学习产出、完成标准",
                "due_day 必须在 1 到总天数之间，任务顺序应符合先基础后综合的学习规律",
                "不要写空泛的‘学习某知识点’，必须说明做什么、留下什么成果、如何判定完成",
            ],
            "output_contract": {
                "title": "路径标题",
                "summary": "说明规划依据和整体节奏",
                "items": [{
                    "title": "第N天 · 阶段名称",
                    "description": "任务：...\n学习产出：...\n完成标准：...",
                    "knowledge_points": ["知识点"],
                    "estimated_minutes": 60,
                    "difficulty": "easy|medium|hard",
                    "due_day": 1,
                }],
            },
        }
        result = self._complete(request.model, [{"role": "system", "content": "你是严谨的学习路径规划师。路径必须每天可执行、结果可验收，只输出严格 JSON。"}, {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)}], max_tokens=min(max(request.model.max_tokens or 3200, 3200), 7000))
        payload = self._parse_json_object(result["content"])
        raw_items = payload.get("items") if isinstance(payload.get("items"), list) else []
        items: list[LearningPathItem] = []
        fallback_points = _clean_points(request.knowledge_points, request.subject)
        for index, item in enumerate(raw_items, start=1):
            if not isinstance(item, dict):
                continue
            title = _safe_text(item.get("title"), "")
            description = _safe_text(item.get("description"), "")
            if not title or not description:
                continue
            if "任务：" not in description:
                description = f"任务：{description}"
            if "学习产出" not in description:
                description += f"\n学习产出：一份“{title}”学习记录和作答过程。"
            if "完成标准" not in description:
                description += "\n完成标准：能够脱离资料复述关键结论，并独立完成一次对应练习。"
            items.append(LearningPathItem(title=title, description=description, knowledge_points=_clean_points(item.get("knowledge_points"), fallback_points[0]), estimated_minutes=max(10, min(int(item.get("estimated_minutes") or 30), 240)), difficulty=str(item.get("difficulty") or "medium"), due_day=max(1, min(int(item.get("due_day") or index), days))))
        if items:
            model_items_by_day = {item.due_day: item for item in items}
            fallback_items_by_day = {item.due_day: item for item in super().generate_learning_path(request).items}
            items = [model_items_by_day.get(day) or fallback_items_by_day[day] for day in range(1, days + 1)]
        descriptions = [item.description for item in items]
        has_required_detail = all("产出" in description and "完成标准" in description for description in descriptions)
        enough_items = len(items) >= min(days, 3)
        descriptions_are_distinct = len(set(descriptions)) == len(descriptions)
        if not items or not enough_items or not has_required_detail or not descriptions_are_distinct:
            return super().generate_learning_path(request)
        return LearningPathGenerateResponse(success=True, title=_safe_text(payload.get("title"), f"{request.subject} 个性化学习路径"), summary=_safe_text(payload.get("summary"), f"围绕目标 {request.goal} 生成 {days} 天学习安排。"), plan_json={"real_provider": True, "days": days, "goal": request.goal, "generation_checks": {"distinct_descriptions": True, "has_deliverables": True, "has_completion_criteria": True}}, items=items)

    def generate_report(self, request: ReportGenerateRequest) -> ReportGenerateResponse:
        try:
            return self._generate_report_with_model(request)
        except Exception:  # noqa: BLE001
            return super().generate_report(request)

    def _generate_report_with_model(self, request: ReportGenerateRequest) -> ReportGenerateResponse:
        prompt = {
            "task": "generate_learning_report",
            "title": request.title,
            "overview": request.overview,
            "learning_evidence": request.learning_evidence,
            "mastery_records": request.mastery_records,
            "profile": request.profile,
            "role_play_enabled": request.role_play_enabled,
            "companion_role": request.companion_role,
            "output_contract": {
                "summary": "基于真实学习行为的阶段概述",
                "suggestion_text": "结合画像、路径进度和测验结果给出具体建议",
                "report_json": {
                    "learning_observations": ["观察"],
                    "next_actions": ["可执行任务"]
                }
            }
        }
        result = self._complete(request.model, [{"role": "system", "content": "你是学习数据分析师，只输出严格 JSON。"}, {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)}], max_tokens=min(request.model.max_tokens or 1800, 3000))
        payload = self._parse_json_object(result["content"])
        return ReportGenerateResponse(success=True, title=request.title, summary=_safe_text(payload.get("summary"), "已根据学习数据生成报告。"), suggestion_text=_safe_text(payload.get("suggestion_text"), "建议继续围绕薄弱点复习。"), report_json=payload.get("report_json") if isinstance(payload.get("report_json"), dict) else {}, chart_data_json=payload.get("chart_data_json") if isinstance(payload.get("chart_data_json"), dict) else {"overview": []})

    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        try:
            if request.resource_type == "knowledge_graph":
                return self._generate_knowledge_graph(request)
            return self._generate_typed_resource_with_model(request)
        except Exception as exc:  # noqa: BLE001
            return self._typed_resource_fallback(request, exc)

    def _generate_typed_resource_with_model(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        subject = request.subject or request.title
        contract = resource_contract(request.resource_type)
        points = _clean_points(request.input_params.get("knowledge_points"), subject)
        output_length = str(request.input_params.get("output_length") or "medium")
        target_characters = {"short": "1200-1800", "medium": "2200-3500", "long": "4000-6500"}.get(output_length, "2200-3500")
        prompt = {
            "task": "generate_typed_learning_resource",
            "title": request.title,
            "subject": subject,
            "resource_type": request.resource_type,
            "resource_label": contract["label"],
            "knowledge_points": points,
            "difficulty": request.input_params.get("difficulty") or "medium",
            "output_length": output_length,
            "target_chinese_characters": target_characters,
            "source_material": _compact_knowledge_context(request.input_params.get("knowledge_context")),
            "mistake_records": request.input_params.get("mistakes") if request.resource_type == "mistake_review" else [],
            "profile": request.profile,
            "role_play_enabled": request.role_play_enabled,
            "companion_role": request.companion_role,
            "type_contract": contract,
            "common_requirements": [
                "内容必须直接服务所选资源类型，不能只输出通用学习建议",
                "逐个覆盖输入知识点，使用具体定义、条件、步骤、例子、对比或任务",
                "资料中有依据时自然引用来源文件名，不得捏造资料原文",
                "角色风格只影响语气和陪伴方式，不能破坏资源结构",
                "错题整理只能使用 mistake_records 中的真实作答；记录为空时必须明确暂无错题，禁止虚构原题、答案或错误行为",
                "Markdown 一级标题只能出现一次，后续使用二级和三级标题",
            ],
            "output_contract": {
                "title": "资源标题",
                "output_summary": "一句话说明生成了什么",
                "knowledge_points": ["实际覆盖的知识点"],
                "content_markdown": "完整 Markdown 正文",
            },
        }
        token_target = {"short": 2200, "medium": 3800, "long": 6500}.get(output_length, 3800)
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是课程资源设计专家。严格遵守用户给出的资源类型合同，生成学生可直接使用的中文学习材料。只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(max(request.model.max_tokens or token_target, token_target), 8000),
        )
        payload = self._parse_json_object(result["content"])
        content = _safe_text(payload.get("content_markdown"), "")
        evidence_metadata: dict[str, Any] = {}
        if request.resource_type == "mistake_review":
            content, evidence_metadata, _, _ = build_resource_markdown(
                resource_type=request.resource_type,
                title=_safe_text(payload.get("title"), request.title),
                subject=subject,
                points=points,
                input_params=request.input_params,
            )
        checks = validate_resource_markdown(request.resource_type, content)
        if not checks["valid"]:
            missing = ", ".join(checks.get("missing_sections") or [])
            raise ValueError(f"模型输出不符合{contract['label']}结构要求，缺少：{missing or '必要内容'}")
        resource_metadata = {
            "resource_type": request.resource_type,
            "real_provider": True,
            "resource_contract": contract,
            "quality_checks": checks,
            "profile_adapted": bool(_active_profile_fields(request.profile)),
            "profile_fields": _active_profile_fields(request.profile),
        }
        if request.resource_type == "mistake_review":
            mistake_records = prompt.get("mistake_records") if isinstance(prompt.get("mistake_records"), list) else []
            resource_metadata.update({
                "has_actual_mistakes": bool(mistake_records),
                "card_count": len(mistake_records),
                "evidence_locked": True,
                **evidence_metadata,
            })
        resource = GeneratedResource(
            resource_type=request.resource_type,
            title=_safe_text(payload.get("title"), request.title),
            subject=subject,
            knowledge_points=_clean_points(payload.get("knowledge_points"), points[0]),
            content_markdown=content,
            content_json=resource_metadata,
            output_summary=_safe_text(payload.get("output_summary"), f"已生成结构完整的{contract['label']}。"),
            quality_score=94.0,
        )
        return ResourceGenerateResponse(success=True, resources=[resource])

    def _typed_resource_fallback(
        self,
        request: ResourceGenerateRequest,
        error: Exception,
    ) -> ResourceGenerateResponse:
        response = super().generate_resource(request)
        contract = resource_contract(request.resource_type)
        for resource in response.resources:
            resource.content_json = {
                **(resource.content_json or {}),
                "real_provider": False,
                "provider_fallback": True,
                "fallback_reason": str(error)[:240],
            }
            resource.output_summary = (
                f"当前 AI 服务返回格式不完整，系统已按{contract['label']}专属结构完成兜底生成。"
            )
        return response

    def _generate_knowledge_graph(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        subject = request.subject or request.title
        prompt = {"task": "generate_knowledge_graph", "title": request.title, "subject": subject, "input_params": {**request.input_params, "knowledge_context": _compact_knowledge_context(request.input_params.get("knowledge_context"))}, "profile": request.profile, "role_play_enabled": request.role_play_enabled, "companion_role": request.companion_role, "requirements": ["节点覆盖全部知识点", "边必须说明前置、组成、对比或应用关系", "不要只把所有节点平铺连接到根节点", "explanation 需要解释关系和推荐学习顺序"], "output_contract": {"title": "标题", "nodes": [{"id": "唯一英文或数字ID", "label": "短标签"}], "edges": [{"source": "节点ID", "target": "节点ID", "label": "关系"}], "explanation": "图谱说明和学习顺序"}}
        result = self._complete(request.model, [{"role": "system", "content": "你是知识图谱设计师，只输出严格 JSON。"}, {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)}], max_tokens=min(max(request.model.max_tokens or 2800, 2800), 5000))
        payload = self._parse_json_object(result["content"])
        nodes = payload.get("nodes") if isinstance(payload.get("nodes"), list) else []
        edges = payload.get("edges") if isinstance(payload.get("edges"), list) else []
        mermaid = _build_mermaid(nodes, edges, subject)
        labels = [str(node.get("label")) for node in nodes if isinstance(node, dict) and node.get("label")]
        content = f"# {_safe_text(payload.get('title'), request.title)}\n\n```mermaid\n{mermaid}\n```\n\n## 图谱说明\n{_safe_text(payload.get('explanation'), '围绕核心概念组织知识点。')}\n\n## 关系解读\n" + "\n".join(f"- {label}：结合图中连线理解其前置、组成或应用关系。" for label in labels) + "\n\n## 学习顺序\n先沿前置关系掌握基础节点，再学习应用节点，最后脱离图谱复述完整结构。"
        checks = validate_resource_markdown(request.resource_type, content)
        if not checks["valid"]:
            missing = ", ".join(checks.get("missing_sections") or [])
            raise ValueError(
                f"模型知识图谱未达到质量门槛：字符数 {checks['character_count']}，"
                f"缺少 {missing or '足够的关系说明'}"
            )
        resource = GeneratedResource(
            resource_type=request.resource_type,
            title=_safe_text(payload.get("title"), request.title),
            subject=subject,
            knowledge_points=_clean_points(request.input_params.get("knowledge_points"), subject),
            content_markdown=content,
            content_json={
                "resource_type": request.resource_type,
                "real_provider": True,
                "resource_contract": resource_contract(request.resource_type),
                "mermaid": mermaid,
                "graph": payload,
                "quality_checks": checks,
            },
            output_summary="已生成包含概念关系和学习顺序的结构化知识图谱。",
            quality_score=93.0,
        )
        return ResourceGenerateResponse(success=True, resources=[resource])

    def _complete(self, config: AiModelConfig, messages: list[dict[str, str]], max_tokens: int | None = None) -> dict[str, Any]:
        if not config.base_url:
            raise ValueError("base_url 为空")
        if not config.api_key:
            raise ValueError("api_key 为空")
        endpoint = config.base_url.rstrip("/") + "/chat/completions"
        payload = {"model": config.model_name, "messages": messages, "temperature": config.temperature, "max_tokens": max_tokens or config.max_tokens or 2048, "stream": False}
        headers = {"Authorization": f"Bearer {config.api_key}", "Content-Type": "application/json"}
        with httpx.Client(timeout=120) as client:
            response = client.post(endpoint, headers=headers, json=payload)
            response.raise_for_status()
            data = response.json()
        content = data.get("choices", [{}])[0].get("message", {}).get("content")
        if not content:
            raise RuntimeError("响应中没有 choices[0].message.content")
        usage = data.get("usage") or {}
        return {"content": content, "token_count": int(usage.get("total_tokens") or max(1, len(content) // 2))}

    def _stream_complete(
        self,
        config: AiModelConfig,
        messages: list[dict[str, str]],
        max_tokens: int | None = None,
    ) -> Iterator[str]:
        if not config.base_url:
            raise ValueError("base_url 为空")
        if not config.api_key:
            raise ValueError("api_key 为空")
        endpoint = config.base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": config.model_name,
            "messages": messages,
            "temperature": config.temperature,
            "max_tokens": max_tokens or config.max_tokens or 2048,
            "stream": True,
        }
        headers = {"Authorization": f"Bearer {config.api_key}", "Content-Type": "application/json"}
        with httpx.Client(timeout=120) as client:
            with client.stream("POST", endpoint, headers=headers, json=payload) as response:
                response.raise_for_status()
                for line in response.iter_lines():
                    if not line or not line.startswith("data:"):
                        continue
                    data = line[5:].strip()
                    if not data or data == "[DONE]":
                        if data == "[DONE]":
                            break
                        continue
                    event = json.loads(data)
                    choices = event.get("choices") or []
                    delta = choices[0].get("delta") if choices and isinstance(choices[0], dict) else None
                    content = delta.get("content") if isinstance(delta, dict) else None
                    if isinstance(content, str) and content:
                        yield content

    def _chat_messages(self, request: ChatRequest) -> list[dict[str, str]]:
        subject = request.subject or "通用学习"
        messages = [
            {"role": "system", "content": "你是 EduAgent Studio 的个性化学习助手。请用中文 Markdown 回答，具体、可执行，服务学生学习。"}
        ]
        role_prompt = self._role_prompt(request)
        if role_prompt:
            messages.append({"role": "system", "content": role_prompt})
        if request.profile:
            messages.append({"role": "system", "content": "以下是当前学习空间的学生画像。请据此调整难度、例子、节奏和输出风格；不要机械复述画像。\n" + json.dumps(request.profile, ensure_ascii=False)})
        for item in request.history[-8:]:
            if item.role in {"user", "assistant", "system"} and item.content:
                messages.append({"role": item.role, "content": item.content})
        messages.append({"role": "user", "content": f"学习主题：{subject}\n问题：{request.message}"})
        return messages

    def _chat_response(
        self,
        request: ChatRequest,
        subject: str,
        content: str,
        token_count: int,
    ) -> ChatResponse:
        return ChatResponse(
            provider_type=request.model.provider_type,
            model_name=request.model.model_name,
            reply_markdown=content,
            reply_json={"real_provider": True, "subject": subject, "role_play": self._role_play_json(request)},
            token_count=token_count,
        )

    def _parse_json_object(self, text: str) -> dict[str, Any]:
        cleaned = text.strip()
        if cleaned.startswith("```"):
            cleaned = re.sub(r"^```(?:json)?", "", cleaned, flags=re.IGNORECASE).strip()
            cleaned = re.sub(r"```$", "", cleaned).strip()
        start = cleaned.find("{")
        end = cleaned.rfind("}")
        if start >= 0 and end >= start:
            cleaned = cleaned[start : end + 1]
        return json.loads(cleaned)

    def _role_prompt(self, request: ChatRequest) -> str:
        role = request.companion_role or {}
        if not request.role_play_enabled or not role:
            return ""
        return (
            "当前对话启用了 AI 角色陪伴模式。请严格以该角色回应，但仍以学习帮助为核心。\n"
            f"角色名称：{_role_value(role, 'role_name', 'roleName', fallback='AI学习伙伴')}\n"
            f"角色身份：{_role_value(role, 'role_identity', 'roleIdentity', fallback='学习陪伴角色')}\n"
            f"角色背景：{_role_value(role, 'background', fallback='')}\n"
            f"角色性格：{_role_value(role, 'personality', fallback='')}\n"
            f"擅长内容：{_role_value(role, 'expertise', fallback='')}\n"
            f"角色爱好：{_role_value(role, 'hobbies', fallback='')}\n"
            f"说话风格：{_role_value(role, 'speaking_style', 'speakingStyle', fallback='清楚、耐心、鼓励式')}\n"
            f"互动场景：{_role_value(role, 'scenario', fallback='')}\n"
            f"陪伴目标：{_role_value(role, 'companion_goal', 'companionGoal', fallback='陪伴学生持续学习')}\n"
            f"边界设置：{_role_value(role, 'boundaries', fallback='')}\n"
            f"额外要求：{_role_value(role, 'custom_prompt', 'customPrompt', fallback='')}\n"
            "回答要求：先给学生能立刻执行的下一步，再解释原因；语气要符合角色。"
        )

    def _role_play_json(self, request: ChatRequest) -> dict[str, Any]:
        role = request.companion_role or {}
        enabled = bool(request.role_play_enabled and role)
        return {"enabled": enabled, "role_name": _role_value(role, "role_name", "roleName", fallback=None) if enabled else None, "speaking_style": _role_value(role, "speaking_style", "speakingStyle", fallback=None) if enabled else None}


def _safe_text(value: Any, fallback: str) -> str:
    text = str(value).strip() if value is not None else ""
    return text or fallback


def _active_profile_fields(profile: dict[str, Any] | None) -> list[str]:
    return sorted(
        str(key)
        for key, value in (profile or {}).items()
        if value not in (None, "", [], {})
    )


def _nullable_text(value: Any) -> str | None:
    text = str(value).strip() if value is not None else ""
    return text or None


def _nullable_list(value: Any) -> list[str] | None:
    if value is None:
        return None
    if not isinstance(value, list):
        return None
    return list(dict.fromkeys(str(item).strip() for item in value if str(item).strip()))[:20]


def _nullable_float(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _clean_points(raw_points: Any, fallback: str) -> list[str]:
    if isinstance(raw_points, list):
        points = [str(point).strip() for point in raw_points if str(point).strip()]
    elif isinstance(raw_points, str):
        points = [part.strip() for part in re.split(r"[,，、;；\n]+", raw_points) if part.strip()]
    else:
        points = []
    return points or [fallback or "综合学习"]


def _role_value(role: dict[str, Any], *keys: str, fallback: str | None = "") -> str:
    for key in keys:
        value = role.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return "" if fallback is None else fallback


def _normalize_choice_options(raw_options: Any) -> list[str]:
    if not isinstance(raw_options, list) or len(raw_options) != 4:
        return []
    labels = ["A", "B", "C", "D"]
    result: list[str] = []
    for index, raw in enumerate(raw_options):
        value = re.sub(r"^[A-Da-d][\.、:：\)）]\s*", "", str(raw).strip())
        if not value:
            return []
        result.append(f"{labels[index]}. {value}")
    return result


def _normalize_choice_answer(raw_answer: Any) -> str:
    value = str(raw_answer or "").strip().upper()
    return value[0] if value and value[0] in {"A", "B", "C", "D"} else ""


def _compact_knowledge_context(raw_context: Any) -> list[dict[str, str]]:
    if not isinstance(raw_context, list):
        return []
    compact: list[dict[str, str]] = []
    for item in raw_context[:10]:
        if not isinstance(item, dict):
            continue
        source = _safe_text(item.get("source") or item.get("source_file_name"), "学习资料")
        content = re.sub(r"\s+", " ", _safe_text(item.get("content") or item.get("chunk_text"), ""))[:1000]
        if content:
            compact.append({"source": source, "content": content})
    return compact


def _build_mermaid(nodes: list[Any], edges: list[Any], subject: str) -> str:
    valid_nodes = []
    seen = set()
    for index, node in enumerate(nodes, start=1):
        if not isinstance(node, dict):
            continue
        node_id = re.sub(r"[^A-Za-z0-9_]", "_", str(node.get("id") or f"N{index}"))[:32]
        label = _safe_text(node.get("label"), subject)
        if node_id not in seen:
            seen.add(node_id)
            valid_nodes.append((node_id, label))
    if not valid_nodes:
        valid_nodes = [("ROOT", subject)]
        seen = {"ROOT"}
    lines = ["graph TD"]
    for node_id, label in valid_nodes:
        lines.append(f'  {node_id}["{label.replace(chr(34), chr(39))}"]')
    for edge in edges:
        if not isinstance(edge, dict):
            continue
        source = re.sub(r"[^A-Za-z0-9_]", "_", str(edge.get("source") or ""))[:32]
        target = re.sub(r"[^A-Za-z0-9_]", "_", str(edge.get("target") or ""))[:32]
        if source in seen and target in seen and source != target:
            label = _safe_text(edge.get("label"), "").replace('"', "'")
            lines.append(f"  {source} -->|{label}| {target}" if label else f"  {source} --> {target}")
    return "\n".join(lines)

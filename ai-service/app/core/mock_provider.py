from datetime import datetime

from app.core.llm_provider import LLMProvider
from app.schemas.agents import AgentRunRequest, AgentRunResponse, AgentStep, GeneratedResource
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.learning_paths import LearningPathGenerateRequest, LearningPathGenerateResponse, LearningPathItem
from app.schemas.model import ModelTestRequest, ModelTestResponse
from app.schemas.quizzes import QuizGenerateRequest, QuizGenerateResponse, QuizQuestion
from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse


class MockLLMProvider(LLMProvider):
    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        model_name = request.model.model_name or "mock-chat-v1"
        return ModelTestResponse(
            success=True,
            provider_type="mock",
            model_name=model_name,
            latency_ms=18,
            message="Mock 模型连接成功，可用于无 API Key 演示。",
            sample_output="你好，我是 EduAgent Mock 学习助手，可以生成学习计划、知识点梳理和练习建议。",
        )

    def chat(self, request: ChatRequest) -> ChatResponse:
        subject = request.subject or "当前学习主题"
        answer = (
            f"我已经收到你的问题：{request.message}\n\n"
            f"围绕「{subject}」，建议按三个步骤推进：\n"
            "1. 先梳理核心概念和前置知识。\n"
            "2. 再用 3-5 道典型题检查薄弱点。\n"
            "3. 最后整理一页 Markdown 复习卡片，便于答辩或考试前快速回顾。\n\n"
            "如果你愿意，我可以继续生成学习计划、知识图谱或练习题。"
        )
        return ChatResponse(
            provider_type="mock",
            model_name=request.model.model_name or "mock-chat-v1",
            reply_markdown=answer,
            reply_json={
                "intent": "learning_guidance",
                "subject": subject,
                "suggested_resources": ["学习计划", "知识点卡片", "练习题"],
                "next_actions": ["生成学习计划", "生成知识图谱", "生成测验题"],
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
        return ChatIntentResponse(
            intent_type=intent,
            confidence=0.86,
            subject=request.subject or "通用学习",
            slots={"message_length": len(message), "mock": True},
        )

    def generate_quiz(self, request: QuizGenerateRequest) -> QuizGenerateResponse:
        subject = request.subject or "综合学习"
        points = _clean_points(request.knowledge_points, subject)
        qtype = request.question_type or "mixed"
        questions: list[QuizQuestion] = []
        for index in range(1, max(1, request.question_count) + 1):
            point = points[(index - 1) % len(points)]
            if qtype == "judge":
                q = self._mock_judge_question(index, point, request.difficulty)
            elif qtype == "single_choice":
                q = self._mock_choice_question(index, point, request.difficulty)
            else:
                if index % 3 == 1:
                    q = self._mock_choice_question(index, point, request.difficulty)
                elif index % 3 == 2:
                    q = self._mock_judge_question(index, point, request.difficulty)
                else:
                    q = self._mock_short_answer_question(index, point, request.difficulty)
            questions.append(q)
        return QuizGenerateResponse(
            success=True,
            title=request.title or f"{subject} 阶段测验",
            subject=subject,
            difficulty=request.difficulty,
            total_score=sum(item.score for item in questions),
            questions=questions,
        )

    def _mock_choice_question(self, index: int, point: str, difficulty: str) -> QuizQuestion:
        return QuizQuestion(
            question_order=index,
            question_type="single_choice",
            stem=f"在学习「{point}」时，下列哪项最能体现实际掌握？",
            options=[
                "A. 能解释概念、条件并完成一个应用例子",
                "B. 只背诵一个结论",
                "C. 跳过练习直接进入下一章",
                "D. 只看答案不复盘",
            ],
            answer_text="A",
            analysis_text="真正掌握需要概念理解、应用练习和错因复盘共同完成。",
            knowledge_points=[point],
            difficulty=difficulty,
        )

    def _mock_judge_question(self, index: int, point: str, difficulty: str) -> QuizQuestion:
        return QuizQuestion(
            question_order=index,
            question_type="judge",
            stem=f"学习「{point}」后，结合例题和错题复盘能帮助暴露薄弱环节。",
            options=["正确", "错误"],
            answer_text="正确",
            analysis_text="例题迁移和错题复盘可以检查是否真正理解应用边界。",
            knowledge_points=[point],
            difficulty=difficulty,
        )

    def _mock_short_answer_question(self, index: int, point: str, difficulty: str) -> QuizQuestion:
        return QuizQuestion(
            question_order=index,
            question_type="short_answer",
            stem=f"请用自己的话说明「{point}」的核心理解路径。",
            answer_text="概念 条件 场景 例题 复盘",
            analysis_text="回答应包含概念、适用条件、应用场景、例题训练和复盘方式。",
            knowledge_points=[point],
            difficulty=difficulty,
        )

    def generate_learning_path(self, request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
        subject = request.subject or "综合学习"
        points = _clean_points(request.knowledge_points, subject)
        days = max(1, request.days)
        items = [
            LearningPathItem(
                title=f"第 {index} 天：攻克 {point}",
                description=f"围绕「{point}」完成概念复述、例题练习、错因记录和一段输出总结。",
                knowledge_points=[point],
                estimated_minutes=35 + index * 5,
                difficulty="medium" if index <= 2 else "hard",
                due_day=min(index, days),
            )
            for index, point in enumerate(points[: max(3, min(7, days))], start=1)
        ]
        return LearningPathGenerateResponse(
            success=True,
            title=f"{subject} 个性化学习路径",
            summary=f"按 {days} 天节奏推进目标：{request.goal}",
            plan_json={"mock": True, "days": days, "goal": request.goal, "knowledge_points": points},
            items=items,
        )

    def generate_report(self, request: ReportGenerateRequest) -> ReportGenerateResponse:
        overview = request.overview
        avg_score = overview.get("average_score", 0)
        mastery = overview.get("average_mastery", 0)
        mastery_names = [
            item.get("knowledge_point") or item.get("name")
            for item in request.mastery_records
            if isinstance(item, dict) and (item.get("knowledge_point") or item.get("name"))
        ]
        weak_text = "、".join(mastery_names[:3]) or "暂无明显薄弱点"
        summary = f"当前平均测验得分 {avg_score}，平均掌握度 {mastery}，需要重点复盘：{weak_text}。"
        return ReportGenerateResponse(
            success=True,
            title=request.title,
            summary=summary,
            suggestion_text="建议优先处理低掌握度知识点，并将资源生成、测验和学习路径联动使用。",
            report_json={"mock": True, "report_type": request.report_type, "overview": overview, "weak_points": mastery_names},
            chart_data_json={
                "mastery": request.mastery_records,
                "overview": [
                    {"name": "资源数", "value": overview.get("resource_count", 0)},
                    {"name": "测验数", "value": overview.get("quiz_count", 0)},
                    {"name": "路径数", "value": overview.get("path_count", 0)},
                ],
            },
        )

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        subject = request.subject or request.title or "学习主题"
        context_markdown = _context_markdown(request.input_params)
        now = datetime.now()
        steps = [
            AgentStep(
                agent_name="PlannerAgent",
                step_order=1,
                step_type="planning",
                output_summary=f"将「{subject}」拆分为目标澄清、知识梳理、练习巩固和复盘改进四个阶段。",
                result_json={"stages": ["目标澄清", "知识梳理", "练习巩固", "复盘改进"]},
            ),
            AgentStep(
                agent_name="KnowledgeAgent",
                step_order=2,
                step_type="generation",
                output_summary=f"提取「{subject}」的核心知识点、先修关系和易错点。",
                result_json={"knowledge_points": [subject, "核心概念", "典型题型", "易错点"]},
            ),
            AgentStep(
                agent_name="ExerciseAgent",
                step_order=3,
                step_type="generation",
                output_summary="生成选择题、简答题和应用题组合，覆盖基础、中等和提升难度。",
                result_json={"question_count": 5, "difficulty": "medium"},
            ),
            AgentStep(
                agent_name="ReviewAgent",
                step_order=4,
                step_type="review",
                output_summary="检查资源结构完整性，给出复习顺序和质量评分。",
                result_json={"quality_score": 88, "review": "结构完整，适合演示。"},
            ),
        ]
        content = (
            f"# {subject} 学习资源\n\n"
            "## 学习目标\n"
            f"- 理解 {subject} 的核心概念。\n"
            "- 能用自己的话解释关键知识点。\n"
            "- 完成一组基础练习并复盘错因。\n\n"
            "## 建议流程\n"
            "1. 15 分钟快速预习。\n"
            "2. 30 分钟知识点梳理。\n"
            "3. 30 分钟练习与订正。\n"
            "4. 10 分钟总结输出。"
            f"{context_markdown}"
        )
        resource = GeneratedResource(
            resource_type=request.resource_type or "plan",
            title=f"{subject} 学习资源",
            subject=subject,
            knowledge_points=[subject, "核心概念", "练习巩固"],
            content_markdown=content,
            content_json={"generated_at": now.isoformat(), "mock": True},
            output_summary=f"已生成「{subject}」学习资源，包含目标、流程和练习建议。",
            quality_score=88.0,
        )
        return AgentRunResponse(
            success=True,
            execution_status="succeeded",
            output_summary=resource.output_summary,
            result_json={"mock": True, "step_count": len(steps), "resource_title": resource.title},
            steps=steps,
            resources=[resource],
        )

    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        resource_type = request.resource_type or "plan"
        subject = request.subject or request.title
        points = request.input_params.get("knowledge_points") or [subject, "核心概念", "典型应用"]
        context_markdown = _context_markdown(request.input_params)
        if resource_type == "knowledge_graph":
            mermaid = (
                "graph TD\n"
                f"  A[{subject}] --> B[核心概念]\n"
                f"  A --> C[典型应用]\n"
                f"  A --> D[易错点]\n"
                "  B --> E[定义与条件]\n"
                "  C --> F[例题训练]\n"
                "  D --> G[错因复盘]\n"
            )
            content = f"# {subject} 知识图谱\n\n```mermaid\n{mermaid}```\n\n## 使用建议\n先按图谱理解依赖关系，再逐个补齐薄弱节点。{context_markdown}"
            resources = [
                GeneratedResource(
                    resource_type=resource_type,
                    title=f"{subject} 知识图谱",
                    subject=subject,
                    knowledge_points=points,
                    content_markdown=content,
                    content_json={"mock": True, "mermaid": mermaid, "nodes": points},
                    output_summary=f"已生成 {subject} Mermaid 知识图谱。",
                    quality_score=90.0,
                )
            ]
            return ResourceGenerateResponse(success=True, resources=resources)
        if resource_type == "quiz_set":
            questions = [
                {"type": "single_choice", "stem": f"{subject} 的学习重点是什么？", "answer": "理解概念并练习应用"},
                {"type": "judge", "stem": "错题复盘能提升掌握度。", "answer": "正确"},
                {"type": "short_answer", "stem": f"简述 {subject} 的复习方法。", "answer": "概念、例题、错因、复盘"},
            ]
            content = "# 习题集\n\n" + "\n".join([f"{idx}. {item['stem']}" for idx, item in enumerate(questions, start=1)]) + context_markdown
            resources = [
                GeneratedResource(
                    resource_type=resource_type,
                    title=f"{subject} 习题集",
                    subject=subject,
                    knowledge_points=points,
                    content_markdown=content,
                    content_json={"mock": True, "questions": questions},
                    output_summary="已生成可演示的结构化习题集。",
                    quality_score=87.0,
                )
            ]
            return ResourceGenerateResponse(success=True, resources=resources)
        if resource_type == "ppt_outline":
            slides = [
                {"page": 1, "title": "学习目标", "bullets": [f"理解 {subject}", "明确产出要求"]},
                {"page": 2, "title": "核心概念", "bullets": points[:3]},
                {"page": 3, "title": "练习与复盘", "bullets": ["典型例题", "错因分析", "后续计划"]},
            ]
            content = "# PPT 大纲\n\n" + "\n".join([f"## {item['page']}. {item['title']}" for item in slides]) + context_markdown
            resources = [
                GeneratedResource(
                    resource_type=resource_type,
                    title=f"{subject} PPT 大纲",
                    subject=subject,
                    knowledge_points=points,
                    content_markdown=content,
                    content_json={"mock": True, "slides": slides},
                    output_summary="已生成分页面向展示的 PPT 大纲。",
                    quality_score=86.0,
                )
            ]
            return ResourceGenerateResponse(success=True, resources=resources)
        agent_result = self.run_agents(
            AgentRunRequest(
                model=request.model,
                title=request.title,
                task_type="resource_generation",
                subject=request.subject,
                resource_type=resource_type,
                input_params=request.input_params,
            )
        )
        return ResourceGenerateResponse(success=True, resources=agent_result.resources)


def _context_markdown(input_params: dict | None) -> str:
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
    source_text = "、".join(sources)
    return "\n\n## 资料依据\n\n" + "\n\n".join(sections) + f"\n\n来源：{source_text}"

def _clean_points(raw_points, fallback: str) -> list[str]:
    if isinstance(raw_points, list):
        points = []
        for point in raw_points:
            points.extend(str(point).replace("，", ",").split(","))
        points = [point.strip() for point in points if point.strip()]
    elif isinstance(raw_points, str):
        points = [point.strip() for point in raw_points.replace("，", ",").split(",") if point.strip()]
    else:
        points = []
    return points or [fallback or "综合学习"]

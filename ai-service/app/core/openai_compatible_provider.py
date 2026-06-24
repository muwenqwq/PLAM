import json
import re
import time
from typing import Any

import httpx

from app.core.mock_provider import MockLLMProvider
from app.schemas.agents import AgentRunRequest, AgentRunResponse, AgentStep, GeneratedResource
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.learning_paths import LearningPathGenerateRequest, LearningPathGenerateResponse, LearningPathItem
from app.schemas.model import AiModelConfig, ModelTestRequest, ModelTestResponse
from app.schemas.quizzes import QuizGenerateRequest, QuizGenerateResponse, QuizQuestion
from app.schemas.reports import ReportGenerateRequest, ReportGenerateResponse
from app.schemas.resources import ResourceGenerateRequest, ResourceGenerateResponse


class OpenAICompatibleProvider(MockLLMProvider):
    """OpenAI-compatible provider used by MiMo, DeepSeek, Qwen and custom gateways."""

    def test_connection(self, request: ModelTestRequest) -> ModelTestResponse:
        config = request.model
        if not config.api_key:
            return ModelTestResponse(
                success=False,
                provider_type=config.provider_type,
                model_name=config.model_name,
                latency_ms=0,
                message="OpenAI-compatible 模型缺少 API Key。",
                sample_output=None,
            )

        started_at = time.perf_counter()
        try:
            sample = self._complete(
                config,
                [
                    {"role": "system", "content": "You are EduAgent Studio. Reply in concise Simplified Chinese."},
                    {"role": "user", "content": request.prompt or "请用一句话介绍你能做什么。"},
                ],
                max_tokens=min(config.max_tokens or 256, 256),
            )
        except Exception as exc:  # noqa: BLE001
            return ModelTestResponse(
                success=False,
                provider_type=config.provider_type,
                model_name=config.model_name,
                latency_ms=int((time.perf_counter() - started_at) * 1000),
                message=f"OpenAI-compatible 连接失败：{self._safe_error(exc)}",
                sample_output=None,
            )

        return ModelTestResponse(
            success=True,
            provider_type=config.provider_type,
            model_name=config.model_name,
            latency_ms=int((time.perf_counter() - started_at) * 1000),
            message="OpenAI-compatible 模型连接成功，已完成真实远程调用。",
            sample_output=sample["content"],
        )

    def chat(self, request: ChatRequest) -> ChatResponse:
        subject = self._safe_topic(request.subject, "通用学习")
        messages = [
            {
                "role": "system",
                "content": (
                    "你是 EduAgent Studio 的个性化学习助手。请用中文 Markdown 回答，"
                    "重点服务学生复习、项目答辩、知识梳理和练习反馈。回答要具体，不要空泛。"
                ),
            }
        ]
        for item in request.history[-8:]:
            if item.role in {"user", "assistant", "system"} and item.content:
                messages.append({"role": item.role, "content": item.content})
        messages.append({"role": "user", "content": f"学习主题：{subject}\n问题：{request.message}"})

        result = self._complete(request.model, messages)
        return ChatResponse(
            provider_type=request.model.provider_type,
            model_name=request.model.model_name,
            reply_markdown=result["content"],
            reply_json={"real_provider": True, "subject": subject},
            token_count=result["token_count"],
        )

    def detect_intent(self, request: ChatIntentRequest) -> ChatIntentResponse:
        prompt = (
            "请判断下面学习请求的意图，只返回 JSON："
            '{"intent_type":"learning_guidance|study_plan|resource_generation|quiz_generation|knowledge_graph",'
            '"confidence":0.0,"subject":"主题","slots":{}}\n'
            f"主题：{request.subject or ''}\n请求：{request.message}"
        )
        try:
            result = self._complete(
                request.model,
                [
                    {"role": "system", "content": "你只输出严格 JSON，不要输出解释。"},
                    {"role": "user", "content": prompt},
                ],
                max_tokens=300,
            )
            payload = self._parse_json_object(result["content"])
            return ChatIntentResponse(
                intent_type=payload.get("intent_type") or "learning_guidance",
                confidence=float(payload.get("confidence") or 0.75),
                subject=payload.get("subject") or request.subject,
                slots=payload.get("slots") or {},
            )
        except Exception:  # noqa: BLE001
            return super().detect_intent(request)

    def generate_quiz(self, request: QuizGenerateRequest) -> QuizGenerateResponse:
        subject = self._safe_topic(request.subject, "综合学习")
        points = self._clean_points(request.knowledge_points, subject)
        question_count = max(1, min(int(request.question_count or 5), 20))
        qtype = request.question_type or "mixed"
        if qtype == "single_choice":
            type_req = ["只生成 single_choice 类型题目。"]
            type_schema = "single_choice"
        elif qtype == "judge":
            type_req = ["只生成 judge 类型题目。"]
            type_schema = "judge"
        else:
            type_req = ["按比例混合 single_choice、judge、short_answer 三种题型。"]
            type_schema = "single_choice|judge|short_answer"
        prompt = {
            "task": "generate_quiz",
            "language": "zh-CN",
            "requirements": [
                "必须根据学科和知识点生成具体题目，禁止只替换关键词的模板题。",
                "题目要覆盖概念理解、场景判断、应用推理和简答解释。",
                *type_req,
                "single_choice 选项必须是 A./B./C./D. 开头，answer_text 只填 A/B/C/D。",
                "judge 的 options 固定为 ['正确','错误']，answer_text 只填 正确 或 错误。",
                "short_answer 的 answer_text 用空格分隔关键得分点，便于后端关键词评分。",
            ],
            "output_schema": {
                "title": "string",
                "subject": "string",
                "difficulty": request.difficulty,
                "questions": [
                    {
                        "question_type": type_schema,
                        "stem": "string",
                        "options": ["string"],
                        "answer_text": "string",
                        "analysis_text": "string",
                        "knowledge_points": ["string"],
                        "score": 10,
                    }
                ],
            },
            "subject": subject,
            "title": request.title,
            "knowledge_points": points,
            "question_count": question_count,
            "difficulty": request.difficulty,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是严谨的学习测验命题教师，只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=self._generation_tokens(request.model, question_count),
        )
        payload = self._parse_json_object(result["content"])
        questions = self._normalize_quiz_questions(payload, points, request.difficulty, question_count)
        return QuizGenerateResponse(
            success=True,
            title=self._safe_topic(payload.get("title"), request.title or f"{subject} 阶段测验"),
            subject=self._safe_topic(payload.get("subject"), subject),
            difficulty=str(payload.get("difficulty") or request.difficulty or "medium"),
            total_score=sum(item.score for item in questions),
            questions=questions,
        )

    def generate_learning_path(self, request: LearningPathGenerateRequest) -> LearningPathGenerateResponse:
        subject = self._safe_topic(request.subject, "综合学习")
        goal = self._safe_topic(request.goal, f"掌握 {subject}")
        points = self._clean_points(request.knowledge_points, subject)
        days = max(1, min(int(request.days or 7), 30))
        prompt = {
            "task": "generate_learning_path",
            "language": "zh-CN",
            "requirements": [
                "必须围绕用户目标生成可执行学习路径，不能只把知识点换进固定句式。",
                "每个 item 要包含明确学习动作、产出物和复盘方式。",
                "estimated_minutes 要合理，difficulty 使用 easy/medium/hard。",
                "due_day 必须在 1 到 days 范围内。",
            ],
            "output_schema": {
                "title": "string",
                "summary": "string",
                "items": [
                    {
                        "title": "string",
                        "description": "string",
                        "knowledge_points": ["string"],
                        "estimated_minutes": 30,
                        "difficulty": "easy|medium|hard",
                        "due_day": 1,
                    }
                ],
            },
            "subject": subject,
            "goal": goal,
            "knowledge_points": points,
            "days": days,
            "preference": request.preference,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是学习路径规划师，只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=self._generation_tokens(request.model, days),
        )
        payload = self._parse_json_object(result["content"])
        items = self._normalize_learning_items(payload, points, days)
        return LearningPathGenerateResponse(
            success=True,
            title=self._safe_topic(payload.get("title"), f"{subject} 个性化学习路径"),
            summary=self._safe_topic(payload.get("summary"), f"围绕目标“{goal}”生成 {days} 天学习安排。"),
            plan_json={
                "real_provider": True,
                "subject": subject,
                "goal": goal,
                "days": days,
                "knowledge_points": points,
                "raw": payload,
            },
            items=items,
        )

    def generate_report(self, request: ReportGenerateRequest) -> ReportGenerateResponse:
        prompt = {
            "task": "generate_learning_report",
            "language": "zh-CN",
            "requirements": [
                "必须基于 overview 和 mastery_records 做学习诊断，不能只复述统计数字。",
                "summary 要指出学习表现、薄弱点和趋势。",
                "suggestion_text 要给出下一阶段可执行建议。",
                "report_json 要包含 weak_points、strengths、next_actions。",
                "chart_data_json 要保留可用于前端图表的数据。",
            ],
            "output_schema": {
                "summary": "string",
                "suggestion_text": "string",
                "report_json": {"weak_points": ["string"], "strengths": ["string"], "next_actions": ["string"]},
                "chart_data_json": {"mastery": [{"name": "string", "value": 0}]},
            },
            "report_type": request.report_type,
            "title": request.title,
            "overview": request.overview,
            "mastery_records": request.mastery_records,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是学习数据分析师，只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(request.model.max_tokens or 1800, 3000),
        )
        payload = self._parse_json_object(result["content"])
        report_json = payload.get("report_json") if isinstance(payload.get("report_json"), dict) else {}
        chart_json = payload.get("chart_data_json") if isinstance(payload.get("chart_data_json"), dict) else {}
        return ReportGenerateResponse(
            success=True,
            title=request.title,
            summary=self._safe_topic(payload.get("summary"), "已根据当前学习数据生成学习报告。"),
            suggestion_text=self._safe_topic(payload.get("suggestion_text"), "建议结合薄弱知识点继续生成测验和复习资源。"),
            report_json={"real_provider": True, "report_type": request.report_type, **report_json},
            chart_data_json=chart_json or self._fallback_chart_data(request.overview, request.mastery_records),
        )

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        subject = self._safe_topic(request.subject or request.title, "学习主题")
        points = self._clean_points_from_params(request.input_params, subject)
        prompt = {
            "task": "multi_agent_learning_resource",
            "language": "zh-CN",
            "requirements": [
                "模拟 PlannerAgent、KnowledgeAgent、ExerciseAgent、ReviewAgent 协作。",
                "生成内容必须围绕用户输入，不要套固定模板。",
                "input_params.knowledge_context 非空时，必须严格基于片段内容生成，并在资源中标注资料来源，禁止补写片段中不存在的事实。",
                "resource_markdown 要能直接作为学习资源展示。",
                "steps 的 output_summary 要体现每个智能体实际完成的工作。",
            ],
            "output_schema": {
                "output_summary": "string",
                "steps": [
                    {
                        "agent_name": "PlannerAgent",
                        "step_order": 1,
                        "step_type": "planning",
                        "output_summary": "string",
                        "result_json": {},
                    }
                ],
                "resource": {
                    "title": "string",
                    "knowledge_points": ["string"],
                    "content_markdown": "string",
                    "output_summary": "string",
                    "quality_score": 90,
                },
            },
            "title": request.title,
            "subject": subject,
            "task_type": request.task_type,
            "resource_type": request.resource_type,
            "knowledge_points": points,
            "input_params": request.input_params,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是多智能体学习资源生成系统，只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(request.model.max_tokens or 2600, 4000),
        )
        payload = self._parse_json_object(result["content"])
        steps = self._normalize_agent_steps(payload)
        resource_payload = payload.get("resource") if isinstance(payload.get("resource"), dict) else {}
        content = self._safe_topic(resource_payload.get("content_markdown"), "")
        if not content:
            content = self._complete(
                request.model,
                [
                    {"role": "system", "content": "请用中文 Markdown 生成学习资源。"},
                    {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
                ],
            )["content"]
        resource = GeneratedResource(
            resource_type=request.resource_type or "plan",
            title=self._safe_topic(resource_payload.get("title"), f"{subject} 学习资源"),
            subject=subject,
            knowledge_points=self._clean_points(resource_payload.get("knowledge_points") or points, subject),
            content_markdown=content,
            content_json={"real_provider": True, "model_name": request.model.model_name, "raw": resource_payload},
            output_summary=self._safe_topic(resource_payload.get("output_summary"), f"已生成《{subject}》学习资源。"),
            quality_score=float(resource_payload.get("quality_score") or 90.0),
        )
        return AgentRunResponse(
            success=True,
            execution_status="succeeded",
            output_summary=self._safe_topic(payload.get("output_summary"), resource.output_summary),
            result_json={"real_provider": True, "token_count": result["token_count"]},
            steps=steps,
            resources=[resource],
        )

    def generate_resource(self, request: ResourceGenerateRequest) -> ResourceGenerateResponse:
        subject = self._safe_topic(request.subject or request.title, "学习主题")
        resource_type = request.resource_type or "plan"
        knowledge_points = self._clean_points_from_params(request.input_params, subject)

        if resource_type == "knowledge_graph":
            return self._generate_knowledge_graph(request, subject, knowledge_points)

        prompt = {
            "task": "generate_learning_resource",
            "language": "zh-CN",
            "requirements": [
                "必须基于输入主题、知识点和资源类型生成具体内容，禁止固定模板换字段。",
                "内容要适合学生直接学习和答辩演示。",
                "Markdown 要有清晰标题、要点、例子、练习或应用建议。",
                "input_params.knowledge_context 非空时，必须严格基于片段内容生成，并在资源中标注资料来源，禁止补写片段中不存在的事实。",
            ],
            "title": request.title,
            "subject": subject,
            "resource_type": resource_type,
            "knowledge_points": knowledge_points,
            "input_params": request.input_params,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是 EduAgent Studio 的学习资源生成助手，请用中文 Markdown 输出。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(request.model.max_tokens or 2400, 4000),
        )
        resource = GeneratedResource(
            resource_type=resource_type,
            title=request.title,
            subject=subject,
            knowledge_points=knowledge_points,
            content_markdown=result["content"],
            content_json={"real_provider": True, "model_name": request.model.model_name},
            output_summary=f"已使用真实模型生成 {resource_type} 类型资源。",
            quality_score=90.0,
        )
        return ResourceGenerateResponse(success=True, resources=[resource])

    def _generate_knowledge_graph(
        self,
        request: ResourceGenerateRequest,
        subject: str,
        knowledge_points: list[str],
    ) -> ResourceGenerateResponse:
        prompt = {
            "task": "generate_knowledge_graph",
            "language": "zh-CN",
            "requirements": [
                "必须根据主题和知识点设计真实知识结构，禁止固定二叉结构。",
                "节点要体现概念、前置依赖、应用场景、易错点和复习顺序。",
                "返回 JSON 后由系统转 Mermaid，不要直接返回 Markdown。",
            ],
            "output_schema": {
                "title": "string",
                "nodes": [{"id": "ROOT", "label": "string"}],
                "edges": [{"source": "ROOT", "target": "N1", "label": "string"}],
                "explanation": "string",
            },
            "title": request.title,
            "subject": subject,
            "knowledge_points": knowledge_points,
            "input_params": request.input_params,
        }
        result = self._complete(
            request.model,
            [
                {"role": "system", "content": "你是知识图谱设计师，只输出严格 JSON。"},
                {"role": "user", "content": json.dumps(prompt, ensure_ascii=False)},
            ],
            max_tokens=min(request.model.max_tokens or 2000, 3500),
        )
        payload = self._parse_json_object(result["content"])
        graph = self._normalize_graph_payload(payload, subject, knowledge_points)
        mermaid = self._build_mermaid_from_graph(graph)
        explanation = self._safe_topic(graph.get("explanation"), f"图谱围绕“{subject}”组织核心概念、依赖关系与复习顺序。")
        title = self._safe_topic(graph.get("title"), request.title)
        content = (
            f"# {title}\n\n"
            f"```mermaid\n{mermaid}\n```\n\n"
            "## 图谱说明\n"
            f"{explanation}\n\n"
            "## 复习使用建议\n"
            "- 先沿主干节点理解概念之间的依赖关系。\n"
            "- 再按边上的关系标签复述每个知识点为什么相连。\n"
            "- 最后针对易错节点生成练习或测验，检查是否真正掌握。"
        )
        resource = GeneratedResource(
            resource_type=request.resource_type,
            title=title,
            subject=subject,
            knowledge_points=knowledge_points,
            content_markdown=content,
            content_json={"real_provider": True, "model_name": request.model.model_name, "mermaid": mermaid, "graph": graph},
            output_summary="已使用真实模型生成结构化知识图谱。",
            quality_score=92.0,
        )
        return ResourceGenerateResponse(success=True, resources=[resource])

    def _complete(self, config: AiModelConfig, messages: list[dict[str, str]], max_tokens: int | None = None) -> dict[str, Any]:
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
            "stream": False,
        }
        headers = {
            "Authorization": f"Bearer {config.api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=120) as client:
            response = client.post(endpoint, headers=headers, json=payload)
            response.raise_for_status()
            data = response.json()

        content = data.get("choices", [{}])[0].get("message", {}).get("content")
        if not content:
            raise RuntimeError("响应中没有 choices[0].message.content")
        usage = data.get("usage") or {}
        return {"content": content, "token_count": int(usage.get("total_tokens") or max(1, len(content) // 2))}

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

    def _clean_points_from_params(self, input_params: dict[str, Any] | None, subject: str) -> list[str]:
        if not input_params:
            return self._clean_points([], subject)
        raw_points = (
            input_params.get("knowledge_points")
            or input_params.get("knowledgePoints")
            or input_params.get("points")
            or input_params.get("keywords")
        )
        return self._clean_points(raw_points, subject)

    def _clean_points(self, raw_points: Any, fallback: str) -> list[str]:
        values: list[str] = []
        if isinstance(raw_points, list):
            for item in raw_points:
                values.extend(self._split_point_text(item))
        elif isinstance(raw_points, str):
            values.extend(self._split_point_text(raw_points))
        cleaned = []
        for item in values:
            text = str(item).strip()
            if text and text not in cleaned:
                cleaned.append(text)
        fallback_text = self._safe_topic(fallback, "综合学习主题")
        return cleaned or [fallback_text]

    def _split_point_text(self, value: Any) -> list[str]:
        return [part.strip() for part in re.split(r"[,，、;\n]+", str(value)) if part.strip()]

    def _safe_topic(self, value: Any, fallback: str) -> str:
        text = str(value).strip() if value is not None else ""
        return text or fallback

    def _generation_tokens(self, config: AiModelConfig, scale: int) -> int:
        requested = int(config.max_tokens or 2048)
        return min(max(1200, requested, scale * 260), 5000)

    def _normalize_quiz_questions(
        self,
        payload: dict[str, Any],
        fallback_points: list[str],
        difficulty: str,
        expected_count: int,
    ) -> list[QuizQuestion]:
        raw_questions = payload.get("questions")
        if not isinstance(raw_questions, list) or not raw_questions:
            raise ValueError("模型未返回 questions 数组")
        questions: list[QuizQuestion] = []
        for index, item in enumerate(raw_questions[:expected_count], start=1):
            if not isinstance(item, dict):
                continue
            question_type = str(item.get("question_type") or item.get("type") or "short_answer").strip()
            if question_type not in {"single_choice", "judge", "short_answer"}:
                question_type = "short_answer"
            options = item.get("options") if isinstance(item.get("options"), list) else []
            if question_type == "judge" and not options:
                options = ["正确", "错误"]
            if question_type == "single_choice" and len(options) < 2:
                question_type = "short_answer"
                options = []
            stem = self._safe_topic(item.get("stem") or item.get("question"), "")
            answer = self._safe_topic(item.get("answer_text") or item.get("answer"), "")
            analysis = self._safe_topic(item.get("analysis_text") or item.get("analysis"), "请结合题干知识点复盘。")
            if not stem or not answer:
                continue
            questions.append(
                QuizQuestion(
                    question_order=index,
                    question_type=question_type,
                    stem=stem,
                    options=[str(option).strip() for option in options if str(option).strip()],
                    answer_text=answer,
                    analysis_text=analysis,
                    knowledge_points=self._clean_points(item.get("knowledge_points"), fallback_points[0]),
                    difficulty=str(item.get("difficulty") or difficulty or "medium"),
                    score=float(item.get("score") or 10),
                )
            )
        if not questions:
            raise ValueError("模型返回的题目缺少题干或答案")
        return questions

    def _normalize_learning_items(
        self,
        payload: dict[str, Any],
        fallback_points: list[str],
        days: int,
    ) -> list[LearningPathItem]:
        raw_items = payload.get("items")
        if not isinstance(raw_items, list) or not raw_items:
            raise ValueError("模型未返回 items 数组")
        items: list[LearningPathItem] = []
        for index, item in enumerate(raw_items, start=1):
            if not isinstance(item, dict):
                continue
            title = self._safe_topic(item.get("title"), "")
            description = self._safe_topic(item.get("description"), "")
            if not title or not description:
                continue
            due_day = int(item.get("due_day") or min(index, days))
            due_day = max(1, min(due_day, days))
            minutes = int(item.get("estimated_minutes") or 30)
            difficulty = str(item.get("difficulty") or "medium")
            if difficulty not in {"easy", "medium", "hard"}:
                difficulty = "medium"
            items.append(
                LearningPathItem(
                    title=title,
                    description=description,
                    knowledge_points=self._clean_points(item.get("knowledge_points"), fallback_points[0]),
                    estimated_minutes=max(10, min(minutes, 240)),
                    difficulty=difficulty,
                    due_day=due_day,
                )
            )
        if not items:
            raise ValueError("模型返回的学习路径缺少有效节点")
        return items

    def _normalize_agent_steps(self, payload: dict[str, Any]) -> list[AgentStep]:
        raw_steps = payload.get("steps") if isinstance(payload.get("steps"), list) else []
        defaults = [
            ("PlannerAgent", "planning"),
            ("KnowledgeAgent", "generation"),
            ("ExerciseAgent", "generation"),
            ("ReviewAgent", "review"),
        ]
        steps: list[AgentStep] = []
        for index, item in enumerate(raw_steps[:6], start=1):
            if not isinstance(item, dict):
                continue
            agent_name, step_type = defaults[min(index - 1, len(defaults) - 1)]
            steps.append(
                AgentStep(
                    agent_name=self._safe_topic(item.get("agent_name"), agent_name),
                    step_order=int(item.get("step_order") or index),
                    step_type=self._safe_topic(item.get("step_type"), step_type),
                    output_summary=self._safe_topic(item.get("output_summary"), "已完成该智能体步骤。"),
                    result_json=item.get("result_json") if isinstance(item.get("result_json"), dict) else {},
                )
            )
        if steps:
            return steps
        return [
            AgentStep(agent_name=name, step_order=index, step_type=step_type, output_summary="模型已完成该阶段分析。")
            for index, (name, step_type) in enumerate(defaults, start=1)
        ]

    def _normalize_graph_payload(self, payload: dict[str, Any], subject: str, points: list[str]) -> dict[str, Any]:
        raw_nodes = payload.get("nodes") if isinstance(payload.get("nodes"), list) else []
        raw_edges = payload.get("edges") if isinstance(payload.get("edges"), list) else []
        nodes: list[dict[str, str]] = []
        seen: set[str] = set()
        for index, item in enumerate(raw_nodes[:18], start=1):
            if not isinstance(item, dict):
                continue
            node_id = self._safe_node_id(item.get("id"), index)
            label = self._safe_topic(item.get("label"), "")
            if label and node_id not in seen:
                seen.add(node_id)
                nodes.append({"id": node_id, "label": label})
        if not nodes:
            nodes = [{"id": "ROOT", "label": subject}] + [
                {"id": f"N{index}", "label": point} for index, point in enumerate(points[:8], start=1)
            ]
            seen = {node["id"] for node in nodes}
        if "ROOT" not in seen:
            nodes.insert(0, {"id": "ROOT", "label": subject})
            seen.add("ROOT")

        edges: list[dict[str, str]] = []
        for item in raw_edges[:24]:
            if not isinstance(item, dict):
                continue
            source = self._safe_node_id(item.get("source"), 0)
            target = self._safe_node_id(item.get("target"), 0)
            if source in seen and target in seen and source != target:
                edges.append({"source": source, "target": target, "label": self._safe_topic(item.get("label"), "")})
        if not edges:
            edges = [{"source": "ROOT", "target": node["id"], "label": "关联"} for node in nodes if node["id"] != "ROOT"]
        return {
            "title": payload.get("title") or f"{subject} 知识图谱",
            "nodes": nodes,
            "edges": edges,
            "explanation": payload.get("explanation"),
        }

    def _build_mermaid_from_graph(self, graph: dict[str, Any]) -> str:
        lines = ["graph TD"]
        for node in graph["nodes"]:
            lines.append(f'  {node["id"]}["{self._safe_mermaid_label(node["label"])}"]')
        for edge in graph["edges"]:
            label = self._safe_mermaid_label(edge.get("label") or "")
            arrow = f' -->|{label}| ' if label else " --> "
            lines.append(f'  {edge["source"]}{arrow}{edge["target"]}')
        return "\n".join(lines)

    def _safe_node_id(self, value: Any, index: int) -> str:
        text = re.sub(r"[^A-Za-z0-9_]", "_", str(value or "").strip())
        if not text or text[0].isdigit():
            text = f"N{index or 1}"
        return text[:32]

    def _safe_mermaid_label(self, value: Any) -> str:
        text = self._safe_topic(value, "知识点")
        return text.replace("\\", "\\\\").replace('"', "'").replace("[", "（").replace("]", "）")

    def _fallback_chart_data(self, overview: dict[str, Any], mastery_records: list[dict[str, Any]]) -> dict[str, Any]:
        return {
            "mastery": mastery_records,
            "overview": [
                {"name": "资源数", "value": overview.get("resource_count", 0)},
                {"name": "测验数", "value": overview.get("quiz_count", 0)},
                {"name": "路径数", "value": overview.get("path_count", 0)},
            ],
        }

    def _safe_error(self, exc: Exception) -> str:
        return str(exc)[:300]

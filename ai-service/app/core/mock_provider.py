from datetime import datetime

from app.core.llm_provider import LLMProvider
from app.schemas.agents import AgentRunRequest, AgentRunResponse, AgentStep, GeneratedResource
from app.schemas.chat import ChatIntentRequest, ChatIntentResponse, ChatRequest, ChatResponse
from app.schemas.model import ModelTestRequest, ModelTestResponse
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

    def run_agents(self, request: AgentRunRequest) -> AgentRunResponse:
        subject = request.subject or request.title or "学习主题"
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
            content = f"# {subject} 知识图谱\n\n```mermaid\n{mermaid}```\n\n## 使用建议\n先按图谱理解依赖关系，再逐个补齐薄弱节点。"
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
            content = "# 习题集\n\n" + "\n".join([f"{idx}. {item['stem']}" for idx, item in enumerate(questions, start=1)])
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
            content = "# PPT 大纲\n\n" + "\n".join([f"## {item['page']}. {item['title']}" for item in slides])
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

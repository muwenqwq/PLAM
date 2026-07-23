import pytest

from app.core.mock_provider import MockLLMProvider
from app.core.resource_templates import RESOURCE_TYPE_LABELS, validate_resource_markdown
from app.schemas.agents import AgentRunRequest
from app.schemas.learning_paths import LearningPathGenerateRequest
from app.schemas.model import AiModelConfig
from app.schemas.resources import ResourceGenerateRequest


TYPE_MARKERS = {
    "lecture_note": ["核心讲解", "例题或场景", "即时自测"],
    "summary": ["一分钟速览", "容易混淆", "自检清单"],
    "knowledge_graph": ["```mermaid", "关系解读", "学习顺序"],
    "quiz_set": ["选择题", "参考答案", "逐项解析"],
    "review_outline": ["优先级", "章节提纲", "考前检查"],
    "mistake_review": ["错因诊断", "订正步骤", "复测标准"],
    "plan": ["每日安排", "学习产出", "验收标准"],
    "case_task": ["案例背景", "交付物", "评价标准"],
}


@pytest.mark.parametrize("resource_type", TYPE_MARKERS)
def test_each_resource_type_has_its_own_complete_structure(resource_type: str):
    provider = MockLLMProvider()
    response = provider.generate_resource(
        ResourceGenerateRequest(
            model_config=AiModelConfig(),
            title=f"数据库系统{RESOURCE_TYPE_LABELS[resource_type]}",
            subject="数据库系统",
            resource_type=resource_type,
            input_params={
                "knowledge_points": ["B+树索引", "事务隔离", "查询优化"],
                "difficulty": "medium",
                "output_length": "medium",
                "knowledge_context": [
                    {
                        "source": "数据库课程讲义.pdf",
                        "content": "B+树索引通过有序叶子节点支持范围查询，使用时需要关注选择性和维护成本。",
                    }
                ],
            },
        )
    )
    resource = response.resources[0]
    assert resource.resource_type == resource_type
    assert resource.content_json["resource_type"] == resource_type
    assert resource.content_json["quality_checks"]["valid"] is True
    assert validate_resource_markdown(resource_type, resource.content_markdown)["valid"] is True
    for marker in TYPE_MARKERS[resource_type]:
        assert marker in resource.content_markdown


def test_resource_types_do_not_return_the_same_generic_body():
    provider = MockLLMProvider()
    bodies = []
    for resource_type in TYPE_MARKERS:
        response = provider.generate_resource(
            ResourceGenerateRequest(
                model_config=AiModelConfig(),
                title=f"软件工程{RESOURCE_TYPE_LABELS[resource_type]}",
                subject="软件工程",
                resource_type=resource_type,
                input_params={"knowledge_points": ["需求分析", "软件测试", "配置管理"]},
            )
        )
        bodies.append(response.resources[0].content_markdown)
    assert len(set(bodies)) == len(TYPE_MARKERS)


def test_resource_metadata_confirms_profile_use_without_copying_profile_values():
    provider = MockLLMProvider()
    response = provider.generate_resource(
        ResourceGenerateRequest(
            model_config=AiModelConfig(),
            title="个性化数据库复习计划",
            subject="数据库系统",
            resource_type="plan",
            input_params={"knowledge_points": ["事务隔离", "B+树索引"]},
            profile={
                "learning_goal": "七天内完成数据库复习",
                "foundation_level": "beginner",
                "output_style": "案例优先",
            },
        )
    )
    metadata = response.resources[0].content_json
    assert metadata["profile_adapted"] is True
    assert {"learning_goal", "foundation_level", "output_style"}.issubset(metadata["profile_fields"])
    assert "七天内完成数据库复习" not in str(metadata)


def test_mistake_review_does_not_invent_student_mistakes():
    provider = MockLLMProvider()
    response = provider.generate_resource(
        ResourceGenerateRequest(
            model_config=AiModelConfig(),
            title="错题整理",
            subject="软件工程",
            resource_type="mistake_review",
            input_params={"knowledge_points": ["UML 建模"]},
        )
    )
    content = response.resources[0].content_markdown
    assert "尚未提供真实错题" in content
    assert response.resources[0].content_json["has_actual_mistakes"] is False


def test_agent_pipeline_exposes_real_inputs_outputs_and_requested_resource_type():
    provider = MockLLMProvider()
    response = provider.run_agents(
        AgentRunRequest(
            model_config=AiModelConfig(),
            title="软件工程案例训练",
            subject="软件工程",
            resource_type="case_task",
            input_params={"knowledge_points": ["需求分析", "验收测试"]},
        )
    )
    assert response.success is True
    assert response.resources[0].resource_type == "case_task"
    assert "案例背景" in response.resources[0].content_markdown
    assert len(response.steps) == 4
    assert all(step.input_json for step in response.steps)
    assert all(step.result_json for step in response.steps)


def test_learning_path_has_distinct_daily_tasks_deliverables_and_acceptance_criteria():
    provider = MockLLMProvider()
    response = provider.generate_learning_path(
        LearningPathGenerateRequest(
            model_config=AiModelConfig(),
            subject="数据库系统",
            goal="七天内完成期末复习并能独立做综合题",
            knowledge_points=["关系模型", "事务隔离", "B+树索引"],
            days=7,
        )
    )
    assert len(response.items) == 7
    assert len({item.description for item in response.items}) == 7
    assert [item.due_day for item in response.items] == list(range(1, 8))
    assert all("学习产出" in item.description for item in response.items)
    assert all("完成标准" in item.description for item in response.items)
    assert response.plan_json["daily_plan"]

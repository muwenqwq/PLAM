from app.core.mock_provider import MockLLMProvider
from app.core.openai_compatible_provider import OpenAICompatibleProvider
from app.schemas.model import AiModelConfig
from app.schemas.resources import ResourceGenerateRequest


def _request(mistakes):
    return ResourceGenerateRequest(
        model_config=AiModelConfig(),
        title="数据库错题整理",
        subject="数据库",
        resource_type="mistake_review",
        input_params={"knowledge_points": ["索引"], "mistakes": mistakes},
    )


def test_mistake_review_does_not_fabricate_when_space_has_no_wrong_answers():
    resource = MockLLMProvider().generate_resource(_request([])).resources[0]
    assert "暂无" in resource.content_markdown or "还没有" in resource.content_markdown
    assert "待填写" not in resource.content_markdown
    assert resource.content_json["card_count"] == 0


def test_mistake_review_uses_actual_quiz_answer_and_option_explanations():
    resource = MockLLMProvider().generate_resource(
        _request([
            {
                "quiz_title": "数据库阶段测验",
                "question_order": 2,
                "stem": "B+ 树索引适合哪类查询？",
                "options": ["A. 范围查询", "B. 只做哈希匹配", "C. 不支持排序", "D. 不能落盘"],
                "student_answer": "B",
                "correct_answer": "A",
                "analysis": "B+ 树叶子节点有序并通过链表相连，适合范围查询。",
                "option_explanations": {"A": "正确，叶子节点有序。", "B": "错误，这是哈希索引的特点。"},
                "knowledge_points": ["B+ 树索引"],
            }
        ])
    ).resources[0]
    assert "B+ 树索引适合哪类查询" in resource.content_markdown
    assert "我的答案**：B" in resource.content_markdown
    assert "正确答案**：A" in resource.content_markdown
    assert "哈希索引的特点" in resource.content_markdown
    assert resource.content_json["card_count"] == 1


def test_real_provider_locks_original_mistake_evidence(monkeypatch):
    mistakes = [
        {
            "quiz_title": "数据库阶段测验",
            "question_order": 2,
            "stem": "B+ 树索引适合哪类查询？",
            "options": ["A. 范围查询", "B. 只做哈希匹配", "C. 不支持排序", "D. 不能落盘"],
            "student_answer": "B",
            "correct_answer": "A",
            "analysis": "B+ 树叶子节点有序并通过链表相连，适合范围查询。",
            "option_explanations": {"A": "正确，叶子节点有序。", "B": "错误，这是哈希索引的特点。"},
            "knowledge_points": ["B+ 树索引"],
        }
    ]
    request = _request(mistakes)
    request.model.provider_type = "openai_compatible"
    request.model.api_key = "test-key"

    model_content = """# 改写后的错题分析

## 错因诊断
模型把原题改写成另一道题，但系统不能把这段内容当成原始证据。这里补充足够长度以通过结构检查。

## 订正步骤
重新理解概念、核对条件并完成练习。

## 正确规则
应当以提交测验时保存的题干、选项和答案为准。

## 变式练习
完成一组相似练习并说明判断依据。

## 复测标准
连续两次作答正确，并能够解释全部选项。为了模拟真实模型输出，这里继续补充学习建议和复盘要求，确保正文长度达到资源质量检查的最低标准。学生需要先定位错误发生在知识边界、题干条件还是排除步骤，再写出可复用的判断规则，最后通过间隔复测确认掌握。"""

    monkeypatch.setattr(
        OpenAICompatibleProvider,
        "_complete",
        lambda self, config, messages, max_tokens=None: {
            "token_count": 100,
            "content": '{"title":"模型改写版","output_summary":"已分析真实错题","knowledge_points":["B+ 树索引"],"content_markdown":' + __import__("json").dumps(model_content, ensure_ascii=False) + '}',
        },
    )

    resource = OpenAICompatibleProvider().generate_resource(request).resources[0]
    assert "B+ 树索引适合哪类查询" in resource.content_markdown
    assert "我的答案**：B" in resource.content_markdown
    assert "正确答案**：A" in resource.content_markdown
    assert resource.content_json["real_provider"] is True
    assert resource.content_json["evidence_locked"] is True

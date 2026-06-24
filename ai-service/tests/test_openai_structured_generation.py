from fastapi.testclient import TestClient

from app.core.openai_compatible_provider import OpenAICompatibleProvider
from app.main import app


client = TestClient(app)


def _real_model():
    return {
        "provider_type": "openai_compatible",
        "base_url": "http://example.test/v1",
        "api_key": "test-key",
        "model_name": "test-chat",
        "temperature": 0.2,
        "max_tokens": 1200,
    }


def test_quiz_generate_uses_model_structured_questions(monkeypatch):
    def fake_complete(self, config, messages, max_tokens=None):
        return {
            "token_count": 120,
            "content": """
            {
              "title": "Linux 权限模型专项测验",
              "subject": "Linux",
              "difficulty": "medium",
              "questions": [
                {
                  "question_type": "single_choice",
                  "stem": "chmod 754 中属主、属组、其他用户分别拥有哪些权限？",
                  "options": ["A. rwx r-x r--", "B. rw- r-x r--", "C. rwx rwx r--", "D. r-- r-x rwx"],
                  "answer_text": "A",
                  "analysis_text": "7 表示 rwx，5 表示 r-x，4 表示 r--。",
                  "knowledge_points": ["Linux 文件权限", "chmod"],
                  "score": 10
                },
                {
                  "question_type": "short_answer",
                  "stem": "简述 Linux 中用户、用户组与文件权限位之间的关系。",
                  "answer_text": "文件权限按属主、属组、其他用户三类分别控制读写执行。",
                  "analysis_text": "应说明三类主体和 rwx 权限位的对应关系。",
                  "knowledge_points": ["Linux 文件权限"],
                  "score": 10
                }
              ]
            }
            """,
        }

    monkeypatch.setattr(OpenAICompatibleProvider, "_complete", fake_complete)
    response = client.post(
        "/ai/quizzes/generate",
        json={
            "model_config": _real_model(),
            "subject": "Linux",
            "knowledge_points": ["Linux 文件权限", "chmod"],
            "question_count": 2,
            "difficulty": "medium",
        },
    )

    assert response.status_code == 200
    data = response.json()
    stems = [item["stem"] for item in data["questions"]]
    assert "chmod 754" in stems[0]
    assert all("只需要记忆结论" not in " ".join(item.get("options", [])) for item in data["questions"])


def test_learning_path_generate_uses_model_plan(monkeypatch):
    def fake_complete(self, config, messages, max_tokens=None):
        return {
            "token_count": 100,
            "content": """
            {
              "title": "Linux 系统学习路径",
              "summary": "先建立命令行与权限模型，再进入进程、服务和日志排查。",
              "items": [
                {
                  "title": "建立文件系统与权限模型",
                  "description": "用 ls、chmod、chown 完成权限观察和修改练习。",
                  "knowledge_points": ["文件权限", "用户组"],
                  "estimated_minutes": 45,
                  "difficulty": "medium",
                  "due_day": 1
                },
                {
                  "title": "进程与服务排查",
                  "description": "使用 ps、systemctl 和 journalctl 分析服务启动失败。",
                  "knowledge_points": ["进程", "systemd"],
                  "estimated_minutes": 50,
                  "difficulty": "hard",
                  "due_day": 2
                }
              ]
            }
            """,
        }

    monkeypatch.setattr(OpenAICompatibleProvider, "_complete", fake_complete)
    response = client.post(
        "/ai/learning-paths/generate",
        json={
            "model_config": _real_model(),
            "subject": "Linux",
            "goal": "三天掌握服务器常用排障",
            "knowledge_points": ["文件权限", "进程", "systemd"],
            "days": 3,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["title"] == "Linux 系统学习路径"
    assert data["items"][0]["title"] == "建立文件系统与权限模型"


def test_report_generate_uses_model_analysis(monkeypatch):
    def fake_complete(self, config, messages, max_tokens=None):
        return {
            "token_count": 80,
            "content": """
            {
              "summary": "学习记录显示资源生成充足，但测验正确率集中卡在 Linux 权限和进程排查。",
              "suggestion_text": "建议下一轮减少泛读，改为每天一次命令行实操和错题复盘。",
              "report_json": {"weak_points": ["Linux 权限", "进程排查"], "next_focus": "实操"},
              "chart_data_json": {"mastery": [{"name": "Linux 权限", "value": 55}]}
            }
            """,
        }

    monkeypatch.setattr(OpenAICompatibleProvider, "_complete", fake_complete)
    response = client.post(
        "/ai/reports/generate",
        json={
            "model_config": _real_model(),
            "title": "Linux 学习周报",
            "overview": {"quiz_count": 2, "average_score": 64},
            "mastery_records": [{"knowledge_point": "Linux 权限", "mastery_level": 55}],
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert "Linux 权限" in data["summary"]
    assert data["report_json"]["next_focus"] == "实操"


def test_knowledge_graph_resource_uses_model_nodes(monkeypatch):
    def fake_complete(self, config, messages, max_tokens=None):
        return {
            "token_count": 100,
            "content": """
            {
              "title": "Linux 权限知识图谱",
              "nodes": [
                {"id": "ROOT", "label": "Linux 权限模型"},
                {"id": "USER", "label": "属主 user"},
                {"id": "GROUP", "label": "属组 group"},
                {"id": "OTHER", "label": "其他用户 other"},
                {"id": "MODE", "label": "rwx 与八进制权限"}
              ],
              "edges": [
                {"source": "ROOT", "target": "USER", "label": "按身份分层"},
                {"source": "ROOT", "target": "GROUP", "label": "按身份分层"},
                {"source": "ROOT", "target": "OTHER", "label": "按身份分层"},
                {"source": "ROOT", "target": "MODE", "label": "映射为权限位"}
              ],
              "explanation": "图谱突出 Linux 文件权限的主体分类和 rwx 位映射。"
            }
            """,
        }

    monkeypatch.setattr(OpenAICompatibleProvider, "_complete", fake_complete)
    response = client.post(
        "/ai/resources/generate",
        json={
            "model_config": _real_model(),
            "title": "Linux 权限知识图谱",
            "subject": "Linux",
            "resource_type": "knowledge_graph",
            "input_params": {"knowledge_points": ["Linux 文件权限", "chmod"]},
        },
    )

    assert response.status_code == 200
    resource = response.json()["resources"][0]
    assert "属主 user" in resource["content_markdown"]
    assert "理解定义" not in resource["content_markdown"]

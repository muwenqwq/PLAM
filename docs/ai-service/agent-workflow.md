# 多智能体工作流

当前 Mock 工作流包含：

1. `PlannerAgent`：拆分学习目标和阶段。
2. `KnowledgeAgent`：抽取知识点和薄弱点。
3. `ExerciseAgent`：生成练习建议。
4. `ReviewAgent`：检查质量并给出评分。

Python 返回 steps 和 resources，Java 负责保存到 `agent_step` 与 `generated_resource`。


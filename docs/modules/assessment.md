# 测验与掌握度模块说明

## 测验流程

1. Java 调用 Python `/ai/quizzes/generate` 生成题目。
2. Java 保存 `quiz` 与 `quiz_question`。
3. 用户提交答案后，Java 对选择题、判断题和简答题进行规则评分。
4. Java 保存或更新 `quiz_answer`。
5. Java 根据得分更新 `mastery_record`。
6. Java 调用 Python `/ai/quizzes/analyze` 生成 Mock 分析文本。

## 掌握度规则

单题掌握度按 `本题得分 / 本题满分 * 100` 计算。掌握度大于等于 80 记为 `mastered`，60 到 80 记为 `learning`，低于 60 记为 `weak`。

## 简答题评分

当前阶段使用关键词覆盖率进行 Mock 评分，最低给 40% 的演示分。后续可替换为真实 LLM 语义评分，但评分结果仍由 Java 写入数据库。

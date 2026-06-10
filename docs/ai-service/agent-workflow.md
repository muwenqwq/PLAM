# Python AI 服务工作流说明

Python AI 服务当前提供模型测试、对话、多智能体 Mock、资源生成、RAG、学习路径、测验和报告生成能力。它不直接访问 MySQL，也不承担业务权限判断。

## 工作流边界

1. Java 负责认证、权限、业务事务和数据库写入。
2. Python 负责生成结构化 Mock 结果。
3. 前端只调用 Java 后端。
4. AI 服务不可用时，Java 通过统一异常返回可诊断信息。

## 新增接口

- `/ai/learning-paths/generate`
- `/ai/learning-paths/adjust`
- `/ai/quizzes/generate`
- `/ai/quizzes/analyze`
- `/ai/reports/generate`

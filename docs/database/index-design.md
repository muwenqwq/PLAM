# 索引设计说明

## 1. 索引设计目标

索引设计服务于三个目标：

1. 支持用户数据隔离：大多数业务查询都会带 `user_id` 和 `deleted = 0`。
2. 支持学习空间聚合：对话、知识库、任务、资源、路径、测验和报告需要按 `space_id` 查询。
3. 支持演示场景性能：Dashboard、任务列表、资源列表和测验结果需要快速查询。

## 2. 唯一约束

| 表 | 唯一索引 | 目的 |
|---|---|---|
| `sys_user` | `uk_sys_user_username` | 用户名唯一，支持登录 |
| `sys_user` | `uk_sys_user_email` | 邮箱唯一，便于后续找回密码 |
| `sys_role` | `uk_sys_role_code` | 角色编码唯一 |
| `sys_user_role` | `uk_sys_user_role_user_role_deleted` | 防止同一用户重复绑定同一角色 |
| `user_profile` | `uk_user_profile_user_deleted` | 一个用户一份学习画像 |
| `learning_preference` | `uk_learning_preference_user_deleted` | 一个用户一份学习偏好 |
| `knowledge_chunk` | `uk_knowledge_chunk_file_index_deleted` | 同一文件内切片序号唯一 |
| `agent_step` | `uk_agent_step_task_order_deleted` | 同一任务内步骤序号唯一 |
| `learning_path_item` | `uk_learning_path_item_order_deleted` | 同一路径内任务序号唯一 |
| `quiz_question` | `uk_quiz_question_order_deleted` | 同一测验内题目序号唯一 |
| `quiz_answer` | `uk_quiz_answer_question_user_deleted` | 同一用户对同一题只保留一条有效答题记录 |
| `mastery_record` | `uk_mastery_record_point_deleted` | 同一用户同一空间同一知识点只有一条有效掌握记录 |

## 3. 常用查询索引

### 3.1 用户维度

- `idx_learning_space_user_status(user_id, status, deleted)`
- `idx_ai_model_provider_user_default(user_id, is_default, deleted)`
- `idx_conversation_user_status(user_id, status, deleted)`
- `idx_agent_task_user_status(user_id, execution_status, deleted)`
- `idx_generated_resource_user_type(user_id, resource_type, deleted)`

这些索引用于“我的学习空间”“我的默认模型”“我的对话”“我的任务”“我的资源”等核心页面。

### 3.2 学习空间维度

- `idx_conversation_space(space_id, deleted)`
- `idx_knowledge_file_user_space(user_id, space_id, deleted)`
- `idx_knowledge_chunk_space(space_id, deleted)`
- `idx_agent_task_space(space_id, deleted)`
- `idx_generated_resource_space(space_id, deleted)`
- `idx_learning_path_user_space(user_id, space_id, deleted)`
- `idx_quiz_user_space(user_id, space_id, deleted)`
- `idx_learning_report_user_space(user_id, space_id, deleted)`

这些索引用于空间详情页和 Dashboard 聚合。

### 3.3 状态和时间维度

- `idx_agent_task_type_created(task_type, created_at)`
- `idx_conversation_updated_at(updated_at)`
- `idx_operation_log_user_created(user_id, created_at)`
- `idx_learning_report_type_created(report_type, created_at)`
- `idx_learning_path_item_due_date(due_date)`

这些索引用于任务排序、近期对话、操作审计、报告查询和今日学习任务。

## 4. 为什么不为所有 JSON 字段建索引

本阶段 JSON 字段主要用于保存 AI 输入、AI 输出、图表数据、知识点列表和配置快照。它们更适合作为结构化内容载体，而不是高频检索条件。后续如果出现稳定查询需求，可以在 Java 后端提取关键字段到普通列，或使用 MySQL 生成列和函数索引。

## 5. 索引维护原则

1. 列表页和 Dashboard 优先使用组合索引。
2. 所有业务查询默认加 `deleted = 0`。
3. 不为低选择性的布尔字段单独建索引。
4. 不在大文本字段上建索引。
5. AI 相关 JSON 结果不直接作为查询主条件。
6. 如果后续接入真实向量库，`knowledge_chunk.embedding_ref` 只保存外部向量引用，不在 MySQL 内做向量检索。

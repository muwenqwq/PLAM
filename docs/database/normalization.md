# 范式分析

## 1. 总体结论

本项目数据库整体满足第三范式（3NF）。各表围绕单一业务实体建模，非主键字段依赖于主键，且避免将多个独立实体混合在同一张表中。对于用户、角色、学习空间、模型配置、对话、知识库、智能体任务、学习资源、测验和报告等实体，都分别建立独立表。

同时，项目为了查询效率、前端展示和 AI 任务追踪，保留了少量合理冗余字段。这些字段不会破坏主体设计的规范性，属于面向工程实践的性能和可展示性优化。

## 2. 第一范式

所有表字段保持原子性：

- 用户基础信息拆分为 `username`、`nickname`、`email`、`phone` 等字段。
- AI 模型配置拆分为 `provider_type`、`base_url`、`model_name`、`embedding_model`、`temperature`、`max_tokens` 等字段。
- 任务执行状态拆分为 `execution_status`、`started_at`、`finished_at`、`error_message` 等字段。

JSON 字段用于保存结构化 AI 输入输出，例如 `input_params`、`result_json`、`content_json` 和 `chart_data_json`。这些字段承载的是不固定结构的 AI 结果或前端图表数据，不用于替代核心实体字段，因此不影响主要业务表的规范化设计。

## 3. 第二范式

所有表使用单列自增主键 `id`，不存在复合主键下的部分依赖问题。多对多关系通过关系表表达：

- `sys_user_role` 连接用户和角色。
- 学习路径和路径项拆分为 `learning_path` 与 `learning_path_item`。
- 测验和题目拆分为 `quiz` 与 `quiz_question`。
- 题目和答题记录拆分为 `quiz_question` 与 `quiz_answer`。

## 4. 第三范式

非主键字段不依赖其他非主键字段：

- 用户角色名称放在 `sys_role`，不直接写入 `sys_user`。
- 学习空间信息放在 `learning_space`，对话、资源和测验只保存 `space_id`。
- 模型配置放在 `ai_model_provider`，智能体任务只保存 `provider_id`。
- 测验题目标准答案放在 `quiz_question`，用户答题结果放在 `quiz_answer`。

## 5. 合理冗余字段

为了提高查询和展示效率，以下字段属于合理冗余：

| 表 | 字段 | 冗余目的 |
|---|---|---|
| `learning_space` | `resource_count` | Dashboard 和空间列表快速展示资源数量 |
| `learning_space` | `task_count` | 快速展示空间内智能体任务数量 |
| `learning_space` | `is_default` | 快速定位当前用户默认学习空间，避免每次额外计算 |
| `user_profile` | `space_id = 0` | 用固定值表达全局画像范围，便于唯一约束和 MyBatis-Plus 查询 |
| `conversation` | `message_count` | 对话列表无需每次统计消息表 |
| `knowledge_file` | `chunk_count` | 文件列表快速展示解析切片数量 |
| `generated_resource` | `output_summary` | 资源列表快速展示摘要 |
| `agent_task` | `output_summary` | 任务列表快速展示结果摘要 |
| `mastery_record` | `last_score` | 掌握度页面快速展示最近表现 |
| `operation_log` | `username` | 日志审计保留操作人快照，避免用户昵称变化影响历史日志 |

这些字段需要由 Java Service 层在写入、更新和删除相关业务数据时同步维护。

## 6. 逻辑删除与范式

所有业务表包含 `deleted` 字段。逻辑删除会让唯一约束和业务查询更复杂，因此部分唯一索引包含 `deleted` 字段，例如 `uk_user_profile_user_space_deleted`。这样可以保留历史数据，同时避免有效数据重复。

## 7. 后续扩展建议

1. 如果系统进入生产阶段，可对稳定核心关系补充强外键，例如 `sys_user_role.user_id -> sys_user.id`。
2. 如果 JSON 字段出现高频查询需求，应提取为普通字段或建立生成列。
3. 如果知识库规模扩大，应将向量检索迁移到 Chroma、Milvus 或 Elasticsearch 等专用系统，MySQL 只保存元数据。

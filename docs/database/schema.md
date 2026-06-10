# 数据库结构设计说明

## 1. 总体设计策略

本项目数据库使用 MySQL 8，数据库名为 `eduagent_studio`。数据库设计服务于 Java Spring Boot + MyBatis-Plus 主后端，要求表名和字段名稳定、规范，便于后续生成 Java Entity、Mapper、Service 和 Controller。

本项目采用“逻辑外键为主、数据库强外键为辅”的设计策略。表之间通过 `user_id`、`role_id`、`space_id`、`conversation_id`、`task_id`、`resource_id`、`quiz_id` 等字段表达关系，但当前阶段不在 DDL 中强制建立大量数据库外键。这样做的原因是：

1. MyBatis-Plus 常用逻辑删除，强外键会增加逻辑删除和演示数据清理的复杂度。
2. AI 任务、知识库和资源生成结果存在异步写入场景，逻辑外键更便于任务失败重试和数据补偿。
3. 比赛和课程验收环境可能需要快速初始化、清空和重建演示数据，减少强外键约束可以降低部署风险。
4. 业务约束由 Java 后端 Service 层统一校验，必要时可在生产阶段对核心稳定关系补充数据库强外键。

## 2. 通用字段规范

所有业务表都包含：

- `id`：`BIGINT AUTO_INCREMENT` 主键。
- `created_at`：创建时间，默认 `CURRENT_TIMESTAMP`。
- `updated_at`：更新时间，默认 `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`。
- `deleted`：逻辑删除标记，`0` 表示未删除，`1` 表示已删除。

需要状态控制的表统一使用：

- `status`：业务状态，例如 `active`、`disabled`、`archived`、`pending`、`succeeded`、`failed`。

字段命名统一使用下划线命名，例如 `user_id`、`created_at`、`updated_at`、`is_default`、`api_key_encrypted`。

## 3. 表清单

| 序号 | 表名 | 说明 |
|---:|---|---|
| 1 | `sys_user` | 系统用户表 |
| 2 | `sys_role` | 系统角色表 |
| 3 | `sys_user_role` | 用户角色关系表 |
| 4 | `learning_space` | 学习空间表 |
| 5 | `user_profile` | 用户学习画像表 |
| 6 | `learning_preference` | 学习偏好表 |
| 7 | `ai_model_provider` | 用户 AI 模型配置表 |
| 8 | `conversation` | 学习对话表 |
| 9 | `conversation_message` | 对话消息表 |
| 10 | `knowledge_file` | 知识库文件表 |
| 11 | `knowledge_chunk` | 知识库切片表 |
| 12 | `agent_task` | 多智能体任务表 |
| 13 | `agent_step` | 多智能体执行步骤表 |
| 14 | `generated_resource` | 生成学习资源表 |
| 15 | `learning_path` | 个性化学习路径表 |
| 16 | `learning_path_item` | 学习路径任务项表 |
| 17 | `quiz` | 在线测验表 |
| 18 | `quiz_question` | 测验题目表 |
| 19 | `quiz_answer` | 用户答题记录表 |
| 20 | `mastery_record` | 知识点掌握度记录表 |
| 21 | `learning_report` | 学习报告表 |
| 22 | `operation_log` | 操作日志表 |

## 4. 核心实体关系

### 4.1 用户与权限

- `sys_user` 与 `sys_role` 通过 `sys_user_role` 建立多对多关系。
- `sys_user.username` 唯一，用于登录。
- `sys_user.password_hash` 保存 BCrypt 哈希，不保存明文密码。

### 4.2 学习空间与画像

- 一个用户可以拥有多个 `learning_space`。
- `learning_space.is_default` 表示当前用户默认学习空间。一个用户最多应有一个有效默认空间，该约束由 Java Service 层在设置默认空间时统一维护。
- `user_profile` 支持全局画像和空间画像：`space_id = 0` 表示当前用户全局画像，`space_id > 0` 表示绑定到某个学习空间的画像。
- `user_profile` 使用 `uk_user_profile_user_space_deleted(user_id, space_id, deleted)` 保证同一用户、同一画像范围只有一条有效记录。
- 一个用户对应一份 `learning_preference`，偏好不存在时后端返回默认偏好，用户修改后再写入数据库。
- 学习空间承载对话、知识库、智能体任务、资源、路径、测验和报告。

### 4.3 AI 模型配置

- `ai_model_provider` 归属于用户。
- `api_key_encrypted` 保存加密后的密钥，`api_key_masked` 只用于前端脱敏展示。
- `is_default` 表示用户默认模型配置，唯一默认约束由 Java Service 层保证。

### 4.4 对话与多智能体

- `conversation` 保存对话会话。
- `conversation_message` 保存用户、助手和系统消息。
- `agent_task` 保存 AI 任务总体状态、输入参数、输出摘要和完整 JSON 结果。
- `agent_step` 保存每个智能体的执行步骤、输入、输出、错误和状态。

### 4.5 知识库与资源

- `knowledge_file` 保存用户上传文件元数据。
- `knowledge_chunk` 保存解析后的文本切片和向量引用。
- `generated_resource` 保存最终生成的 Markdown 内容、结构化 JSON 内容和质量评分。

### 4.6 测验、掌握度和报告

- `quiz`、`quiz_question`、`quiz_answer` 构成在线测验闭环。
- `mastery_record` 根据测验结果记录知识点掌握程度和薄弱程度。
- `learning_report` 保存统计报告、图表数据和 AI 学习建议。

## 5. 安全设计

1. 密码字段只保存 BCrypt 哈希。
2. API Key 不保存明文，演示数据只使用 Mock 模型配置。
3. `operation_log.request_params` 必须在 Java 后端脱敏后再写入。
4. 用户数据通过 `user_id` 做隔离，所有查询默认带上 `user_id` 和 `deleted = 0`。

## 6. 初始化脚本

- `schema.sql`：创建数据库、删除旧表、创建新表和索引。
- `seed.sql`：插入演示角色、演示账号、学习空间、Mock 模型配置、对话、知识库、智能体任务、资源、测验和报告。

推荐执行：

```bash
mysql -u root -p < backend/src/main/resources/sql/schema.sql
mysql -u root -p eduagent_studio < backend/src/main/resources/sql/seed.sql
```

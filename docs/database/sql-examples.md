# 典型 SQL 查询

以下 SQL 用于说明数据库设计如何支撑系统主要业务流程。实际 Java 后端将通过 MyBatis-Plus Mapper 和 XML/Wrapper 实现。

## 1. 查询用户及角色

```sql
SELECT
  u.id,
  u.username,
  u.nickname,
  u.user_type,
  r.role_code,
  r.role_name
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE u.username = 'demo_student'
  AND u.deleted = 0
  AND u.status = 'active';
```

## 2. 查询用户学习空间列表

```sql
SELECT
  id,
  space_name,
  subject,
  resource_count,
  task_count,
  status,
  updated_at
FROM learning_space
WHERE user_id = 3
  AND deleted = 0
ORDER BY updated_at DESC
LIMIT 20 OFFSET 0;
```

## 3. 查询用户默认模型配置

```sql
SELECT
  id,
  provider_name,
  provider_type,
  base_url,
  api_key_masked,
  model_name,
  embedding_model,
  temperature,
  max_tokens,
  stream_enabled,
  is_default,
  status
FROM ai_model_provider
WHERE user_id = 3
  AND is_default = 1
  AND deleted = 0
  AND status = 'active'
ORDER BY updated_at DESC
LIMIT 1;
```

注意：查询结果只能返回 `api_key_masked`，不能返回 `api_key_encrypted` 给前端。

## 4. 查询对话及最近消息

```sql
SELECT
  c.id AS conversation_id,
  c.title,
  c.intent_type,
  c.summary,
  m.id AS message_id,
  m.message_role,
  m.content_md,
  m.created_at
FROM conversation c
LEFT JOIN conversation_message m
  ON m.conversation_id = c.id
  AND m.deleted = 0
WHERE c.user_id = 3
  AND c.space_id = 1
  AND c.deleted = 0
ORDER BY c.updated_at DESC, m.created_at ASC;
```

## 5. 查询智能体任务步骤

```sql
SELECT
  t.id AS task_id,
  t.title,
  t.task_type,
  t.execution_status AS task_status,
  s.agent_name,
  s.step_order,
  s.step_type,
  s.execution_status AS step_status,
  s.output_summary,
  s.error_message
FROM agent_task t
JOIN agent_step s ON s.task_id = t.id AND s.deleted = 0
WHERE t.id = 1
  AND t.user_id = 3
  AND t.deleted = 0
ORDER BY s.step_order ASC;
```

## 6. 查询学习空间内生成资源

```sql
SELECT
  id,
  resource_type,
  title,
  subject,
  output_summary,
  quality_score,
  export_status,
  created_at
FROM generated_resource
WHERE user_id = 3
  AND space_id = 1
  AND deleted = 0
  AND status = 'active'
ORDER BY created_at DESC;
```

## 7. 查询知识库文件和切片数量

```sql
SELECT
  f.id,
  f.original_name,
  f.file_type,
  f.parser_status,
  f.chunk_count,
  COUNT(c.id) AS actual_chunk_count
FROM knowledge_file f
LEFT JOIN knowledge_chunk c
  ON c.knowledge_file_id = f.id
  AND c.deleted = 0
WHERE f.user_id = 3
  AND f.space_id = 1
  AND f.deleted = 0
GROUP BY f.id, f.original_name, f.file_type, f.parser_status, f.chunk_count;
```

## 8. 查询今日学习任务

```sql
SELECT
  i.id,
  i.title,
  i.description,
  i.estimated_minutes,
  i.due_date,
  i.status
FROM learning_path_item i
WHERE i.user_id = 3
  AND i.space_id = 1
  AND i.deleted = 0
  AND i.due_date <= CURRENT_DATE
  AND i.status IN ('todo', 'doing')
ORDER BY i.due_date ASC, i.item_order ASC;
```

## 9. 查询测验成绩和答案解析

```sql
SELECT
  q.title AS quiz_title,
  qq.question_order,
  qq.question_type,
  qq.stem,
  qa.answer_text,
  qa.score,
  qa.is_correct,
  qa.feedback_text,
  qq.analysis_text
FROM quiz q
JOIN quiz_question qq ON qq.quiz_id = q.id AND qq.deleted = 0
LEFT JOIN quiz_answer qa
  ON qa.question_id = qq.id
  AND qa.user_id = q.user_id
  AND qa.deleted = 0
WHERE q.id = 1
  AND q.user_id = 3
  AND q.deleted = 0
ORDER BY qq.question_order ASC;
```

## 10. 查询薄弱知识点

```sql
SELECT
  knowledge_point,
  subject,
  mastery_level,
  weakness_level,
  last_score,
  review_count,
  updated_at
FROM mastery_record
WHERE user_id = 3
  AND space_id = 1
  AND deleted = 0
  AND weakness_level >= 30
ORDER BY weakness_level DESC, updated_at DESC;
```

## 11. Dashboard 聚合查询

```sql
SELECT
  (SELECT COUNT(*) FROM learning_space WHERE user_id = 3 AND deleted = 0) AS space_count,
  (SELECT COUNT(*) FROM generated_resource WHERE user_id = 3 AND deleted = 0) AS resource_count,
  (SELECT COUNT(*) FROM agent_task WHERE user_id = 3 AND deleted = 0) AS agent_task_count,
  (SELECT COUNT(*) FROM quiz WHERE user_id = 3 AND deleted = 0) AS quiz_count;
```

## 12. 操作日志审计查询

```sql
SELECT
  id,
  username,
  module_name,
  operation_type,
  target_type,
  target_id,
  result_status,
  error_message,
  created_at
FROM operation_log
WHERE deleted = 0
  AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY created_at DESC
LIMIT 100;
```

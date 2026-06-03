-- EduAgent Studio seed data
-- Target: MySQL 8.x
--
-- Demo accounts for local demonstration only:
-- 1. username: demo_admin
--    plaintext password: password
--    BCrypt database value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--    role: ADMIN
-- 2. username: demo_teacher
--    plaintext password: password
--    BCrypt database value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--    role: TEACHER
-- 3. username: demo_student
--    plaintext password: password
--    BCrypt database value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--    role: STUDENT
--
-- The password hash above is a standard BCrypt hash for the plaintext password "password".
-- Replace all demo credentials before production deployment.
-- No plaintext API key is stored in this seed file. The model provider uses Mock mode.

SET NAMES utf8mb4;
USE eduagent_studio;

INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES
  (1, 'ADMIN', '系统管理员', '负责系统配置、用户管理和日志查看。', 'active'),
  (2, 'TEACHER', '教师用户', '负责课程空间、学习资源和测验管理。', 'active'),
  (3, 'STUDENT', '学生用户', '负责学习空间、个性化学习和在线测验。', 'active');

INSERT INTO sys_user (id, username, password, nickname, email, phone, avatar_url, user_type, status, last_login_at)
VALUES
  (1, 'demo_admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '演示管理员', 'admin@example.com', NULL, NULL, 'admin', 'active', CURRENT_TIMESTAMP),
  (2, 'demo_teacher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '数据库课程教师', 'teacher@example.com', NULL, NULL, 'teacher', 'active', CURRENT_TIMESTAMP),
  (3, 'demo_student', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '学生演示账号', 'student@example.com', NULL, NULL, 'student', 'active', CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (id, user_id, role_id, status)
VALUES
  (1, 1, 1, 'active'),
  (2, 2, 2, 'active'),
  (3, 3, 3, 'active');

INSERT INTO learning_space (id, user_id, space_name, subject, description, cover_url, visibility, resource_count, task_count, status)
VALUES
  (1, 3, '数据库系统期末复习', '数据库系统', '围绕范式、SQL 查询、索引和事务进行期末复习。', NULL, 'private', 2, 1, 'active'),
  (2, 2, 'Java 后端课程备课', 'Java 软件开发', '面向 Spring Boot、REST API 和数据库设计的课程备课空间。', NULL, 'private', 1, 1, 'active');

INSERT INTO user_profile (id, user_id, real_name, school, major, grade_level, learning_goal, foundation_level, interest_tags, target_exam, weekly_available_hours, status)
VALUES
  (1, 3, '张同学', '示例大学', '软件工程', '大三', '两周内系统复习数据库系统，重点掌握范式、SQL 查询优化和索引设计。', 'intermediate', '["数据库","后端开发","软件杯"]', '数据库系统期末考试', 8.00, 'active'),
  (2, 2, '李老师', '示例大学', '计算机科学与技术', '教师', '准备数据库与 Java 后端课程的课堂讲义、案例和测验。', 'advanced', '["教学设计","Java","数据库"]', NULL, 6.00, 'active');

INSERT INTO learning_preference (id, user_id, preferred_resource_types, output_style, difficulty_preference, language_preference, study_time_slots, notification_enabled, status)
VALUES
  (1, 3, '["学习计划","复习提纲","习题集","知识图谱"]', 'structured', 'adaptive', 'zh-CN', '["weekday_evening","weekend_morning"]', 1, 'active'),
  (2, 2, '["课程讲义","案例任务","测验题"]', 'detailed', 'normal', 'zh-CN', '["weekday_afternoon"]', 0, 'active');

INSERT INTO ai_model_provider (
  id, user_id, provider_name, provider_type, base_url, api_key_encrypted, api_key_masked,
  model_name, embedding_model, temperature, max_tokens, stream_enabled, is_default, status, remark
)
VALUES
  (1, 3, '本地 Mock 模型', 'mock', 'mock://local/llm', NULL, 'MOCK-ONLY', 'mock-chat-v1', 'mock-embedding-v1', 0.70, 2048, 1, 1, 'active', '用于无真实 API Key 的完整演示。'),
  (2, 2, '教师演示 Mock 模型', 'mock', 'mock://local/teacher-llm', NULL, 'MOCK-ONLY', 'mock-teacher-v1', 'mock-embedding-v1', 0.60, 2048, 1, 1, 'active', '教师备课场景演示配置。');

INSERT INTO conversation (id, user_id, space_id, title, intent_type, summary, message_count, status)
VALUES
  (1, 3, 1, '数据库系统复习对话', 'exam_review', '用户希望复习范式、SQL 查询和索引设计，系统建议先完成知识点梳理再做测验。', 2, 'active');

INSERT INTO conversation_message (id, conversation_id, user_id, message_role, content_md, content_json, token_count, status)
VALUES
  (1, 1, 3, 'user', '我两周后要考数据库系统，帮我复习范式、SQL 查询和索引。', '{"intent":"exam_review","subject":"数据库系统"}', 36, 'active'),
  (2, 1, 3, 'assistant', '建议先复习关系模型和范式，再完成 SQL 查询练习，最后通过索引与事务题巩固。', '{"resource_suggestions":["学习计划","习题集","知识图谱"]}', 58, 'active');

INSERT INTO knowledge_file (id, user_id, space_id, original_name, storage_path, file_type, file_size, parser_status, chunk_count, checksum, error_message, status)
VALUES
  (1, 3, 1, '数据库系统复习资料.md', 'demo/sample-data/database-review.md', 'md', 4096, 'parsed', 3, 'demo-database-review-md', NULL, 'active');

INSERT INTO knowledge_chunk (id, knowledge_file_id, user_id, space_id, chunk_index, content_text, content_hash, token_count, metadata, embedding_ref, status)
VALUES
  (1, 1, 3, 1, 1, '第一范式要求属性不可再分，第二范式要求非主属性完全依赖于候选键。', 'chunk-hash-001', 42, '{"section":"范式"}', 'mock-vector-001', 'active'),
  (2, 1, 3, 1, 2, 'SQL 查询优化需要关注索引、连接顺序、过滤条件和执行计划。', 'chunk-hash-002', 38, '{"section":"SQL 查询"}', 'mock-vector-002', 'active'),
  (3, 1, 3, 1, 3, '索引可以提升查询性能，但会增加写入成本和存储成本。', 'chunk-hash-003', 34, '{"section":"索引"}', 'mock-vector-003', 'active');

INSERT INTO agent_task (
  id, user_id, space_id, provider_id, task_type, title, input_params, execution_status,
  output_summary, result_json, error_message, started_at, finished_at, status
)
VALUES
  (1, 3, 1, 1, 'resource_generation', '生成数据库系统复习计划', '{"subject":"数据库系统","days":14,"resource_types":["学习计划","习题集","知识图谱"]}', 'succeeded', '已生成 14 天复习计划、知识点结构和练习建议。', '{"plan_days":14,"agents":["PlannerAgent","KnowledgeAgent","ExerciseAgent","ReviewAgent"]}', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active');

INSERT INTO agent_step (
  id, task_id, user_id, agent_name, step_order, step_type, execution_status,
  input_json, output_summary, result_json, error_message, started_at, finished_at, status
)
VALUES
  (1, 1, 3, 'PlannerAgent', 1, 'planning', 'succeeded', '{"goal":"数据库系统复习"}', '拆分为范式、SQL、索引、事务四个阶段。', '{"stages":["范式","SQL","索引","事务"]}', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active'),
  (2, 1, 3, 'KnowledgeAgent', 2, 'generation', 'succeeded', '{"subject":"数据库系统"}', '生成知识点层级和先修关系。', '{"graph":"erDiagram placeholder"}', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active'),
  (3, 1, 3, 'ExerciseAgent', 3, 'generation', 'succeeded', '{"difficulty":"adaptive"}', '生成选择题、简答题和 SQL 练习。', '{"question_count":5}', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active'),
  (4, 1, 3, 'ReviewAgent', 4, 'review', 'succeeded', '{"quality_check":true}', '内容结构完整，难度适中。', '{"quality_score":88}', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active');

INSERT INTO generated_resource (
  id, user_id, space_id, task_id, resource_type, title, subject, knowledge_points,
  content_markdown, content_json, output_summary, quality_score, export_status, status
)
VALUES
  (1, 3, 1, 1, 'plan', '数据库系统 14 天复习计划', '数据库系统', '["范式","SQL 查询","索引","事务"]', '# 数据库系统 14 天复习计划\n第 1-3 天复习关系模型与范式；第 4-7 天训练 SQL 查询；第 8-10 天学习索引；第 11-14 天完成综合测验。', '{"days":14,"focus":["范式","SQL","索引","事务"]}', '适合期末复习的阶段计划。', 88.00, 'markdown', 'active'),
  (2, 3, 1, 1, 'graph', '数据库系统知识图谱', '数据库系统', '["关系模型","范式","SQL","索引"]', '```mermaid\ngraph TD\nA[数据库系统] --> B[关系模型]\nA --> C[SQL 查询]\nA --> D[索引]\n```', '{"mermaid":"graph TD; A-->B;"}', '用于答辩展示的知识结构图。', 86.00, 'none', 'active');

INSERT INTO learning_path (id, user_id, space_id, title, goal, subject, plan_json, progress_rate, start_date, target_date, status)
VALUES
  (1, 3, 1, '数据库系统考前复习路径', '掌握范式、SQL 查询、索引和事务核心考点。', '数据库系统', '{"items":["范式基础","SQL 练习","索引设计","综合测验"]}', 25.00, CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), 'active');

INSERT INTO learning_path_item (id, path_id, user_id, space_id, item_order, title, description, resource_id, knowledge_points, estimated_minutes, due_date, completed_at, status)
VALUES
  (1, 1, 3, 1, 1, '复习范式基础', '理解 1NF、2NF、3NF 和 BCNF 的判断方法。', 1, '["1NF","2NF","3NF","BCNF"]', 90, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), CURRENT_TIMESTAMP, 'done'),
  (2, 1, 3, 1, 2, '完成 SQL 查询练习', '训练连接查询、分组聚合和子查询。', NULL, '["JOIN","GROUP BY","子查询"]', 120, DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), NULL, 'todo');

INSERT INTO quiz (id, user_id, space_id, resource_id, title, subject, difficulty, question_count, total_score, status)
VALUES
  (1, 3, 1, NULL, '数据库系统阶段测验', '数据库系统', 'normal', 2, 20.00, 'published');

INSERT INTO quiz_question (
  id, quiz_id, user_id, question_order, question_type, stem, options_json,
  answer_text, analysis_text, knowledge_points, difficulty, score, status
)
VALUES
  (1, 1, 3, 1, 'single_choice', '第二范式主要解决什么问题？', '["属性不可再分","非主属性对候选键的部分依赖","传递依赖","索引失效"]', '非主属性对候选键的部分依赖', '第二范式要求在满足第一范式基础上，非主属性完全依赖于候选键。', '["第二范式"]', 'normal', 10.00, 'active'),
  (2, 1, 3, 2, 'short_answer', '简述索引提升查询性能的原因和代价。', NULL, '索引通过有序结构减少扫描范围，但会增加写入维护成本和存储成本。', '回答需要同时说明查询收益和写入、存储代价。', '["索引"]', 'normal', 10.00, 'active');

INSERT INTO quiz_answer (id, quiz_id, question_id, user_id, answer_text, score, is_correct, feedback_text, submitted_at, status)
VALUES
  (1, 1, 1, 3, '非主属性对候选键的部分依赖', 10.00, 1, '回答正确。', CURRENT_TIMESTAMP, 'reviewed'),
  (2, 1, 2, 3, '索引能减少扫描，但写入更慢。', 8.00, 1, '说明了主要点，可以补充存储成本。', CURRENT_TIMESTAMP, 'reviewed');

INSERT INTO mastery_record (
  id, user_id, space_id, knowledge_point, subject, mastery_level, weakness_level,
  last_quiz_id, last_score, review_count, status
)
VALUES
  (1, 3, 1, '第二范式', '数据库系统', 82.00, 18.00, 1, 10.00, 2, 'active'),
  (2, 3, 1, '索引设计', '数据库系统', 68.00, 32.00, 1, 8.00, 1, 'weak');

INSERT INTO learning_report (
  id, user_id, space_id, report_type, title, summary, report_json, chart_data_json, suggestion_text, status
)
VALUES
  (1, 3, 1, 'weekly', '数据库系统学习周报', '本周已完成范式基础复习，索引设计仍需加强。', '{"completed_items":1,"weak_points":["索引设计"]}', '{"labels":["范式","SQL","索引"],"values":[82,72,68]}', '建议继续完成 SQL 查询练习，并重点复习索引选择性和联合索引。', 'active');

INSERT INTO operation_log (
  id, user_id, username, module_name, operation_type, target_type, target_id,
  request_method, request_uri, request_params, ip_address, user_agent,
  result_status, error_message, status
)
VALUES
  (1, 3, 'demo_student', 'auth', 'login', 'sys_user', 3, 'POST', '/api/auth/login', '{"username":"demo_student"}', '127.0.0.1', 'EduAgent Demo Client', 'success', NULL, 'active'),
  (2, 3, 'demo_student', 'agent_task', 'create', 'agent_task', 1, 'POST', '/api/agent-tasks', '{"task_type":"resource_generation"}', '127.0.0.1', 'EduAgent Demo Client', 'success', NULL, 'active');

-- EduAgent Studio seed data
-- Target: MySQL 8.x
--
-- Demo accounts for local demonstration only:
-- 1. username: demo_admin
--    plaintext password: 123456
--    BCrypt database value: $2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea
--    role: ADMIN
-- 2. username: demo_teacher
--    plaintext password: 123456
--    BCrypt database value: $2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea
--    role: TEACHER
-- 3. username: demo_student
--    plaintext password: 123456
--    BCrypt database value: $2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea
--    role: STUDENT
--
-- The password hash above is a BCrypt hash for the plaintext password "123456".
-- Replace all demo credentials before production deployment.
-- No plaintext API key is stored in this seed file. The model provider uses Mock mode.

SET NAMES utf8mb4;
USE eduagent_studio;

INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES
  (1, 'ADMIN', '系统管理员', '负责系统配置、用户管理和日志查看。', 'active'),
  (2, 'TEACHER', '教师用户', '负责课程空间、学习资源和测验管理。', 'active'),
  (3, 'STUDENT', '学生用户', '负责学习空间、个性化学习和在线测验。', 'active');

INSERT INTO sys_user (id, username, password_hash, nickname, email, phone, avatar_url, user_type, status, last_login_at)
VALUES
  (1, 'demo_admin', '$2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea', '演示管理员', 'admin@example.com', NULL, NULL, 'admin', 'active', CURRENT_TIMESTAMP),
  (2, 'demo_teacher', '$2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea', '数据库课程教师', 'teacher@example.com', NULL, NULL, 'teacher', 'active', CURRENT_TIMESTAMP),
  (3, 'demo_student', '$2a$10$4TByW6RaL8Rhj3LIXzEeGeWlACrRhJtlzDTH4IRgIYpJaDnIUnqea', '学生演示账号', 'student@example.com', NULL, NULL, 'student', 'active', CURRENT_TIMESTAMP);

INSERT INTO sys_user_role (id, user_id, role_id, status)
VALUES
  (1, 1, 1, 'active'),
  (2, 2, 2, 'active'),
  (3, 3, 3, 'active');

INSERT INTO learning_space (id, user_id, space_name, subject, description, cover_url, visibility, is_default, resource_count, task_count, status)
VALUES
  (1, 3, '数据库系统期末复习', '数据库系统', '围绕范式、SQL 查询、索引和事务进行期末复习。', NULL, 'private', 1, 0, 0, 'active'),
  (2, 2, 'Java 后端课程备课', 'Java 软件开发', '面向 Spring Boot、REST API 和数据库设计的课程备课空间。', NULL, 'private', 1, 0, 0, 'active');

INSERT INTO user_profile (
  id, user_id, space_id, real_name, school, major, grade_level, learning_goal, subject_direction,
  foundation_level, interest_tags, weak_points, target_exam, weekly_available_hours,
  available_time_slots, output_style, profile_source, status
)
VALUES
  (1, 3, 0, '张同学', '示例大学', '软件工程', '大三', '两周内系统复习数据库系统，重点掌握范式、SQL 查询优化和索引设计。', '数据库系统',
   'intermediate', '["数据库","后端开发","软件杯"]', '["事务隔离级别","索引优化"]', '数据库系统期末考试', 8.00,
   '["weekday_evening","weekend_morning"]', 'markdown', 'manual', 'active'),
  (2, 2, 0, '李老师', '示例大学', '计算机科学与技术', '教师', '准备数据库与 Java 后端课程的课堂讲义、案例和测验。', 'Java 后端开发',
   'advanced', '["教学设计","Java","数据库"]', '["课堂案例设计"]', NULL, 6.00,
   '["weekday_afternoon"]', 'detailed', 'manual', 'active'),
  (3, 3, 1, '张同学', '示例大学', '软件工程', '大三', '完成数据库系统期末复习并生成错题训练。', '数据库系统',
   'intermediate', '["数据库","SQL","索引"]', '["范式判断","复杂查询"]', '数据库系统期末考试', 6.00,
   '["weekday_evening"]', 'markdown', 'manual', 'active');

INSERT INTO learning_preference (
  id, user_id, preferred_resource_types, output_style, content_length_preference,
  difficulty_preference, language_preference, study_time_slots, notification_enabled,
  knowledge_graph_enabled, quiz_enabled, review_plan_enabled, status
)
VALUES
  (1, 3, '["学习计划","复习提纲","习题集","知识图谱"]', 'markdown', 'medium',
   'medium', 'zh-CN', '["weekday_evening","weekend_morning"]', 1,
   1, 1, 1, 'active'),
  (2, 2, '["课程讲义","案例任务","测验题"]', 'detailed', 'long',
   'adaptive', 'zh-CN', '["weekday_afternoon"]', 0,
   1, 1, 0, 'active');

INSERT INTO ai_model_provider (
  id, user_id, provider_name, provider_type, base_url, api_key_encrypted, api_key_masked,
  model_name, embedding_model, temperature, max_tokens, stream_enabled, is_default, status, remark
)
VALUES
  (1, 3, '本地 Mock 模型', 'mock', 'mock://local/llm', NULL, 'MOCK-ONLY', 'mock-chat-v1', 'mock-embedding-v1', 0.70, 2048, 1, 1, 'active', '用于无真实 API Key 的完整演示。'),
  (2, 2, '教师演示 Mock 模型', 'mock', 'mock://local/teacher-llm', NULL, 'MOCK-ONLY', 'mock-teacher-v1', 'mock-embedding-v1', 0.60, 2048, 1, 1, 'active', '教师备课场景演示配置。');

-- The seed file intentionally does not pre-create conversations, knowledge files,
-- Agent tasks, generated resources, learning paths, quizzes, mastery records,
-- reports, or operation logs. These records should be produced during the live
-- demonstration so users can clearly distinguish real AI-generated results from
-- initial account and configuration data.

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
-- No plaintext API key is stored in this seed file. The model provider uses local demo mode.

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
   'intermediate', '["数据库","后端开发","软件设计"]', '["事务隔离级别","索引优化"]', '数据库系统期末考试', 8.00,
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
  (1, 3, '本地演示模型', 'mock', 'mock://local/llm', NULL, 'DEMO-ONLY', 'mock-chat-v1', 'mock-embedding-v1', 0.70, 2048, 1, 1, 'active', '用于无真实 API Key 时的本地演示兜底。'),
  (2, 2, '教师演示模型', 'mock', 'mock://local/teacher-llm', NULL, 'DEMO-ONLY', 'mock-teacher-v1', 'mock-embedding-v1', 0.60, 2048, 1, 1, 'active', '教师备课场景演示配置。');

INSERT INTO ai_companion_role (
  id, user_id, role_name, role_identity, avatar_url, theme_color, background, personality,
  expertise, hobbies, speaking_style, scenario, companion_goal, boundaries, custom_prompt,
  tags, is_default, status
)
VALUES
  (1, 3, '温柔学姐小知', '高年级学姐', NULL, '#409EFF',
   '正在陪你准备考试的高年级学姐，擅长把复杂内容拆成容易理解的小步骤。',
   '温柔、耐心、积极反馈，会先降低焦虑再推进学习。',
   '英语六级、数据库复习、学习规划、错题复盘',
   '做笔记、分享方法、轻音乐、晚自习',
   '鼓励式、伙伴式、循循善诱，先说下一步再解释原因。',
   '晚自习、自习室、考前冲刺',
   '帮我坚持学习、缓解焦虑、督促打卡。',
   '不直接代写作业答案，先引导理解；遇到不会的问题优先拆步骤。',
   '回答前先把任务拆成两步，用轻松但不敷衍的语气陪伴学习。',
   '陪伴型,鼓励式,英语六级,晚自习', 1, 'active'),
  (2, 3, '严格督学导师', '学习督导老师', NULL, '#67C23A',
   '负责帮助学生稳定推进复习计划的督学导师。',
   '直接、严谨、重视计划执行和复盘。',
   '复习计划、阶段测验、薄弱点诊断',
   '时间管理、清单复盘、阶段目标',
   '现实、直白、少兜圈，先指出问题再给行动清单。',
   '考前冲刺、阶段复盘、学习计划调整',
   '让学习目标更清楚，减少拖延，形成稳定学习节奏。',
   '不制造焦虑，不给无法执行的空泛建议。',
   '回答要短、硬核、能执行，每次给出下一步动作。',
   '督学型,计划型,复盘,冲刺', 0, 'active');

-- The seed file intentionally does not pre-create conversations, knowledge files,
-- Agent tasks, generated resources, learning paths, quizzes, mastery records,
-- reports, or operation logs. These records should be produced during the live
-- demonstration so users can clearly distinguish generated results from initial
-- account and configuration data.
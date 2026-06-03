-- LearnAgent-A3 MySQL database schema
-- Target: MySQL 8.x
-- Usage:
--   mysql -u root -p < database/schema.sql

CREATE DATABASE IF NOT EXISTS learnagent_a3
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE learnagent_a3;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS tutor_message;
DROP TABLE IF EXISTS assessment_result;
DROP TABLE IF EXISTS study_plan_node;
DROP TABLE IF EXISTS study_plan;
DROP TABLE IF EXISTS resource;
DROP TABLE IF EXISTS profile_snapshot;
DROP TABLE IF EXISTS document_chunk;
DROP TABLE IF EXISTS knowledge_dependency;
DROP TABLE IF EXISTS knowledge_point;
DROP TABLE IF EXISTS ai_task;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE student (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL COMMENT '学生姓名',
  student_no VARCHAR(64) NULL COMMENT '学号或演示账号',
  major VARCHAR(128) NOT NULL COMMENT '专业',
  grade VARCHAR(32) NOT NULL COMMENT '年级',
  email VARCHAR(128) NULL COMMENT '邮箱',
  password_hash VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码哈希',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_no (student_no),
  KEY idx_student_major_grade (major, grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生基础信息';

CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_code VARCHAR(64) NOT NULL COMMENT '课程编码',
  course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
  description TEXT NULL COMMENT '课程说明',
  stage_scope JSON NULL COMMENT '演示阶段范围、重点和排除项',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_course_code (course_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息';

CREATE TABLE ai_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL COMMENT '对外任务 ID，供接口返回和 Agent 轨迹关联',
  student_id BIGINT NULL,
  course_id BIGINT NULL,
  task_type VARCHAR(64) NOT NULL COMMENT 'profile_extract/resource_generate/study_plan_generate/tutor_ask/assessment_analyze',
  request_json JSON NULL COMMENT 'Java 接收或转发给 Python 的请求摘要',
  response_json JSON NULL COMMENT 'Python/Java 返回的结构化结果摘要',
  status VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT 'created/running/success/failed',
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_task_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_ai_task_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  UNIQUE KEY uk_ai_task_task_id (task_id),
  KEY idx_ai_task_student_time (student_id, created_at),
  KEY idx_ai_task_type_status (task_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 任务记录，满足 SRS 中 Java 创建任务记录和 taskId 追踪要求';

CREATE TABLE knowledge_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  point_key VARCHAR(96) NOT NULL COMMENT '知识点业务标识',
  name VARCHAR(128) NOT NULL COMMENT '知识点名称',
  point_type VARCHAR(32) NOT NULL COMMENT 'concept/method/problem/optimization',
  difficulty TINYINT NOT NULL COMMENT '1-5',
  chapter_file VARCHAR(255) NULL COMMENT '来源章节文件',
  learning_outcomes JSON NULL COMMENT '学习产出',
  demo_priority VARCHAR(32) NULL COMMENT 'highest/high/medium/low',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_knowledge_point_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE KEY uk_knowledge_point_key (course_id, point_key),
  KEY idx_knowledge_point_course_order (course_id, sort_order),
  CHECK (difficulty BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程知识点';

CREATE TABLE knowledge_dependency (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  from_knowledge_point_id BIGINT NOT NULL COMMENT '前置知识点',
  to_knowledge_point_id BIGINT NOT NULL COMMENT '后续知识点',
  relation VARCHAR(64) NOT NULL COMMENT 'prerequisite/concept_basis/problem_application 等',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_knowledge_dependency_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_knowledge_dependency_from
    FOREIGN KEY (from_knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_knowledge_dependency_to
    FOREIGN KEY (to_knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE KEY uk_knowledge_dependency_edge (from_knowledge_point_id, to_knowledge_point_id, relation),
  KEY idx_knowledge_dependency_course (course_id),
  KEY idx_knowledge_dependency_to (to_knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点依赖关系，用于 SRS 学习路径规划';

CREATE TABLE document_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chunk_key VARCHAR(128) NOT NULL COMMENT '切片业务标识',
  course_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NULL,
  source_file VARCHAR(255) NOT NULL,
  title VARCHAR(255) NULL,
  content LONGTEXT NOT NULL COMMENT '切片文本',
  embedding_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/indexed/failed',
  vector_collection VARCHAR(128) NULL COMMENT 'Chroma collection 名称',
  metadata JSON NULL COMMENT '页码、章节、标签等元数据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_document_chunk_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_document_chunk_knowledge_point
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  UNIQUE KEY uk_document_chunk_key (chunk_key),
  KEY idx_document_chunk_course (course_id),
  KEY idx_document_chunk_kp (knowledge_point_id),
  KEY idx_document_chunk_embedding (embedding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程文档切片元数据';

CREATE TABLE profile_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  task_id VARCHAR(64) NULL COMMENT '画像生成任务 ID',
  profile_json JSON NOT NULL COMMENT '画像 JSON：专业、课程、目标、基础、薄弱点、偏好、掌握度等',
  source VARCHAR(32) NOT NULL DEFAULT 'conversation' COMMENT 'conversation/assessment/manual',
  version_no INT NOT NULL DEFAULT 1 COMMENT '画像版本号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_profile_snapshot_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_profile_snapshot_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_profile_snapshot_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_profile_student_course_time (student_id, course_id, created_at),
  KEY idx_profile_task (task_id),
  CHECK (version_no >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像快照';

CREATE TABLE resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NULL,
  task_id VARCHAR(64) NULL COMMENT '资源生成任务 ID',
  resource_type VARCHAR(64) NOT NULL COMMENT 'explanation_doc/mindmap/quiz/reading_material/code_lab/micro_lesson',
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  format VARCHAR(32) NOT NULL DEFAULT 'markdown' COMMENT 'markdown/json/mermaid/code',
  quality_score DECIMAL(4,2) NULL COMMENT '0-5 质量分',
  status VARCHAR(32) NOT NULL DEFAULT 'completed' COMMENT 'generating/completed/failed/archived',
  sources_json JSON NULL COMMENT '课程依据或引用来源',
  review_json JSON NULL COMMENT 'Critic Agent 审查结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_resource_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_resource_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_resource_knowledge_point
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_resource_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_resource_student_time (student_id, created_at),
  KEY idx_resource_course_kp (course_id, knowledge_point_id),
  KEY idx_resource_task (task_id),
  KEY idx_resource_type_status (resource_type, status),
  CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个性化学习资源';

CREATE TABLE study_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  task_id VARCHAR(64) NULL COMMENT '路径规划任务 ID',
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/completed/archived',
  total_estimated_minutes INT NULL,
  profile_snapshot_id BIGINT NULL COMMENT '规划基于的画像版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_study_plan_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_study_plan_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_study_plan_profile
    FOREIGN KEY (profile_snapshot_id) REFERENCES profile_snapshot(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_study_plan_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_study_plan_student_course (student_id, course_id, status),
  KEY idx_study_plan_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径主表';

CREATE TABLE study_plan_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NULL,
  node_order INT NOT NULL COMMENT '学习顺序',
  knowledge_point_name VARCHAR(128) NOT NULL,
  recommended_resource_ids JSON NULL COMMENT '推荐资源 ID 数组',
  estimated_minutes INT NULL,
  reason TEXT NULL COMMENT '推荐理由',
  completion_criteria TEXT NULL COMMENT '完成标准',
  status VARCHAR(32) NOT NULL DEFAULT 'not_started' COMMENT 'not_started/in_progress/completed/skipped',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_study_plan_node_plan
    FOREIGN KEY (plan_id) REFERENCES study_plan(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_study_plan_node_kp
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  UNIQUE KEY uk_study_plan_node_order (plan_id, node_order),
  KEY idx_study_plan_node_status (plan_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径节点';

CREATE TABLE assessment_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  knowledge_point_id BIGINT NULL,
  resource_id BIGINT NULL COMMENT '关联练习资源',
  task_id VARCHAR(64) NULL COMMENT '评估任务 ID',
  quiz_title VARCHAR(255) NOT NULL,
  score DECIMAL(6,2) NOT NULL,
  total_score DECIMAL(6,2) NOT NULL DEFAULT 100,
  mastery_delta_json JSON NULL COMMENT '掌握度变化',
  details_json JSON NULL COMMENT '题目对错和解析',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_assessment_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_assessment_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_assessment_kp
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_assessment_resource
    FOREIGN KEY (resource_id) REFERENCES resource(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_assessment_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_assessment_student_time (student_id, submitted_at),
  KEY idx_assessment_course_kp (course_id, knowledge_point_id),
  CHECK (score >= 0),
  CHECK (total_score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习评估结果';

CREATE TABLE agent_run (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  student_id BIGINT NULL,
  course_id BIGINT NULL,
  agent_name VARCHAR(64) NOT NULL,
  agent_role VARCHAR(128) NULL,
  input_summary TEXT NULL,
  output_summary TEXT NULL,
  model_name VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT 'success/failed/running',
  latency_ms INT NULL,
  error_message TEXT NULL,
  trace_json JSON NULL COMMENT '完整 trace、prompt 摘要、引用片段等',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_run_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_agent_run_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_agent_run_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  KEY idx_agent_run_task (task_id, created_at),
  KEY idx_agent_run_student (student_id, created_at),
  KEY idx_agent_run_status (status),
  CHECK (latency_ms IS NULL OR latency_ms >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多智能体运行记录';

CREATE TABLE tutor_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL COMMENT '一次智能辅导会话 ID',
  task_id VARCHAR(64) NULL COMMENT '回答生成任务 ID',
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  message_role VARCHAR(16) NOT NULL COMMENT 'user/assistant/system',
  content LONGTEXT NOT NULL COMMENT '用户问题或 AI 回答',
  sources_json JSON NULL COMMENT '课程知识库来源',
  suggested_resource_ids JSON NULL COMMENT '推荐下一步资源 ID',
  safety_status VARCHAR(32) NOT NULL DEFAULT 'passed' COMMENT 'passed/warning/blocked',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tutor_message_student
    FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_tutor_message_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_tutor_message_task
    FOREIGN KEY (task_id) REFERENCES ai_task(task_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  KEY idx_tutor_message_session (session_id, created_at),
  KEY idx_tutor_message_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能辅导问答记录，支持 SRS FR-13 来源可追踪';

-- Useful read models for Java backend demos.

CREATE OR REPLACE VIEW v_latest_profile AS
SELECT ps.*
FROM profile_snapshot ps
JOIN (
  SELECT student_id, course_id, MAX(created_at) AS max_created_at
  FROM profile_snapshot
  GROUP BY student_id, course_id
) latest
  ON latest.student_id = ps.student_id
 AND latest.course_id = ps.course_id
 AND latest.max_created_at = ps.created_at;

CREATE OR REPLACE VIEW v_student_resource_summary AS
SELECT
  s.id AS student_id,
  s.name AS student_name,
  c.id AS course_id,
  c.course_name,
  r.resource_type,
  COUNT(*) AS resource_count,
  AVG(r.quality_score) AS avg_quality_score
FROM student s
JOIN resource r ON r.student_id = s.id
JOIN course c ON c.id = r.course_id
GROUP BY s.id, s.name, c.id, c.course_name, r.resource_type;

CREATE OR REPLACE VIEW v_task_agent_summary AS
SELECT
  t.task_id,
  t.task_type,
  t.status AS task_status,
  COUNT(ar.id) AS agent_count,
  SUM(CASE WHEN ar.status = 'success' THEN 1 ELSE 0 END) AS success_count,
  SUM(CASE WHEN ar.status = 'failed' THEN 1 ELSE 0 END) AS failed_count,
  SUM(COALESCE(ar.latency_ms, 0)) AS total_latency_ms
FROM ai_task t
LEFT JOIN agent_run ar ON ar.task_id = t.task_id
GROUP BY t.task_id, t.task_type, t.status;

-- API-facing compatibility views.
-- These views keep database columns normalized while giving the Java backend
-- camelCase field names expected by the current Pinia stores and Vue components.

CREATE OR REPLACE VIEW v_api_latest_profile AS
SELECT
  id AS profileSnapshotId,
  task_id AS taskId,
  student_id AS studentId,
  course_id AS courseId,
  profile_json AS profile,
  source,
  version_no AS versionNo,
  created_at AS createdAt
FROM v_latest_profile;

CREATE OR REPLACE VIEW v_api_profile_history AS
SELECT
  id,
  id AS profileSnapshotId,
  task_id AS taskId,
  student_id AS studentId,
  course_id AS courseId,
  profile_json AS profile,
  source,
  version_no AS versionNo,
  created_at AS createdAt
FROM profile_snapshot;

CREATE OR REPLACE VIEW v_api_resource AS
SELECT
  id,
  student_id AS studentId,
  course_id AS courseId,
  knowledge_point_id AS knowledgePointId,
  task_id AS taskId,
  resource_type AS resourceType,
  title,
  content,
  format,
  quality_score AS qualityScore,
  status,
  sources_json AS sources,
  review_json AS review,
  created_at AS createdAt,
  updated_at AS updatedAt
FROM resource;

CREATE OR REPLACE VIEW v_api_study_plan_node AS
SELECT
  id,
  plan_id AS planId,
  knowledge_point_id AS knowledgePointId,
  node_order AS `order`,
  knowledge_point_name AS knowledgePoint,
  recommended_resource_ids AS recommendedResourceIds,
  CONCAT(estimated_minutes, ' 分钟') AS estimatedDuration,
  estimated_minutes AS estimatedMinutes,
  reason,
  completion_criteria AS completionCriteria,
  status,
  created_at AS createdAt,
  updated_at AS updatedAt
FROM study_plan_node;

CREATE OR REPLACE VIEW v_api_study_plan AS
SELECT
  sp.id AS planId,
  sp.student_id AS studentId,
  sp.course_id AS courseId,
  sp.task_id AS taskId,
  sp.title,
  sp.status,
  sp.total_estimated_minutes AS totalEstimatedMinutes,
  sp.profile_snapshot_id AS profileSnapshotId,
  sp.created_at AS createdAt,
  sp.updated_at AS updatedAt
FROM study_plan sp;

CREATE OR REPLACE VIEW v_api_agent_run AS
SELECT
  id,
  task_id AS taskId,
  student_id AS studentId,
  course_id AS courseId,
  agent_name AS agentName,
  agent_role AS agentRole,
  input_summary AS inputSummary,
  output_summary AS outputSummary,
  model_name AS modelName,
  status,
  latency_ms AS latencyMs,
  error_message AS errorMessage,
  trace_json AS trace,
  created_at AS createdAt
FROM agent_run;

CREATE OR REPLACE VIEW v_api_assessment_result AS
SELECT
  id,
  student_id AS studentId,
  course_id AS courseId,
  knowledge_point_id AS knowledgePointId,
  resource_id AS resourceId,
  task_id AS taskId,
  quiz_title AS quizTitle,
  score,
  total_score AS totalScore,
  mastery_delta_json AS masteryDelta,
  details_json AS details,
  submitted_at AS submittedAt
FROM assessment_result;

CREATE OR REPLACE VIEW v_api_analytics AS
SELECT
  s.id AS studentId,
  COUNT(ar.id) AS totalQuizzes,
  COALESCE(ROUND(AVG(ar.score), 2), 0) AS averageScore,
  JSON_EXTRACT(vlp.profile_json, '$.masteryMap') AS masteryMap,
  JSON_EXTRACT(vlp.profile_json, '$.weakness') AS weakPoints,
  CASE
    WHEN COUNT(ar.id) = 0 THEN JSON_ARRAY()
    ELSE JSON_ARRAYAGG(
      JSON_OBJECT(
        'id', ar.id,
        'quizTitle', ar.quiz_title,
        'score', ar.score,
        'totalScore', ar.total_score,
        'submittedAt', ar.submitted_at,
        'details', ar.details_json
      )
    )
  END AS recentResults
FROM student s
LEFT JOIN v_latest_profile vlp ON vlp.student_id = s.id
LEFT JOIN assessment_result ar ON ar.student_id = s.id
GROUP BY s.id, vlp.profile_json;

CREATE OR REPLACE VIEW v_api_tutor_message AS
SELECT
  id,
  session_id AS sessionId,
  task_id AS taskId,
  student_id AS studentId,
  course_id AS courseId,
  message_role AS role,
  content,
  sources_json AS sources,
  suggested_resource_ids AS suggestedResources,
  safety_status AS safetyStatus,
  created_at AS createdAt
FROM tutor_message;

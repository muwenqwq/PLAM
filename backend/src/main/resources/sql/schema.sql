-- EduAgent Studio database schema
-- Target: MySQL 8.x
-- Design note: logical foreign keys are preferred; database foreign keys are intentionally omitted in this phase.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS eduagent_studio
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE eduagent_studio;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS learning_report;
DROP TABLE IF EXISTS mastery_record;
DROP TABLE IF EXISTS quiz_answer;
DROP TABLE IF EXISTS quiz_question;
DROP TABLE IF EXISTS quiz;
DROP TABLE IF EXISTS learning_path_item;
DROP TABLE IF EXISTS learning_path;
DROP TABLE IF EXISTS generated_resource;
DROP TABLE IF EXISTS agent_step;
DROP TABLE IF EXISTS agent_task;
DROP TABLE IF EXISTS knowledge_chunk;
DROP TABLE IF EXISTS knowledge_file;
DROP TABLE IF EXISTS conversation_message;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS ai_model_provider;
DROP TABLE IF EXISTS learning_preference;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS learning_space;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  username VARCHAR(64) NOT NULL COMMENT 'Login username',
  password_hash VARCHAR(128) NOT NULL COMMENT 'BCrypt password hash',
  nickname VARCHAR(64) NOT NULL COMMENT 'Display name',
  email VARCHAR(128) NULL COMMENT 'Email address',
  phone VARCHAR(32) NULL COMMENT 'Phone number',
  avatar_url VARCHAR(512) NULL COMMENT 'Avatar URL',
  user_type VARCHAR(32) NOT NULL DEFAULT 'student' COMMENT 'student, teacher, admin',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  last_login_at DATETIME NULL COMMENT 'Last login time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_email (email),
  KEY idx_sys_user_status_deleted (status, deleted),
  KEY idx_sys_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='System user';

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  role_code VARCHAR(64) NOT NULL COMMENT 'Role code',
  role_name VARCHAR(64) NOT NULL COMMENT 'Role name',
  description VARCHAR(255) NULL COMMENT 'Role description',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status_deleted (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='System role';

CREATE TABLE sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Logical FK to sys_user.id',
  role_id BIGINT NOT NULL COMMENT 'Logical FK to sys_role.id',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role_user_role_deleted (user_id, role_id, deleted),
  KEY idx_sys_user_role_user_id (user_id),
  KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User role relation';

CREATE TABLE learning_space (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_name VARCHAR(128) NOT NULL COMMENT 'Learning space name',
  subject VARCHAR(128) NOT NULL COMMENT 'Subject or course',
  description VARCHAR(1000) NULL COMMENT 'Space description',
  cover_url VARCHAR(512) NULL COMMENT 'Cover URL',
  visibility VARCHAR(32) NOT NULL DEFAULT 'private' COMMENT 'private, shared',
  resource_count INT NOT NULL DEFAULT 0 COMMENT 'Cached generated resource count',
  task_count INT NOT NULL DEFAULT 0 COMMENT 'Cached agent task count',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_learning_space_user_status (user_id, status, deleted),
  KEY idx_learning_space_subject (subject),
  KEY idx_learning_space_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Learning space';

CREATE TABLE user_profile (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Logical FK to sys_user.id',
  real_name VARCHAR(64) NULL COMMENT 'Real name',
  school VARCHAR(128) NULL COMMENT 'School name',
  major VARCHAR(128) NULL COMMENT 'Major name',
  grade_level VARCHAR(64) NULL COMMENT 'Grade level',
  learning_goal VARCHAR(1000) NULL COMMENT 'Main learning goal',
  foundation_level VARCHAR(64) NULL COMMENT 'beginner, intermediate, advanced',
  interest_tags JSON NULL COMMENT 'Interest tags',
  target_exam VARCHAR(128) NULL COMMENT 'Target exam or assessment',
  weekly_available_hours DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Available hours per week',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, incomplete',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_profile_user_deleted (user_id, deleted),
  KEY idx_user_profile_foundation (foundation_level),
  KEY idx_user_profile_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User learning profile';

CREATE TABLE learning_preference (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Logical FK to sys_user.id',
  preferred_resource_types JSON NULL COMMENT 'Preferred resource types',
  output_style VARCHAR(64) NOT NULL DEFAULT 'structured' COMMENT 'structured, concise, detailed',
  difficulty_preference VARCHAR(64) NOT NULL DEFAULT 'adaptive' COMMENT 'easy, normal, hard, adaptive',
  language_preference VARCHAR(64) NOT NULL DEFAULT 'zh-CN' COMMENT 'Output language',
  study_time_slots JSON NULL COMMENT 'Preferred study time slots',
  notification_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Notification enabled flag',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_preference_user_deleted (user_id, deleted),
  KEY idx_learning_preference_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User learning preference';

CREATE TABLE ai_model_provider (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  provider_name VARCHAR(128) NOT NULL COMMENT 'Provider display name',
  provider_type VARCHAR(64) NOT NULL COMMENT 'mock, openai_compatible, deepseek, qwen, ollama, custom',
  base_url VARCHAR(512) NULL COMMENT 'Model API base URL',
  api_key_encrypted VARCHAR(1024) NULL COMMENT 'Encrypted API key, never plaintext',
  api_key_masked VARCHAR(128) NULL COMMENT 'Masked API key for display',
  model_name VARCHAR(128) NOT NULL COMMENT 'Chat model name',
  embedding_model VARCHAR(128) NULL COMMENT 'Embedding model name',
  temperature DECIMAL(4,2) NOT NULL DEFAULT 0.70 COMMENT 'Generation temperature',
  max_tokens INT NOT NULL DEFAULT 2048 COMMENT 'Max output tokens',
  stream_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Streaming output enabled',
  is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Default provider flag',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled, test_failed',
  remark VARCHAR(500) NULL COMMENT 'Remark',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_ai_model_provider_user_default (user_id, is_default, deleted),
  KEY idx_ai_model_provider_type_status (provider_type, status, deleted),
  KEY idx_ai_model_provider_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User AI model provider configuration';

CREATE TABLE conversation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NULL COMMENT 'Logical FK to learning_space.id',
  title VARCHAR(200) NOT NULL COMMENT 'Conversation title',
  intent_type VARCHAR(64) NULL COMMENT 'Recognized intent type',
  summary VARCHAR(1000) NULL COMMENT 'Conversation summary',
  message_count INT NOT NULL DEFAULT 0 COMMENT 'Cached message count',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_conversation_user_status (user_id, status, deleted),
  KEY idx_conversation_space (space_id, deleted),
  KEY idx_conversation_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Learning conversation';

CREATE TABLE conversation_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  conversation_id BIGINT NOT NULL COMMENT 'Logical FK to conversation.id',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  message_role VARCHAR(32) NOT NULL COMMENT 'user, assistant, system',
  content_md LONGTEXT NOT NULL COMMENT 'Markdown message content',
  content_json JSON NULL COMMENT 'Structured message content',
  token_count INT NOT NULL DEFAULT 0 COMMENT 'Token count',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, hidden',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_conversation_message_conversation (conversation_id, deleted, created_at),
  KEY idx_conversation_message_user_role (user_id, message_role, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Conversation message';

CREATE TABLE knowledge_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  original_name VARCHAR(255) NOT NULL COMMENT 'Original file name',
  storage_path VARCHAR(512) NOT NULL COMMENT 'Storage path',
  file_type VARCHAR(32) NOT NULL COMMENT 'pdf, docx, pptx, txt, md',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT 'File size in bytes',
  parser_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending, parsed, failed',
  chunk_count INT NOT NULL DEFAULT 0 COMMENT 'Chunk count',
  checksum VARCHAR(128) NULL COMMENT 'File checksum',
  error_message TEXT NULL COMMENT 'Parse error message',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_knowledge_file_user_space (user_id, space_id, deleted),
  KEY idx_knowledge_file_parser_status (parser_status, deleted),
  KEY idx_knowledge_file_checksum (checksum)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Knowledge base file';

CREATE TABLE knowledge_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  knowledge_file_id BIGINT NOT NULL COMMENT 'Logical FK to knowledge_file.id',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  chunk_index INT NOT NULL COMMENT 'Chunk order in file',
  content_text TEXT NOT NULL COMMENT 'Chunk text',
  content_hash VARCHAR(128) NULL COMMENT 'Chunk content hash',
  token_count INT NOT NULL DEFAULT 0 COMMENT 'Token count',
  metadata JSON NULL COMMENT 'Chunk metadata',
  embedding_ref VARCHAR(255) NULL COMMENT 'Vector storage reference',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_chunk_file_index_deleted (knowledge_file_id, chunk_index, deleted),
  KEY idx_knowledge_chunk_space (space_id, deleted),
  KEY idx_knowledge_chunk_hash (content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Knowledge file chunk';

CREATE TABLE agent_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NULL COMMENT 'Logical FK to learning_space.id',
  provider_id BIGINT NULL COMMENT 'Logical FK to ai_model_provider.id',
  task_type VARCHAR(64) NOT NULL COMMENT 'resource_generation, path_planning, quiz_generation, report_generation',
  title VARCHAR(200) NOT NULL COMMENT 'Task title',
  input_params JSON NULL COMMENT 'Structured task input parameters',
  execution_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending, running, succeeded, failed, cancelled',
  output_summary TEXT NULL COMMENT 'Output summary',
  result_json JSON NULL COMMENT 'Full structured task result',
  error_message TEXT NULL COMMENT 'Error message',
  started_at DATETIME NULL COMMENT 'Task start time',
  finished_at DATETIME NULL COMMENT 'Task finish time',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_agent_task_user_status (user_id, execution_status, deleted),
  KEY idx_agent_task_space (space_id, deleted),
  KEY idx_agent_task_type_created (task_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI agent task';

CREATE TABLE agent_step (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  task_id BIGINT NOT NULL COMMENT 'Logical FK to agent_task.id',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  agent_name VARCHAR(128) NOT NULL COMMENT 'Agent name',
  step_order INT NOT NULL COMMENT 'Step order',
  step_type VARCHAR(64) NOT NULL COMMENT 'planning, generation, review, assessment',
  execution_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending, running, succeeded, failed',
  input_json JSON NULL COMMENT 'Structured step input',
  output_summary TEXT NULL COMMENT 'Step output summary',
  result_json JSON NULL COMMENT 'Full structured step result',
  error_message TEXT NULL COMMENT 'Step error message',
  started_at DATETIME NULL COMMENT 'Step start time',
  finished_at DATETIME NULL COMMENT 'Step finish time',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, hidden',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_step_task_order_deleted (task_id, step_order, deleted),
  KEY idx_agent_step_task_status (task_id, execution_status, deleted),
  KEY idx_agent_step_agent_name (agent_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI agent execution step';

CREATE TABLE generated_resource (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NULL COMMENT 'Logical FK to learning_space.id',
  task_id BIGINT NULL COMMENT 'Logical FK to agent_task.id',
  resource_type VARCHAR(64) NOT NULL COMMENT 'plan, lecture, outline, quiz, case, graph, report',
  title VARCHAR(200) NOT NULL COMMENT 'Resource title',
  subject VARCHAR(128) NULL COMMENT 'Subject',
  knowledge_points JSON NULL COMMENT 'Knowledge points',
  content_markdown LONGTEXT NULL COMMENT 'Markdown content',
  content_json JSON NULL COMMENT 'Structured content',
  output_summary TEXT NULL COMMENT 'Output summary',
  quality_score DECIMAL(5,2) NULL COMMENT 'Review quality score',
  export_status VARCHAR(32) NOT NULL DEFAULT 'none' COMMENT 'none, markdown, pdf, ppt_outline',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_generated_resource_user_type (user_id, resource_type, deleted),
  KEY idx_generated_resource_space (space_id, deleted),
  KEY idx_generated_resource_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Generated learning resource';

CREATE TABLE learning_path (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  title VARCHAR(200) NOT NULL COMMENT 'Learning path title',
  goal VARCHAR(1000) NOT NULL COMMENT 'Learning goal',
  subject VARCHAR(128) NOT NULL COMMENT 'Subject',
  plan_json JSON NULL COMMENT 'Structured path plan',
  progress_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Progress percentage',
  start_date DATE NULL COMMENT 'Start date',
  target_date DATE NULL COMMENT 'Target date',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, completed, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_learning_path_user_space (user_id, space_id, deleted),
  KEY idx_learning_path_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Personalized learning path';

CREATE TABLE learning_path_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  path_id BIGINT NOT NULL COMMENT 'Logical FK to learning_path.id',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  item_order INT NOT NULL COMMENT 'Item order',
  title VARCHAR(200) NOT NULL COMMENT 'Item title',
  description VARCHAR(1000) NULL COMMENT 'Item description',
  resource_id BIGINT NULL COMMENT 'Logical FK to generated_resource.id',
  knowledge_points JSON NULL COMMENT 'Related knowledge points',
  estimated_minutes INT NOT NULL DEFAULT 0 COMMENT 'Estimated minutes',
  due_date DATE NULL COMMENT 'Due date',
  completed_at DATETIME NULL COMMENT 'Completed time',
  status VARCHAR(32) NOT NULL DEFAULT 'todo' COMMENT 'todo, doing, done, skipped',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_path_item_order_deleted (path_id, item_order, deleted),
  KEY idx_learning_path_item_user_status (user_id, status, deleted),
  KEY idx_learning_path_item_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Learning path item';

CREATE TABLE quiz (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  resource_id BIGINT NULL COMMENT 'Logical FK to generated_resource.id',
  title VARCHAR(200) NOT NULL COMMENT 'Quiz title',
  subject VARCHAR(128) NOT NULL COMMENT 'Subject',
  difficulty VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT 'easy, normal, hard',
  question_count INT NOT NULL DEFAULT 0 COMMENT 'Question count',
  total_score DECIMAL(6,2) NOT NULL DEFAULT 100.00 COMMENT 'Total score',
  status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft, published, finished, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_quiz_user_space (user_id, space_id, deleted),
  KEY idx_quiz_status (status, deleted),
  KEY idx_quiz_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Online quiz';

CREATE TABLE quiz_question (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  quiz_id BIGINT NOT NULL COMMENT 'Logical FK to quiz.id',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  question_order INT NOT NULL COMMENT 'Question order',
  question_type VARCHAR(32) NOT NULL COMMENT 'single_choice, multiple_choice, judge, short_answer, coding',
  stem TEXT NOT NULL COMMENT 'Question stem',
  options_json JSON NULL COMMENT 'Question options',
  answer_text TEXT NULL COMMENT 'Standard answer',
  analysis_text TEXT NULL COMMENT 'Answer analysis',
  knowledge_points JSON NULL COMMENT 'Related knowledge points',
  difficulty VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT 'easy, normal, hard',
  score DECIMAL(6,2) NOT NULL DEFAULT 10.00 COMMENT 'Question score',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_question_order_deleted (quiz_id, question_order, deleted),
  KEY idx_quiz_question_user (user_id, deleted),
  KEY idx_quiz_question_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Quiz question';

CREATE TABLE quiz_answer (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  quiz_id BIGINT NOT NULL COMMENT 'Logical FK to quiz.id',
  question_id BIGINT NOT NULL COMMENT 'Logical FK to quiz_question.id',
  user_id BIGINT NOT NULL COMMENT 'Answer user id',
  answer_text TEXT NULL COMMENT 'User answer',
  score DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT 'Answer score',
  is_correct TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Correct flag',
  feedback_text TEXT NULL COMMENT 'Feedback text',
  submitted_at DATETIME NULL COMMENT 'Submit time',
  status VARCHAR(32) NOT NULL DEFAULT 'submitted' COMMENT 'submitted, reviewed',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quiz_answer_question_user_deleted (question_id, user_id, deleted),
  KEY idx_quiz_answer_quiz_user (quiz_id, user_id, deleted),
  KEY idx_quiz_answer_submitted_at (submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Quiz answer';

CREATE TABLE mastery_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  knowledge_point VARCHAR(200) NOT NULL COMMENT 'Knowledge point',
  subject VARCHAR(128) NOT NULL COMMENT 'Subject',
  mastery_level DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Mastery level 0-100',
  weakness_level DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT 'Weakness level 0-100',
  last_quiz_id BIGINT NULL COMMENT 'Last related quiz id',
  last_score DECIMAL(6,2) NULL COMMENT 'Last quiz score',
  review_count INT NOT NULL DEFAULT 0 COMMENT 'Review count',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, mastered, weak',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mastery_record_point_deleted (user_id, space_id, knowledge_point, deleted),
  KEY idx_mastery_record_user_weakness (user_id, weakness_level, deleted),
  KEY idx_mastery_record_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Knowledge mastery record';

CREATE TABLE learning_report (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NOT NULL COMMENT 'Owner user id',
  space_id BIGINT NOT NULL COMMENT 'Logical FK to learning_space.id',
  report_type VARCHAR(64) NOT NULL COMMENT 'daily, weekly, quiz, final',
  title VARCHAR(200) NOT NULL COMMENT 'Report title',
  summary TEXT NULL COMMENT 'Report summary',
  report_json JSON NULL COMMENT 'Structured report content',
  chart_data_json JSON NULL COMMENT 'Chart data for frontend',
  suggestion_text TEXT NULL COMMENT 'Learning suggestions',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_learning_report_user_space (user_id, space_id, deleted),
  KEY idx_learning_report_type_created (report_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Learning report';

CREATE TABLE operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  user_id BIGINT NULL COMMENT 'Operator user id',
  username VARCHAR(64) NULL COMMENT 'Operator username snapshot',
  module_name VARCHAR(128) NOT NULL COMMENT 'Business module name',
  operation_type VARCHAR(64) NOT NULL COMMENT 'create, update, delete, query, login, export',
  target_type VARCHAR(128) NULL COMMENT 'Target object type',
  target_id BIGINT NULL COMMENT 'Target object id',
  request_method VARCHAR(16) NULL COMMENT 'HTTP method',
  request_uri VARCHAR(512) NULL COMMENT 'Request URI',
  request_params JSON NULL COMMENT 'Sanitized request params',
  ip_address VARCHAR(64) NULL COMMENT 'Client IP address',
  user_agent VARCHAR(512) NULL COMMENT 'User agent',
  result_status VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT 'success, failure',
  error_message TEXT NULL COMMENT 'Error message',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active, archived',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag',
  PRIMARY KEY (id),
  KEY idx_operation_log_user_created (user_id, created_at),
  KEY idx_operation_log_module_operation (module_name, operation_type),
  KEY idx_operation_log_result_status (result_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Operation log';

SET FOREIGN_KEY_CHECKS = 1;

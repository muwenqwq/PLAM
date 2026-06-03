-- LearnAgent-A3 database quick try script
-- Run after schema.sql and seed.sql:
--   mysql -u root -p learnagent_a3 < database/try_queries.sql

USE learnagent_a3;
SET NAMES utf8mb4;

-- 1. 先看有哪些演示数据
SELECT '1. Demo 学生' AS step;
SELECT id, name, major, grade FROM student;

SELECT '2. Demo 课程' AS step;
SELECT id, course_code, course_name FROM course;

SELECT '3. 知识点列表' AS step;
SELECT id, point_key, name, point_type, difficulty
FROM knowledge_point
ORDER BY sort_order;

-- 2. 前端画像页需要的数据
SELECT '4. 前端画像页：最新画像' AS step;
SELECT taskId, profileSnapshotId, JSON_PRETTY(profile) AS profile, createdAt
FROM v_api_latest_profile
WHERE studentId = 1 AND courseId = 1;

-- 3. 前端资源页需要的数据
SELECT '5. 前端资源页：资源列表' AS step;
SELECT id, resourceType, title, format, qualityScore, status, createdAt
FROM v_api_resource
WHERE studentId = 1
ORDER BY createdAt;

-- 4. 前端学习路径页需要的数据
SELECT '6. 前端学习路径页：路径节点' AS step;
SELECT `order`, knowledgePoint, estimatedDuration, status, reason
FROM v_api_study_plan_node
WHERE planId = 3001
ORDER BY `order`;

-- 5. 前端 Agent 记录页需要的数据
SELECT '7. 前端 Agent 记录页：默认 task_mock_001' AS step;
SELECT taskId, agentName, status, latencyMs, outputSummary
FROM v_api_agent_run
WHERE taskId = 'task_mock_001'
ORDER BY createdAt;

-- 6. 试着写入一条新任务和一条新资源
-- 这个写入是可重复执行的：重复执行时会更新同一个 task_try_001。
SELECT '8. 写入测试：新增一个 AI 任务' AS step;
INSERT INTO ai_task
  (task_id, student_id, course_id, task_type, request_json, response_json, status, started_at, finished_at)
VALUES
  (
    'task_try_001',
    1,
    1,
    'resource_generate',
    JSON_OBJECT('note', '手动测试写入'),
    JSON_OBJECT('status', 'ok'),
    'success',
    NOW(),
    NOW()
  )
ON DUPLICATE KEY UPDATE
  request_json = VALUES(request_json),
  response_json = VALUES(response_json),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;

SELECT task_id, task_type, status, created_at, updated_at
FROM ai_task
WHERE task_id = 'task_try_001';

SELECT '9. 写入测试：新增一条测试资源' AS step;
INSERT INTO resource
  (student_id, course_id, knowledge_point_id, task_id, resource_type, title, content, format, quality_score, status, sources_json)
VALUES
  (
    1,
    1,
    5,
    'task_try_001',
    'explanation_doc',
    '手动测试资源：状态转移方程',
    '# 状态转移方程测试资源\n\n这是一条通过 try_queries.sql 写入的测试资源。',
    'markdown',
    4.20,
    'completed',
    JSON_ARRAY(JSON_OBJECT('file', '02_state_transition_equations.md'))
  );

SELECT id, resourceType, title, qualityScore, createdAt
FROM v_api_resource
WHERE taskId = 'task_try_001'
ORDER BY id DESC
LIMIT 5;

-- 如果不想保留测试资源，可以手动执行：
-- DELETE FROM resource WHERE task_id = 'task_try_001';
-- DELETE FROM ai_task WHERE task_id = 'task_try_001';

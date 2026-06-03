-- LearnAgent-A3 demo seed data
-- Run after schema.sql:
--   mysql -u root -p learnagent_a3 < database/seed.sql

USE learnagent_a3;
SET NAMES utf8mb4;

INSERT INTO student (id, name, student_no, major, grade, email, password_hash)
VALUES
  (1, '小林', 'demo-student-001', '计算机科学与技术', '大二', 'xiaolin@example.com',
   '$2a$10$.Un8SK7TNNDr8Ef724wViunDZKjzYqs6vDz39/9SMLyi8.4QE4eBS');
-- 演示密码: 123456 (BCrypt 哈希)

INSERT INTO course (id, course_code, course_name, description, stage_scope)
VALUES
  (
    1,
    'algorithm_design_analysis_dp',
    '算法设计与分析：动态规划专题',
    '聚焦动态规划章节，用于演示学习画像、资源生成、路径规划、智能辅导和学习评估闭环。',
    JSON_OBJECT(
      'focus', JSON_ARRAY('状态转移方程', '记忆化搜索'),
      'problems', JSON_ARRAY('TSP', '01背包', '矩阵链相乘'),
      'excluded', JSON_ARRAY('深度学习完整课程', '多门课程', '多专业扩展')
    )
  );

INSERT INTO ai_task
  (id, task_id, student_id, course_id, task_type, request_json, response_json, status, started_at, finished_at, created_at)
VALUES
  (1, 'task_dp_001', 1, 1, 'resource_generate', JSON_OBJECT('resourceTypes', JSON_ARRAY('explanation_doc', 'mindmap', 'quiz', 'reading_material', 'code_lab')), JSON_OBJECT('resourceIds', JSON_ARRAY(2001, 2002, 2003, 2004, 2005), 'profileSnapshotId', 1001, 'studyPlanId', 3001), 'success', '2026-06-02 09:30:00', '2026-06-02 09:45:00', '2026-06-02 09:30:00'),
  (2, 'task_assessment_001', 1, 1, 'assessment_analyze', JSON_OBJECT('resourceId', 2003, 'quizTitle', '状态定义专项测验'), JSON_OBJECT('score', 70, 'weakPoints', JSON_ARRAY('TSP状态压缩')), 'success', '2026-06-02 10:30:00', '2026-06-02 10:30:03', '2026-06-02 10:30:00'),
  (3, 'task_tutor_001', 1, 1, 'tutor_ask', JSON_OBJECT('question', '01 背包一维优化为什么要倒序遍历容量？'), JSON_OBJECT('suggestedResources', JSON_ARRAY(2003, 2005), 'sourceCount', 2), 'success', '2026-06-02 14:00:00', '2026-06-02 14:00:04', '2026-06-02 14:00:00'),
  (4, 'task_mock_001', 1, 1, 'resource_generate', JSON_OBJECT('compatibility', 'AgentRecords.vue default task id'), JSON_OBJECT('aliasOf', 'task_dp_001'), 'success', '2026-06-02 09:30:00', '2026-06-02 09:45:00', '2026-06-02 09:30:00');

INSERT INTO knowledge_point
  (id, course_id, point_key, name, point_type, difficulty, chapter_file, learning_outcomes, demo_priority, sort_order)
VALUES
  (1, 1, 'dp_overview', '动态规划基本思想', 'concept', 1, '01_dynamic_programming_overview.md', JSON_ARRAY('说明动态规划的作用', '区分递归、动态规划和贪心'), 'high', 1),
  (2, 1, 'optimal_substructure', '最优子结构', 'concept', 1, '01_dynamic_programming_overview.md', JSON_ARRAY('解释大问题最优解如何由子问题最优解组成'), 'high', 2),
  (3, 1, 'overlapping_subproblems', '重叠子问题', 'concept', 1, '01_dynamic_programming_overview.md', JSON_ARRAY('识别递归过程中的重复状态'), 'high', 3),
  (4, 1, 'state_definition', '状态定义', 'method', 2, '02_state_transition_equations.md', JSON_ARRAY('用变量完整描述子问题', '判断状态是否缺少必要信息'), 'highest', 4),
  (5, 1, 'state_transition_equation', '状态转移方程', 'method', 3, '02_state_transition_equations.md', JSON_ARRAY('根据决策写出递推关系', '解释转移方程中 max 或 min 的含义'), 'highest', 5),
  (6, 1, 'boundary_condition', '边界条件', 'method', 2, '02_state_transition_equations.md', JSON_ARRAY('写出最小子问题的直接答案', '检查递推是否能启动'), 'high', 6),
  (7, 1, 'memoized_search', '记忆化搜索', 'method', 3, '03_memoized_search.md', JSON_ARRAY('把普通递归改写为带缓存的搜索', '说明记忆化搜索与动态规划的关系'), 'highest', 7),
  (8, 1, 'knapsack_01_2d', '01 背包二维 DP', 'problem', 3, '05_01_knapsack.md', JSON_ARRAY('写出 dp[i][w] 的含义', '解释选与不选两种决策'), 'highest', 8),
  (9, 1, 'knapsack_01_1d', '01 背包一维优化', 'optimization', 4, '05_01_knapsack.md', JSON_ARRAY('把二维 DP 压缩为一维数组', '解释容量倒序遍历的原因'), 'high', 9),
  (10, 1, 'matrix_chain_dp', '矩阵链相乘区间 DP', 'problem', 4, '06_matrix_chain_multiplication.md', JSON_ARRAY('写出 dp[i][j] 的含义', '枚举最后一次切分位置 k'), 'high', 10),
  (11, 1, 'bitmask_state', '二进制集合状态', 'method', 4, '04_tsp_dynamic_programming.md', JSON_ARRAY('用 mask 表示访问集合', '使用位运算判断元素是否访问'), 'medium', 11),
  (12, 1, 'tsp_bitmask_dp', 'TSP 状态压缩动态规划', 'problem', 5, '04_tsp_dynamic_programming.md', JSON_ARRAY('写出 dp[mask][i] 的含义', '推导 TSP 状态转移方程'), 'high', 12),
  (13, 1, 'dp_problem_recognition', '动态规划问题识别', 'method', 3, '07_dp_comparison_and_problem_recognition.md', JSON_ARRAY('判断题目是否适合动态规划', '选择前缀、区间或集合状态设计方式'), 'high', 13);

INSERT INTO knowledge_dependency
  (course_id, from_knowledge_point_id, to_knowledge_point_id, relation)
VALUES
  (1, 1, 2, 'concept_basis'),
  (1, 1, 3, 'concept_basis'),
  (1, 2, 4, 'prerequisite'),
  (1, 3, 7, 'prerequisite'),
  (1, 4, 5, 'prerequisite'),
  (1, 5, 6, 'requires_boundary'),
  (1, 4, 8, 'problem_application'),
  (1, 5, 8, 'problem_application'),
  (1, 8, 9, 'optimization_after_mastery'),
  (1, 4, 10, 'problem_application'),
  (1, 5, 10, 'problem_application'),
  (1, 11, 12, 'problem_application'),
  (1, 8, 13, 'comparison_case'),
  (1, 10, 13, 'comparison_case'),
  (1, 12, 13, 'comparison_case');

INSERT INTO document_chunk
  (chunk_key, course_id, knowledge_point_id, source_file, title, content, embedding_status, vector_collection, metadata)
VALUES
  ('chunk_dp_overview_001', 1, 1, '01_dynamic_programming_overview.md', '动态规划算法概述', '动态规划是一种通过保存子问题结果来避免重复计算的算法设计方法，通常适用于最优子结构、重叠子问题和状态可表达的问题。', 'indexed', 'course_algorithm_design_analysis_dp', JSON_OBJECT('chapter', '动态规划算法概述')),
  ('chunk_state_transition_001', 1, 5, '02_state_transition_equations.md', '状态转移方程', '状态转移方程回答当前状态的答案如何由更小或更简单的状态推出。01 背包中需要比较选与不选当前物品。', 'indexed', 'course_algorithm_design_analysis_dp', JSON_OBJECT('chapter', '状态转移方程')),
  ('chunk_knapsack_001', 1, 8, '05_01_knapsack.md', '01 背包', '01 背包中 dp[i][w] 表示只考虑前 i 个物品，背包容量不超过 w 时可以获得的最大价值。', 'indexed', 'course_algorithm_design_analysis_dp', JSON_OBJECT('chapter', '01 背包'));

INSERT INTO profile_snapshot
  (id, student_id, course_id, task_id, profile_json, source, version_no, created_at)
VALUES
  (
    1001,
    1,
    1,
    'task_dp_001',
    JSON_OBJECT(
      'major', '计算机科学与技术',
      'grade', '大二',
      'course', '算法设计与分析：动态规划专题',
      'goal', '一周内掌握动态规划四步法，重点突破状态定义、状态转移方程和记忆化搜索',
      'foundation', '已经具备基础编程、递归、数组和简单复杂度分析能力，但不擅长从题意抽象状态',
      'weakness', JSON_ARRAY('状态定义不完整', '状态转移方程只会套公式', '01 背包一维优化循环方向容易写错', 'TSP 中容易漏掉访问集合 mask'),
      'preference', JSON_ARRAY('分步推导', '表格填充示例', '代码实验', '错因诊断'),
      'timeBudget', '每天约 60 分钟',
      'masteryMap', JSON_OBJECT(
        '动态规划基本思想', 0.68,
        '状态定义', 0.36,
        '状态转移方程', 0.28,
        '记忆化搜索', 0.42,
        '01背包', 0.33,
        '矩阵链相乘', 0.24,
        'TSP状态压缩', 0.18
      )
    ),
    'conversation',
    1,
    '2026-06-02 09:30:00'
  );

INSERT INTO resource
  (id, student_id, course_id, knowledge_point_id, task_id, resource_type, title, content, format, quality_score, status, sources_json, review_json, created_at)
VALUES
  (2001, 1, 1, 5, 'task_dp_001', 'explanation_doc', '动态规划四步法：从状态定义到转移方程', '# 动态规划四步法讲义\n\n核心步骤：定义状态、写出状态转移方程、确定边界条件、确定计算顺序。', 'markdown', 4.70, 'completed', JSON_ARRAY(JSON_OBJECT('file', '01_dynamic_programming_overview.md')), JSON_OBJECT('passed', true, 'score', 4.7), '2026-06-02 09:35:00'),
  (2002, 1, 1, 5, 'task_dp_001', 'mindmap', '动态规划专题知识图谱', 'mindmap\n  root((动态规划专题))\n    基本思想\n    四步法\n    典型题目', 'mermaid', 4.50, 'completed', JSON_ARRAY(JSON_OBJECT('file', 'knowledge_points.json')), JSON_OBJECT('passed', true, 'score', 4.5), '2026-06-02 09:36:00'),
  (2003, 1, 1, 8, 'task_dp_001', 'quiz', '动态规划核心练习：状态、转移与错因诊断', '# 动态规划核心练习\n\n1. 为什么状态定义必须包含足够的信息？\n2. 写出 01 背包状态转移方程。', 'markdown', 4.40, 'completed', JSON_ARRAY(JSON_OBJECT('file', 'exercises/dp_core_exercises.json')), JSON_OBJECT('passed', true, 'score', 4.4), '2026-06-02 09:37:00'),
  (2004, 1, 1, 13, 'task_dp_001', 'reading_material', '动态规划题型对比：01 背包、矩阵链相乘与 TSP', '# 动态规划题型对比\n\n01 背包是选择型 DP，矩阵链相乘是区间 DP，TSP 是状态压缩 DP。', 'markdown', 4.60, 'completed', JSON_ARRAY(JSON_OBJECT('file', '07_dp_comparison_and_problem_recognition.md')), JSON_OBJECT('passed', true, 'score', 4.6), '2026-06-02 09:38:00'),
  (2005, 1, 1, 9, 'task_dp_001', 'code_lab', '01 背包代码实验：二维 DP 与一维优化', '# 01 背包代码实验\n\n目标：实现二维 DP 和一维优化，并解释容量倒序遍历原因。', 'markdown', 4.80, 'completed', JSON_ARRAY(JSON_OBJECT('file', 'code_labs/knapsack_01.py')), JSON_OBJECT('passed', true, 'score', 4.8), '2026-06-02 09:39:00');

INSERT INTO study_plan
  (id, student_id, course_id, task_id, title, status, total_estimated_minutes, profile_snapshot_id, created_at)
VALUES
  (3001, 1, 1, 'task_dp_001', '小林的动态规划专题学习路径', 'active', 325, 1001, '2026-06-02 09:45:00');

INSERT INTO study_plan_node
  (plan_id, knowledge_point_id, node_order, knowledge_point_name, recommended_resource_ids, estimated_minutes, reason, completion_criteria, status)
VALUES
  (3001, 1, 1, '动态规划基本思想', JSON_ARRAY(2001), 30, '先建立动态规划适用条件的判断框架。', '能够区分动态规划、普通递归和贪心算法。', 'completed'),
  (3001, 4, 2, '状态定义', JSON_ARRAY(2001, 2003), 45, '画像显示状态定义不完整是主要薄弱点。', '能够写出 01 背包、TSP、矩阵链相乘的状态含义。', 'in_progress'),
  (3001, 5, 3, '状态转移方程', JSON_ARRAY(2001, 2002, 2003), 50, '状态转移方程是最高优先级知识点。', '能够解释 01 背包转移方程中的选与不选。', 'not_started'),
  (3001, 7, 4, '记忆化搜索', JSON_ARRAY(2001, 2004), 40, '学生已有递归基础，适合通过递归加缓存过渡。', '能够写出 dfs(state) 与 memo 的通用模板。', 'not_started'),
  (3001, 8, 5, '01 背包二维 DP 与一维优化', JSON_ARRAY(2003, 2005), 60, '01 背包能同时训练状态、转移和循环顺序。', '代码实验输出正确，并能说明一维优化为什么倒序遍历。', 'not_started');

INSERT INTO assessment_result
  (student_id, course_id, knowledge_point_id, resource_id, task_id, quiz_title, score, total_score, mastery_delta_json, details_json, submitted_at)
VALUES
  (
    1,
    1,
    4,
    2003,
    'task_assessment_001',
    '状态定义专项测验',
    70,
    100,
    JSON_OBJECT('状态定义', 0.05, 'TSP状态压缩', -0.02),
    JSON_ARRAY(
      JSON_OBJECT('question', '为什么状态定义必须包含足够信息？', 'correct', true),
      JSON_OBJECT('question', 'TSP 中 dp[mask][i] 的含义', 'correct', false),
      JSON_OBJECT('question', '矩阵链相乘 dp[i][j] 的含义', 'correct', true)
    ),
    '2026-06-02 10:30:00'
  );

INSERT INTO agent_run
  (task_id, student_id, course_id, agent_name, agent_role, input_summary, output_summary, model_name, status, latency_ms, trace_json, created_at)
VALUES
  ('task_dp_001', 1, 1, 'ProfileAgent', '画像抽取与更新', '学生表示会递归和数组，但不会定义动态规划状态。', '抽取专业、课程、目标、基础、薄弱点、偏好、时间和掌握度。', 'MiMo', 'success', 2180, JSON_OBJECT('profileSnapshotId', 1001), '2026-06-02 09:30:00'),
  ('task_dp_001', 1, 1, 'KnowledgeGraphAgent', '知识点依赖构建', '读取 knowledge_points.json 和 dependencies.json。', '识别动态规划知识点和推荐学习路径。', 'RuleEngine', 'success', 760, JSON_OBJECT('courseId', 'algorithm_design_analysis_dp'), '2026-06-02 09:31:00'),
  ('task_dp_001', 1, 1, 'RetrieverAgent', '课程知识库检索', '检索动态规划基本思想、状态转移方程和 01 背包资料。', '召回课程章节片段、练习题和代码实验基准。', 'Chroma', 'success', 940, JSON_OBJECT('chunkKeys', JSON_ARRAY('chunk_dp_overview_001', 'chunk_state_transition_001', 'chunk_knapsack_001')), '2026-06-02 09:32:00'),
  ('task_dp_001', 1, 1, 'PlannerAgent', '资源生成计划', '根据画像和知识依赖规划资源生成任务。', '确定讲义、导图、练习、拓展阅读和代码实验 5 类资源。', 'MiMo', 'success', 1420, JSON_OBJECT('resourceTypes', JSON_ARRAY('explanation_doc', 'mindmap', 'quiz', 'reading_material', 'code_lab')), '2026-06-02 09:33:00'),
  ('task_dp_001', 1, 1, 'ResourceAgent', '学习资源生成', '基于课程语料和学生画像生成个性化资源。', '生成 5 类动态规划学习资源。', 'MiMo', 'success', 5360, JSON_OBJECT('resourceIds', JSON_ARRAY(2001, 2002, 2003, 2004, 2005)), '2026-06-02 09:35:00'),
  ('task_dp_001', 1, 1, 'CriticAgent', '质量审查', '检查资源是否符合课程范围和学生画像。', '确认资源聚焦动态规划专题，质量分均大于 4.4。', 'MiMo', 'success', 2210, JSON_OBJECT('passed', true), '2026-06-02 09:40:00');

INSERT INTO agent_run
  (task_id, student_id, course_id, agent_name, agent_role, input_summary, output_summary, model_name, status, latency_ms, trace_json, created_at)
SELECT
  'task_mock_001',
  student_id,
  course_id,
  agent_name,
  agent_role,
  input_summary,
  output_summary,
  model_name,
  status,
  latency_ms,
  JSON_SET(COALESCE(trace_json, JSON_OBJECT()), '$.aliasOf', 'task_dp_001'),
  created_at
FROM agent_run
WHERE task_id = 'task_dp_001';

INSERT INTO tutor_message
  (session_id, task_id, student_id, course_id, message_role, content, sources_json, suggested_resource_ids, safety_status, created_at)
VALUES
  (
    'session_tutor_001',
    NULL,
    1,
    1,
    'user',
    '01 背包一维优化为什么要倒序遍历容量？',
    NULL,
    NULL,
    'passed',
    '2026-06-02 14:00:00'
  ),
  (
    'session_tutor_001',
    'task_tutor_001',
    1,
    1,
    'assistant',
    '01 背包一维优化必须倒序遍历容量，因为每个物品只能被使用一次。如果正序遍历，dp[w-weight[i]] 可能已经在本轮包含当前物品，继续更新 dp[w] 就会导致同一个物品被重复选择。',
    JSON_ARRAY(
      JSON_OBJECT('file', '05_01_knapsack.md', 'section', '一维空间优化'),
      JSON_OBJECT('file', 'exercises/dp_core_exercises.json', 'section', 'knapsack_002 / debug_001')
    ),
    JSON_ARRAY(2003, 2005),
    'passed',
    '2026-06-02 14:00:04'
  );

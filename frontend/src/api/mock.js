/**
 * Mock 数据集 —— 后端未就绪时，各 API 模块可引用这里的数据
 * 后端就绪后删除本文件并移除 API 模块中的 mock 分支即可
 */

export const mockProfile = {
  taskId: 'task_mock_001',
  profileSnapshotId: 1001,
  profile: {
    major: '计算机科学与技术',
    grade: '大二',
    course: '人工智能导论',
    goal: '两周内掌握 A* 搜索算法并完成 Python 路径规划实验',
    foundation: 'Python 基础一般，搜索算法基础较弱',
    weakness: ['不理解启发式函数', '混淆 g(n)、h(n)、f(n)'],
    preference: ['图解', '短讲解', '代码案例'],
    timeBudget: '每天约 45 分钟',
    masteryMap: {
      'BFS/DFS': 0.7,
      '启发式函数': 0.3,
      'A*算法': 0.2,
      '路径规划': 0.15
    }
  }
}

export const mockProfileHistory = [
  { ...mockProfile, id: 1001, createdAt: '2026-06-01T10:00:00', source: 'conversation' },
  {
    id: 1000,
    taskId: 'task_mock_000',
    profileSnapshotId: 1000,
    profile: { ...mockProfile.profile, goal: '学会基础搜索算法', masteryMap: { 'BFS/DFS': 0.5, '启发式函数': 0.1 } },
    createdAt: '2026-05-30T08:00:00',
    source: 'conversation'
  }
]

export const mockResources = [
  {
    id: 2001,
    resourceType: 'explanation_doc',
    title: 'A* 搜索算法个性化讲义',
    format: 'markdown',
    qualityScore: 4.5,
    status: 'completed',
    createdAt: '2026-06-01T10:05:00',
    content: '# A* 搜索算法\n\n## 什么是 A*？\nA* 是一种启发式搜索算法...\n\n## 核心公式\n`f(n) = g(n) + h(n)`\n\n- `g(n)`：从起点到节点 n 的实际代价\n- `h(n)`：从节点 n 到目标的启发式估计\n- `f(n)`：综合评估值'
  },
  {
    id: 2002,
    resourceType: 'mindmap',
    title: 'A* 搜索算法思维导图',
    format: 'mermaid',
    qualityScore: 4.2,
    status: 'completed',
    createdAt: '2026-06-01T10:06:00',
    content: 'mindmap\n  root((A*搜索算法))\n    基础概念\n      启发式搜索\n      代价函数\n      最优性\n    核心公式\n      f(n) = g(n) + h(n)\n      g(n) 实际代价\n      h(n) 启发估计\n    启发函数\n      曼哈顿距离\n      欧几里得距离\n    应用场景\n      路径规划\n      游戏AI\n      机器人导航'
  },
  {
    id: 2003,
    resourceType: 'quiz',
    title: 'A* 搜索分层练习题',
    format: 'markdown',
    qualityScore: 4.0,
    status: 'completed',
    createdAt: '2026-06-01T10:07:00',
    content: '# A* 搜索练习题\n\n## 基础题\n**Q1:** A* 算法中 f(n) 的含义是什么？\n\nA. 从起点到终点的总代价\nB. 从起点到节点n的实际代价加上从n到目标的估计代价\nC. 节点n的深度\nD. 节点n的子节点数量\n\n**答案：B**\n\n## 进阶题\n**Q2:** 给定如下图，请手动执行 A* 算法，画出 OPEN 和 CLOSED 表的变化过程。'
  },
  {
    id: 2004,
    resourceType: 'reading_material',
    title: 'A* 算法拓展阅读：从 Dijkstra 到 A*',
    format: 'markdown',
    qualityScore: 4.3,
    status: 'completed',
    createdAt: '2026-06-01T10:08:00',
    content: '# 从 Dijkstra 到 A*\n\n## Dijkstra 算法回顾\nDijkstra 算法是一种贪心的最短路径算法...\n\n## A* 的改进\nA* 在 Dijkstra 的基础上引入了启发式函数 h(n)...\n\n## 推荐阅读\n- [Amit\'s A* Pages](http://theory.stanford.edu/~amitp/GameProgramming/)\n- Russell & Norvig, AI: A Modern Approach, Chapter 3'
  },
  {
    id: 2005,
    resourceType: 'code_lab',
    title: 'A* 路径规划 Python 实操',
    format: 'markdown',
    qualityScore: 4.6,
    status: 'completed',
    createdAt: '2026-06-01T10:09:00',
    content: '# A* 路径规划实验\n\n## 实验目标\n用 Python 实现 A* 算法，在网格地图上找到从起点到终点的最短路径。\n\n## 代码模板\n```python\ndef a_star(grid, start, end):\n    open_set = [start]\n    came_from = {}\n    g_score = {start: 0}\n    f_score = {start: heuristic(start, end)}\n    \n    while open_set:\n        current = min(open_set, key=lambda n: f_score.get(n, float(\'inf\')))\n        if current == end:\n            return reconstruct_path(came_from, current)\n        \n        open_set.remove(current)\n        for neighbor in get_neighbors(grid, current):\n            tentative_g = g_score[current] + 1\n            if tentative_g < g_score.get(neighbor, float(\'inf\')):\n                came_from[neighbor] = current\n                g_score[neighbor] = tentative_g\n                f_score[neighbor] = tentative_g + heuristic(neighbor, end)\n                if neighbor not in open_set:\n                    open_set.append(neighbor)\n    return None\n```\n\n## 实验步骤\n1. 理解代码模板\n2. 实现 `heuristic` 和 `get_neighbors` 函数\n3. 运行测试用例\n4. 可视化路径结果'
  }
]

export const mockStudyPlan = {
  planId: 'plan_mock_001',
  studentId: 1,
  courseId: 1,
  createdAt: '2026-06-01T10:10:00',
  nodes: [
    {
      id: 1,
      order: 1,
      knowledgePoint: 'BFS/DFS 基础回顾',
      recommendedResourceIds: [2001],
      estimatedDuration: '30 分钟',
      reason: 'A* 算法建立在图搜索基础之上，需要先巩固 BFS/DFS 知识',
      completionCriteria: '能独立画出 BFS/DFS 搜索过程',
      status: 'completed'
    },
    {
      id: 2,
      order: 2,
      knowledgePoint: '启发式函数理解',
      recommendedResourceIds: [2001, 2004],
      estimatedDuration: '45 分钟',
      reason: '你的画像显示对启发式函数理解较弱，这是 A* 的核心概念',
      completionCriteria: '能解释 h(n) 的作用并举例说明曼哈顿距离',
      status: 'in_progress'
    },
    {
      id: 3,
      order: 3,
      knowledgePoint: 'A* 算法原理',
      recommendedResourceIds: [2001, 2002, 2003],
      estimatedDuration: '60 分钟',
      reason: '掌握 f(n)=g(n)+h(n) 的含义和 A* 的完整流程',
      completionCriteria: '完成练习题 Q1-Q3 并全部正确',
      status: 'not_started'
    },
    {
      id: 4,
      order: 4,
      knowledgePoint: 'A* 代码实操',
      recommendedResourceIds: [2005],
      estimatedDuration: '60 分钟',
      reason: '结合代码理解算法实现，动手完成路径规划实验',
      completionCriteria: 'Python 代码运行成功并输出正确路径',
      status: 'not_started'
    },
    {
      id: 5,
      order: 5,
      knowledgePoint: '综合练习与评估',
      recommendedResourceIds: [2003],
      estimatedDuration: '30 分钟',
      reason: '检验整体掌握情况，查漏补缺',
      completionCriteria: '综合练习正确率 ≥ 80%',
      status: 'not_started'
    }
  ]
}

export const mockTutorResponse = {
  answer: '**g(n) 和 h(n) 的区别：**\n\n- **g(n)**：从起点到节点 n 的**实际已走过的距离**。它是确定的，不会随估计而变化。\n- **h(n)**：从节点 n 到终点的**启发式估计距离**。它是猜测的，取决于你选择的启发函数。\n\n**简单类比：**\n假设你开车从北京到上海：\n- g(n) = 你已经开了多远（比如到了济南，开了 400km）\n- h(n) = 从济南到上海的预估距离（比如 800km）\n- f(n) = g(n) + h(n) = 1200km，这就是 A* 用来排序的综合评分\n\n**关键点：** h(n) 必须是**可采纳的**（不高估），否则 A* 不保证最优解。',
  sources: [
    { file: '人工智能导论_第3章_搜索算法.md', section: '3.4 A*搜索' },
    { file: '人工智能导论_第3章_搜索算法.md', section: '3.2 启发式搜索' }
  ],
  suggestedResources: [2001, 2003],
  agentTrace: [
    { agentName: 'RetrieverAgent', status: 'success', outputSummary: '检索到 3 个相关文档片段' },
    { agentName: 'TutorAgent', status: 'success', outputSummary: '基于课程资料生成回答，引用 2 处来源' }
  ]
}

export const mockAnalytics = {
  studentId: 1,
  totalQuizzes: 3,
  averageScore: 72,
  masteryMap: {
    'BFS/DFS': 0.7,
    '启发式函数': 0.3,
    'A*算法': 0.2,
    '路径规划': 0.15
  },
  weakPoints: ['启发式函数', 'A*算法', '路径规划'],
  recentResults: [
    {
      id: 1,
      quizTitle: 'A* 搜索基础测验',
      score: 60,
      totalScore: 100,
      submittedAt: '2026-06-01T11:00:00',
      details: [
        { question: 'f(n) 的含义', correct: true },
        { question: 'h(n) 的可采纳性', correct: false },
        { question: 'A* 搜索过程', correct: false }
      ]
    },
    {
      id: 2,
      quizTitle: 'BFS/DFS 复习测验',
      score: 85,
      totalScore: 100,
      submittedAt: '2026-05-31T14:00:00',
      details: [
        { question: 'BFS 遍历顺序', correct: true },
        { question: 'DFS 遍历顺序', correct: true },
        { question: '时间复杂度', correct: true }
      ]
    }
  ]
}

export const mockAgentRuns = [
  {
    id: 1,
    taskId: 'task_mock_001',
    agentName: 'ProfileAgent',
    inputSummary: '学生输入："我是计算机大二学生，想两周内学会 A* 搜索算法..."',
    outputSummary: '抽取 9 个画像维度，识别薄弱点 2 个',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 2340,
    errorMessage: null,
    createdAt: '2026-06-01T10:00:00'
  },
  {
    id: 2,
    taskId: 'task_mock_001',
    agentName: 'RetrieverAgent',
    inputSummary: '检索知识点：A* 搜索算法',
    outputSummary: '从课程知识库检索到 5 个相关文档片段',
    modelName: 'Chroma',
    status: 'success',
    latencyMs: 890,
    errorMessage: null,
    createdAt: '2026-06-01T10:01:00'
  },
  {
    id: 3,
    taskId: 'task_mock_001',
    agentName: 'PlannerAgent',
    inputSummary: '基于画像和知识点规划资源生成方案',
    outputSummary: '规划生成 5 类资源：讲义、导图、题目、阅读、代码实验',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 1560,
    errorMessage: null,
    createdAt: '2026-06-01T10:02:00'
  },
  {
    id: 4,
    taskId: 'task_mock_001',
    agentName: 'ResourceAgent',
    inputSummary: '生成个性化讲义',
    outputSummary: '生成讲义 1500 字，包含公式、图解和代码示例',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 5200,
    errorMessage: null,
    createdAt: '2026-06-01T10:03:00'
  },
  {
    id: 5,
    taskId: 'task_mock_001',
    agentName: 'QuizAgent',
    inputSummary: '生成分层练习题',
    outputSummary: '生成基础题 3 道、进阶题 2 道、综合题 1 道',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 4100,
    errorMessage: null,
    createdAt: '2026-06-01T10:04:00'
  },
  {
    id: 6,
    taskId: 'task_mock_001',
    agentName: 'CriticAgent',
    inputSummary: '审查生成的 5 类资源质量',
    outputSummary: '讲义 4.5 分、导图 4.2 分、题目 4.0 分、阅读 4.3 分、代码 4.6 分',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 3200,
    errorMessage: null,
    createdAt: '2026-06-01T10:06:00'
  },
  {
    id: 7,
    taskId: 'task_mock_001',
    agentName: 'PathAgent',
    inputSummary: '规划学习路径，考虑知识点依赖和学生薄弱点',
    outputSummary: '生成 5 个学习节点，预计总时长 3.5 小时',
    modelName: 'MiMo',
    status: 'success',
    latencyMs: 2800,
    errorMessage: null,
    createdAt: '2026-06-01T10:08:00'
  }
]

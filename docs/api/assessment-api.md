# 测验与掌握度 API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/quizzes/generate` | 生成测验 |
| GET | `/api/quizzes` | 分页查询测验 |
| GET | `/api/quizzes/{id}` | 查询测验详情 |
| POST | `/api/quizzes/{id}/submit` | 提交答案并评分 |
| GET | `/api/quizzes/{id}/result` | 查询结果 |
| GET | `/api/quizzes/{id}/analysis` | 查询分析 |
| GET | `/api/mastery/me` | 查询当前用户掌握度 |

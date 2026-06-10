# 用户画像与学习偏好接口说明

所有接口均需要：

```http
Authorization: Bearer <token>
```

## 1. 查询当前用户全局画像

```http
GET /api/profiles/me
```

画像不存在时返回空画像结构，`status` 为 `incomplete`，不会自动写入数据库。

## 2. 创建或更新当前用户全局画像

```http
PUT /api/profiles/me
Content-Type: application/json
```

请求示例：

```json
{
  "realName": "张同学",
  "school": "示例大学",
  "major": "软件工程",
  "gradeLevel": "大三",
  "learningGoal": "系统复习数据库系统并完成期末测验训练",
  "subjectDirection": "数据库系统",
  "foundationLevel": "intermediate",
  "interestTags": ["数据库", "SQL", "软件杯"],
  "weakPoints": ["事务隔离级别", "索引优化"],
  "weeklyAvailableHours": 8,
  "availableTimeSlots": ["weekday_evening", "weekend_morning"],
  "outputStyle": "markdown",
  "profileSource": "manual"
}
```

## 3. 查询学习空间画像

```http
GET /api/profiles/space/{spaceId}
```

后端会先校验学习空间属于当前登录用户。空间画像不存在时返回空画像结构。

## 4. 创建或更新学习空间画像

```http
PUT /api/profiles/space/{spaceId}
Content-Type: application/json
```

请求体与全局画像一致，但数据会绑定到指定学习空间。

## 5. 查询当前用户学习偏好

```http
GET /api/preferences/me
```

偏好不存在时返回默认值：

```json
{
  "outputStyle": "markdown",
  "contentLengthPreference": "medium",
  "difficultyPreference": "medium",
  "languagePreference": "zh-CN",
  "knowledgeGraphEnabled": true,
  "quizEnabled": true,
  "reviewPlanEnabled": true,
  "status": "active"
}
```

## 6. 创建或更新当前用户学习偏好

```http
PUT /api/preferences/me
Content-Type: application/json
```

请求示例：

```json
{
  "preferredResourceTypes": ["学习计划", "复习提纲", "习题集", "知识图谱"],
  "outputStyle": "markdown",
  "contentLengthPreference": "medium",
  "difficultyPreference": "medium",
  "languagePreference": "zh-CN",
  "studyTimeSlots": ["weekday_evening"],
  "notificationEnabled": true,
  "knowledgeGraphEnabled": true,
  "quizEnabled": true,
  "reviewPlanEnabled": true
}
```

## 7. 数据隔离

全局画像和学习偏好按当前登录用户 `user_id` 查询。空间画像必须先通过学习空间归属校验，再按 `user_id` 和 `space_id` 查询或写入。

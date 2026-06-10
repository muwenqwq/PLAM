# 学习空间接口说明

所有接口均需要：

```http
Authorization: Bearer <token>
```

## 1. 创建学习空间

```http
POST /api/learning-spaces
Content-Type: application/json
```

请求示例：

```json
{
  "spaceName": "数据库系统复习",
  "subject": "数据库系统",
  "description": "围绕范式、SQL 查询、索引和事务进行期末复习。",
  "visibility": "private"
}
```

响应数据包含 `id`、`spaceName`、`subject`、`visibility`、`isDefault`、`status` 等字段。

## 2. 分页查询学习空间

```http
GET /api/learning-spaces?pageNum=1&pageSize=10&keyword=数据库&status=active
```

返回 `Result<PageResult<LearningSpaceVO>>`。查询只返回当前登录用户的数据。

## 3. 查询默认学习空间

```http
GET /api/learning-spaces/default
```

如果当前用户没有默认空间，`data` 返回 `null`。

## 4. 查询详情

```http
GET /api/learning-spaces/{id}
```

如果学习空间不存在或不属于当前用户，返回业务错误，不暴露资源归属信息。

## 5. 更新学习空间

```http
PUT /api/learning-spaces/{id}
Content-Type: application/json
```

请求示例：

```json
{
  "spaceName": "数据库系统冲刺复习",
  "subject": "数据库系统",
  "visibility": "private",
  "status": "active"
}
```

## 6. 删除学习空间

```http
DELETE /api/learning-spaces/{id}
```

删除使用逻辑删除。删除默认空间后，后端会把剩余空间中最近更新的一条设为默认空间；如果没有剩余空间，则默认空间为空。

## 7. 设置默认学习空间

```http
POST /api/learning-spaces/{id}/default
```

设置成功后，当前用户其他学习空间的默认状态会被取消。

## 8. 查询学习空间摘要

```http
GET /api/learning-spaces/{id}/summary
```

当前阶段返回 `resourceCount`、`taskCount`、`generatedResourceCount`、`activeTaskCount` 等缓存统计字段。后续阶段会接入真实对话、资源和任务统计。

# AI 角色扮演学习陪伴设计

## 目标
把 AI 角色卡从展示能力接入为真实功能：用户可以创建高自由度角色，并在学习对话中启用角色扮演模式，让 AI 以指定身份、性格、语气和陪伴目标参与学习。

## 范围
- 后端新增角色卡持久化、用户隔离、默认角色、CRUD 和应用到会话。
- 对话会话保存当前角色和是否启用角色扮演。
- Java 后端向 Python AI 服务传递角色设定。
- Python AI 服务把角色设定注入 system prompt，本地演示模型也体现角色语气。
- 前端新增角色卡页面，并在聊天页选择/启用角色。

## 数据模型
新增 `ai_companion_role` 表，字段包括用户、角色名称、身份、头像、主题色、背景、性格、擅长学科、爱好、说话风格、互动环境、陪伴目标、禁忌边界、自定义提示词、标签、是否默认、状态。`conversation` 表增加 `role_id` 和 `role_play_enabled`。

## 接口
- `GET /api/companion-roles`：分页查询角色卡。
- `GET /api/companion-roles/active`：获取默认或首个可用角色。
- `POST /api/companion-roles`：创建角色。
- `PUT /api/companion-roles/{id}`：更新角色。
- `DELETE /api/companion-roles/{id}`：逻辑删除。
- `POST /api/companion-roles/{id}/default`：设为默认。
- `POST /api/chat/conversations/{id}/role`：把角色应用到会话或关闭角色扮演。

## 前端
新增 `RoleCompanion.vue`，通过卡片 + 表单提供高自由度配置。`BasicLayout` 增加“AI 角色”入口。`ChatAssistant.vue` 顶部增加角色选择和角色扮演开关，发送消息时使用当前会话角色。

## 验证
先写后端 controller 测试和 AI 服务 provider 测试，让缺失能力失败；实现后跑 Maven 测试、pytest、前端 build 和必要的页面烟测。

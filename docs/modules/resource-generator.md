# 多智能体任务与资源生成说明

## 1. 模块定位

多智能体任务模块用于把学习目标转化为可保存的学习资源。当前阶段采用同步执行方式，先打通演示链路，后续再升级为异步任务队列。

## 2. 当前流程

1. 用户创建 Agent 任务。
2. Java 保存 `agent_task`，状态为 `running`。
3. Java 调用 Python `/ai/agents/run`。
4. Python Mock 返回多个 Agent 步骤和最终资源。
5. Java 保存 `agent_step`。
6. Java 保存 `generated_resource`。
7. Java 更新任务状态为 `succeeded` 或 `failed`。

## 3. 后续扩展

后续可引入 Redis 队列、任务状态轮询、SSE/WebSocket 推送、RAG 知识库检索和更细粒度的 Agent 协作。


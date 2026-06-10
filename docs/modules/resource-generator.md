# 资源生成中心说明

## 支持资源类型

资源生成中心基于 `generated_resource` 表保存最终结果，当前支持：

- 学习计划：`plan`
- 讲义：`lecture_note`
- 复习提纲：`review_outline`
- 习题集：`quiz_set`
- 案例任务：`case_task`
- 知识图谱：`knowledge_graph`
- PPT 大纲：`ppt_outline`

## Mermaid 知识图谱

`knowledge_graph` 类型资源必须在 `content_markdown` 中保存 Mermaid 代码块，并在 `content_json.mermaid` 中保存 Mermaid 原文，便于前端渲染和后续导出。

## 导出策略

当前阶段 Markdown 导出直接返回字符串，不生成实际文件。后续部署阶段可扩展为异步导出任务，并将文件路径写入 `export_status` 或新增导出记录表。

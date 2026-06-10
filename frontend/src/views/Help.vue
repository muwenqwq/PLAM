<template>
  <div class="page">
    <PageHeader title="演示帮助" description="用于答辩前快速确认账号、服务启动和 7 分钟演示主线。" />
    <el-card>
      <el-steps direction="vertical" :active="8">
        <el-step title="启动 MySQL" description="导入 backend/src/main/resources/sql/schema.sql 和 seed.sql" />
        <el-step title="启动 Python AI 服务" description="cd ai-service && python -m uvicorn app.main:app --host 127.0.0.1 --port 8000" />
        <el-step title="启动 Java 后端" description="cd backend && mvn.cmd spring-boot:run" />
        <el-step title="启动 Vue 前端" description="cd frontend && npm run dev" />
        <el-step title="使用 demo_student / 123456 登录" description="演示学生完整学习闭环" />
        <el-step title="模型配置与智能对话" description="验证前端只调用 Java /api/**" />
        <el-step title="Agent、知识库、资源与图谱" description="展示 Mock AI 和 Mermaid 图谱" />
        <el-step title="路径、测验、报告" description="形成个性化学习闭环" />
      </el-steps>
    </el-card>
    <div class="grid two">
      <el-card>
        <template #header>演示账号</template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="学生">demo_student / 123456</el-descriptions-item>
          <el-descriptions-item label="教师">demo_teacher / 123456</el-descriptions-item>
          <el-descriptions-item label="管理员">demo_admin / 123456</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card>
        <template #header>架构边界</template>
        <p>前端只调用 Java 后端 `/api/**`，不直接调用 Python AI 服务。</p>
        <p>Java 负责认证、权限、业务事务和 MySQL 写入；Python 只负责 Mock AI/RAG 生成。</p>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
</script>

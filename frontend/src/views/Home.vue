<template>
  <main class="home">
    <section class="hero">
      <nav>
        <div class="brand-mark">智</div>
        <strong>智学工坊 EduAgent Studio</strong>
        <RouterLink to="/login"><el-button type="primary">进入演示系统</el-button></RouterLink>
      </nav>
      <div class="hero-grid">
        <div>
          <h1>面向软件杯 A3 赛题的个性化学习智能体系统</h1>
          <p>用 Java 主后端、MySQL 规范数据库设计和 Python AI Mock 微服务，串联知识库、资源生成、学习路径、测验与学习报告闭环。</p>
          <div class="hero-actions">
            <RouterLink to="/login"><el-button type="primary" size="large">使用演示账号登录</el-button></RouterLink>
            <RouterLink to="/help"><el-button size="large">查看演示流程</el-button></RouterLink>
          </div>
        </div>
        <div class="preview panel">
          <div class="panel-body">
            <h3>7 分钟答辩主线</h3>
            <el-steps direction="vertical" :active="5">
              <el-step title="登录与 Dashboard" description="查看学习闭环概览" />
              <el-step title="模型配置与智能对话" description="验证 Mock AI 能力" />
              <el-step title="Agent 资源生成" description="展示多智能体流程" />
              <el-step title="知识库与知识图谱" description="演示 Mock RAG 和 Mermaid" />
              <el-step title="路径、测验、报告" description="形成完整学习闭环" />
            </el-steps>
          </div>
        </div>
      </div>
    </section>
    <section class="features">
      <div v-for="item in features" :key="item.title" class="route-card">
        <el-icon size="28"><component :is="item.icon" /></el-icon>
        <h3>{{ item.title }}</h3>
        <p>{{ item.text }}</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
const features = [
  { title: 'Java 主后端', text: 'Spring Boot 3、MyBatis-Plus、JWT 和统一响应结构。', icon: 'Cpu' },
  { title: 'MySQL 数据库设计', text: '完整业务表、逻辑外键、索引和演示数据。', icon: 'Coin' },
  { title: 'Python AI 微服务', text: 'Mock 对话、RAG、测验、路径和报告生成。', icon: 'MagicStick' },
  { title: '多智能体资源生成', text: 'Planner、Knowledge、Exercise、Review 流程可视化。', icon: 'Operation' },
  { title: '个性化学习闭环', text: '知识库、资源、路径、测验、掌握度和报告串联。', icon: 'Guide' }
]
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: radial-gradient(circle at top left, #dbeafe, transparent 34%), #f8fbff;
}

.hero {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 42px;
}

nav {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 12px;
}

nav .el-button {
  margin-left: auto;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-weight: 800;
}

.hero-grid {
  min-height: calc(100vh - 180px);
  display: grid;
  grid-template-columns: 1.1fr .9fr;
  gap: 32px;
  align-items: center;
}

h1 {
  margin: 0;
  font-size: 48px;
  line-height: 1.12;
  letter-spacing: 0;
}

.hero p {
  margin: 20px 0;
  color: var(--muted);
  font-size: 18px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.features {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px 48px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.features h3 {
  margin: 12px 0 8px;
}

.features p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

@media (max-width: 960px) {
  .hero-grid,
  .features {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 34px;
  }
}
</style>

<template>
  <main class="home-page">
    <header class="home-nav">
      <RouterLink to="/" class="home-brand">
        <span class="brand-mark">智</span>
        <span><strong>智学工坊</strong><small>学生学习工作台</small></span>
      </RouterLink>
      <div class="nav-actions">
        <ThemeToggle />
        <RouterLink to="/login"><el-button type="primary">进入学习空间</el-button></RouterLink>
      </div>
    </header>

    <section class="home-hero">
      <div class="hero-copy">
        <h1>让课程资料真正变成<br><span>可以使用的学习内容。</span></h1>
        <p>在同一个学习空间里上传资料、根据资料提问、生成笔记与练习题，并用测验和报告持续检查进度。</p>
        <div class="hero-actions">
          <RouterLink to="/login"><el-button type="primary" size="large">开始学习</el-button></RouterLink>
          <RouterLink to="/register"><el-button size="large">创建学生账号</el-button></RouterLink>
        </div>
        <div class="workflow">
          <span v-for="(item, index) in workflow" :key="item"><b>{{ index + 1 }}</b>{{ item }}</span>
        </div>
      </div>

      <div class="product-preview" aria-label="学习工作台预览">
        <div class="preview-sidebar">
          <span class="mini-brand">智</span>
          <i v-for="n in 6" :key="n" :class="{ active: n === 1 }"></i>
        </div>
        <div class="preview-main">
          <div class="preview-head"><div><strong>下午好，Demo Student</strong><span>今天想学习什么？</span></div><i></i></div>
          <div class="preview-banner"><strong>继续今天的学习</strong><span>从上传资料、提问或生成复习资源开始</span></div>
          <div class="preview-actions">
            <article v-for="item in previewActions" :key="item.label">
              <el-icon><component :is="item.icon" /></el-icon><strong>{{ item.label }}</strong><span>{{ item.text }}</span>
            </article>
          </div>
          <div class="preview-lists"><div></div><div></div></div>
        </div>
      </div>
    </section>

    <section class="feature-strip">
      <article v-for="item in features" :key="item.title">
        <el-icon><component :is="item.icon" /></el-icon>
        <div><h2>{{ item.title }}</h2><p>{{ item.text }}</p></div>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import ThemeToggle from '@/components/ThemeToggle.vue'

const workflow = ['上传资料', '资料问答', '生成资源', '测验复习']
const previewActions = [
  { label: '上传资料', text: '整理课程文件', icon: 'UploadFilled' },
  { label: '资料提问', text: '查找真实答案', icon: 'ChatDotRound' },
  { label: '资源生成', text: '笔记与练习题', icon: 'DocumentAdd' },
  { label: '开始测验', text: '检查掌握情况', icon: 'EditPen' }
]
const features = [
  { title: '围绕真实资料学习', text: '上传的课程文件和生成资源按学习空间隔离，并用于真实检索与问答。', icon: 'Collection' },
  { title: '把复杂功能变成步骤', text: '从资料来源到资源类型，再到生成方式，按学生能理解的路径完成操作。', icon: 'Guide' },
  { title: '持续看到学习进度', text: '测验结果和学习报告帮助你发现薄弱点，并明确下一步复习方向。', icon: 'TrendCharts' }
]
</script>

<style scoped>
.home-page { min-height: 100vh; color: var(--text); background: var(--bg); overflow: hidden; }
.home-nav { width: min(1240px, calc(100% - 40px)); height: 78px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; }
.home-brand { display: flex; align-items: center; gap: 11px; }
.brand-mark { width: 42px; height: 42px; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg, #7657d6, #5486da); border-radius: 13px; font-weight: 800; box-shadow: 0 10px 24px rgba(118,87,214,.24); }
.home-brand strong, .home-brand small { display: block; line-height: 1.25; }
.home-brand strong { color: var(--text-strong); font-size: 16px; }
.home-brand small { color: var(--muted); font-size: 11px; }
.nav-actions { display: flex; align-items: center; gap: 10px; }
.home-hero { width: min(1240px, calc(100% - 40px)); min-height: calc(100vh - 150px); margin: 0 auto; display: grid; grid-template-columns: .82fr 1.18fr; align-items: center; gap: 54px; padding: 44px 0 70px; }
.hero-copy h1 { margin: 0; color: var(--text-strong); font-size: clamp(42px, 5.5vw, 68px); line-height: 1.08; }
.hero-copy h1 span { color: var(--primary); }
.hero-copy > p { max-width: 620px; margin: 24px 0; color: var(--muted); font-size: 17px; line-height: 1.85; }
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.workflow { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 34px; color: var(--muted); font-size: 13px; }
.workflow span { display: inline-flex; align-items: center; gap: 7px; }
.workflow b { width: 24px; height: 24px; display: grid; place-items: center; color: var(--primary); background: var(--primary-soft); border: 1px solid var(--primary-border); border-radius: 50%; }
.product-preview { min-height: 520px; display: grid; grid-template-columns: 72px 1fr; overflow: hidden; background: var(--surface-glass); border: 1px solid var(--line); border-radius: 22px; box-shadow: var(--shadow-lg); transform: perspective(1200px) rotateY(-2deg) rotateX(1deg); }
.preview-sidebar { padding: 18px 12px; display: flex; flex-direction: column; align-items: center; gap: 13px; background: var(--sidebar-bg); border-right: 1px solid var(--line); }
.mini-brand { width: 38px; height: 38px; display: grid; place-items: center; margin-bottom: 10px; color: #fff; background: linear-gradient(135deg, #7657d6, #5486da); border-radius: 12px; font-weight: 800; }
.preview-sidebar i { width: 38px; height: 38px; border-radius: 11px; background: var(--surface-soft); }
.preview-sidebar i.active { background: var(--primary-soft); box-shadow: inset 3px 0 var(--primary); }
.preview-main { min-width: 0; padding: 22px; }
.preview-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.preview-head strong, .preview-head span { display: block; }
.preview-head strong { color: var(--text-strong); font-size: 17px; }
.preview-head span { color: var(--muted); font-size: 11px; }
.preview-head i { width: 92px; height: 34px; background: var(--surface-soft); border: 1px solid var(--line); border-radius: 999px; }
.preview-banner { display: grid; gap: 4px; padding: 18px; background: var(--primary-soft); border: 1px solid var(--primary-border); border-radius: 15px; }
.preview-banner strong { color: var(--text-strong); }
.preview-banner span { color: var(--muted); font-size: 12px; }
.preview-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 14px; }
.preview-actions article { min-height: 112px; display: grid; align-content: start; gap: 5px; padding: 14px; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; }
.preview-actions .el-icon { color: var(--primary); font-size: 21px; }
.preview-actions strong { font-size: 13px; }
.preview-actions span { color: var(--muted); font-size: 11px; }
.preview-lists { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-top: 14px; }
.preview-lists div { height: 116px; background: var(--surface-soft); border: 1px solid var(--line); border-radius: 14px; }
.feature-strip { width: min(1240px, calc(100% - 40px)); margin: -18px auto 0; padding: 0 0 50px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.feature-strip article { display: flex; gap: 14px; padding: 19px; background: var(--surface-glass); border: 1px solid var(--line); border-radius: 15px; box-shadow: var(--shadow-sm); }
.feature-strip .el-icon { flex: 0 0 auto; margin-top: 2px; color: var(--primary); font-size: 24px; }
.feature-strip h2 { margin: 0 0 5px; color: var(--text-strong); font-size: 16px; }
.feature-strip p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.65; }
@media (max-width: 1020px) { .home-hero { grid-template-columns: 1fr; padding-top: 50px; } .product-preview { transform: none; } .feature-strip { margin-top: 0; } }
@media (max-width: 760px) { .home-nav { width: min(100% - 28px, 1240px); } .home-brand small { display: none; } .home-hero { width: min(100% - 28px, 1240px); min-height: auto; padding: 54px 0; } .hero-copy h1 { font-size: 42px; } .product-preview { min-height: 450px; grid-template-columns: 54px 1fr; } .preview-sidebar { padding: 14px 8px; } .preview-sidebar i { width: 34px; height: 34px; } .preview-main { padding: 14px; } .feature-strip { width: min(100% - 28px, 1240px); grid-template-columns: 1fr; } }
@media (max-width: 520px) { .nav-actions .el-button { display: none; } .hero-copy h1 { font-size: 36px; } .product-preview { min-height: 410px; } .preview-actions article { min-height: 96px; padding: 10px; } .preview-lists { grid-template-columns: 1fr; } .preview-lists div:last-child { display: none; } }
</style>
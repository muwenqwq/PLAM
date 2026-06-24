<template>
  <main class="auth-page">
    <div class="auth-topbar">
      <RouterLink to="/" class="auth-brand" aria-label="返回智学工坊首页">
        <span class="brand-mark">智</span>
        <span><strong>智学工坊</strong><small>学生学习工作台</small></span>
      </RouterLink>
      <ThemeToggle />
    </div>

    <section class="auth-shell">
      <div class="auth-intro">
        <span class="intro-label">欢迎回来</span>
        <h1>把今天要学的内容，整理成清晰的下一步。</h1>
        <p>上传课程资料、基于资料提问、生成复习资源，再用测验检查掌握情况。</p>
        <div class="intro-points">
          <div><el-icon><Collection /></el-icon><span>资料统一整理</span></div>
          <div><el-icon><ChatDotRound /></el-icon><span>根据资料回答</span></div>
          <div><el-icon><DocumentAdd /></el-icon><span>生成学习资源</span></div>
        </div>
      </div>

      <div class="auth-card">
        <div class="auth-heading">
          <h2>登录学习空间</h2>
          <p>继续你的资料整理、资源生成和阶段复习。</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="full" @click="handleLogin">登录</el-button>
        </el-form>
        <button class="demo-account" type="button" @click="fillStudent">
          <el-icon><MagicStick /></el-icon>
          <span><strong>使用学生演示账号</strong><small>demo_student / 123456</small></span>
        </button>
        <p class="auth-switch">还没有账号？<RouterLink to="/register">注册学生账号</RouterLink></p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Collection, DocumentAdd, Lock, MagicStick, User } from '@element-plus/icons-vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'demo_student', password: '123456' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fillStudent() {
  form.username = 'demo_student'
  form.password = '123456'
}

async function handleLogin() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.loginByPassword(form.username, form.password)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; padding: 24px; color: var(--text); background: var(--bg); position: relative; overflow: hidden; }
.auth-page::before {
  content: ''; position: fixed; inset: 0; pointer-events: none; opacity: .52;
  background-image: linear-gradient(var(--line) 1px, transparent 1px), linear-gradient(90deg, var(--line) 1px, transparent 1px);
  background-size: 56px 56px; mask-image: linear-gradient(to bottom, black, transparent 76%);
}
.auth-topbar { position: relative; z-index: 1; width: min(1180px, 100%); margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.auth-brand { display: flex; align-items: center; gap: 11px; }
.brand-mark { width: 42px; height: 42px; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg, #7657d6, #5486da); border-radius: 13px; font-weight: 800; box-shadow: 0 10px 24px rgba(118,87,214,.24); }
.auth-brand strong, .auth-brand small { display: block; line-height: 1.25; }
.auth-brand strong { color: var(--text-strong); font-size: 16px; }
.auth-brand small { color: var(--muted); font-size: 11px; }
.auth-shell { position: relative; z-index: 1; width: min(980px, 100%); min-height: calc(100vh - 116px); margin: 26px auto 0; display: grid; grid-template-columns: 1.08fr .92fr; align-items: center; gap: 60px; }
.auth-intro { padding: 28px 0; }
.intro-label { display: inline-flex; margin-bottom: 18px; color: var(--primary); font-size: 13px; font-weight: 700; }
.auth-intro h1 { max-width: 560px; margin: 0; color: var(--text-strong); font-size: clamp(36px, 5vw, 56px); line-height: 1.15; }
.auth-intro > p { max-width: 540px; margin: 22px 0 30px; color: var(--muted); font-size: 17px; line-height: 1.8; }
.intro-points { display: flex; flex-wrap: wrap; gap: 10px; }
.intro-points div { display: inline-flex; align-items: center; gap: 8px; padding: 9px 12px; color: var(--text); background: var(--surface-glass); border: 1px solid var(--line); border-radius: 10px; box-shadow: var(--shadow-sm); }
.intro-points .el-icon { color: var(--primary); }
.auth-card { padding: 30px; background: var(--surface-glass); border: 1px solid var(--line); border-radius: var(--radius-xl); box-shadow: var(--shadow-lg); backdrop-filter: blur(22px); }
.auth-heading h2 { margin: 0; color: var(--text-strong); font-size: 26px; }
.auth-heading p { margin: 7px 0 24px; color: var(--muted); }
.full { width: 100%; min-height: 48px; margin-top: 4px; }
.demo-account { width: 100%; display: flex; align-items: center; gap: 11px; margin-top: 16px; padding: 12px 14px; color: var(--text); text-align: left; background: var(--surface-soft); border: 1px solid var(--line); border-radius: 12px; cursor: pointer; }
.demo-account:hover { border-color: var(--primary-border); background: var(--primary-soft); }
.demo-account .el-icon { color: var(--primary); font-size: 20px; }
.demo-account strong, .demo-account small { display: block; }
.demo-account strong { font-size: 13px; }
.demo-account small { margin-top: 2px; color: var(--muted); }
.auth-switch { margin: 18px 0 0; color: var(--muted); text-align: center; font-size: 13px; }
.auth-switch a { color: var(--primary); font-weight: 700; }
@media (max-width: 820px) { .auth-shell { grid-template-columns: 1fr; gap: 20px; padding: 40px 0; } .auth-intro { display: none; } .auth-card { width: min(460px, 100%); margin: auto; } }
@media (max-width: 520px) { .auth-page { padding: 16px; } .auth-card { padding: 24px 18px; } }
</style>
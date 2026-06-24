<template>
  <main class="auth-page">
    <div class="auth-topbar">
      <RouterLink to="/" class="auth-brand" aria-label="返回智学工坊首页">
        <span class="brand-mark">智</span>
        <span><strong>智学工坊</strong><small>学生学习工作台</small></span>
      </RouterLink>
      <ThemeToggle />
    </div>

    <section class="register-shell">
      <div class="register-copy">
        <span>创建学习账号</span>
        <h1>建立自己的学习空间，让资料和进度始终有序。</h1>
        <p>注册后可以按课程管理资料，生成复习内容，并持续查看测验与学习报告。</p>
      </div>

      <div class="auth-card">
        <div class="auth-heading">
          <h2>注册学生账号</h2>
          <p>填写基础信息后即可开始使用。</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-row">
            <el-form-item label="用户名" prop="username"><el-input v-model="form.username" size="large" placeholder="登录时使用" :prefix-icon="User" autocomplete="username" /></el-form-item>
            <el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" size="large" placeholder="页面显示名称" :prefix-icon="EditPen" /></el-form-item>
          </div>
          <el-form-item label="邮箱"><el-input v-model="form.email" size="large" placeholder="选填，用于接收学习提醒" :prefix-icon="Message" autocomplete="email" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" show-password placeholder="至少 6 位" :prefix-icon="Lock" autocomplete="new-password" /></el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="full" @click="submit">创建账号</el-button>
        </el-form>
        <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { EditPen, Lock, Message, User } from '@element-plus/icons-vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', email: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}

async function submit() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; padding: 24px; color: var(--text); background: var(--bg); position: relative; overflow: hidden; }
.auth-page::before { content: ''; position: fixed; inset: 0; pointer-events: none; opacity: .52; background-image: linear-gradient(var(--line) 1px, transparent 1px), linear-gradient(90deg, var(--line) 1px, transparent 1px); background-size: 56px 56px; mask-image: linear-gradient(to bottom, black, transparent 76%); }
.auth-topbar { position: relative; z-index: 1; width: min(1180px, 100%); margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.auth-brand { display: flex; align-items: center; gap: 11px; }
.brand-mark { width: 42px; height: 42px; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg, #7657d6, #5486da); border-radius: 13px; font-weight: 800; box-shadow: 0 10px 24px rgba(118,87,214,.24); }
.auth-brand strong, .auth-brand small { display: block; line-height: 1.25; }
.auth-brand strong { color: var(--text-strong); font-size: 16px; }
.auth-brand small { color: var(--muted); font-size: 11px; }
.register-shell { position: relative; z-index: 1; width: min(1040px, 100%); min-height: calc(100vh - 116px); margin: 26px auto 0; display: grid; grid-template-columns: .9fr 1.1fr; align-items: center; gap: 56px; }
.register-copy > span { color: var(--primary); font-weight: 700; font-size: 13px; }
.register-copy h1 { margin: 16px 0 18px; color: var(--text-strong); font-size: clamp(34px, 4.6vw, 52px); line-height: 1.17; }
.register-copy p { margin: 0; color: var(--muted); font-size: 16px; line-height: 1.8; }
.auth-card { padding: 30px; background: var(--surface-glass); border: 1px solid var(--line); border-radius: var(--radius-xl); box-shadow: var(--shadow-lg); backdrop-filter: blur(22px); }
.auth-heading h2 { margin: 0; color: var(--text-strong); font-size: 26px; }
.auth-heading p { margin: 7px 0 24px; color: var(--muted); }
.full { width: 100%; min-height: 48px; margin-top: 4px; }
.auth-switch { margin: 18px 0 0; color: var(--muted); text-align: center; font-size: 13px; }
.auth-switch a { color: var(--primary); font-weight: 700; }
@media (max-width: 840px) { .register-shell { grid-template-columns: 1fr; gap: 22px; padding: 38px 0; } .register-copy { display: none; } .auth-card { width: min(560px, 100%); margin: auto; } }
@media (max-width: 560px) { .auth-page { padding: 16px; } .auth-card { padding: 24px 18px; } }
</style>
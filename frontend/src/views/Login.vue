<template>
  <main class="auth-page">
    <el-card class="auth-card">
      <h1>智学工坊 EduAgent Studio</h1>
      <p>登录后进入完整学习智能体演示流程。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="full" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="demo">
        <span>演示账号</span>
        <el-button v-for="item in accounts" :key="item.username" size="small" @click="fill(item)">{{ item.label }}</el-button>
      </div>
      <RouterLink to="/register" class="link">没有账号？前往注册</RouterLink>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'demo_student', password: '123456' })
const accounts = [
  { label: '学生 demo_student', username: 'demo_student', password: '123456' },
  { label: '教师 demo_teacher', username: 'demo_teacher', password: '123456' },
  { label: '管理员 demo_admin', username: 'demo_admin', password: '123456' }
]
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(item: any) {
  form.username = item.username
  form.password = item.password
}

async function handleLogin() {
  await formRef.value?.validate()
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
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #e0f2fe, #eef2ff 48%, #f8fafc);
}

.auth-card {
  width: min(440px, 100%);
  border-radius: 8px;
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

p {
  color: var(--muted);
  margin: 0 0 22px;
}

.full {
  width: 100%;
}

.demo {
  margin: 18px 0 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.demo span,
.link {
  color: var(--muted);
  font-size: 13px;
}
</style>

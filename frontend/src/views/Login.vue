<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="card-header">
        <div class="logo-icon">💡</div>
        <h2 class="title">欢迎回来 👋</h2>
        <p class="subtitle">登录你的 LearnAgent 账号</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">账号</label>
          <input 
            id="username" 
            v-model="loginForm.username" 
            type="text" 
            placeholder="请输入账号 (admin)" 
            autocomplete="off"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password" 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码 (123456)" 
            autocomplete="off"
            required
          />
        </div>

        <div v-if="errorMessage" class="error-message">
          ⚠️ {{ errorMessage }}
        </div>

        <button type="submit" class="submit-btn" :disabled="isLoading">
          <span v-if="isLoading" class="spinner"></span>
          {{ isLoading ? '登录中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 响应式状态管理
const loginForm = reactive({
  username: '',
  password: ''
})
const isLoading = ref(false)
const errorMessage = ref('')

// 处理登录逻辑
const handleLogin = async () => {
  // 重置错误状态
  errorMessage.value = ''
  
  if (!loginForm.username || !loginForm.password) {
    errorMessage.value = '账号和密码不能为空'
    return
  }

  // 开启 loading 状态
  isLoading.value = true

  // 模拟网络请求 (真实项目中这里会替换成 axios 请求后端的 login 接口)
  setTimeout(() => {
    // 模拟账号密码校验
    if (loginForm.username === 'admin' && loginForm.password === '123456') {
      // 登录成功 —— 写入用户信息到 Pinia（持久化到 localStorage）
      userStore.setUserInfo({
        name: '小林',
        target: '两周内掌握 A* 搜索算法',
        imgSrc: 'https://api.dicebear.com/7.x/notionists/svg?seed=Felix',
        studentId: 1,
        courseId: 1,
        token: 'mock_token_' + Date.now()
      })
      router.push('/dashboard')
    } else {
      // 登录失败
      errorMessage.value = '账号或密码错误，请检查'
      isLoading.value = false
    }
  }, 1000) // 延迟 1 秒模拟网络延迟
}
</script>

<style scoped>
/* 全局背景包裹器 */
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-page);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 居中的白色卡片 */
.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  padding: 40px;
  box-sizing: border-box;
}

/* 头部样式 */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}
.logo-icon {
  font-size: 40px;
  margin-bottom: 12px;
}
.title {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: var(--text-heading);
}
.subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-subtle);
}

/* 表单组样式 */
.form-group {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
}
.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.form-group input {
  padding: 12px 16px;
  border: 1px solid var(--border-default);
  border-radius: 8px;
  font-size: 15px;
  color: var(--text-body);
  transition: all 0.2s;
  outline: none;
}
.form-group input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-focus);
}

/* 错误提示 */
.error-message {
  background-color: var(--color-error-bg);
  color: var(--color-error);
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 20px;
  border: 1px solid var(--color-error-border);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: var(--color-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: background-color 0.2s;
}
.submit-btn:hover:not(:disabled) {
  background-color: var(--color-primary-dark);
}
.submit-btn:disabled {
  background-color: var(--text-placeholder);
  cursor: not-allowed;
}

/* 简易的加载动画 */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
  margin-right: 8px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
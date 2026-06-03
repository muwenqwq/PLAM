import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(router)
app.use(pinia)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')

// 挂载后恢复持久化的主题设置（pinia-plugin-persistedstate 已写入 localStorage）
try {
  const saved = JSON.parse(localStorage.getItem('user') || '{}')
  if (saved.theme === 'dark') {
    document.documentElement.dataset.theme = 'dark'
    document.documentElement.classList.add('dark')
  }
} catch { /* 忽略 */ }
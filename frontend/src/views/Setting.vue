<template>
  <div class="setting-page">
    <div class="page-header">
      <h2>⚙️ 设置</h2>
      <p>管理个人信息和系统配置</p>
    </div>

    <!-- 个人信息 -->
    <el-card shadow="never" class="setting-card">
      <template #header><span>👤 个人信息</span></template>
      <el-form :model="form" label-width="80px" class="setting-form">
        <el-form-item label="头像">
          <div class="avatar-area">
            <el-avatar :size="64" :src="userStore.imgSrc" />
            <el-button text type="primary" size="small" @click="refreshAvatar">
              换一个头像
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.name" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input v-model="form.target" placeholder="请输入当前学习目标" />
        </el-form-item>
        <el-form-item label="学生 ID">
          <el-input-number v-model="form.studentId" :min="1" />
        </el-form-item>
        <el-form-item label="课程 ID">
          <el-input-number v-model="form.courseId" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 外观设置 -->
    <el-card shadow="never" class="setting-card">
      <template #header><span>🎨 外观设置</span></template>
      <el-form label-width="80px" class="setting-form">
        <el-form-item label="主题模式">
          <el-radio-group v-model="currentTheme" @change="handleThemeChange">
            <el-radio-button value="light">☀️ 浅色</el-radio-button>
            <el-radio-button value="dark">🌙 深色</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 系统信息 -->
    <el-card shadow="never" class="setting-card">
      <template #header><span>ℹ️ 系统信息</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="项目名称">LearnAgent-A3</el-descriptions-item>
        <el-descriptions-item label="版本">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="技术栈">Vue 3 + Element Plus + Pinia</el-descriptions-item>
        <el-descriptions-item label="后端">Java Spring Boot + Python FastAPI</el-descriptions-item>
        <el-descriptions-item label="数据库">MySQL 8.x + Chroma</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 危险操作 -->
    <el-card shadow="never" class="setting-card danger-card">
      <template #header><span>⚠️ 危险操作</span></template>
      <div class="danger-actions">
        <div class="danger-item">
          <div>
            <h4>清空所有数据</h4>
            <p>清除本地存储的所有用户数据，恢复默认状态</p>
          </div>
          <el-button type="danger" @click="handleClearData">清空数据</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 主题切换
const currentTheme = ref(userStore.theme || 'light')
function handleThemeChange(val) {
  userStore.setTheme(val)
}

// 表单数据（从 store 初始化）
const form = reactive({
  name: userStore.name === '访客' ? '' : userStore.name,
  target: userStore.target === '请先登录' ? '' : userStore.target,
  studentId: userStore.studentId || 1,
  courseId: userStore.courseId || 1
})

// 保存个人信息
function handleSave() {
  userStore.setUserInfo({
    name: form.name || '访客',
    target: form.target || '请先登录',
    imgSrc: userStore.imgSrc,
    studentId: form.studentId,
    courseId: form.courseId,
    token: userStore.token
  })
  ElMessage.success('保存成功')
}

// 重置表单
function resetForm() {
  form.name = userStore.name === '访客' ? '' : userStore.name
  form.target = userStore.target === '请先登录' ? '' : userStore.target
  form.studentId = userStore.studentId || 1
  form.courseId = userStore.courseId || 1
}

// 随机换头像
function refreshAvatar() {
  const seed = Math.random().toString(36).slice(2, 8)
  userStore.setUserInfo({
    name: userStore.name,
    target: userStore.target,
    imgSrc: `https://api.dicebear.com/7.x/notionists/svg?seed=${seed}`,
    studentId: userStore.studentId,
    courseId: userStore.courseId,
    token: userStore.token
  })
}

// 清空所有数据
async function handleClearData() {
  try {
    await ElMessageBox.confirm('确定要清空所有本地数据吗？此操作不可撤销。', '确认清空', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    localStorage.clear()
    userStore.clearUserInfo()
    ElMessage.success('数据已清空，页面将刷新')
    setTimeout(() => location.reload(), 1000)
  } catch {
    // 取消
  }
}
</script>

<style scoped>
.setting-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
/* .page-header 样式已移至 style.css 全局定义 */
.setting-card { border-radius: 12px; }
.setting-form {
  max-width: 480px;
}
.avatar-area {
  display: flex;
  align-items: center;
  gap: 16px;
}
.danger-card :deep(.el-card__header) {
  background: var(--color-error-bg);
}
.danger-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}
.danger-item h4 {
  margin: 0 0 4px;
  font-size: 14px;
  color: var(--text-body);
}
.danger-item p {
  margin: 0;
  font-size: 12px;
  color: var(--text-placeholder);
}
</style>

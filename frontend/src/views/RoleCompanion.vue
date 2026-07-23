<template>
  <div class="page role-page">
    <PageHeader title="AI 角色" description="创建可保存、可复用的学习陪伴角色，并在 AI 对话中启用。" />

    <div class="role-layout">
      <section class="role-panel role-list-panel">
        <div class="panel-head">
          <div>
            <h2>我的角色</h2>
            <p>保存后的角色可以在 AI 对话里一键启用。</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="newRole">新建</el-button>
        </div>

        <el-scrollbar class="role-scroll">
          <button
            v-for="role in roles"
            :key="role.id"
            class="role-card"
            :class="{ active: role.id === form.id }"
            type="button"
            @click="selectRole(role)"
          >
            <span class="avatar" :style="{ background: role.themeColor || '#409EFF' }">{{ role.roleName?.slice(0, 1) || 'AI' }}</span>
            <span class="role-copy">
              <strong>{{ role.roleName }}</strong>
              <em>{{ role.roleIdentity || '学习陪伴角色' }}</em>
              <span>{{ role.tags || role.speakingStyle || '自定义学习陪伴' }}</span>
            </span>
            <el-tag v-if="role.defaultRole" size="small" type="success">默认</el-tag>
          </button>
          <EmptyState v-if="!roles.length" title="还没有角色" description="先新建一个适合当前学习任务的陪伴角色。" />
        </el-scrollbar>
      </section>

      <section class="role-panel editor-panel">
        <div class="template-row">
          <el-button v-for="item in templates" :key="item.roleName" round @click="applyTemplate(item)">{{ item.roleName }}</el-button>
        </div>

        <el-form label-position="top" class="role-form">
          <div class="form-grid three">
            <el-form-item label="角色名称">
              <el-input v-model="form.roleName" maxlength="100" placeholder="例如：温柔学姐小知" />
            </el-form-item>
            <el-form-item label="角色身份">
              <el-input v-model="form.roleIdentity" maxlength="100" placeholder="导师、学姐、学习搭子、督学教练" />
            </el-form-item>
            <el-form-item label="主题色">
              <el-color-picker v-model="form.themeColor" />
            </el-form-item>
          </div>

          <div class="form-grid two">
            <el-form-item label="角色背景">
              <el-input v-model="form.background" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="这个角色为什么陪我学习、正在什么学习场景里帮助我。" />
            </el-form-item>
            <el-form-item label="角色性格">
              <el-input v-model="form.personality" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="温柔、严格、幽默、耐心、直接、积极反馈……" />
            </el-form-item>
          </div>

          <div class="form-grid two">
            <el-form-item label="擅长内容">
              <el-input v-model="form.expertise" maxlength="500" placeholder="英语六级、数据库、编程调试、论文写作……" />
            </el-form-item>
            <el-form-item label="角色爱好">
              <el-input v-model="form.hobbies" maxlength="500" placeholder="做笔记、打卡、分享方法、轻音乐……" />
            </el-form-item>
          </div>

          <div class="form-grid two">
            <el-form-item label="说话风格">
              <el-input v-model="form.speakingStyle" maxlength="500" placeholder="鼓励式、伙伴式、直白硬核、循循善诱……" />
            </el-form-item>
            <el-form-item label="互动场景">
              <el-input v-model="form.scenario" maxlength="500" placeholder="晚自习、考前冲刺、论文修改、编程调试……" />
            </el-form-item>
          </div>

          <div class="form-grid two">
            <el-form-item label="陪伴目标">
              <el-input v-model="form.companionGoal" maxlength="500" placeholder="缓解焦虑、督促打卡、拆解难题、保持节奏……" />
            </el-form-item>
            <el-form-item label="角色标签">
              <el-input v-model="form.tags" maxlength="500" placeholder="陪伴型、鼓励式、考前冲刺" />
            </el-form-item>
          </div>

          <div class="form-grid two">
            <el-form-item label="边界设置">
              <el-input v-model="form.boundaries" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="例如：不直接代写答案，先引导理解；不要制造焦虑。" />
            </el-form-item>
            <el-form-item label="额外提示词">
              <el-input v-model="form.customPrompt" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="任何你希望角色长期遵守的互动规则。" />
            </el-form-item>
          </div>

          <div class="preview-strip">
            <div>
              <span class="preview-dot" :style="{ background: form.themeColor || '#409EFF' }" />
              <strong>{{ form.roleName || '未命名角色' }}</strong>
              <em>{{ form.roleIdentity || '学习陪伴角色' }}</em>
            </div>
            <p>“{{ previewText }}”</p>
          </div>

          <div class="form-actions">
            <el-checkbox v-model="form.defaultRole">设为默认角色</el-checkbox>
            <span class="spacer" />
            <el-button v-if="form.id" :icon="ChatDotRound" @click="startChat">用此角色对话</el-button>
            <el-button v-if="form.id" :icon="Star" @click="setDefault">设为默认</el-button>
            <el-button v-if="form.id" type="danger" plain :icon="Delete" @click="removeRole">删除</el-button>
            <el-button type="primary" :loading="saving" :icon="Check" @click="saveRole">保存角色</el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Check, Delete, Plus, Star } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  CompanionRole,
  createCompanionRole,
  deleteCompanionRole,
  listCompanionRoles,
  setDefaultCompanionRole,
  updateCompanionRole
} from '@/api/companionRole'

const router = useRouter()
const roles = ref<CompanionRole[]>([])
const saving = ref(false)
const form = reactive<CompanionRole>(emptyRole())

const templates: CompanionRole[] = [
  {
    roleName: '温柔学姐',
    roleIdentity: '高年级学姐',
    themeColor: '#409EFF',
    background: '正在陪你准备考试的高年级学姐，擅长把复杂内容讲得更容易理解。',
    personality: '温柔、耐心、积极反馈',
    expertise: '英语六级、学习规划、错题复盘',
    hobbies: '做笔记、分享方法、轻音乐',
    speakingStyle: '鼓励式、伙伴式、循循善诱',
    scenario: '晚自习、自习室、考前冲刺',
    companionGoal: '帮我坚持学习、缓解焦虑、督促打卡',
    boundaries: '不直接代写作业答案，先引导理解。',
    customPrompt: '回答前先把任务拆成两步，语气轻松但不敷衍。',
    tags: '陪伴型、鼓励式、晚自习'
  },
  {
    roleName: '严格督学',
    roleIdentity: '学习督导老师',
    themeColor: '#67C23A',
    personality: '直接、严谨、重视计划执行',
    expertise: '复习计划、阶段测验、薄弱点诊断',
    speakingStyle: '直白、硬核、少兜圈',
    scenario: '考前冲刺、阶段复盘',
    companionGoal: '减少拖延，形成稳定学习节奏',
    boundaries: '不制造焦虑，不给空泛建议。',
    customPrompt: '每次回答都给出下一步动作。',
    tags: '督学型、计划型、复盘'
  },
  {
    roleName: '编程搭子',
    roleIdentity: '一起排错的学习伙伴',
    themeColor: '#E6A23C',
    personality: '冷静、耐心、喜欢一步步定位问题',
    expertise: 'Java、Spring Boot、前后端联调、报错分析',
    speakingStyle: '先复现、再定位、最后给修改建议',
    scenario: '编程调试、项目答辩前联调',
    companionGoal: '降低排错挫败感，帮我把问题定位清楚',
    boundaries: '不跳过验证步骤，不凭空猜测环境。',
    customPrompt: '遇到报错时先总结关键信息，再给最小修复路径。',
    tags: '编程、调试、搭子'
  }
]

const previewText = computed(() => form.speakingStyle || '别着急，我们先把这道题拆成两步，学会第一步就已经在进步了。')

function emptyRole(): CompanionRole {
  return {
    id: undefined,
    roleName: '',
    roleIdentity: '',
    themeColor: '#409EFF',
    background: '',
    personality: '',
    expertise: '',
    hobbies: '',
    speakingStyle: '',
    scenario: '',
    companionGoal: '',
    boundaries: '',
    customPrompt: '',
    tags: '',
    defaultRole: false,
    status: 'active'
  }
}

function assignForm(role: CompanionRole) {
  Object.keys(form).forEach((key) => delete (form as any)[key])
  Object.assign(form, emptyRole(), role)
}

function newRole() {
  assignForm(emptyRole())
}

function selectRole(role: CompanionRole) {
  assignForm({ ...role })
}

function applyTemplate(role: CompanionRole) {
  assignForm({ ...emptyRole(), ...role, id: undefined, defaultRole: false })
}

async function loadRoles() {
  const data = await listCompanionRoles({ pageNum: 1, pageSize: 100 })
  roles.value = data.records || []
}

async function saveRole() {
  if (!form.roleName?.trim()) return ElMessage.warning('请先填写角色名称')
  saving.value = true
  try {
    const payload = { ...form }
    if (!form.id) delete payload.id
    const saved = form.id ? await updateCompanionRole(form.id, payload) : await createCompanionRole(payload)
    ElMessage.success(form.id ? '角色已更新' : '角色已新建')
    assignForm(saved)
    await loadRoles()
  } finally {
    saving.value = false
  }
}

async function setDefault() {
  if (!form.id) return
  const saved = await setDefaultCompanionRole(form.id)
  ElMessage.success('已设为默认角色')
  assignForm(saved)
  await loadRoles()
}

async function startChat() {
  if (!form.id) await saveRole()
  if (!form.id) return
  await router.push({ path: '/chat', query: { roleId: String(form.id), rolePlay: '1', new: '1' } })
}

async function removeRole() {
  if (!form.id) return
  await ElMessageBox.confirm('删除后聊天中将不能继续选择这个角色。', '删除角色', { type: 'warning' })
  await deleteCompanionRole(form.id)
  ElMessage.success('角色已删除')
  newRole()
  await loadRoles()
}

onMounted(async () => {
  await loadRoles()
})
</script>

<style scoped>
.role-page { gap: 16px; }
.role-layout { display: grid; grid-template-columns: 330px minmax(0, 1fr); gap: 16px; min-height: 0; }
.role-panel { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); }
.role-list-panel { min-height: 680px; padding: 16px; }
.panel-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
.panel-head h2 { margin: 0; font-size: 18px; color: var(--text-strong); }
.panel-head p { margin: 4px 0 0; color: var(--muted); font-size: 12px; }
.role-scroll { height: 600px; }
.role-card { width: 100%; display: flex; align-items: center; gap: 12px; padding: 12px; margin-bottom: 10px; text-align: left; background: var(--surface-soft); border: 1px solid var(--line); border-radius: var(--radius-md); cursor: pointer; }
.role-card.active, .role-card:hover { border-color: var(--primary); background: var(--primary-soft); }
.avatar { width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center; color: #fff; border-radius: 50%; font-weight: 800; }
.role-copy { min-width: 0; flex: 1; }
.role-copy strong, .role-copy em, .role-copy span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.role-copy strong { color: var(--text-strong); font-size: 14px; }
.role-copy em { margin-top: 2px; color: var(--muted); font-size: 12px; font-style: normal; }
.role-copy span { margin-top: 4px; color: var(--muted); font-size: 11px; }
.editor-panel { padding: 18px; }
.template-row { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.role-form { display: flex; flex-direction: column; gap: 2px; }
.form-grid { display: grid; gap: 14px; }
.form-grid.two { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.form-grid.three { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 110px; }
.preview-strip { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin: 4px 0 16px; padding: 14px 16px; border: 1px solid var(--line); border-radius: var(--radius-md); background: var(--surface-soft); }
.preview-strip div { display: flex; align-items: center; gap: 9px; min-width: 0; }
.preview-strip strong { color: var(--text-strong); }
.preview-strip em { color: var(--muted); font-style: normal; }
.preview-strip p { margin: 0; color: var(--text); font-size: 13px; }
.preview-dot { width: 12px; height: 12px; border-radius: 999px; }
.form-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.spacer { flex: 1; }
@media (max-width: 1100px) {
  .role-layout { grid-template-columns: 1fr; }
  .role-list-panel { min-height: 0; }
  .role-scroll { height: 320px; }
  .form-grid.two, .form-grid.three { grid-template-columns: 1fr; }
  .preview-strip, .form-actions { align-items: stretch; flex-direction: column; }
  .spacer { display: none; }
}
</style>
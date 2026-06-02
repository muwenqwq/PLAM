export const menuConfig = [
    { icon: '🎛️', cn: '仪表盘', name: 'Dashboard', path: '/dashboard', component: () => import('@/views/Dashboard.vue') },
    { icon: '👤', cn: '个人资料', name: 'Profile', path: '/profile', component: () => import('@/views/Profile.vue') },
    { icon: '📚', cn: '学习资源', name: 'Resources', path: '/resources', component: () => import('@/views/Resources.vue') },
    { icon: '📅', cn: '学习计划', name: 'StudyPlan', path: '/study-plan', component: () => import('@/views/Plan.vue') },
    { icon: '👨‍🏫', cn: '智能辅导', name: 'Tutor', path: '/tutor', component: () => import('@/views/Tutor.vue') },
    { icon: '📊', cn: '数据统计', name: 'Analytics', path: '/analytics', component: () => import('@/views/Statistics.vue') },
    { icon: '🤖', cn: 'Agent 记录', name: 'AgentRuns', path: '/agent-runs', component: () => import('@/views/AgentRecords.vue') },
    { icon: '⚙️', cn: '设置', name: 'Settings', path: '/settings', component: () => import('@/views/Setting.vue'), position: 'bottom' },
    { icon: '🚪', cn: '退出登录', name: 'Login', path: '/login', component: () => import('@/views/Login.vue'), position: 'bottom' }
];
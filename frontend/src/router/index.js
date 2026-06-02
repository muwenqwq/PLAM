import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import Login from '@/views/Login.vue'
import { menuConfig } from '@/config/menu.js'

const dynamicMenuRouter = menuConfig.filter(item => item.path !== '/login').map(item => ({
  path: item.path,
  name: item.name,
  component: item.component,
  meta: {
    title: item.cn,
    icon: item.icon
  }
}));

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      ...dynamicMenuRouter
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
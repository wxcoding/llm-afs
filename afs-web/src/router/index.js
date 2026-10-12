import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('@/views/home/index.vue') },
  { path: '/login', component: () => import('@/views/auth/index.vue') },
  { path: '/chat', component: () => import('@/views/chat/index.vue') },
  { path: '/chat/:sessionId', component: () => import('@/views/chat/index.vue') },
  { path: '/cases', component: () => import('@/views/cases/index.vue') },
  { path: '/knowledge', component: () => import('@/views/knowledge/index.vue') },
  { path: '/audit', component: () => import('@/views/audit/index.vue') },
  { path: '/profile', component: () => import('@/views/user/profile/index.vue') },
  { path: '/users', component: () => import('@/views/user/management/index.vue') },
  { path: '/config', component: () => import('@/views/config/index.vue') },
  { path: '/config/dict', component: () => import('@/views/dict/index.vue') },
  // 兼容旧路径
  { path: '/knowledge/audit', redirect: '/audit' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

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

// 登录守卫：未登录访问业务页面时重定向到登录页
const PUBLIC_PATHS = new Set(['/', '/login'])
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!PUBLIC_PATHS.has(to.path) && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router

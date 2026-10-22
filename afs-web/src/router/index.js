import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
  { path: '/login', component: () => import('@/views/auth/index.vue'), meta: { blank: true, title: '登录' } },
  { path: '/chat', component: () => import('@/views/chat/index.vue'), meta: { title: '智能问答' } },
  { path: '/chat/:sessionId', component: () => import('@/views/chat/index.vue'), meta: { title: '智能问答' } },
  { path: '/cases', component: () => import('@/views/cases/index.vue'), meta: { title: '案例分析' } },
  { path: '/knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { title: '知识库' } },
  { path: '/audit', component: () => import('@/views/audit/index.vue'), meta: { title: '知识审核' } },
  { path: '/profile', component: () => import('@/views/user/profile/index.vue'), meta: { title: '个人中心' } },
  { path: '/users', component: () => import('@/views/user/management/index.vue'), meta: { title: '用户管理' } },
  { path: '/config', component: () => import('@/views/config/index.vue'), meta: { title: '模型配置' } },
  { path: '/config/dict', component: () => import('@/views/dict/index.vue'), meta: { title: '数据字典' } },
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

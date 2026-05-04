import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/home/index.vue'
import Login from '../views/auth/Login.vue'
import Chat from '../views/chat/Chat.vue'
import Cases from '../views/cases/Cases.vue'
import Knowledge from '../views/knowledge/Knowledge.vue'
import Profile from '../views/user/Profile.vue'
import UserManagement from '../views/user/UserManagement.vue'
import Config from '../views/config/index.vue'
import Audit from '../views/audit/index.vue'
import Dict from '../views/dict/index.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/chat', component: Chat },
  { path: '/chat/:sessionId', component: Chat },
  { path: '/cases', component: Cases },
  { path: '/knowledge', component: Knowledge },
  { path: '/knowledge/audit', component: Audit },
  { path: '/profile', component: Profile },
  { path: '/users', component: UserManagement },
  { path: '/config', component: Config },
  { path: '/config/dict', component: Dict },
  { path: '/audit', component: Audit }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

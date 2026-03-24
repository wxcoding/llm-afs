import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Chat from '../views/Chat.vue'
import Cases from '../views/Cases.vue'
import Knowledge from '../views/Knowledge.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/chat', component: Chat },
  { path: '/chat/:sessionId', component: Chat },
  { path: '/cases', component: Cases },
  { path: '/knowledge', component: Knowledge }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

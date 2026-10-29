import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/brand.css'
import router from './router'
import App from './App.vue'
import dict from './utils/dict'
import { useUserStore } from './stores/user'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(ElementPlus)
app.use(router)

const userStore = useUserStore(pinia)

// 已登录后才初始化字典：登录页不发起任何后端请求，避免未登录时请求受保护接口报错
router.afterEach(() => {
  if (window.dictInitialized || !userStore.token) return
  window.dictInitialized = true
  dict.initCommonDicts().catch(e => console.error('字典初始化失败', e))
})

app.mount('#app')

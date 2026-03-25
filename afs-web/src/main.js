import { createApp, ref } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'

const app = createApp(App)
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
app.provide('user', user)
app.use(ElementPlus)
app.use(router)
app.mount('#app')

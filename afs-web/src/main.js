import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import dict from './utils/dict'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)

// 初始化字典
app.mixin({
  mounted() {
    if (!window.dictInitialized) {
      window.dictInitialized = true
      dict.initCommonDicts().catch(e => console.error('字典初始化失败', e))
    }
  }
})

app.mount('#app')

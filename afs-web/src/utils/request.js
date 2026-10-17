import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    // 后端统一返回 Result 结构，success === false 时集中提示
    if (res && res.success === false) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  error => {
    // 登录过期/未登录：清除本地状态并跳转登录页
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.clearUser()
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }
    const message = error.response?.data?.message || error.message || '请求失败'
    console.error('Response error:', error)
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service

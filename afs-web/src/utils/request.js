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
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      const serverMessage = error.response?.data?.message
      // 登录/注册接口返回 401 属于凭证校验失败，提示后端返回的具体原因
      if (url.includes('/user/login') || url.includes('/user/register')) {
        ElMessage.error(serverMessage || '用户名或密码错误')
        return Promise.reject(error)
      }
      // 其他接口 401：登录过期/未登录，清除本地状态并跳转登录页
      const userStore = useUserStore()
      userStore.clearUser()
      ElMessage.warning(serverMessage || '登录已过期，请重新登录')
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

<template>
  <div class="login-page">
    <div class="login-box">
      <h2>{{ isRegister ? '用户注册' : '用户登录' }}</h2>
      <el-form :model="form" @submit.prevent="handleSubmit">
        <el-form-item prop="username">
          <el-input 
            v-model="form.username" 
            :placeholder="isRegister ? '用户名（3-20位字母、数字、下划线）' : '用户名'"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            :placeholder="isRegister ? '密码（6-20位）' : '密码'"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            style="width: 100%" 
            @click="handleSubmit"
            :loading="loading"
          >
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="switch-mode">
        <span>{{ isRegister ? '已有账号？' : '没有账号？' }}</span>
        <el-link type="primary" @click="switchMode">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </el-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, inject } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const user = inject('user')
    const isRegister = ref(false)
    const loading = ref(false)
    const formRef = ref(null)
    const form = ref({
      username: '',
      password: ''
    })

    const usernameRules = [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字、下划线', trigger: 'blur' }
    ]

    const passwordRules = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
    ]

    const switchMode = () => {
      isRegister.value = !isRegister.value
      if (isRegister.value) {
        form.value.username = ''
        form.value.password = ''
        if (formRef.value) {
          formRef.value.clearValidate()
        }
      }
    }

    const handleSubmit = async () => {
      if (!form.value.username || !form.value.password) {
        ElMessage.warning('请填写用户名和密码')
        return
      }

      if (isRegister.value) {
        if (form.value.username.length < 3 || form.value.username.length > 20) {
          ElMessage.warning('用户名长度应为3-20位')
          return
        }
        if (!/^[a-zA-Z0-9_]+$/.test(form.value.username)) {
          ElMessage.warning('用户名只能包含字母、数字、下划线')
          return
        }
        if (form.value.password.length < 6 || form.value.password.length > 20) {
          ElMessage.warning('密码长度应为6-20位')
          return
        }
      }

      loading.value = true
      try {
        const url = isRegister.value ? '/api/user/register' : '/api/user/login'
        const res = await axios.post(url, form.value)
        
        if (res.data.success) {
          ElMessage.success(isRegister.value ? '注册成功' : '登录成功')
          localStorage.setItem('user', JSON.stringify(res.data.user))
          user.value = res.data.user
          router.push('/chat')
        } else {
          ElMessage.error(res.data.message)
        }
      } catch (e) {
        ElMessage.error('请求失败，请检查网络')
      } finally {
        loading.value = false
      }
    }

    return {
      isRegister,
      loading,
      form,
      formRef,
      switchMode,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  width: 400px;
}

.login-box h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.switch-mode {
  text-align: center;
  color: #666;
}
</style>

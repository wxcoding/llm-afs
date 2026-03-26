<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-pattern"></div>
      <div class="bg-gradient"></div>
    </div>
    <div class="login-container">
      <div class="login-left">
        <div class="brand-content">
          <div class="brand-icon">🛡️</div>
          <h1 class="brand-title">防诈骗知识问答系统</h1>
          <p class="brand-desc">智能识别诈骗手段 · 守护您的财产安全</p>
          <div class="feature-list">
            <div class="feature-item">
              <span class="feature-icon">🔍</span>
              <span>智能问答</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📚</span>
              <span>案例分析</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">💡</span>
              <span>知识库</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-box">
          <div class="login-header">
            <h2>{{ isRegister ? '新用户注册' : '欢迎回来' }}</h2>
            <p>{{ isRegister ? '创建账号，开始您的防诈之旅' : '登录您的账号' }}</p>
          </div>
          <el-form :model="form" @submit.prevent="handleSubmit">
            <el-form-item>
              <el-input 
                v-model="form.username" 
                :placeholder="isRegister ? '用户名（3-20位字母、数字、下划线）' : '请输入用户名'"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item>
              <el-input 
                v-model="form.password" 
                type="password" 
                :placeholder="isRegister ? '密码（6-20位）' : '请输入密码'"
                prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                class="login-btn"
                @click="handleSubmit"
                :loading="loading"
                size="large"
              >
                {{ isRegister ? '立即注册' : '立即登录' }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="switch-mode">
            <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
            <el-link type="primary" @click="switchMode" class="switch-link">
              {{ isRegister ? '立即登录' : '立即注册' }}
            </el-link>
          </div>
        </div>
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
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 80%, rgba(64, 158, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(103, 194, 58, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(230, 162, 60, 0.05) 0%, transparent 40%);
}

.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100vh;
}

.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.brand-content {
  text-align: center;
  color: white;
  max-width: 400px;
}

.brand-icon {
  font-size: 80px;
  margin-bottom: 24px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
  background: linear-gradient(90deg, #fff, #87ceeb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-desc {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 30px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateX(5px);
}

.feature-icon {
  font-size: 20px;
}

.login-right {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
}

.login-box {
  width: 100%;
  max-width: 380px;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #999;
}

.login-box :deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 8px;
}

.login-box :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.4);
}

.switch-mode {
  text-align: center;
  margin-top: 24px;
  color: #666;
  font-size: 14px;
}

.switch-link {
  font-weight: 500;
  margin-left: 4px;
}

.switch-link:hover {
  text-decoration: underline;
}
</style>

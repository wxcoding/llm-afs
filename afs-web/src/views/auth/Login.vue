<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
      <div class="bg-blob blob-4"></div>
    </div>
    <div class="login-container">
      <div class="login-left">
        <div class="brand-content">
          <div class="brand-icon-wrapper">
            <div class="brand-icon-ring"></div>
            <div class="brand-icon">🛡️</div>
          </div>
          <h1 class="brand-title">防诈骗智能问答系统</h1>
          <p class="brand-desc">基于大模型AI技术，智能识别诈骗手段，全方位守护您的财产安全</p>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon-box">
                <span class="feature-icon">🔍</span>
              </div>
              <div class="feature-text">
                <div class="feature-title">智能问答</div>
                <div class="feature-desc">7×24小时在线答疑</div>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon-box">
                <span class="feature-icon">📚</span>
              </div>
              <div class="feature-text">
                <div class="feature-title">案例分析</div>
                <div class="feature-desc">真实案例深度解析</div>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon-box">
                <span class="feature-icon">💡</span>
              </div>
              <div class="feature-text">
                <div class="feature-title">知识库</div>
                <div class="feature-desc">全面防诈知识储备</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-box">
          <div class="login-header">
            <div class="login-badge">
              <span class="badge-dot"></span>
              <span class="badge-text">{{ isRegister ? '注册新账号' : '安全登录' }}</span>
            </div>
            <h2>{{ isRegister ? '加入我们' : '欢迎回来' }}</h2>
            <p>{{ isRegister ? '创建您的专属账号，开启智能防诈之旅' : '登录您的账号，继续守护财产安全' }}</p>
          </div>
          <el-form :model="form" @submit.prevent="handleSubmit" class="login-form">
            <div class="form-item-wrapper">
              <div class="input-wrapper">
                <div class="input-prefix">👤</div>
                <el-input 
                  v-model="form.username" 
                  :placeholder="isRegister ? '用户名（3-20位字母、数字、下划线）' : '请输入用户名'"
                  size="large"
                  class="custom-input"
                />
              </div>
            </div>
            <div class="form-item-wrapper">
              <div class="input-wrapper">
                <div class="input-prefix">🔐</div>
                <el-input 
                  v-model="form.password" 
                  type="password" 
                  :placeholder="isRegister ? '密码（6-20位）' : '请输入密码'"
                  show-password
                  size="large"
                  class="custom-input"
                />
              </div>
            </div>
            <div class="form-item-wrapper">
              <el-button 
                type="primary" 
                class="login-btn"
                @click="handleSubmit"
                :loading="loading"
                size="large"
              >
                <span class="btn-text">{{ isRegister ? '立即注册' : '立即登录' }}</span>
                <span class="btn-arrow">→</span>
              </el-button>
            </div>
          </el-form>
          <div class="switch-mode">
            <div class="switch-divider">
              <span class="divider-text">或者</span>
            </div>
            <span class="switch-label">{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
            <el-button type="primary" text @click="switchMode" class="switch-btn">
              {{ isRegister ? '立即登录' : '立即注册' }}
            </el-button>
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
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 30%, #f0fdf4 60%, #fef3c7 85%, #fce7f3 100%);
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: blobMove 20s ease-in-out infinite;
}

.blob-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  top: -150px;
  left: -100px;
}

.blob-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #10b981, #34d399);
  top: 40%;
  right: -100px;
  animation-delay: -6s;
}

.blob-3 {
  width: 450px;
  height: 450px;
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  bottom: -120px;
  left: 30%;
  animation-delay: -12s;
}

.blob-4 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #ec4899, #f472b6);
  top: 20%;
  left: 50%;
  animation-delay: -8s;
}

@keyframes blobMove {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(60px, -40px) scale(1.1);
  }
  66% {
    transform: translate(-40px, 60px) scale(0.95);
  }
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
  padding: 60px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
}

.brand-content {
  text-align: center;
  color: white;
  max-width: 480px;
}

.brand-icon-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 32px;
}

.brand-icon-ring {
  position: absolute;
  width: 120px;
  height: 120px;
  border: 2px solid rgba(59, 130, 246, 0.3);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: ringPulse 2.5s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0;
  }
}

.brand-icon {
  font-size: 88px;
  position: relative;
  z-index: 1;
  animation: float 3.5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.brand-title {
  font-size: 40px;
  font-weight: 800;
  margin-bottom: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #a5b4fc 50%, #94a3b8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.brand-desc {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
  line-height: 1.8;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.04) 100%);
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.35s ease;
  text-align: left;
}

.feature-item:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.06) 100%);
  transform: translateX(8px);
  border-color: rgba(59, 130, 246, 0.4);
}

.feature-icon-box {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon {
  font-size: 24px;
}

.feature-text {
  flex: 1;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.brand-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding-top: 32px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.login-right {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(32px);
}

.login-box {
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 50px;
  margin-bottom: 20px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 50%;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.4);
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 0 8px rgba(79, 70, 229, 0);
  }
}

.badge-text {
  font-size: 13px;
  font-weight: 600;
  color: #4338ca;
  letter-spacing: 0.3px;
}

.login-header h2 {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 10px;
  letter-spacing: -0.5px;
}

.login-header p {
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
}

.login-form {
  margin-bottom: 8px;
}

.form-item-wrapper {
  margin-bottom: 20px;
}

.form-item-wrapper:last-child {
  margin-bottom: 0;
}

.input-wrapper {
  position: relative;
}

.input-prefix {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  z-index: 10;
}

.custom-input :deep(.el-input__wrapper) {
  padding: 12px 16px 12px 48px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  box-shadow: none;
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
  background: #ffffff;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #4f46e5;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
}

.login-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  transition: all 0.35s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.3);
  margin: 0;
  padding: 0;
}

.login-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.4);
}

.btn-arrow {
  transition: transform 0.3s ease;
}

.login-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.switch-mode {
  text-align: center;
  padding-top: 28px;
}

.switch-divider {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.switch-divider::before,
.switch-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider-text {
  padding: 0 20px;
  font-size: 13px;
  color: #94a3b8;
}

.switch-label {
  font-size: 14px;
  color: #64748b;
  margin-right: 6px;
}

.switch-btn {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 8px;
  color: #4f46e5;
}

.switch-btn:hover {
  color: #7c3aed;
  text-decoration: underline;
}
</style>

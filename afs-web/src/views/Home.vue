<template>
  <div class="home">
    <div class="hero-section">
      <div class="hero-background">
        <div class="gradient-overlay"></div>
        <div class="grid-pattern"></div>
        <div class="glow-orb orb-1"></div>
        <div class="glow-orb orb-2"></div>
        <div class="glow-orb orb-3"></div>
      </div>
      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-label">
            <span class="label-dot"></span>
            <span class="label-text">AI驱动的智能防护平台</span>
          </div>
          <h1 class="hero-title">
            让诈骗无所遁形
          </h1>
          <p class="hero-description">
            基于先进大语言模型技术，构建全方位智能防诈骗体系。
            实时识别、精准预警、专业指导，为您的财产安全保驾护航。
          </p>
          <div class="hero-stats">
            <div class="stat-item">
              <div class="stat-icon">🎯</div>
              <div class="stat-content">
                <div class="stat-value">98.5<span class="stat-unit">%</span></div>
                <div class="stat-label">识别准确率</div>
              </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-icon">📊</div>
              <div class="stat-content">
                <div class="stat-value">10K<span class="stat-unit">+</span></div>
                <div class="stat-label">案例数据库</div>
              </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-icon">⏰</div>
              <div class="stat-content">
                <div class="stat-value">24<span class="stat-unit">/7</span></div>
                <div class="stat-label">全天候守护</div>
              </div>
            </div>
          </div>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="$router.push('/chat')" class="btn-primary">
              <span>开始智能咨询</span>
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" @click="$router.push('/knowledge')" class="btn-outline">
              <span>查看知识库</span>
            </el-button>
          </div>
          <div class="hero-trust">
            <div class="trust-label">已为</div>
            <div class="trust-users">
              <div class="user-avatar">👨‍💼</div>
              <div class="user-avatar">👩‍💻</div>
              <div class="user-avatar">👨‍🎓</div>
              <div class="user-avatar">👩‍🏫</div>
              <div class="user-more">+999</div>
            </div>
            <div class="trust-text">用户提供服务</div>
          </div>
        </div>
        <div class="hero-right">
          <div class="demo-card">
            <div class="demo-header">
              <div class="demo-status">
                <span class="status-dot"></span>
                <span>系统在线</span>
              </div>
              <div class="demo-time">实时对话</div>
            </div>
            <div class="demo-content">
              <div class="message bot">
                <div class="message-avatar">🤖</div>
                <div class="message-body">
                  <div class="message-text">您好！我是您的智能防诈骗助手。请问有什么可以帮助您的吗？</div>
                </div>
              </div>
              <div class="message user">
                <div class="message-body">
                  <div class="message-text">我收到一条短信说我中奖了，需要点击链接领取</div>
                </div>
                <div class="message-avatar">👤</div>
              </div>
              <div class="message bot warning">
                <div class="message-avatar">🤖</div>
                <div class="message-body">
                  <div class="warning-badge">
                    <span class="warning-icon">⚠️</span>
                    <span>风险预警</span>
                  </div>
                  <div class="message-text">检测到高风险内容！这是典型的中奖诈骗手法。请勿点击任何链接或提供个人信息。</div>
                </div>
              </div>
            </div>
            <div class="demo-footer">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="features-section">
      <div class="section-header">
        <div class="section-badge">核心功能</div>
        <h2>全方位防护体系</h2>
        <p>多维度防诈骗服务，构建坚实的安全防线</p>
      </div>
      <div class="features-grid">
        <div class="feature-card" @click="$router.push('/chat')">
          <div class="feature-icon-wrapper">
            <div class="feature-icon">💬</div>
            <div class="feature-icon-bg"></div>
          </div>
          <h3>智能问答</h3>
          <p>AI大模型在线答疑，实时解答各类防诈骗相关问题，提供专业建议</p>
          <div class="feature-arrow">→</div>
          <div class="feature-hover-bg"></div>
        </div>
        <div class="feature-card" @click="$router.push('/cases')">
          <div class="feature-icon-wrapper">
            <div class="feature-icon">📚</div>
            <div class="feature-icon-bg"></div>
          </div>
          <h3>案例分析</h3>
          <p>真实诈骗案例分析，深入了解骗子常用套路和作案手法</p>
          <div class="feature-arrow">→</div>
          <div class="feature-hover-bg"></div>
        </div>
        <div class="feature-card" @click="$router.push('/knowledge')">
          <div class="feature-icon-wrapper">
            <div class="feature-icon">📖</div>
            <div class="feature-icon-bg"></div>
          </div>
          <h3>知识库</h3>
          <p>防诈骗知识大全，涵盖各类诈骗类型和防范技巧</p>
          <div class="feature-arrow">→</div>
          <div class="feature-hover-bg"></div>
        </div>
      </div>
    </div>

    <div class="tips-section">
      <div class="section-header">
        <div class="section-badge">安全提示</div>
        <h2>防诈小贴士</h2>
        <p>掌握这些技巧，远离诈骗陷阱</p>
      </div>
      <div class="tips-grid">
        <div class="tip-card">
          <div class="tip-number">01</div>
          <div class="tip-icon">🚫</div>
          <h3>不轻信</h3>
          <p>不轻信来历不明的电话和短信，不管骗子使用什么甜言蜜语，都不要轻信</p>
        </div>
        <div class="tip-card">
          <div class="tip-number">02</div>
          <div class="tip-icon">🔒</div>
          <h3>不透露</h3>
          <p>不向对方透露自己及家人的身份信息、银行卡号、验证码等重要信息</p>
        </div>
        <div class="tip-card">
          <div class="tip-number">03</div>
          <div class="tip-icon">💰</div>
          <h3>不转账</h3>
          <p>不向陌生人转账汇款，学会核实对方身份，涉及资金往来要多方确认</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'Home',
  components: {
    ArrowRight
  },
  setup() {
    const stats = ref({
      cases: 10000,
      knowledge: 5000,
      protection: 98.5
    })

    const formatNumber = (num) => {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num.toLocaleString()
    }

    const loadStats = async () => {
      try {
        const res = await axios.get('/api/stats')
        if (res.data) {
          stats.value = res.data
        }
      } catch (e) {
        console.log('使用默认统计数据')
      }
    }

    onMounted(() => {
      loadStats()
    })

    return {
      stats,
      formatNumber
    }
  }
}
</script>

<style scoped>
.home {
  padding: 0;
  background: #f5f7fa;
}

.hero-section {
  position: relative;
  min-height: 720px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

.gradient-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
}

.grid-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(50px);
  }
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
  animation: orbFloat 15s ease-in-out infinite;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #3b82f6 0%, transparent 70%);
  top: -200px;
  left: -150px;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #8b5cf6 0%, transparent 70%);
  bottom: -150px;
  right: -100px;
  animation-delay: -5s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #06b6d4 0%, transparent 70%);
  top: 40%;
  left: 50%;
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 100px 60px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 100px;
  align-items: center;
}

.hero-left {
  color: white;
}

.hero-label {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 50px;
  margin-bottom: 36px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.hero-label:hover {
  background: rgba(59, 130, 246, 0.18);
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateY(-2px);
}

.label-dot {
  width: 10px;
  height: 10px;
  background: #3b82f6;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.6);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.3);
  }
}

.label-text {
  font-size: 14px;
  font-weight: 600;
  color: #93c5fd;
  letter-spacing: 0.5px;
}

.hero-title {
  font-size: 68px;
  font-weight: 800;
  line-height: 1.08;
  margin: 0 0 32px 0;
  letter-spacing: -2px;
  background: linear-gradient(135deg, #ffffff 0%, #e2e8f0 50%, #94a3b8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: titleFadeIn 1s ease-out;
}

@keyframes titleFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-description {
  font-size: 20px;
  line-height: 1.8;
  color: #cbd5e1;
  margin: 0 0 48px 0;
  max-width: 580px;
  animation: descFadeIn 1s ease-out 0.2s both;
}

@keyframes descFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-bottom: 48px;
  padding: 28px 32px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  animation: statsFadeIn 1s ease-out 0.4s both;
}

@keyframes statsFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
  filter: drop-shadow(0 4px 8px rgba(59, 130, 246, 0.3));
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: #3b82f6;
  line-height: 1;
  margin-bottom: 6px;
  text-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

.stat-unit {
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 56px;
  background: linear-gradient(180deg, transparent 0%, rgba(255, 255, 255, 0.15) 50%, transparent 100%);
}

.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
  animation: actionsFadeIn 1s ease-out 0.6s both;
}

@keyframes actionsFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  padding: 20px 44px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
}

.btn-primary:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.45);
}

.btn-primary .btn-icon {
  transition: transform 0.3s ease;
}

.btn-primary:hover .btn-icon {
  transform: translateX(6px);
}

.btn-outline {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 20px 44px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.btn-outline:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-3px);
}

.hero-trust {
  display: flex;
  align-items: center;
  gap: 16px;
  animation: trustFadeIn 1s ease-out 0.8s both;
}

@keyframes trustFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.trust-label {
  font-size: 14px;
  color: #64748b;
}

.trust-users {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid #1e293b;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-left: -10px;
  transition: all 0.3s ease;
}

.user-avatar:first-child {
  margin-left: 0;
}

.user-avatar:hover {
  transform: translateY(-4px) scale(1.1);
  z-index: 10;
}

.user-more {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: 2px solid #1e293b;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: white;
  margin-left: -10px;
}

.trust-text {
  font-size: 14px;
  color: #64748b;
}

.hero-right {
  display: flex;
  justify-content: center;
  align-items: center;
  animation: cardFadeIn 1s ease-out 0.3s both;
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateX(40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.demo-card {
  width: 100%;
  max-width: 500px;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 
    0 32px 64px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  transition: all 0.4s ease;
}

.demo-card:hover {
  transform: translateY(-8px);
  box-shadow: 
    0 40px 80px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.15);
}

.demo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.demo-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #10b981;
}

.status-dot {
  width: 10px;
  height: 10px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
  box-shadow: 0 0 12px rgba(16, 185, 129, 0.6);
}

.demo-time {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  padding: 6px 12px;
  background: rgba(100, 116, 139, 0.1);
  border-radius: 6px;
}

.demo-content {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.message-body {
  flex: 1;
  max-width: 80%;
}

.message-text {
  padding: 16px 20px;
  background: #f1f5f9;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.message.user .message-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}

.message.bot.warning .message-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.warning-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #fcd34d;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #92400e;
  width: fit-content;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.25);
}

.warning-icon {
  font-size: 14px;
}

.message.bot.warning .message-text {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #fcd34d;
}

.demo-footer {
  padding: 16px 28px;
  background: #fafafa;
  border-top: 1px solid #e2e8f0;
}

.typing-indicator {
  display: flex;
  gap: 6px;
  align-items: center;
  padding: 8px 16px;
  background: #f1f5f9;
  border-radius: 20px;
  width: fit-content;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-8px);
    opacity: 1;
  }
}

.features-section {
  padding: 100px 60px;
  background: white;
}

.section-header {
  text-align: center;
  margin-bottom: 64px;
}

.section-badge {
  display: inline-block;
  padding: 8px 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
  border-radius: 50px;
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 20px;
  letter-spacing: 0.5px;
}

.section-header h2 {
  font-size: 42px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
}

.section-header p {
  font-size: 18px;
  color: #64748b;
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 36px;
  cursor: pointer;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

.feature-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  border-color: #3b82f6;
}

.feature-icon-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  margin-bottom: 28px;
}

.feature-icon {
  position: relative;
  z-index: 2;
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  transition: all 0.4s ease;
}

.feature-icon-bg {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 72px;
  height: 72px;
  background: #3b82f6;
  border-radius: 18px;
  opacity: 0;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  transform: translate(-4px, -4px);
}

.feature-card:hover .feature-icon-bg {
  opacity: 0.15;
  transform: translate(4px, 4px);
}

.feature-card h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 14px 0;
}

.feature-card p {
  font-size: 15px;
  color: #64748b;
  line-height: 1.7;
  margin: 0;
}

.feature-arrow {
  position: absolute;
  top: 36px;
  right: 36px;
  font-size: 24px;
  color: #cbd5e1;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-arrow {
  color: #3b82f6;
  transform: translateX(8px);
}

.feature-hover-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.03) 0%, transparent 100%);
  opacity: 0;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-hover-bg {
  opacity: 1;
}

.tips-section {
  padding: 100px 60px;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.tip-card {
  background: white;
  border-radius: 20px;
  padding: 36px;
  border: 1px solid #e2e8f0;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

.tip-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 100%);
  transform: scaleX(0);
  transition: transform 0.4s ease;
}

.tip-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.tip-card:hover::before {
  transform: scaleX(1);
}

.tip-number {
  font-size: 56px;
  font-weight: 900;
  color: #e2e8f0;
  line-height: 1;
  margin-bottom: 20px;
  transition: all 0.4s ease;
}

.tip-card:hover .tip-number {
  color: #cbd5e1;
  transform: scale(1.05);
}

.tip-icon {
  font-size: 40px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
}

.tip-card h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 14px 0;
}

.tip-card p {
  font-size: 15px;
  color: #64748b;
  line-height: 1.7;
  margin: 0;
}

@media (max-width: 1024px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 60px;
    padding: 60px 40px;
  }

  .hero-title {
    font-size: 48px;
  }

  .hero-stats {
    flex-direction: column;
    gap: 24px;
  }

  .stat-divider {
    width: 100%;
    height: 1px;
  }

  .features-grid,
  .tips-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>

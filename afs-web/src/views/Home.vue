<template>
  <div class="home">
    <div class="hero-background">
      <div class="gradient-overlay"></div>
      <div class="grid-pattern"></div>
      <div class="floating-orb orb-1"></div>
      <div class="floating-orb orb-2"></div>
      <div class="floating-orb orb-3"></div>
    </div>
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-header">
          <div class="hero-badge">
            <span class="badge-icon">🛡️</span>
            <span class="badge-text">智能防诈骗平台</span>
          </div>
          <h1 class="hero-title">智能防诈 · 安全守护</h1>
          <p class="hero-description">
            基于先进大语言模型技术，构建全方位智能防诈骗体系。
            实时识别、精准预警、专业指导，为您的财产安全保驾护航。
          </p>
        </div>
        
        <div class="hero-stats">
          <div class="stat-card">
            <div class="stat-icon">📖</div>
            <div class="stat-value">{{ stats.knowledgeCount || 0 }}<span class="stat-unit">篇</span></div>
            <div class="stat-label">知识库文章</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📚</div>
            <div class="stat-value">{{ stats.caseCount || 0 }}<span class="stat-unit">个</span></div>
            <div class="stat-label">诈骗案例</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">💬</div>
            <div class="stat-value">{{ stats.messageCount || 0 }}<span class="stat-unit">次</span></div>
            <div class="stat-label">智能问答</div>
          </div>
        </div>
        
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/chat')" class="primary-btn">
            <span>开始智能咨询</span>
            <span class="btn-arrow">→</span>
          </el-button>
          <el-button size="large" @click="$router.push('/knowledge')" class="secondary-btn">
            <span>查看知识库</span>
          </el-button>
        </div>
        
        <div class="hero-demo">
          <div class="demo-card">
            <div class="demo-header">
              <div class="demo-title">实时对话演示</div>
              <div class="demo-status">
                <span class="status-dot"></span>
                <span>系统在线</span>
              </div>
            </div>
            <div class="demo-content">
              <div class="message user">
                <div class="message-avatar">👤</div>
                <div class="message-text">我收到一条短信说我中奖了，需要点击链接领取</div>
              </div>
              <div class="message bot warning">
                <div class="message-avatar">🤖</div>
                <div class="message-text">
                  <span class="warning-tag">风险预警</span>
                  检测到高风险内容！这是典型的中奖诈骗手法。请勿点击任何链接或提供个人信息。
                </div>
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
      knowledgeCount: 0,
      caseCount: 0,
      sessionCount: 0,
      messageCount: 0,
      userCount: 0
    })

    const loadStats = async () => {
      try {
        const res = await axios.get('/api/stats')
        if (res.data && res.data.success) {
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
      loadStats
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.gradient-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1) 0%, rgba(124, 58, 237, 0.05) 100%);
}

.grid-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(79, 70, 229, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.05) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 60s linear infinite;
}

@keyframes gridMove {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(50px, 50px);
  }
}

.floating-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
  animation: orbFloat 25s ease-in-out infinite;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  top: -200px;
  left: -100px;
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #10b981, #34d399);
  bottom: -150px;
  right: -100px;
  animation-delay: -8s;
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  top: 40%;
  right: 20%;
  animation-delay: -16s;
}

@keyframes orbFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(80px, -60px) scale(1.1);
  }
  66% {
    transform: translate(-60px, 80px) scale(0.9);
  }
}

.hero-section {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.hero-content {
  max-width: 1200px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.hero-header {
  margin-bottom: 60px;
  max-width: 800px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 50px;
  margin-bottom: 28px;
  transition: all 0.3s ease;
}

.hero-badge:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.15);
}

.badge-icon {
  font-size: 20px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.badge-text {
  font-size: 14px;
  font-weight: 600;
  color: #4338ca;
  letter-spacing: 0.5px;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
  line-height: 1.2;
}

.hero-description {
  font-size: 20px;
  color: #64748b;
  margin-bottom: 48px;
  line-height: 1.8;
}

.hero-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 60px;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 32px 40px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  transition: all 0.4s ease;
  min-width: 200px;
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.15);
  border-color: #4f46e5;
}

.stat-icon {
  font-size: 40px;
  margin-bottom: 16px;
  filter: drop-shadow(0 4px 8px rgba(79, 70, 229, 0.2));
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.stat-unit {
  font-size: 18px;
  font-weight: 600;
  color: #64748b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.hero-actions {
  display: flex;
  gap: 20px;
  margin-bottom: 80px;
  flex-wrap: wrap;
  justify-content: center;
}

.primary-btn {
  padding: 16px 40px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 16px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  transition: all 0.35s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.3);
  color: white;
}

.primary-btn:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(79, 70, 229, 0.4);
}

.btn-arrow {
  transition: transform 0.3s ease;
}

.primary-btn:hover .btn-arrow {
  transform: translateX(6px);
}

.secondary-btn {
  padding: 16px 40px;
  font-size: 18px;
  font-weight: 600;
  color: #4f46e5;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid #4f46e5;
  border-radius: 16px;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
}

.secondary-btn:hover {
  color: #7c3aed;
  border-color: #7c3aed;
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.15);
}

.hero-demo {
  width: 100%;
  max-width: 600px;
}

.demo-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 
    0 32px 64px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.2);
  transition: all 0.4s ease;
  padding: 40px;
  backdrop-filter: blur(20px);
}

.demo-card:hover {
  transform: translateY(-12px);
  box-shadow: 
    0 40px 80px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.3);
}

.demo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.demo-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.demo-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border: 1px solid #bbf7d0;
  border-radius: 20px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
}

.demo-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  max-width: 100%;
}

.message.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-avatar {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.message-text {
  flex: 1;
  max-width: 85%;
  padding: 20px 24px;
  background: #f1f5f9;
  border-radius: 18px;
  font-size: 16px;
  line-height: 1.7;
  color: #334155;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  position: relative;
}

.message.user .message-text {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.3);
}

.message.bot.warning .message-text {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid #fcd34d;
  box-shadow: 0 6px 20px rgba(251, 191, 36, 0.25);
}

.warning-tag {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(251, 191, 36, 0.2);
  border: 1px solid #fbbf24;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #92400e;
  margin-right: 12px;
  margin-bottom: 8px;
}

.message-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.message.user .message-text {
  align-items: flex-end;
  text-align: right;
}

.features-section {
  padding: 100px 60px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.02) 0%, rgba(124, 58, 237, 0.01) 100%);
  position: relative;
  z-index: 1;
}

.section-header {
  text-align: center;
  margin-bottom: 64px;
}

.section-badge {
  display: inline-block;
  padding: 10px 24px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 50px;
  font-size: 14px;
  font-weight: 600;
  color: #4338ca;
  margin-bottom: 24px;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.section-badge:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.15);
}

.section-header h2 {
  font-size: 42px;
  font-weight: 800;
  background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
}

.section-header p {
  font-size: 18px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.feature-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 36px;
  cursor: pointer;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.feature-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.15);
  border-color: #4f46e5;
}

.feature-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 28px;
}

.feature-icon {
  position: relative;
  z-index: 2;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  transition: all 0.4s ease;
  filter: drop-shadow(0 4px 8px rgba(79, 70, 229, 0.2));
}

.feature-icon-bg {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border-radius: 20px;
  opacity: 0;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-icon {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  transform: translate(-4px, -4px);
  color: white;
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
  color: #4f46e5;
  transform: translateX(8px);
}

.feature-hover-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.03) 0%, transparent 100%);
  opacity: 0;
  transition: all 0.4s ease;
}

.feature-card:hover .feature-hover-bg {
  opacity: 1;
}

.tips-section {
  padding: 100px 60px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.01) 0%, rgba(124, 58, 237, 0.005) 100%);
  position: relative;
  z-index: 1;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.tip-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 36px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.tip-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #4f46e5 0%, #7c3aed 100%);
  transform: scaleX(0);
  transition: transform 0.4s ease;
}

.tip-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.15);
  border-color: #4f46e5;
}

.tip-card:hover::before {
  transform: scaleX(1);
}

.tip-number {
  font-size: 56px;
  font-weight: 900;
  color: rgba(79, 70, 229, 0.35);
  line-height: 1;
  margin-bottom: 20px;
  transition: all 0.4s ease;
}

.tip-card:hover .tip-number {
  color: rgba(79, 70, 229, 0.5);
  transform: scale(1.05);
}

.tip-icon {
  font-size: 40px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 8px rgba(79, 70, 229, 0.2));
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
  .hero-section {
    flex-direction: column;
  }

  .hero-left,
  .hero-right {
    width: 100%;
  }

  .hero-right {
    padding: 40px;
  }

  .brand-title {
    font-size: 32px;
  }

  .features-grid,
  .tips-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>

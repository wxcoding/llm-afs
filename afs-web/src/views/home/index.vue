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
          <p class="hero-description">基于先进大语言模型技术，构建全方位智能防诈骗体系。实时识别、精准预警、专业指导，为您的财产安全保驾护航。</p>
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
      </div>
    </div>

    <div class="quick-access-section">
      <div class="section-header">
        <div class="section-badge">快捷入口</div>
        <h2>常见问题咨询</h2>
        <p>点击这些问题，快速获取专业解答</p>
      </div>
      <div class="quick-access-grid">
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">📱</div>
          <div class="quick-access-text">什么是电信诈骗？</div>
        </div>
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">🔍</div>
          <div class="quick-access-text">如何识别网络诈骗？</div>
        </div>
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">⚠️</div>
          <div class="quick-access-text">遇到诈骗怎么办？</div>
        </div>
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">🎯</div>
          <div class="quick-access-text">有哪些常见诈骗手法？</div>
        </div>
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">💳</div>
          <div class="quick-access-text">刷单返利是真的吗？</div>
        </div>
        <div class="quick-access-card" @click="$router.push('/chat')">
          <div class="quick-access-icon">🛡️</div>
          <div class="quick-access-text">如何保护个人信息？</div>
        </div>
      </div>
    </div>

    <div class="cases-preview-section">
      <div class="section-header">
        <div class="section-badge">案例警示</div>
        <h2>最新诈骗案例</h2>
        <p>了解真实案例，提高防骗意识</p>
      </div>
      <div class="cases-preview-grid" v-if="recentCases.length > 0">
        <div class="case-preview-card" v-for="c in recentCases" :key="c.id" @click="$router.push('/cases')">
          <div class="case-preview-header">
            <el-tag :type="getCaseTypeTag(c.type)" size="small">{{ c.type }}</el-tag>
            <span class="case-preview-date">{{ formatDate(c.createTime) }}</span>
          </div>
          <div class="case-preview-title">{{ c.title }}</div>
          <div class="case-preview-desc">{{ getCaseDesc(c.content) }}</div>
        </div>
      </div>
      <div class="cases-preview-empty" v-else>
        <el-empty description="暂无案例数据" :image-size="80"></el-empty>
      </div>
      <div class="cases-preview-more">
        <el-button type="primary" plain @click="$router.push('/cases')">查看更多案例 →</el-button>
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
    const recentCases = ref([])

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

    const loadRecentCases = async () => {
      try {
        const res = await axios.get('/api/cases', { params: { page: 1, pageSize: 3 } })
        if (res.data && res.data.success) {
          recentCases.value = res.data.list || []
        }
      } catch (e) {
        console.log('暂无案例数据')
      }
    }

    const getCaseTypeTag = (type) => {
      const map = {
        '冒充客服': 'danger',
        '刷单诈骗': 'warning',
        '杀猪盘': 'danger',
        '冒充公检法': 'danger',
        '网络贷款': 'warning',
        '虚假购物': 'info'
      }
      return map[type] || 'info'
    }

    const formatDate = (date) => {
      if (!date) return ''
      return new Date(date).toLocaleDateString('zh-CN')
    }

    const getCaseDesc = (content) => {
      if (!content) return ''
      return content.length > 80 ? content.substring(0, 80) + '...' : content
    }

    onMounted(() => {
      loadStats()
      loadRecentCases()
    })

    return {
      stats,
      recentCases,
      loadStats,
      loadRecentCases,
      getCaseTypeTag,
      formatDate,
      getCaseDesc
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
  min-height: auto;
  padding: 40px 60px 60px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  margin-bottom: 30px;
  max-width: 800px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 50px;
  margin-bottom: 16px;
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
  font-size: 42px;
  font-weight: 800;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
  line-height: 1.2;
}

.hero-description {
  font-size: 16px;
  color: #64748b;
  margin-bottom: 30px;
  line-height: 1.6;
}

.hero-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 30px;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 20px 28px;
  text-align: center;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.4s ease;
  min-width: 140px;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.12);
  border-color: #4f46e5;
}

.stat-icon {
  font-size: 28px;
  margin-bottom: 10px;
  filter: drop-shadow(0 4px 8px rgba(79, 70, 229, 0.2));
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 4px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
  flex-wrap: wrap;
  justify-content: center;
}

.primary-btn {
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  transition: all 0.35s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3);
  color: white;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.4);
}

.btn-arrow {
  transition: transform 0.3s ease;
}

.primary-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.secondary-btn {
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
  color: #4f46e5;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid #4f46e5;
  border-radius: 12px;
  transition: all 0.3s ease;
  backdrop-filter: blur(20px);
}

.secondary-btn:hover {
  color: #7c3aed;
  border-color: #7c3aed;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.15);
}

.section-header {
  text-align: center;
  margin-bottom: 32px;
}

.section-badge {
  display: inline-block;
  padding: 6px 16px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 50px;
  font-size: 12px;
  font-weight: 600;
  color: #4338ca;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.section-badge:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.15);
}

.section-header h2 {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 10px 0;
  letter-spacing: -1px;
}

.section-header p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.quick-access-section {
  padding: 50px 60px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.02) 0%, rgba(103, 194, 58, 0.01) 100%);
  position: relative;
  z-index: 1;
}

.quick-access-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.quick-access-card {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 12px;
}

.quick-access-card:hover {
  border-color: #409eff;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.quick-access-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f0ff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-access-text {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
}

.cases-preview-section {
  padding: 50px 60px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.02) 0%, rgba(230, 162, 60, 0.01) 100%);
  position: relative;
  z-index: 1;
}

.cases-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto 20px;
}

.case-preview-card {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.case-preview-card:hover {
  border-color: #e6a23c;
  box-shadow: 0 6px 16px rgba(230, 162, 60, 0.12);
  transform: translateY(-2px);
}

.case-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.case-preview-date {
  font-size: 11px;
  color: #909399;
}

.case-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-preview-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.cases-preview-empty {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
}

.cases-preview-more {
  text-align: center;
}

@media (max-width: 1024px) {
  .quick-access-grid,
  .cases-preview-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>

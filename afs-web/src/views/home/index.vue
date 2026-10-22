<template>
  <div class="home">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2 class="welcome-title">欢迎使用智能反诈系统</h2>
          <p class="welcome-desc">基于大模型 AI 技术，智能识别诈骗手段，全方位守护您的安全</p>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" @click="$router.push('/chat')">
            <el-icon style="margin-right: 4px"><ChatDotRound /></el-icon>
            开始智能咨询
          </el-button>
          <el-button @click="$router.push('/knowledge')">
            <el-icon style="margin-right: 4px"><Reading /></el-icon>
            查看知识库
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 数据统计 -->
    <div class="stats-row">
      <el-card class="stat-card" shadow="never" @click="$router.push('/knowledge')">
        <div class="stat-inner">
          <div class="stat-icon stat-icon-blue">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.knowledgeCount || 0 }}</div>
            <div class="stat-label">知识库文章</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="never" @click="$router.push('/cases')">
        <div class="stat-inner">
          <div class="stat-icon stat-icon-orange">
            <el-icon :size="24"><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.caseCount || 0 }}</div>
            <div class="stat-label">诈骗案例</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="never" @click="$router.push('/chat')">
        <div class="stat-inner">
          <div class="stat-icon stat-icon-green">
            <el-icon :size="24"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.messageCount || 0 }}</div>
            <div class="stat-label">智能问答</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 快捷入口 -->
    <el-card class="quick-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">常见问题咨询</span>
          <span class="card-subtitle">点击快速获取专业解答</span>
        </div>
      </template>
      <div class="quick-grid">
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><Iphone /></el-icon>
          <span class="quick-text">什么是电信诈骗？</span>
        </div>
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><Search /></el-icon>
          <span class="quick-text">如何识别网络诈骗？</span>
        </div>
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><Warning /></el-icon>
          <span class="quick-text">遇到诈骗怎么办？</span>
        </div>
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><Aim /></el-icon>
          <span class="quick-text">有哪些常见诈骗手法？</span>
        </div>
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><CreditCard /></el-icon>
          <span class="quick-text">银行卡被盗刷怎么办？</span>
        </div>
        <div class="quick-item" @click="$router.push('/chat')">
          <el-icon class="quick-icon"><Lock /></el-icon>
          <span class="quick-text">如何保护个人信息？</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  ChatDotRound,
  Reading,
  FolderOpened,
  Iphone,
  Search,
  Warning,
  Aim,
  CreditCard,
  Lock
} from '@element-plus/icons-vue'
import { getStats } from '@/api/stats'

const stats = ref({
  knowledgeCount: 0,
  caseCount: 0,
  sessionCount: 0,
  messageCount: 0,
  userCount: 0
})

const loadStats = async () => {
  try {
    const res = await getStats()
    if (res.success && res.data) {
      stats.value = res.data
    }
  } catch (e) {
    console.log('使用默认统计数据')
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 欢迎卡片 */
.welcome-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.welcome-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
}

.welcome-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* 数据统计行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-blue {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon-orange {
  background: #fdf6ec;
  color: #e6a23c;
}

.stat-icon-green {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 快捷入口 */
.quick-card {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.card-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 12px;
  color: #909399;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}

.quick-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.quick-icon {
  font-size: 16px;
  color: #409eff;
  flex-shrink: 0;
}

.quick-text {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-item:hover .quick-text {
  color: #409eff;
}

/* 响应式 */
@media (max-width: 992px) {
  .stats-row,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

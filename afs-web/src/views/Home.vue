<template>
  <div class="home">
    <div class="hero">
      <h1>🛡️ 防诈骗知识问答系统</h1>
      <p>基于AI大模型的智能防诈骗助手，帮助您了解常见诈骗手法，提高防骗意识</p>
    </div>

    <div class="features">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="feature-card" @click="$router.push('/chat')">
            <div class="feature-icon">💬</div>
            <h3>智能问答</h3>
            <p>AI大模型在线答疑，解答各类防诈骗相关问题</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card" @click="$router.push('/cases')">
            <div class="feature-icon">📚</div>
            <h3>案例分析</h3>
            <p>真实诈骗案例分析，了解骗子常用套路</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card" @click="$router.push('/knowledge')">
            <div class="feature-icon">📖</div>
            <h3>知识库</h3>
            <p>防诈骗知识大全，防范技巧一网打尽</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="tips-section">
      <h2>⚠️ 牢记防诈骗要点</h2>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="tip-card">
            <h4>十个凡是</h4>
            <p>凡是自称公检法要求汇款的、凡是让你汇款到"安全账户"的...都是诈骗！</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="tip-card">
            <h4>六个一律</h4>
            <p>接电话谈到银行卡、中奖、公检法的...一律挂掉！</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="tip-card">
            <h4>紧急处理</h4>
            <p>遇到诈骗立即报警，保留证据，挽回损失！</p>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home'
}
</script>

<style scoped>
.home {
  padding: 20px 0;
}

.hero {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
  margin-bottom: 40px;
}

.hero h1 {
  font-size: 42px;
  margin-bottom: 16px;
}

.hero p {
  font-size: 18px;
  opacity: 0.9;
}

.features {
  margin-bottom: 40px;
}

.feature-card {
  background: white;
  padding: 30px;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #333;
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

.tips-section {
  background: white;
  padding: 40px;
  border-radius: 12px;
}

.tips-section h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.tip-card {
  padding: 24px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.tip-card h4 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #409eff;
}

.tip-card p {
  color: #666;
  line-height: 1.6;
}
</style>

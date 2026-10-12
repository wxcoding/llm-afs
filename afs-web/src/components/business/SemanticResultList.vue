<template>
  <div class="semantic-results">
    <h3>语义搜索结果</h3>
    <el-card v-for="(r, idx) in results" :key="idx" class="semantic-card" shadow="hover">
      <div class="semantic-header">
        <el-tag :type="r.source === 'knowledge' ? 'success' : 'warning'" size="small">
          {{ r.source === 'knowledge' ? '知识库' : '案例库' }}
        </el-tag>
        <span class="semantic-title">{{ r.title || '无标题' }}</span>
        <el-tag v-if="r.category" size="small" type="info">{{ r.category }}</el-tag>
        <span class="semantic-score">相似度: {{ (r.score * 100).toFixed(1) }}%</span>
      </div>
      <div class="semantic-content">{{ r.content }}</div>
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  results: { type: Array, default: () => [] }
})
</script>

<style scoped>
.semantic-results h3 {
  margin-bottom: 16px;
  color: #333;
}

.semantic-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.semantic-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.semantic-title {
  font-weight: 600;
  color: #333;
}

.semantic-score {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.semantic-content {
  color: #666;
  line-height: 1.6;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
}
</style>

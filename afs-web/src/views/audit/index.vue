<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>知识审核</h2>
    </div>

    <el-empty v-if="pendingAudits.length === 0" description="暂无待审核内容" />

    <div v-else class="audit-list">
      <el-card v-for="item in pendingAudits" :key="item.id" class="audit-card">
        <template #header>
          <div class="audit-header">
            <span class="audit-title">{{ item.title }}</span>
            <el-tag type="warning">待审核</el-tag>
          </div>
        </template>
        <div class="audit-content">
          <p><strong>分类：</strong>{{ item.category }}</p>
          <p><strong>提交时间：</strong>{{ formatTime(item.submitTime) }}</p>
          <el-divider />
          <p class="content-text">{{ item.content }}</p>
        </div>
        <div class="audit-actions">
          <el-input v-model="item.auditComment" type="textarea" :rows="2" placeholder="审核意见（可选）" />
          <div class="action-buttons">
            <el-button type="success" @click="handleApprove(item)">通过</el-button>
            <el-button type="danger" @click="handleReject(item)">拒绝</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingAudits, approveAudit, rejectAudit } from '@/api/knowledgeEnhanced'

const pendingAudits = ref([])
const auditUserId = ref(parseInt(localStorage.getItem('userId') || '1'))

const loadAudits = async () => {
  try {
    const res = await getPendingAudits()
    pendingAudits.value = (res.data || []).map(item => ({ ...item, auditComment: '' }))
  } catch (error) {
    console.error('加载审核列表失败:', error)
  }
}

const handleApprove = async (item) => {
  try {
    await approveAudit(item.id, auditUserId.value, item.auditComment)
    ElMessage.success('审核通过')
    loadAudits()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleReject = async (item) => {
  try {
    await rejectAudit(item.id, auditUserId.value, item.auditComment)
    ElMessage.success('已拒绝')
    loadAudits()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadAudits()
})
</script>

<style scoped>
.audit-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.audit-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audit-card {
  margin-bottom: 0;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.audit-title {
  font-weight: bold;
  color: #303133;
}

.audit-content p {
  margin: 8px 0;
  color: #606266;
}

.content-text {
  color: #303133;
  line-height: 1.6;
}

.audit-actions {
  margin-top: 16px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>

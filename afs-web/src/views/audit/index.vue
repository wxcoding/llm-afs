<template>
  <div class="audit-page">
    <el-card v-if="pendingAudits.length === 0" shadow="never">
      <el-empty description="暂无待审核内容" />
    </el-card>

    <div v-else class="audit-list">
      <el-card v-for="item in pendingAudits" :key="item.id" shadow="never" class="audit-card">
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
import { getPendingAudits, approveAudit, rejectAudit } from '@/api/knowledge'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const pendingAudits = ref([])

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
    await approveAudit(item.id, userStore.userId, item.auditComment)
    ElMessage.success('审核通过')
    loadAudits()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleReject = async (item) => {
  try {
    await rejectAudit(item.id, userStore.userId, item.auditComment)
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
  padding: 0;
}

.audit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.audit-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.audit-content p {
  margin: 6px 0;
  color: #606266;
  font-size: 13px;
}

.content-text {
  color: #303133;
  line-height: 1.6;
}

.audit-actions {
  margin-top: 12px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
</style>

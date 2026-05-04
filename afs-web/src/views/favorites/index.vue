<template>
  <div class="favorites-page">
    <div class="page-header">
      <h2>我的收藏</h2>
      <el-button @click="handleClearHistory" type="danger" plain>清空历史</el-button>
    </div>

    <el-empty v-if="favorites.length === 0" description="暂无收藏内容">
      <el-button type="primary" @click="$router.push('/chat')">开始对话</el-button>
    </el-empty>

    <div v-else class="favorites-list">
      <el-card v-for="item in favorites" :key="item.id" class="favorite-card" shadow="hover">
        <div class="favorite-header">
          <h4>{{ item.title }}</h4>
          <el-button type="danger" size="small" @click="handleDelete(item.id)">删除</el-button>
        </div>
        <div class="favorite-meta">
          <span>收藏时间：{{ formatTime(item.createTime) }}</span>
        </div>
        <div class="favorite-actions">
          <el-button type="primary" size="small" @click="handleUse(item)">使用</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserFavorites, removeFavorite } from '@/api/conversation'

const favorites = ref([])
const userId = ref(parseInt(localStorage.getItem('userId') || '1'))

const loadFavorites = async () => {
  try {
    const res = await getUserFavorites(userId.value)
    favorites.value = res.data || []
  } catch (error) {
    console.error('加载收藏失败:', error)
  }
}

const handleDelete = async (id) => {
  try {
    await removeFavorite(id)
    ElMessage.success('删除成功')
    loadFavorites()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleClearHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有收藏吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
}

const handleUse = (item) => {
  ElMessage.info('已复制到输入框')
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.favorites-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.favorite-card {
  margin-bottom: 0;
}

.favorite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.favorite-header h4 {
  margin: 0;
  color: #303133;
}

.favorite-meta {
  color: #909399;
  font-size: 12px;
  margin-bottom: 12px;
}

.favorite-actions {
  display: flex;
  gap: 8px;
}
</style>

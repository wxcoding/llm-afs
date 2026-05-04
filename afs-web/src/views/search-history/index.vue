<template>
  <div class="search-history-page">
    <div class="page-header">
      <h2>搜索历史</h2>
      <el-button @click="handleClearHistory" type="danger" plain>清空历史</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="我的搜索" name="history">
        <el-empty v-if="searchHistory.length === 0" description="暂无搜索历史" />
        <div v-else class="history-list">
          <div v-for="item in searchHistory" :key="item.id" class="history-item" @click="handleSearch(item.keyword)">
            <span class="keyword">{{ item.keyword }}</span>
            <span class="meta">
              {{ item.resultCount }} 条结果 · {{ formatTime(item.createTime) }}
            </span>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="热门搜索" name="hot">
        <div class="hot-list">
          <div v-for="(item, index) in hotSearch" :key="item.id" class="hot-item">
            <span :class="['rank', { top: index < 3 }]">{{ index + 1 }}</span>
            <span class="keyword">{{ item.keyword }}</span>
            <span class="count">{{ item.searchCount }} 次搜索</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSearchHistory, deleteSearchHistory, getHotSearch } from '@/api/search'

const activeTab = ref('history')
const searchHistory = ref([])
const hotSearch = ref([])
const userId = ref(parseInt(localStorage.getItem('userId') || '1'))

const loadData = async () => {
  try {
    const [historyRes, hotRes] = await Promise.all([
      getSearchHistory({ userId: userId.value, limit: 50 }),
      getHotSearch(20)
    ])
    searchHistory.value = historyRes.data || []
    hotSearch.value = hotRes.data || []
  } catch (error) {
    console.error('加载搜索历史失败:', error)
  }
}

const handleClearHistory = async () => {
  try {
    await deleteSearchHistory(userId.value)
    ElMessage.success('已清空')
    loadData()
  } catch (error) {
    ElMessage.error('清空失败')
  }
}

const handleSearch = (keyword) => {
  ElMessage.info(`搜索: ${keyword}`)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.search-history-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-list,
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.history-item:hover {
  background: #ecf5ff;
}

.history-item .keyword {
  display: block;
  color: #303133;
  font-weight: 500;
}

.history-item .meta {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.hot-item .rank {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  font-size: 12px;
  margin-right: 12px;
}

.hot-item .rank.top {
  background: #f56c6c;
}

.hot-item .keyword {
  flex: 1;
  color: #303133;
}

.hot-item .count {
  color: #909399;
  font-size: 12px;
}
</style>

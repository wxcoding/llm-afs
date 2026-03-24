<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2>📖 防诈骗知识库</h2>
      <p>全面了解各类诈骗手法和防范技巧</p>
    </div>

    <div class="filter-bar">
      <el-select v-model="selectedCategory" placeholder="选择分类" @change="loadKnowledge">
        <el-option label="全部分类" value="" />
        <el-option label="防范技巧" value="防范技巧" />
        <el-option label="诈骗类型" value="诈骗类型" />
        <el-option label="应对方法" value="应对方法" />
      </el-select>
    </div>

    <div class="knowledge-list">
      <el-card 
        v-for="k in knowledgeList" 
        :key="k.id" 
        class="knowledge-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <el-tag type="success">{{ k.category }}</el-tag>
            <h3>{{ k.title }}</h3>
          </div>
        </template>
        <div class="knowledge-content">
          <pre>{{ k.content }}</pre>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

export default {
  name: 'Knowledge',
  setup() {
    const knowledgeList = ref([])
    const selectedCategory = ref('')

    const loadKnowledge = async () => {
      const url = selectedCategory.value 
        ? `/api/knowledge?category=${selectedCategory.value}`
        : '/api/knowledge'
      const res = await axios.get(url)
      if (res.data.success) {
        knowledgeList.value = res.data.list
      }
    }

    onMounted(() => {
      loadKnowledge()
    })

    return {
      knowledgeList,
      selectedCategory,
      loadKnowledge
    }
  }
}
</script>

<style scoped>
.knowledge-page {
  padding: 20px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
}

.filter-bar {
  margin-bottom: 24px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.knowledge-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.knowledge-content {
  max-height: 300px;
  overflow-y: auto;
}

.knowledge-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
  line-height: 1.8;
  color: #666;
}
</style>

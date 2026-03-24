<template>
  <div class="cases-page">
    <div class="page-header">
      <h2>📚 诈骗案例分析</h2>
      <p>了解真实诈骗案例，提高警惕</p>
    </div>

    <div class="filter-bar">
      <el-select v-model="selectedType" placeholder="选择类型" @change="loadCases">
        <el-option label="全部类型" value="" />
        <el-option label="电信诈骗" value="电信诈骗" />
        <el-option label="网络诈骗" value="网络诈骗" />
        <el-option label="情感诈骗" value="情感诈骗" />
        <el-option label="投资诈骗" value="投资诈骗" />
      </el-select>
    </div>

    <div class="cases-list">
      <el-row :gutter="20">
        <el-col :span="12" v-for="c in cases" :key="c.id">
          <div class="case-card" @click="showDetail(c)">
            <div class="case-header">
              <span class="case-type">{{ c.type }}</span>
              <h3>{{ c.title }}</h3>
            </div>
            <p class="case-content">{{ c.content }}</p>
            <div class="case-footer">
              <el-button type="primary" size="small">查看详情</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="dialogVisible" :title="currentCase?.title" width="600px">
      <div v-if="currentCase" class="case-detail">
        <el-tag type="danger" class="type-tag">{{ currentCase.type }}</el-tag>
        
        <div class="detail-section">
          <h4>📖 案例经过</h4>
          <p>{{ currentCase.content }}</p>
        </div>
        
        <div class="detail-section tips-section">
          <h4>💡 防范提示</h4>
          <p>{{ currentCase.tips }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

export default {
  name: 'Cases',
  setup() {
    const cases = ref([])
    const selectedType = ref('')
    const dialogVisible = ref(false)
    const currentCase = ref(null)

    const loadCases = async () => {
      const url = selectedType.value 
        ? `/api/cases?type=${selectedType.value}`
        : '/api/cases'
      const res = await axios.get(url)
      if (res.data.success) {
        cases.value = res.data.cases
      }
    }

    const showDetail = (c) => {
      currentCase.value = c
      dialogVisible.value = true
    }

    onMounted(() => {
      loadCases()
    })

    return {
      cases,
      selectedType,
      dialogVisible,
      currentCase,
      loadCases,
      showDetail
    }
  }
}
</script>

<style scoped>
.cases-page {
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

.cases-list {
  min-height: 400px;
}

.case-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.case-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}

.case-header {
  margin-bottom: 12px;
}

.case-type {
  display: inline-block;
  background: #f56c6c;
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  margin-bottom: 8px;
}

.case-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.case-content {
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-footer {
  margin-top: 16px;
  text-align: right;
}

.case-detail {
  padding: 10px;
}

.type-tag {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
}

.detail-section p {
  line-height: 1.8;
  color: #666;
}

.tips-section {
  background: #f0f9ff;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.tips-section h4 {
  color: #409eff;
}
</style>

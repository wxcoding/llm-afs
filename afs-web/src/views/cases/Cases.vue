<template>
  <div class="cases-page">
    <div class="page-header">
      <h2>📚 诈骗案例分析</h2>
      <p>了解真实诈骗案例，提高警惕</p>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
        <el-select v-model="selectedType" placeholder="选择类型" @change="loadCases" style="width: 150px">
          <el-option label="全部类型" value="" />
          <el-option label="电信诈骗" value="电信诈骗" />
          <el-option label="网络诈骗" value="网络诈骗" />
          <el-option label="情感诈骗" value="情感诈骗" />
          <el-option label="投资诈骗" value="投资诈骗" />
        </el-select>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="showAddDialog">+ 新增案例</el-button>
      </div>
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
              <el-button type="primary" size="small" @click.stop="showDetail(c)">查看详情</el-button>
              <el-button type="warning" size="small" @click.stop="showEditDialog(c)">编辑</el-button>
              <el-button type="danger" size="small" @click.stop="handleDelete(c.id)">删除</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="detailVisible" :title="currentCase?.title" width="600px">
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

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑案例' : '新增案例'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="电信诈骗" value="电信诈骗" />
            <el-option label="网络诈骗" value="网络诈骗" />
            <el-option label="情感诈骗" value="情感诈骗" />
            <el-option label="投资诈骗" value="投资诈骗" />
          </el-select>
        </el-form-item>
        <el-form-item label="案例内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入案例内容" />
        </el-form-item>
        <el-form-item label="防范提示">
          <el-input v-model="form.tips" type="textarea" :rows="4" placeholder="请输入防范提示" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Cases',
  setup() {
    const cases = ref([])
    const selectedType = ref('')
    const detailVisible = ref(false)
    const formVisible = ref(false)
    const isEdit = ref(false)
    const submitting = ref(false)
    const currentCase = ref(null)
    const form = ref({
      id: null,
      title: '',
      type: '',
      content: '',
      tips: ''
    })

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
      detailVisible.value = true
    }

    const showAddDialog = () => {
      isEdit.value = false
      form.value = { id: null, title: '', type: '', content: '', tips: '' }
      formVisible.value = true
    }

    const showEditDialog = (c) => {
      isEdit.value = true
      form.value = { id: c.id, title: c.title, type: c.type, content: c.content, tips: c.tips }
      formVisible.value = true
    }

    const handleSubmit = async () => {
      if (!form.value.title || !form.value.type || !form.value.content) {
        ElMessage.warning('请填写完整信息')
        return
      }
      submitting.value = true
      try {
        if (isEdit.value) {
          await axios.put(`/api/cases/${form.value.id}`, form.value)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/cases', form.value)
          ElMessage.success('添加成功')
        }
        formVisible.value = false
        loadCases()
      } catch (e) {
        ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
      } finally {
        submitting.value = false
      }
    }

    const handleDelete = async (id) => {
      try {
        await ElMessageBox.confirm('确定要删除该案例吗？删除后将同时从向量库中移除。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await axios.delete(`/api/cases/${id}`)
        ElMessage.success('删除成功')
        loadCases()
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
      }
    }

    onMounted(() => {
      loadCases()
    })

    return {
      cases,
      selectedType,
      detailVisible,
      formVisible,
      isEdit,
      submitting,
      currentCase,
      form,
      loadCases,
      showDetail,
      showAddDialog,
      showEditDialog,
      handleSubmit,
      handleDelete
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-right {
  display: flex;
  gap: 8px;
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
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

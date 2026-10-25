<template>
  <div class="cases-page">
    <!-- 类型筛选 + 操作按钮 -->
    <div class="filter-card">
      <div class="type-pills">
        <button
          class="pill"
          :class="{ active: selectedType === '' }"
          @click="selectType('')"
        >全部</button>
        <button
          v-for="(meta, type) in typeMeta"
          :key="type"
          class="pill"
          :class="{ active: selectedType === type }"
          @click="selectType(type)"
        >
          {{ meta.icon }} {{ type }}
        </button>
      </div>
      <el-button v-if="isAdmin" type="primary" @click="showAddDialog">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>新增案例
      </el-button>
    </div>

    <!-- 案例列表 -->
    <div v-if="cases.length" class="cases-grid">
      <el-card
        v-for="c in cases"
        :key="c.id"
        shadow="hover"
        class="case-card"
        @click="showDetail(c)"
      >
        <div class="card-top">
          <el-tag
            :color="typeMeta[c.type]?.color || '#f56c6c'"
            effect="dark"
            size="small"
            class="type-tag"
          >
            {{ typeMeta[c.type]?.icon }} {{ c.type }}
          </el-tag>
          <span v-if="isAdmin" class="card-actions" @click.stop>
            <el-button link type="primary" size="small" @click="showEditDialog(c)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(c.id)">删除</el-button>
          </span>
        </div>
        <h3 class="card-title">{{ c.title }}</h3>
        <p class="card-content">{{ c.content }}</p>
        <div v-if="c.tips" class="card-tip">
          <span class="tip-label">💡 提示：</span>
          <span class="tip-text">{{ c.tips }}</span>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-card v-else shadow="never">
      <el-empty description="暂无案例，点击右上角新增" :image-size="120" />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" width="640px">
      <template #header>
        <div v-if="currentCase" class="detail-head">
          <el-tag
            :color="typeMeta[currentCase.type]?.color || '#f56c6c'"
            effect="dark"
            size="small"
          >
            {{ typeMeta[currentCase.type]?.icon }} {{ currentCase.type }}
          </el-tag>
          <h3>{{ currentCase.title }}</h3>
        </div>
      </template>
      <div v-if="currentCase" class="case-detail">
        <section class="detail-section">
          <h4>案例经过</h4>
          <p class="detail-text">{{ currentCase.content }}</p>
        </section>
        <section class="detail-section tips-box">
          <h4>防范提示</h4>
          <p class="detail-text">{{ currentCase.tips || '暂无防范提示' }}</p>
        </section>
      </div>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑案例' : '新增案例'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入案例标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option v-for="(meta, type) in typeMeta" :key="type" :label="type" :value="type" />
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { getAllCases, getCasesByType, addCase, updateCase, deleteCase } from '@/api/cases'
import { useUserStore } from '@/stores/user'

// 仅管理员可见案例增删改操作
const { isAdmin } = storeToRefs(useUserStore())


// 类型元信息：统一驱动徽章配色与图标
const typeMeta = {
  电信诈骗: { color: '#f56c6c', icon: '📞' },
  网络诈骗: { color: '#e6a23c', icon: '🌐' },
  情感诈骗: { color: '#06b6d4', icon: '💌' },
  投资诈骗: { color: '#7c6cf5', icon: '📈' }
}

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

const selectType = (type) => {
  selectedType.value = type
  loadCases()
}

const loadCases = async () => {
  const res = selectedType.value
    ? await getCasesByType(selectedType.value)
    : await getAllCases()
  if (res.success) {
    cases.value = res.data || []
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
      await updateCase(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await addCase(form.value)
      ElMessage.success('添加成功')
    }
    formVisible.value = false
    loadCases()
  } catch (e) {
    // 拦截器已统一提示错误
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
    await deleteCase(id)
    ElMessage.success('删除成功')
    loadCases()
  } catch (e) {
    if (e !== 'cancel') {
      // 拦截器已统一提示错误
    }
  }
}

onMounted(() => {
  loadCases()
})
</script>

<style scoped>
.cases-page {
  padding: 0;
}

/* 筛选卡片 */
.filter-card {
  background: #fff;
  padding: 12px 16px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.type-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.pill:hover {
  border-color: #409eff;
  color: #409eff;
}

.pill.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

/* 卡片网格 */
.cases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.case-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.type-tag {
  border: none;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.card-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.card-content {
  margin: 0 0 12px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-tip {
  background: #f4f4f5;
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 12px;
  color: #606266;
  display: flex;
  gap: 4px;
}

.tip-label {
  flex-shrink: 0;
}

.tip-text {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 详情弹窗 */
.detail-head h3 {
  margin: 8px 0 0;
  font-size: 18px;
  color: #303133;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.detail-text {
  margin: 0;
  line-height: 1.7;
  color: #606266;
  font-size: 13px;
  white-space: pre-wrap;
}

.tips-box {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  padding: 12px 14px;
  border-radius: 4px;
}

.tips-box h4 {
  color: #409eff;
}

@media (max-width: 768px) {
  .cases-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="cases-page">
    <!-- Hero 头部 -->
    <div class="hero">
      <div class="hero-deco deco-1"></div>
      <div class="hero-deco deco-2"></div>
      <div class="hero-content">
        <span class="hero-badge">🛡️ 防骗案例库</span>
        <h2>诈骗案例分析</h2>
        <p>了解真实诈骗案例，识别话术套路，提高警惕</p>
      </div>
    </div>

    <!-- 筛选栏：类型胶囊 + 新增 -->
    <div class="toolbar">
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
          :style="selectedType === type ? { background: meta.color, borderColor: meta.color } : {}"
          @click="selectType(type)"
        >
          {{ meta.icon }} {{ type }}
        </button>
      </div>
      <el-button type="primary" round class="add-btn" @click="showAddDialog">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>新增案例
      </el-button>
    </div>

    <!-- 案例卡片流 -->
    <div v-if="cases.length" class="cases-grid">
      <article
        v-for="c in cases"
        :key="c.id"
        class="case-card"
        :style="{ '--accent': typeMeta[c.type]?.color || '#f56c6c' }"
        @click="showDetail(c)"
      >
        <div class="card-top">
          <span class="type-chip">
            {{ typeMeta[c.type]?.icon }} {{ c.type }}
          </span>
          <span class="card-actions">
            <el-tooltip content="编辑" placement="top">
              <button class="icon-btn" @click.stop="showEditDialog(c)">✏️</button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <button class="icon-btn danger" @click.stop="handleDelete(c.id)">🗑️</button>
            </el-tooltip>
          </span>
        </div>
        <h3 class="card-title">{{ c.title }}</h3>
        <p class="card-content">{{ c.content }}</p>
        <div v-if="c.tips" class="card-tip">
          <span class="tip-label">💡 提示</span>
          <span class="tip-text">{{ c.tips }}</span>
        </div>
        <div class="card-more">查看详情 →</div>
      </article>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else
      description="暂无案例，点击右上角新增"
      :image-size="120"
      class="empty-box"
    />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" width="620px" class="detail-dialog">
      <template #header>
        <div v-if="currentCase" class="detail-head">
          <span
            class="type-chip large"
            :style="{ background: typeMeta[currentCase.type]?.color || '#f56c6c' }"
          >
            {{ typeMeta[currentCase.type]?.icon }} {{ currentCase.type }}
          </span>
          <h3>{{ currentCase.title }}</h3>
        </div>
      </template>
      <div v-if="currentCase" class="case-detail">
        <section class="detail-section">
          <h4><span class="sec-icon">📖</span>案例经过</h4>
          <p class="detail-text">{{ currentCase.content }}</p>
        </section>
        <section class="detail-section tips-box">
          <h4><span class="sec-icon">💡</span>防范提示</h4>
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
import { getAllCases, getCasesByType, addCase, updateCase, deleteCase } from '@/api/cases'

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
  padding: 20px 0;
}

/* ---------- Hero ---------- */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  padding: 36px 40px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 14px;
}

.hero h2 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
}

.hero p {
  margin: 0;
  opacity: 0.88;
  font-size: 15px;
}

.hero-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.deco-1 {
  width: 220px;
  height: 220px;
  right: -60px;
  top: -80px;
}

.deco-2 {
  width: 140px;
  height: 140px;
  right: 140px;
  bottom: -70px;
}

/* ---------- 工具栏 ---------- */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.type-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill {
  border: 1.5px solid #e4e7ed;
  background: #fff;
  color: #606266;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
}

.pill:hover {
  border-color: #667eea;
  color: #667eea;
}

.pill.active {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.add-btn {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* ---------- 卡片网格 ---------- */
.cases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.case-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 20px 20px 16px;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.case-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--accent), transparent 130%);
}

.case-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.13);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--accent, #f56c6c);
  color: #fff;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.type-chip.large {
  font-size: 13px;
  padding: 5px 16px;
}

.card-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.25s;
}

.case-card:hover .card-actions {
  opacity: 1;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: #f4f6fa;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s, transform 0.2s;
}

.icon-btn:hover {
  background: #e8ecf5;
  transform: scale(1.08);
}

.icon-btn.danger:hover {
  background: #fde2e2;
}

.card-title {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: #1f2430;
  line-height: 1.4;
}

.card-content {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f7f8fc;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.tip-label {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}

.tip-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-more {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
  transition: letter-spacing 0.25s;
}

.case-card:hover .card-more {
  letter-spacing: 1px;
}

.empty-box {
  padding: 60px 0;
}

/* ---------- 详情弹窗 ---------- */
.detail-head h3 {
  margin: 12px 0 0;
  font-size: 20px;
  color: #1f2430;
}

.case-detail {
  padding: 4px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #1f2430;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sec-icon {
  font-size: 16px;
}

.detail-text {
  margin: 0;
  line-height: 1.85;
  color: #4b5563;
  font-size: 14px;
  white-space: pre-wrap;
}

.tips-box {
  background: linear-gradient(135deg, #f0f5ff, #f5f0ff);
  border-left: 4px solid #667eea;
  padding: 16px 18px;
  border-radius: 12px;
}

.tips-box h4 {
  color: #667eea;
}

/* ---------- 移动端适配 ---------- */
@media (max-width: 768px) {
  .hero {
    padding: 28px 24px;
  }

  .hero h2 {
    font-size: 24px;
  }

  .cases-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    opacity: 1;
  }
}
</style>

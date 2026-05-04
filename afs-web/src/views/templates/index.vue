<template>
  <div class="templates-page">
    <div class="page-header">
      <h2>对话模板</h2>
      <el-button type="primary" @click="dialogVisible = true">创建模板</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="公共模板" name="public">
        <div class="templates-grid">
          <el-card v-for="item in publicTemplates" :key="item.id" class="template-card" shadow="hover">
            <h4>{{ item.title }}</h4>
            <p class="template-content">{{ item.content }}</p>
            <div class="template-footer">
              <span class="usage-count">使用 {{ item.usageCount }} 次</span>
              <el-button type="primary" size="small" @click="handleUse(item)">使用</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
      <el-tab-pane label="我的模板" name="mine">
        <div class="templates-grid">
          <el-card v-for="item in myTemplates" :key="item.id" class="template-card" shadow="hover">
            <h4>{{ item.title }}</h4>
            <p class="template-content">{{ item.content }}</p>
            <div class="template-footer">
              <span class="usage-count">使用 {{ item.usageCount }} 次</span>
              <el-button-group>
                <el-button type="danger" size="small" @click="handleDelete(item.id)">删除</el-button>
                <el-button type="primary" size="small" @click="handleUse(item)">使用</el-button>
              </el-button-group>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="创建模板" width="500px">
      <el-form :model="templateForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="templateForm.title" placeholder="请输入模板标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="templateForm.content" type="textarea" :rows="4" placeholder="请输入模板内容" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="templateForm.category" placeholder="请选择分类">
            <el-option label="防骗咨询" value="防骗咨询" />
            <el-option label="案例分析" value="案例分析" />
            <el-option label="知识问答" value="知识问答" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="公开">
          <el-switch v-model="templateForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPublicTemplates, getUserTemplates, createTemplate, deleteTemplate, useTemplate } from '@/api/conversation'

const activeTab = ref('public')
const dialogVisible = ref(false)
const publicTemplates = ref([])
const myTemplates = ref([])
const userId = ref(parseInt(localStorage.getItem('userId') || '1'))

const templateForm = ref({
  title: '',
  content: '',
  category: '防骗咨询',
  isPublic: false
})

const loadTemplates = async () => {
  try {
    const [pubRes, myRes] = await Promise.all([
      getPublicTemplates(),
      getUserTemplates(userId.value)
    ])
    publicTemplates.value = pubRes.data || []
    myTemplates.value = myRes.data || []
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

const handleCreate = async () => {
  try {
    await createTemplate({
      ...templateForm.value,
      createUserId: userId.value
    })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    templateForm.value = { title: '', content: '', category: '防骗咨询', isPublic: false }
    loadTemplates()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleDelete = async (id) => {
  try {
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleUse = async (item) => {
  try {
    await useTemplate(item.id)
    ElMessage.success('已复制到输入框')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.templates-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.template-card h4 {
  margin: 0 0 12px 0;
  color: #303133;
}

.template-content {
  color: #606266;
  font-size: 14px;
  margin-bottom: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.template-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.usage-count {
  color: #909399;
  font-size: 12px;
}
</style>

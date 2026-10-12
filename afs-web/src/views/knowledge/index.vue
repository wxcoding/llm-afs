<template>
  <div class="knowledge-page">
    <PageHeader title="📖 防诈骗知识库" subtitle="全面了解各类诈骗手法和防范技巧" />

    <FilterBar>
      <template #left>
        <el-select v-model="selectedCategory" placeholder="选择分类" @change="loadKnowledge" style="width: 150px">
          <el-option label="全部分类" value="" />
          <el-option
            v-for="option in categoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识..."
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </template>
      <template #right>
        <el-button type="primary" @click="showAddDialog">+ 新增知识</el-button>
        <el-button type="success" @click="showUploadDialog">📎 上传文档</el-button>
        <el-button type="warning" @click="syncVectorStore" :loading="syncing">同步向量库</el-button>
      </template>
    </FilterBar>

    <div class="search-tabs">
      <el-radio-group v-model="searchMode" @change="handleSearchModeChange">
        <el-radio-button value="keyword">关键词搜索</el-radio-button>
        <el-radio-button value="semantic">语义搜索</el-radio-button>
      </el-radio-group>
    </div>

    <SemanticResultList v-if="searchMode === 'semantic' && semanticResults.length > 0" :results="semanticResults" />

    <div v-else class="knowledge-list">
      <KnowledgeItem
        v-for="k in knowledgeList"
        :key="k.id"
        :item="k"
        :expanded="expandedIds.includes(k.id)"
        :status-text="getStatusText(k.status)"
        @toggle="toggleExpand(k.id)"
        @edit="showEditDialog"
        @delete="handleDelete"
      />
    </div>

    <el-empty v-if="knowledgeList.length === 0 && searchMode === 'keyword'" description="暂无知识内容">
      <el-button type="primary" @click="showUploadDialog">上传文档</el-button>
    </el-empty>

    <KnowledgeFormDialog
      v-model:visible="dialogVisible"
      v-model:submit-mode="submitMode"
      :is-edit="isEdit"
      :form="form"
      :submitting="submitting"
      :category-options="categoryOptions"
      @submit="handleSubmit"
    />

    <KnowledgeUploadDialog
      v-model:visible="uploadDialogVisible"
      :uploading="uploading"
      :upload-form="uploadForm"
      :file-list="fileList"
      :category-options="categoryOptions"
      @upload="handleUpload"
      @file-change="handleFileChange"
      @file-remove="handleFileRemove"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitForAudit } from '@/api/knowledgeEnhanced'
import dict from '@/utils/dict'
import PageHeader from '@/components/common/PageHeader.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import KnowledgeItem from '@/components/business/KnowledgeItem.vue'
import KnowledgeFormDialog from '@/components/business/KnowledgeFormDialog.vue'
import KnowledgeUploadDialog from '@/components/business/KnowledgeUploadDialog.vue'
import SemanticResultList from '@/components/business/SemanticResultList.vue'

const knowledgeList = ref([])
const expandedIds = ref([])
const selectedCategory = ref('')
const searchKeyword = ref('')
const searchMode = ref('keyword')
const semanticResults = ref([])
const dialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const syncing = ref(false)
const uploading = ref(false)
const fileList = ref([])
const form = ref({ id: null, title: '', category: '', content: '' })
const uploadForm = ref({ category: '' })
const user = inject('user')
const submitMode = ref('direct')

const categoryOptions = ref([])

const initDicts = async () => {
  await dict.getBatchDict(['knowledge_category', 'knowledge_status'])
  categoryOptions.value = dict.getDictOptions('knowledge_category')
}

const loadKnowledge = async () => {
  const params = {}
  if (selectedCategory.value) params.category = selectedCategory.value
  if (searchKeyword.value && searchMode.value === 'keyword') params.keyword = searchKeyword.value
  const res = await axios.get('/api/knowledge', { params })
  if (res.data.success) {
    knowledgeList.value = res.data.list
  }
}

const handleSearch = () => {
  if (searchMode.value === 'semantic' && searchKeyword.value) {
    doSemanticSearch()
  } else {
    loadKnowledge()
  }
}

const doSemanticSearch = async () => {
  if (!searchKeyword.value) return
  try {
    const res = await axios.get('/api/knowledge/search/semantic', {
      params: { query: searchKeyword.value, topK: 10 }
    })
    if (res.data.success) {
      semanticResults.value = res.data.results
    }
  } catch (e) {
    ElMessage.error('语义搜索失败')
  }
}

const handleSearchModeChange = () => {
  semanticResults.value = []
  if (searchMode.value === 'keyword') {
    loadKnowledge()
  }
}

const toggleExpand = (id) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx > -1) {
    expandedIds.value.splice(idx, 1)
  } else {
    expandedIds.value.push(id)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  submitMode.value = 'direct'
  form.value = { id: null, title: '', category: '', content: '' }
  dialogVisible.value = true
}

const showEditDialog = (k) => {
  isEdit.value = true
  submitMode.value = 'direct'
  form.value = { id: k.id, title: k.title, category: k.category, content: k.content }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.title || !form.value.category || !form.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    if (submitMode.value === 'audit') {
      const userId = user?.value?.id || parseInt(localStorage.getItem('userId') || '1')
      await submitForAudit({
        knowledgeId: isEdit.value ? form.value.id : null,
        title: form.value.title,
        content: form.value.content,
        category: form.value.category,
        submitUserId: userId
      })
      ElMessage.success('已提交审核，请等待管理员审核')
    } else {
      if (isEdit.value) {
        await axios.put(`/api/knowledge/${form.value.id}`, form.value)
        ElMessage.success('更新成功')
      } else {
        await axios.post('/api/knowledge', form.value)
        ElMessage.success('添加成功')
      }
    }
    dialogVisible.value = false
    loadKnowledge()
  } catch (e) {
    ElMessage.error(submitMode.value === 'audit' ? '提交审核失败' : (isEdit.value ? '更新失败' : '添加失败'))
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识吗？删除后将同时从向量库中移除。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/knowledge/${id}`)
    ElMessage.success('删除成功')
    loadKnowledge()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const syncVectorStore = async () => {
  syncing.value = true
  try {
    await axios.post('/api/knowledge/sync-vector')
    ElMessage.success('向量库同步完成')
  } catch (e) {
    ElMessage.error('同步失败')
  } finally {
    syncing.value = false
  }
}

const showUploadDialog = () => {
  fileList.value = []
  uploadForm.value.category = ''
  uploadDialogVisible.value = true
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

const handleUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  let successCount = 0
  let failCount = 0

  try {
    for (const fileItem of fileList.value) {
      const file = fileItem.raw
      const formData = new FormData()
      formData.append('file', file)
      if (uploadForm.value.category) {
        formData.append('category', uploadForm.value.category)
      }

      try {
        const res = await axios.post('/api/knowledge/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        const responseData = res.data || {}
        const isSuccess = responseData.success === true || responseData.success === 'true' || responseData.success === 1

        if (isSuccess) {
          successCount++
        } else {
          failCount++
        }
      } catch (e) {
        failCount++
      }
    }

    let msg = `上传完成：成功 ${successCount} 个`
    if (failCount > 0) {
      msg += `，失败 ${failCount} 个`
    }
    ElMessage.success(msg)

    if (successCount > 0) {
      uploadDialogVisible.value = false
      fileList.value = []
      loadKnowledge()
    }
  } finally {
    uploading.value = false
  }
}

const getStatusText = (status) => {
  return dict.getDictName('knowledge_status', status) || '已发布'
}

onMounted(() => {
  initDicts()
  loadKnowledge()
})
</script>

<style scoped>
.knowledge-page {
  padding: 20px 0;
}

.search-tabs {
  margin-bottom: 20px;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>

<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2>📖 防诈骗知识库</h2>
      <p>全面了解各类诈骗手法和防范技巧</p>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
        <el-select v-model="selectedCategory" placeholder="选择分类" @change="loadKnowledge" style="width: 150px">
          <el-option label="全部分类" value="" />
          <el-option label="防范技巧" value="防范技巧" />
          <el-option label="诈骗类型" value="诈骗类型" />
          <el-option label="应对方法" value="应对方法" />
          <el-option label="文档资料" value="文档资料" />
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
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="showAddDialog">+ 新增知识</el-button>
        <el-button type="success" @click="showUploadDialog">📎 上传文档</el-button>
        <el-button type="warning" @click="syncVectorStore" :loading="syncing">同步向量库</el-button>
      </div>
    </div>

    <div class="search-tabs">
      <el-radio-group v-model="searchMode" @change="handleSearchModeChange">
        <el-radio-button value="keyword">关键词搜索</el-radio-button>
        <el-radio-button value="semantic">语义搜索</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="searchMode === 'semantic' && semanticResults.length > 0" class="semantic-results">
      <h3>语义搜索结果</h3>
      <el-card v-for="(r, idx) in semanticResults" :key="idx" class="semantic-card" shadow="hover">
        <div class="semantic-header">
          <el-tag :type="r.source === 'knowledge' ? 'success' : 'warning'" size="small">
            {{ r.source === 'knowledge' ? '知识库' : '案例库' }}
          </el-tag>
          <span class="semantic-title">{{ r.title || '无标题' }}</span>
          <el-tag v-if="r.category" size="small" type="info">{{ r.category }}</el-tag>
          <span class="semantic-score">相似度: {{ (r.score * 100).toFixed(1) }}%</span>
        </div>
        <div class="semantic-content">{{ r.content }}</div>
      </el-card>
    </div>

    <div v-else class="knowledge-list">
      <el-card
        v-for="k in knowledgeList"
        :key="k.id"
        class="knowledge-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <div class="card-header-left">
              <el-tag :type="getCategoryTagType(k.category)">{{ k.category }}</el-tag>
              <h3>{{ k.title }}</h3>
            </div>
            <div class="card-header-right">
              <el-button type="primary" size="small" @click="showEditDialog(k)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(k.id)">删除</el-button>
            </div>
          </div>
        </template>
        <div class="knowledge-content">
          <pre>{{ k.content }}</pre>
        </div>
      </el-card>
    </div>

    <el-empty v-if="knowledgeList.length === 0 && searchMode === 'keyword'" description="暂无知识内容">
      <el-button type="primary" @click="showUploadDialog">上传文档</el-button>
    </el-empty>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识' : '新增知识'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入知识标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="防范技巧" value="防范技巧" />
            <el-option label="诈骗类型" value="诈骗类型" />
            <el-option label="应对方法" value="应对方法" />
            <el-option label="文档资料" value="文档资料" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入知识内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" title="上传文档" width="600px">
      <div class="upload-tip">
        <el-alert type="info" :closable="false">
          <template #title>
            支持的文件格式：PDF、Word（.doc/.docx）、Markdown（.md）、Excel（.xls/.xlsx）、文本文件（.txt）
          </template>
        </el-alert>
      </div>
      <el-form :model="uploadForm" label-width="80px" style="margin-top: 16px">
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            class="upload-component"
            :auto-upload="false"
            :limit="10"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            accept=".pdf,.doc,.docx,.md,.markdown,.txt,.xls,.xlsx"
            multiple
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持批量上传，单个文件不超过10MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" placeholder="不选择则自动分类" clearable style="width: 100%">
            <el-option label="自动分类" value="" />
            <el-option label="防范技巧" value="防范技巧" />
            <el-option label="诈骗类型" value="诈骗类型" />
            <el-option label="应对方法" value="应对方法" />
            <el-option label="文档资料" value="文档资料" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          上传 {{ fileList.length > 0 ? `(${fileList.length}个文件)` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Knowledge',
  setup() {
    const knowledgeList = ref([])
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
    const uploadRef = ref(null)
    const fileList = ref([])
    const form = ref({
      id: null,
      title: '',
      category: '',
      content: ''
    })
    const uploadForm = ref({
      category: ''
    })

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

    const showAddDialog = () => {
      isEdit.value = false
      form.value = { id: null, title: '', category: '', content: '' }
      dialogVisible.value = true
    }

    const showEditDialog = (k) => {
      isEdit.value = true
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
        if (isEdit.value) {
          await axios.put(`/api/knowledge/${form.value.id}`, form.value)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/knowledge', form.value)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadKnowledge()
      } catch (e) {
        ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
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
      let failDetails = []

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
            console.log('上传响应:', res)
            console.log('响应数据:', res.data)
            
            const responseData = res.data || {}
            const successField = responseData.success
            
            console.log('success字段:', successField, typeof successField)
            
            const isSuccess = successField === true || successField === 'true' || successField === 1
            
            if (isSuccess) {
              successCount++
              console.log('文件上传成功:', file.name)
            } else {
              failCount++
              const errorMsg = responseData.message || responseData.msg || '上传失败'
              failDetails.push(`${file.name}: ${errorMsg}`)
              console.log('文件上传失败:', file.name, errorMsg)
            }
          } catch (e) {
            failCount++
            console.error('上传异常:', e)
            failDetails.push(`${file.name}: ${e.message || '上传失败'}`)
          }
        }

        let msg = `上传完成：成功 ${successCount} 个`
        if (failCount > 0) {
          msg += `，失败 ${failCount} 个`
          console.error('上传失败详情:', failDetails)
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

    const getCategoryTagType = (category) => {
      const map = {
        '防范技巧': 'success',
        '诈骗类型': 'danger',
        '应对方法': 'warning',
        '文档资料': 'info'
      }
      return map[category] || ''
    }

    onMounted(() => {
      loadKnowledge()
    })

    return {
      knowledgeList,
      selectedCategory,
      searchKeyword,
      searchMode,
      semanticResults,
      dialogVisible,
      uploadDialogVisible,
      isEdit,
      submitting,
      syncing,
      uploading,
      uploadRef,
      fileList,
      form,
      uploadForm,
      loadKnowledge,
      handleSearch,
      handleSearchModeChange,
      showAddDialog,
      showEditDialog,
      handleSubmit,
      handleDelete,
      syncVectorStore,
      showUploadDialog,
      handleFileChange,
      handleFileRemove,
      handleUpload,
      getCategoryTagType
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

.search-tabs {
  margin-bottom: 20px;
}

.semantic-results h3 {
  margin-bottom: 16px;
  color: #333;
}

.semantic-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.semantic-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.semantic-title {
  font-weight: 600;
  color: #333;
}

.semantic-score {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.semantic-content {
  color: #666;
  line-height: 1.6;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
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
  justify-content: space-between;
  align-items: center;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header-left h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.card-header-right {
  display: flex;
  gap: 4px;
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

.upload-tip {
  margin-bottom: 0;
}

.upload-component {
  width: 100%;
}
</style>
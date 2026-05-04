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
      <div
        v-for="k in knowledgeList"
        :key="k.id"
        class="knowledge-item"
        :class="{ expanded: expandedIds.includes(k.id) }"
      >
        <div class="knowledge-item-header" @click="toggleExpand(k.id)">
          <div class="knowledge-item-left">
            <el-tag :type="getCategoryTagType(k.category)" size="small">{{ k.category }}</el-tag>
            <h3 class="knowledge-item-title">{{ k.title }}</h3>
            <el-tag :type="getStatusTagType(k.status)" size="small">{{ getStatusText(k.status) }}</el-tag>
          </div>
          <div class="knowledge-item-right">
            <el-button type="primary" size="small" @click.stop="showEditDialog(k)">编辑</el-button>
            <el-button type="danger" size="small" @click.stop="handleDelete(k.id)">删除</el-button>
            <i :class="expandedIds.includes(k.id) ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="expand-icon"></i>
          </div>
        </div>
        <div class="knowledge-item-summary" v-show="!expandedIds.includes(k.id)">
          {{ getSummary(k.content) }}
        </div>
        <div class="knowledge-item-content" v-show="expandedIds.includes(k.id)">
          <pre>{{ k.content }}</pre>
        </div>
      </div>
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
            <el-option 
              v-for="option in categoryOptions" 
              :key="option.value" 
              :label="option.label" 
              :value="option.value" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入知识内容" />
        </el-form-item>
        <el-form-item label="提交方式">
          <el-radio-group v-model="submitMode">
            <el-radio value="direct">直接保存</el-radio>
            <el-radio value="audit">提交审核</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ submitMode === 'audit' ? '提交审核' : '确定' }}</el-button>
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
            <el-option 
              v-for="option in categoryOptions" 
              :key="option.value" 
              :label="option.label" 
              :value="option.value" 
            />
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
import { ref, onMounted, inject } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitForAudit } from '@/api/knowledgeEnhanced'
import dict from '@/utils/dict'

export default {
  name: 'Knowledge',
  setup() {
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
    const user = inject('user')
    const submitMode = ref('direct') // 'direct' 直接保存 | 'audit' 提交审核
    
    // 字典数据
    const categoryOptions = ref([])
    const statusOptions = ref([])

    // 初始化字典
    const initDicts = async () => {
      // 批量获取字典
      await dict.getBatchDict(['knowledge_category', 'knowledge_status'])
      
      // 获取分类选项
      categoryOptions.value = dict.getDictOptions('knowledge_category')
      statusOptions.value = dict.getDictOptions('knowledge_status')
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

    const getSummary = (content) => {
      if (!content) return ''
      return content.length > 150 ? content.substring(0, 150) + '...' : content
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
          // 提交审核模式
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
          // 直接保存模式
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

    const getStatusText = (status) => {
      return dict.getDictName('knowledge_status', status) || '已发布'
    }

    const getStatusTagType = (status) => {
      const map = {
        'DRAFT': 'info',
        'PENDING_REVIEW': 'warning',
        'ACTIVE': 'success',
        'REJECTED': 'danger'
      }
      return map[status] || 'success'
    }

    onMounted(() => {
      initDicts()
      loadKnowledge()
    })

    return {
      knowledgeList,
      expandedIds,
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
      submitMode,
      categoryOptions,
      statusOptions,
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
      getCategoryTagType,
      getStatusText,
      getStatusTagType,
      toggleExpand,
      getSummary
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
  gap: 12px;
}

.knowledge-item {
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
  overflow: hidden;
}

.knowledge-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.knowledge-item.expanded {
  border-color: #409eff;
}

.knowledge-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s ease;
}

.knowledge-item-header:hover {
  background: #f0f7ff;
}

.knowledge-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.knowledge-item-title {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.expand-icon {
  font-size: 14px;
  color: #909399;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.knowledge-item-summary {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.knowledge-item-content {
  padding: 16px;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.knowledge-item-content pre {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #262626;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.upload-tip {
  margin-bottom: 0;
}

.upload-component {
  width: 100%;
}
</style>
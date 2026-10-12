import request from '@/utils/request'

// ==================== 基础 CRUD ====================

export const getAllKnowledge = () => {
  return request.get('/knowledge')
}

export const getKnowledgeByCategory = (category) => {
  return request.get('/knowledge', { params: { category } })
}

export const searchKnowledge = (keyword) => {
  return request.get('/knowledge', { params: { keyword } })
}

export const getKnowledgeById = (id) => {
  return request.get(`/knowledge/${id}`)
}

export const addKnowledge = (data) => {
  return request.post('/knowledge', data)
}

export const updateKnowledge = (id, data) => {
  return request.put(`/knowledge/${id}`, data)
}

export const deleteKnowledge = (id) => {
  return request.delete(`/knowledge/${id}`)
}

// ==================== 搜索与向量同步 ====================

export const semanticSearch = (query, topK = 5) => {
  return request.get('/knowledge/search/semantic', { params: { query, topK } })
}

export const syncVectorStore = () => {
  return request.post('/knowledge/sync-vector')
}

// ==================== 文档上传 ====================

export const uploadDocument = (file, category, title) => {
  const formData = new FormData()
  formData.append('file', file)
  if (category) formData.append('category', category)
  if (title) formData.append('title', title)
  return request.post('/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadDocumentsBatch = (files, category) => {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  if (category) formData.append('category', category)
  return request.post('/knowledge/upload/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 标签管理 ====================

export const createTag = (data) => {
  return request.post('/knowledge/tags', data)
}

export const getAllTags = () => {
  return request.get('/knowledge/tags')
}

export const deleteTag = (tagId) => {
  return request.delete(`/knowledge/tags/${tagId}`)
}

export const addTagToKnowledge = (knowledgeId, tagId) => {
  return request.post(`/knowledge/tags/knowledge/${knowledgeId}`, null, { params: { tagId } })
}

export const removeTagFromKnowledge = (knowledgeId, tagId) => {
  return request.delete(`/knowledge/tags/knowledge/${knowledgeId}`, { params: { tagId } })
}

export const getTagsForKnowledge = (knowledgeId) => {
  return request.get(`/knowledge/tags/knowledge/${knowledgeId}`)
}

// ==================== 版本控制 ====================

export const getVersionHistory = (knowledgeId) => {
  return request.get(`/knowledge/versions/${knowledgeId}`)
}

export const rollbackToVersion = (versionId) => {
  return request.post(`/knowledge/versions/${versionId}/rollback`)
}

// ==================== 审核流程 ====================

export const submitForAudit = (data) => {
  return request.post('/knowledge/audit/submit', data)
}

export const getPendingAudits = () => {
  return request.get('/knowledge/audit/pending')
}

export const approveAudit = (auditId, auditUserId, comment) => {
  return request.post(`/knowledge/audit/${auditId}/approve`, null, { params: { auditUserId, comment } })
}

export const rejectAudit = (auditId, auditUserId, comment) => {
  return request.post(`/knowledge/audit/${auditId}/reject`, null, { params: { auditUserId, comment } })
}

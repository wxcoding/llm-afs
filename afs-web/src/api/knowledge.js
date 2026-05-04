import request from '@/utils/request'

export const getAllKnowledge = () => {
  return request.get('/api/knowledge/list')
}

export const getKnowledgeByCategory = (category) => {
  return request.get('/api/knowledge/list', { params: { category } })
}

export const getKnowledgeById = (id) => {
  return request.get(`/api/knowledge/${id}`)
}

export const addKnowledge = (data) => {
  return request.post('/api/knowledge', data)
}

export const updateKnowledge = (id, data) => {
  return request.put(`/api/knowledge/${id}`, data)
}

export const deleteKnowledge = (id) => {
  return request.delete(`/api/knowledge/${id}`)
}

export const searchKnowledge = (keyword) => {
  return request.get('/api/knowledge/search', { params: { keyword } })
}

export const semanticSearch = (query, topK = 5) => {
  return request.get('/api/knowledge/semantic-search', { params: { query, topK } })
}

export const uploadDocument = (file, category, title) => {
  const formData = new FormData()
  formData.append('file', file)
  if (category) formData.append('category', category)
  if (title) formData.append('title', title)
  return request.post('/api/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadDocumentsBatch = (files, category) => {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  if (category) formData.append('category', category)
  return request.post('/api/knowledge/upload-batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

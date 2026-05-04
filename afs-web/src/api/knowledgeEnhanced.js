import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const createTag = (data) => {
  return request.post('/knowledge/enhanced/tags', data)
}

export const getAllTags = () => {
  return request.get('/knowledge/enhanced/tags')
}

export const deleteTag = (tagId) => {
  return request.delete(`/knowledge/enhanced/tags/${tagId}`)
}

export const addTagToKnowledge = (knowledgeId, tagId) => {
  return request.post(`/knowledge/enhanced/tags/knowledge/${knowledgeId}`, null, { params: { tagId } })
}

export const removeTagFromKnowledge = (knowledgeId, tagId) => {
  return request.delete(`/knowledge/enhanced/tags/knowledge/${knowledgeId}`, { params: { tagId } })
}

export const getTagsForKnowledge = (knowledgeId) => {
  return request.get(`/knowledge/enhanced/tags/knowledge/${knowledgeId}`)
}

export const getVersionHistory = (knowledgeId) => {
  return request.get(`/knowledge/enhanced/versions/${knowledgeId}`)
}

export const rollbackToVersion = (versionId) => {
  return request.post(`/knowledge/enhanced/versions/${versionId}/rollback`)
}

export const submitForAudit = (data) => {
  return request.post('/knowledge/enhanced/audit/submit', data)
}

export const getPendingAudits = () => {
  return request.get('/knowledge/enhanced/audit/pending')
}

export const approveAudit = (auditId, auditUserId, comment) => {
  return request.post(`/knowledge/enhanced/audit/${auditId}/approve`, null, { params: { auditUserId, comment } })
}

export const rejectAudit = (auditId, auditUserId, comment) => {
  return request.post(`/knowledge/enhanced/audit/${auditId}/reject`, null, { params: { auditUserId, comment } })
}

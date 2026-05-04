import request from '@/utils/request'

export const getAllCases = () => {
  return request.get('/api/cases')
}

export const getCasesByType = (type) => {
  return request.get('/api/cases', { params: { type } })
}

export const getCaseById = (id) => {
  return request.get(`/api/cases/${id}`)
}

export const addCase = (data) => {
  return request.post('/api/cases', data)
}

export const updateCase = (id, data) => {
  return request.put(`/api/cases/${id}`, data)
}

export const deleteCase = (id) => {
  return request.delete(`/api/cases/${id}`)
}

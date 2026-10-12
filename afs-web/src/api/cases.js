import request from '@/utils/request'

export const getAllCases = () => {
  return request.get('/cases')
}

export const getCasesByType = (type) => {
  return request.get('/cases', { params: { type } })
}

export const getCaseById = (id) => {
  return request.get(`/cases/${id}`)
}

export const addCase = (data) => {
  return request.post('/cases', data)
}

export const updateCase = (id, data) => {
  return request.put(`/cases/${id}`, data)
}

export const deleteCase = (id) => {
  return request.delete(`/cases/${id}`)
}

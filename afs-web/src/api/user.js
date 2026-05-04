import request from '@/utils/request'

export const login = (data) => {
  return request.post('/api/user/login', data)
}

export const register = (data) => {
  return request.post('/api/user/register', data)
}

export const getUserById = (id) => {
  return request.get(`/api/user/${id}`)
}

export const updateUser = (data) => {
  return request.put('/api/user/update', data)
}

export const getAllUsers = () => {
  return request.get('/api/user/list')
}

export const createUser = (data) => {
  return request.post('/api/user/create', data)
}

export const updateUserByAdmin = (id, data) => {
  return request.put(`/api/user/admin/update/${id}`, data)
}

export const deleteUser = (id) => {
  return request.delete(`/api/user/delete/${id}`)
}

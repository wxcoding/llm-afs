import request from '@/utils/request'

export const login = (data) => {
  return request.post('/user/login', data)
}

export const logout = () => {
  return request.post('/user/logout')
}

export const register = (data) => {
  return request.post('/user/register', data)
}

export const getUserById = (id) => {
  return request.get(`/user/${id}`)
}

export const updateUser = (data) => {
  return request.put('/user/update', data)
}

export const getAllUsers = () => {
  return request.get('/user/list')
}

export const createUser = (data) => {
  return request.post('/user/create', data)
}

export const updateUserByAdmin = (id, data) => {
  return request.put(`/user/admin/update/${id}`, data)
}

export const deleteUser = (id) => {
  return request.delete(`/user/delete/${id}`)
}

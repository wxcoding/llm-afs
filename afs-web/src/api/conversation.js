import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const addFavorite = (data) => {
  return request.post('/conversation/favorites', data)
}

export const removeFavorite = (favoriteId) => {
  return request.delete(`/conversation/favorites/${favoriteId}`)
}

export const getUserFavorites = (userId) => {
  return request.get('/conversation/favorites', { params: { userId } })
}

export const createTemplate = (data) => {
  return request.post('/conversation/templates', data)
}

export const getPublicTemplates = () => {
  return request.get('/conversation/templates/public')
}

export const getUserTemplates = (userId) => {
  return request.get('/conversation/templates/user', { params: { userId } })
}

export const useTemplate = (templateId) => {
  return request.post(`/conversation/templates/${templateId}/use`)
}

export const deleteTemplate = (templateId) => {
  return request.delete(`/conversation/templates/${templateId}`)
}

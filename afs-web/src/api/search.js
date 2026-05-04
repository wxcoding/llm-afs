import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const saveSearch = (data) => {
  return request.post('/search/history', null, { params: data })
}

export const getSearchHistory = (params) => {
  return request.get('/search/history', { params })
}

export const deleteSearchHistory = (userId) => {
  return request.delete('/search/history', { params: { userId } })
}

export const getHotSearch = (limit = 10) => {
  return request.get('/search/hot', { params: { limit } })
}

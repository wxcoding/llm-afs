import request from '@/utils/request'

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

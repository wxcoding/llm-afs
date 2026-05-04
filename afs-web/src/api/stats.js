import request from '@/utils/request'

export const getStats = () => {
  return request.get('/api/stats')
}

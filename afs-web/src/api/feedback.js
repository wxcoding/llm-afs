import request from '@/utils/request'

export const submitFeedback = (messageId, data) => {
  return request.post(`/feedback/message/${messageId}`, null, { params: data })
}

export const getFeedback = (messageId, userId) => {
  return request.get(`/feedback/message/${messageId}`, { params: { userId } })
}

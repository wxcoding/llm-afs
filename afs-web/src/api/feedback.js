import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export const submitFeedback = (messageId, data) => {
  return request.post(`/feedback/message/${messageId}`, null, { params: data })
}

export const getFeedback = (messageId, userId) => {
  return request.get(`/feedback/message/${messageId}`, { params: { userId } })
}

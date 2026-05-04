import request from '@/utils/request'

export const getChatSessions = (userId) => {
  return request.get(`/api/chat/sessions/${userId}`)
}

export const getSessionMessages = (sessionId) => {
  return request.get(`/api/chat/session/${sessionId}`)
}

export const sendMessage = (data) => {
  return request.post('/api/chat/send', data)
}

export const deleteSession = (sessionId) => {
  return request.delete(`/api/chat/session/${sessionId}`)
}

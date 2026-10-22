import request from '@/utils/request'

export const getChatSessions = () => {
  return request.get('/chat/sessions')
}

export const getSessionMessages = (sessionId) => {
  return request.get(`/chat/history/${sessionId}`)
}

export const sendMessage = (data) => {
  return request.post('/chat/send', data)
}

export const deleteSession = (sessionId) => {
  return request.delete(`/chat/session/${sessionId}`)
}

export const renameSession = (sessionId, title) => {
  return request.put(`/chat/session/${sessionId}`, { title })
}

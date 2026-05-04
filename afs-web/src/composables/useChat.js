import { ref, onUnmounted } from 'vue'

export const useChat = () => {
  const messages = ref([])
  const isTyping = ref(false)
  let eventSource = null
  
  const addMessage = (message) => {
    messages.value.push({
      ...message,
      id: Date.now(),
      timestamp: new Date().toISOString()
    })
  }
  
  const clearMessages = () => {
    messages.value = []
  }
  
  const connectSSE = (userId, sessionId, content, onMessage, onError, onError) => {
    isTyping.value = true
    
    const params = new URLSearchParams({
      userId,
      content
    })
    
    if (sessionId) params.append('sessionId', sessionId)
    
    eventSource = new EventSource(`/api/chat/stream?${params.toString()}`)
    
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        onMessage && onMessage(data)
      } catch (e) {
        console.error('SSE message error:', e)
      }
    }
    
    eventSource.onerror = (error) => {
      isTyping.value = false
      onError && onError(error)
      eventSource.close()
    }
  }
  
  const disconnectSSE = () => {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    isTyping.value = false
  }
  
  onUnmounted(() => {
    disconnectSSE()
  })
  
  return {
    messages,
    isTyping,
    addMessage,
    clearMessages,
    connectSSE,
    disconnectSSE
  }
}

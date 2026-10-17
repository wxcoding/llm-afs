<template>
  <div class="chat-page">
    <div class="chat-container">
      <SessionList
        :sessions="sessions"
        :current-session-id="sessionId"
        @select="selectSession"
        @delete="deleteSession"
        @new-chat="newChat"
      />

      <div class="chat-main">
        <div ref="messagesContainer" class="chat-messages">
          <QuickQuestions v-if="messages.length === 0" :questions="quickQuestions" @select="sendQuickQuestion" />

          <MessageBubble
            v-for="(msg, index) in messages"
            :key="index"
            :msg="msg"
            @toggle-sources="toggleSources(index)"
          />

          <div v-if="loading" class="message assistant">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
              <span class="loading-dots">正在思考中...</span>
            </div>
          </div>
        </div>

        <ChatInput v-model="inputMessage" :loading="loading" @send="sendMessage" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChatSessions, getSessionMessages, deleteSession as deleteSessionApi } from '@/api/chat'
import SessionList from '@/components/business/SessionList.vue'
import MessageBubble from '@/components/business/MessageBubble.vue'
import ChatInput from '@/components/business/ChatInput.vue'
import QuickQuestions from '@/components/business/QuickQuestions.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const sessionId = ref(null)
const sessions = ref([])
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

const quickQuestions = [
  '什么是电信诈骗？',
  '如何识别网络诈骗？',
  '遇到诈骗怎么办？',
  '有哪些常见诈骗手法？',
  '刷单返利是真的吗？',
  '如何保护个人信息？'
]

const loadSessions = async () => {
  if (!userStore.isLoggedIn) return
  const res = await getChatSessions(userStore.userId)
  if (res.success) {
    sessions.value = res.data || []
  }
}

const loadHistory = async (id) => {
  const res = await getSessionMessages(id)
  if (res.success) {
    messages.value = (res.data || []).map(m => ({
      ...m,
      sources: m.sources ? (typeof m.sources === 'string' ? JSON.parse(m.sources) : m.sources) : [],
      showSources: false
    }))
    scrollToBottom()
  }
}

const selectSession = (id) => {
  sessionId.value = id
  loadHistory(id)
}

const newChat = () => {
  sessionId.value = null
  messages.value = []
  inputMessage.value = ''
}

const deleteSession = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSessionApi(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (sessionId.value === id) {
      newChat()
    }
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const toggleSources = (index) => {
  messages.value[index].showSources = !messages.value[index].showSources
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const sendMessage = async (content) => {
  if (!content || loading.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true

  try {
    const params = {
      userId: userStore.userId,
      content: content,
      sessionId: sessionId.value
    }

    messages.value.push({ role: 'user', content })
    scrollToBottom()

    const assistantMessage = {
      role: 'assistant',
      content: '',
      sources: [],
      showSources: false
    }
    const assistantIndex = messages.value.length
    messages.value.push(assistantMessage)

    const response = await fetch('/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let sessionIdReceived = null
    let buffer = ''

    const handleLine = (line) => {
      // 兼容 "data: {...}"（手写）与 "data:{...}"（Spring SSE 输出，无空格）两种格式
      if (!line.startsWith('data:')) return
      const data = line.substring(5).trimStart()
      if (data === '[DONE]') return
      try {
        const json = JSON.parse(data)
        if (json.sessionId) {
          sessionIdReceived = json.sessionId
        }
        if (json.sources) {
          messages.value[assistantIndex].sources = json.sources
        }
        if (json.content) {
          messages.value[assistantIndex].content += json.content
          const container = messagesContainer.value
          if (container) {
            const isAtBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 20
            if (isAtBottom) {
              scrollToBottom()
            }
          }
        }
      } catch (e) {
        console.error('解析流式数据失败:', e)
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      // 最后一段可能被网络分片截断，留到下一个分片拼接
      buffer = lines.pop()

      for (const line of lines) {
        handleLine(line)
      }
    }

    if (buffer) {
      handleLine(buffer)
    }

    if (sessionIdReceived) {
      sessionId.value = sessionIdReceived
      await loadSessions()
    }
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    loading.value = false
  }
}

const sendQuickQuestion = (question) => {
  sendMessage(question)
}

onMounted(() => {
  if (route.params.sessionId) {
    sessionId.value = parseInt(route.params.sessionId)
    loadHistory(sessionId.value)
  }
  loadSessions()
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 140px);
}

.chat-container {
  display: flex;
  height: 100%;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #fafafa;
}

.loading-dots {
  display: inline-block;
  animation: dotPulse 1.5s infinite;
}

@keyframes dotPulse {
  0%, 60%, 100% {
    opacity: 0.3;
  }
  30% {
    opacity: 1;
  }
}

.message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message-avatar {
  font-size: 22px;
  margin-right: 10px;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  border-radius: 50%;
}

.message-content {
  background: white;
  padding: 10px 14px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}
</style>

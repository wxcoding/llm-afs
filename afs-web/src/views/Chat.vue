<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>对话历史</h3>
          <el-button type="primary" size="small" @click="newChat">新建对话</el-button>
        </div>
        <div class="session-list">
          <div 
            v-for="session in sessions" 
            :key="session.id"
            :class="['session-item', { active: sessionId === session.id }]"
            @click="selectSession(session.id)"
          >
            <span class="session-title">{{ session.title }}</span>
            <el-button 
              type="danger" 
              size="small" 
              text 
              @click.stop="deleteSession(session.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div class="chat-main">
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-tip">
            <h2>🛡️ 欢迎使用防诈骗问答助手</h2>
            <p>我可以帮您解答关于各类诈骗手法的问题，请直接输入您的问题开始咨询。</p>
            <div class="quick-questions">
              <el-tag 
                v-for="q in quickQuestions" 
                :key="q"
                @click="sendQuickQuestion(q)"
                class="quick-tag"
              >
                {{ q }}
              </el-tag>
            </div>
          </div>

          <div 
            v-for="(msg, index) in messages" 
            :key="index"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">
              {{ msg.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="message-content">
              <pre>{{ msg.content }}</pre>
            </div>
          </div>

          <div v-if="loading" class="message assistant">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
              <span class="loading-dots">正在思考中...</span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入您的问题..."
            @keydown.enter.ctrl="sendMessage"
            :disabled="loading"
          />
          <el-button 
            type="primary" 
            @click="sendMessage" 
            :loading="loading"
            :disabled="!inputMessage.trim()"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Chat',
  setup() {
    const route = useRoute()
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    
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
      '有哪些常见诈骗手法？'
    ]

    const loadSessions = async () => {
      if (!user) return
      const res = await axios.get(`/api/chat/sessions/${user.id}`)
      if (res.data.success) {
        sessions.value = res.data.sessions
      }
    }

    const loadHistory = async (id) => {
      const res = await axios.get(`/api/chat/history/${id}`)
      if (res.data.success) {
        messages.value = res.data.messages
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
      sessions.value = sessions.value.filter(s => s.id !== id)
      if (sessionId.value === id) {
        newChat()
      }
    }

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
      })
    }

    const sendMessage = async () => {
      if (!inputMessage.value.trim() || loading.value) return
      if (!user) {
        ElMessage.warning('请先登录')
        return
      }

      const content = inputMessage.value.trim()
      inputMessage.value = ''
      loading.value = true

      try {
        const params = {
          userId: user.id,
          content: content,
          sessionId: sessionId.value
        }
        
        // 先添加用户消息
        const userMessage = {
          role: 'user',
          content: content
        }
        messages.value.push(userMessage)
        scrollToBottom()
        
        // 创建助手消息容器
        const assistantMessage = {
          role: 'assistant',
          content: ''
        }
        const assistantIndex = messages.value.length
        messages.value.push(assistantMessage)
        
        // 使用fetch API实现流式接收
        const response = await fetch('/api/chat/send', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(params)
        })
        
        if (!response.ok) {
          throw new Error('请求失败')
        }
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let sessionIdReceived = null
        
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          const chunk = decoder.decode(value, { stream: true })
          // 解析服务器发送的事件
          const lines = chunk.split('\n')
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              if (data === '[DONE]') {
                break
              }
              
              try {
                const json = JSON.parse(data)
                if (json.sessionId) {
                  sessionIdReceived = json.sessionId
                }
                if (json.content) {
                  // 更新助手消息内容
                  messages.value[assistantIndex].content += json.content
                  // 检查用户是否正在拖动滚动条
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
          }
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
      inputMessage.value = question
      sendMessage()
    }

    onMounted(() => {
      if (route.params.sessionId) {
        sessionId.value = parseInt(route.params.sessionId)
        loadHistory(sessionId.value)
      }
      loadSessions()
    })

    return {
      sessionId,
      sessions,
      messages,
      inputMessage,
      loading,
      messagesContainer,
      quickQuestions,
      selectSession,
      newChat,
      deleteSession,
      sendMessage,
      sendQuickQuestion
    }
  }
}
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
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.chat-sidebar {
  width: 250px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.welcome-tip {
  text-align: center;
  padding: 40px;
}

.welcome-tip h2 {
  margin-bottom: 16px;
}

.welcome-tip p {
  color: #666;
  margin-bottom: 24px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.quick-tag {
  cursor: pointer;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.message.assistant .message-avatar {
  background: #e6f7ff;
}

.message-content {
  max-width: 70%;
  margin: 0 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f5f5;
}

.message.user .message-content {
  background: #409eff;
  color: white;
}

.message-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
  line-height: 1.6;
}

.loading-dots {
  color: #999;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 12px;
}

.chat-input .el-textarea {
  flex: 1;
}
</style>

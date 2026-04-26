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
            <p class="rag-tip">💡 本助手已接入知识库，回答将基于专业防诈骗知识进行检索增强</p>
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
            <div class="message-body">
              <div class="message-content">
                <pre>{{ msg.content }}</pre>
              </div>
              <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                <div class="sources-header" @click="toggleSources(index)">
                  <span>📚 参考来源 ({{ msg.sources.length }})</span>
                  <span class="sources-toggle">{{ msg.showSources ? '收起 ▲' : '展开 ▼' }}</span>
                </div>
                <div v-show="msg.showSources" class="sources-list">
                  <div v-for="(src, si) in msg.sources" :key="si" class="source-item">
                    <el-tag :type="src.source === 'knowledge' ? 'success' : 'warning'" size="small">
                      {{ src.source === 'knowledge' ? '知识库' : '案例库' }}
                    </el-tag>
                    <span class="source-title">{{ src.title || '无标题' }}</span>
                    <span class="source-score">{{ (src.score * 100).toFixed(1) }}%</span>
                  </div>
                </div>
              </div>
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
            placeholder="请输入您的问题... (Ctrl+Enter 发送)"
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
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

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
      '有哪些常见诈骗手法？',
      '刷单返利是真的吗？',
      '如何保护个人信息？'
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
        messages.value = res.data.messages.map(m => ({
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
        await axios.delete(`/api/chat/session/${id}`)
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

        const userMessage = {
          role: 'user',
          content: content
        }
        messages.value.push(userMessage)
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
      toggleSources,
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
  margin-bottom: 12px;
}

.rag-tip {
  color: #409eff !important;
  font-size: 14px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 12px;
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

.message-body {
  max-width: 70%;
  margin: 0 12px;
}

.message-content {
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

.message-sources {
  margin-top: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}

.sources-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
}

.sources-header:hover {
  background: #ecf5ff;
}

.sources-toggle {
  color: #409eff;
  font-size: 12px;
}

.sources-list {
  padding: 8px 12px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.source-item:last-child {
  border-bottom: none;
}

.source-title {
  flex: 1;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-score {
  color: #999;
  font-size: 12px;
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

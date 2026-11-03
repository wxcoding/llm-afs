<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <el-button class="new-chat-btn" @click="$emit('new-chat')">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>
        新对话
      </el-button>
    </div>
    <div class="session-list">
      <template v-for="group in groupedSessions" :key="group.label">
        <div class="group-label">{{ group.label }}</div>
        <div
          v-for="session in group.items"
          :key="session.id"
          :class="['session-item', { active: currentSessionId === session.id }]"
          @click="handleSelect(session)"
        >
          <!-- 内联编辑模式 -->
          <input
            v-if="editingId === session.id"
            ref="editInputRef"
            v-model="editingTitle"
            class="session-edit-input"
            @click.stop
            @blur="handleSaveEdit(session)"
            @keyup.enter="handleSaveEdit(session)"
            @keyup.esc="handleCancelEdit"
          />
          <!-- 普通显示模式 -->
          <template v-else>
            <el-icon class="session-icon"><ChatLineRound /></el-icon>
            <span class="session-title">{{ session.title }}</span>
            <div class="session-actions" @click.stop>
              <el-tooltip content="重命名" placement="top" :show-after="300">
                <span class="action-btn edit-btn" @click="handleStartEdit(session)">
                  <Edit />
                </span>
              </el-tooltip>
              <el-tooltip content="删除" placement="top" :show-after="300">
                <span class="action-btn delete-btn" @click="$emit('delete', session.id)">
                  <Delete />
                </span>
              </el-tooltip>
            </div>
          </template>
        </div>
      </template>
      <div v-if="sessions.length === 0" class="session-empty">
        暂无对话记录
      </div>
    </div>

    <div v-if="user" class="sidebar-footer">
      <el-dropdown trigger="click" placement="top-start" @command="handleUserCommand">
        <div class="user-chip">
          <el-avatar :size="28" :src="user.avatar || '/avatars/default.jpg'" />
          <span class="user-name">{{ user.nickname || user.username }}</span>
          <el-icon class="more-icon"><MoreFilled /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Delete, Edit, MoreFilled, ChatLineRound } from '@element-plus/icons-vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/user'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [Number, null], default: null }
})

const emit = defineEmits(['select', 'delete', 'new-chat', 'rename'])

const router = useRouter()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)

// 内联编辑状态
const editingId = ref(null)
const editingTitle = ref('')
const editInputRef = ref(null)

const handleSelect = (session) => {
  if (editingId.value === session.id) return
  emit('select', session.id)
}

const handleStartEdit = async (session) => {
  editingId.value = session.id
  editingTitle.value = session.title
  await nextTick()
  // 自动聚焦并全选文本
  const input = Array.isArray(editInputRef.value) ? editInputRef.value[0] : editInputRef.value
  if (input) {
    input.focus()
    input.select()
  }
}

const handleSaveEdit = (session) => {
  const newTitle = editingTitle.value.trim()
  if (newTitle && newTitle !== session.title) {
    emit('rename', { id: session.id, title: newTitle })
  }
  editingId.value = null
  editingTitle.value = ''
}

const handleCancelEdit = () => {
  editingId.value = null
  editingTitle.value = ''
}

// 按创建时间分组：今天 / 昨天 / 7 天内 / 更早
const groupedSessions = computed(() => {
  const groups = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '7 天内', items: [] },
    { label: '更早', items: [] }
  ]
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const DAY = 24 * 60 * 60 * 1000

  for (const s of props.sessions) {
    const t = s.createTime ? new Date(String(s.createTime).replace(' ', 'T')).getTime() : NaN
    if (Number.isNaN(t)) {
      groups[3].items.push(s)
    } else if (t >= startOfToday) {
      groups[0].items.push(s)
    } else if (t >= startOfToday - DAY) {
      groups[1].items.push(s)
    } else if (t >= startOfToday - 7 * DAY) {
      groups[2].items.push(s)
    } else {
      groups[3].items.push(s)
    }
  }
  return groups.filter(g => g.items.length > 0)
})

const handleUserCommand = async (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略：网络异常或令牌已过期时仍需清理本地登录态
    }
    userStore.clearUser()
    router.push('/login')
  }
}
</script>

<style scoped>
.chat-sidebar {
  width: 240px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.new-chat-btn {
  width: 100%;
  color: #409eff;
  border-color: #d9ecff;
  background: #ecf5ff;
  font-weight: 500;
}

.new-chat-btn:hover {
  color: #ffffff;
  background: #409eff;
  border-color: #409eff;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.group-label {
  font-size: 12px;
  color: #909399;
  padding: 8px 12px 4px;
}

.session-item {
  padding: 10px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px;
  transition: background-color 0.2s;
  gap: 8px;
  min-height: 40px;
  box-sizing: border-box;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.session-item.active .session-title {
  color: #409eff;
  font-weight: 500;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
  min-width: 0;
}

.session-icon {
  flex-shrink: 0;
  font-size: 15px;
  color: #c0c4cc;
}

.session-item.active .session-icon {
  color: #409eff;
}

.session-edit-input {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  padding: 4px 8px;
  border: 1px solid #409eff;
  border-radius: 4px;
  outline: none;
  color: #303133;
  background: #ffffff;
  font-family: inherit;
}

.session-edit-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.session-actions {
  display: none;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.session-item:hover .session-actions,
.session-item.active .session-actions {
  display: flex;
}

.action-btn {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer;
  color: #909399;
  transition: all 0.2s;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.action-btn:hover {
  background: #ffffff;
  color: #409eff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.delete-btn:hover {
  color: #f56c6c;
  background: #fef0f0;
}

.session-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

/* 底部用户区 */
.sidebar-footer {
  border-top: 1px solid #f0f0f0;
  padding: 8px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  outline: none;
}

.user-chip:hover {
  background: #f5f7fa;
}

.user-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-icon {
  color: #909399;
  flex-shrink: 0;
}
</style>

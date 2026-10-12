<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <h3>对话历史</h3>
      <el-button type="primary" size="small" @click="$emit('new-chat')">新建对话</el-button>
    </div>
    <div class="session-list">
      <div
        v-for="session in sessions"
        :key="session.id"
        :class="['session-item', { active: currentSessionId === session.id }]"
        @click="$emit('select', session.id)"
      >
        <span class="session-title">{{ session.title }}</span>
        <el-button type="danger" size="small" text @click.stop="$emit('delete', session.id)">删除</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [Number, null], default: null }
})

defineEmits(['select', 'delete', 'new-chat'])
</script>

<style scoped>
.chat-sidebar {
  width: 250px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 15px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #e8f4fd;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #303133;
}
</style>

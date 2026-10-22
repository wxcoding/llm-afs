<template>
  <div :class="['message', msg.role]">
    <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
    <div class="message-body">
      <div class="message-content">
        <pre v-if="msg.content">{{ msg.content }}</pre>
      </div>
      <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
        <div class="sources-header" @click="$emit('toggle-sources')">
          <span class="sources-title">
            <i class="el-icon-collection"></i>
            参考来源 ({{ msg.sources.length }})
          </span>
          <span class="sources-toggle">
            {{ msg.showSources ? '收起' : '展开' }}
            <i :class="msg.showSources ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
          </span>
        </div>
        <div v-show="msg.showSources" class="sources-list">
          <div v-for="(src, si) in msg.sources" :key="si" class="source-item">
            <el-tag :type="src.type === 'knowledge' ? 'success' : 'warning'" size="small">
              {{ src.type === 'knowledge' ? '知识库' : '案例库' }}
            </el-tag>
            <span class="source-title">{{ src.title || '无标题' }}</span>
            <span class="source-score">{{ (src.score * 100).toFixed(1) }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  msg: { type: Object, required: true }
})

defineEmits(['toggle-sources'])
</script>

<style scoped>
.message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
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

.message.user .message-avatar {
  margin-right: 0;
  margin-left: 10px;
  background: #e6f7ff;
}

.message-body {
  flex: 1;
  min-width: 0;
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message.user .message-body {
  align-items: flex-end;
}

.message-content {
  background: white;
  padding: 10px 14px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.message-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  font-size: 14px;
  line-height: 1.7;
  color: #262626;
  font-weight: 400;
}

.message.assistant .message-content {
  background: #ffffff;
  border-color: #e8e8e8;
}

.message.user .message-content {
  background: #1890ff;
  border-color: #1890ff;
}

.message.user .message-content pre {
  color: #ffffff;
}

.message-sources {
  margin-top: 8px;
  width: 100%;
}

.sources-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  font-size: 13px;
  color: #409eff;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.sources-header:hover {
  background: #ecf5ff;
}

.sources-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.sources-title i {
  font-size: 14px;
}

.sources-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.sources-toggle i {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.sources-list {
  margin-top: 8px;
  padding-left: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

.source-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}

.source-score {
  font-size: 11px;
  color: #909399;
}
</style>

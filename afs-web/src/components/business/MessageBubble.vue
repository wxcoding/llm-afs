<template>
  <div :class="['message', msg.role]">
    <div class="message-avatar">
      <ShieldIcon v-if="msg.role !== 'user'" :size="18" />
      <el-icon v-else :size="16"><User /></el-icon>
    </div>
    <div class="message-body">
      <div class="message-content">
        <pre v-if="msg.content">{{ msg.content }}</pre>
      </div>
      <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
        <div class="sources-header" @click="$emit('toggle-sources')">
          <span class="sources-title">
            <el-icon :size="14"><Collection /></el-icon>
            参考来源 ({{ msg.sources.length }})
          </span>
          <el-icon :size="12" class="sources-arrow">
            <ArrowUp v-if="msg.showSources" />
            <ArrowDown v-else />
          </el-icon>
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
import { User, Collection, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import ShieldIcon from '@/components/common/ShieldIcon.vue'

defineProps({
  msg: { type: Object, required: true }
})

defineEmits(['toggle-sources'])
</script>

<style scoped>
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.message.assistant .message-avatar {
  background: #ecf5ff;
  color: #409eff;
}

.message.user .message-avatar {
  background: #f0f2f5;
  color: #909399;
}

.message-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.message.user .message-body {
  align-items: flex-end;
}

/* AI 消息：无气泡，正文直接排版 */
.message.assistant .message-content {
  padding: 4px 0;
}

/* 用户消息：品牌蓝气泡右对齐 */
.message.user .message-content {
  background: #409eff;
  border-radius: 16px;
  padding: 10px 16px;
  max-width: 70%;
}

.message-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  font-weight: 400;
}

.message.user .message-content pre {
  color: #ffffff;
}

.message-sources {
  margin-top: 8px;
  width: 100%;
}

.sources-header {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #409eff;
  padding: 4px 10px;
  border-radius: 6px;
  transition: background-color 0.2s ease;
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

.sources-arrow {
  color: #a0cfff;
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

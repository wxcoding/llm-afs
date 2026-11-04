<template>
  <div :class="['message', msg.role]">
    <div class="message-avatar">
      <ShieldIcon v-if="msg.role !== 'user'" :size="22" />
      <el-avatar v-else :size="36" :src="userAvatar" class="user-avatar">{{ userInitial }}</el-avatar>
    </div>
    <div class="message-body">
      <div v-if="msg.content" class="message-content">
        <div class="markdown-body" v-html="renderedContent"></div>
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
            <span v-if="src.score >= 0" class="source-score">{{ (src.score * 100).toFixed(1) }}%</span>
            <span v-else class="source-score">—</span>
          </div>
        </div>
      </div>
      <div v-else-if="msg.noEvidence && !msg.degraded" class="message-no-evidence">
        <el-icon :size="13"><InfoFilled /></el-icon>
        本次回答未引用知识库内容
      </div>
      <div v-if="msg.degraded" class="message-degraded">
        <el-icon :size="13"><WarningFilled /></el-icon>
        {{ msg.sources && msg.sources.length > 0 ? '以上为知识库原文摘录，未经模型总结' : '回答服务暂时异常，请稍后重试' }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Collection, ArrowUp, ArrowDown, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import ShieldIcon from '@/components/common/ShieldIcon.vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  msg: { type: Object, required: true }
})

defineEmits(['toggle-sources'])

const userStore = useUserStore()
// 用户头像优先取个人资料，未设置或加载失败时回退到默认头像 / 昵称首字
const userAvatar = computed(() => userStore.user?.avatar || '/avatars/default.jpg')
const userInitial = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

// 关闭 html 保留原始 HTML 的解析，从源头规避 XSS；链接统一新窗口打开
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const defaultLinkOpen = md.renderer.rules.link_open
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, idx, options, env, self)
    : self.renderToken(tokens, idx, options)
}

const renderedContent = computed(() => md.render(props.msg.content || ''))
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
  width: 36px;
  height: 36px;
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
  background: transparent;
}

.user-avatar {
  background: #409eff;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
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

.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  font-weight: 400;
  word-break: break-word;
}

.markdown-body :deep(p) {
  margin: 0 0 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ol),
.markdown-body :deep(ul) {
  margin: 0 0 8px;
  padding-left: 20px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  padding: 10px 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0 0 8px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(a) {
  color: #409eff;
  text-decoration: none;
  border-bottom: 1px solid rgba(64, 158, 255, 0.4);
}

.message.user .markdown-body {
  color: #ffffff;
}

.message.user .markdown-body :deep(a) {
  color: #ffffff;
  border-bottom-color: rgba(255, 255, 255, 0.6);
}

.message.user .markdown-body :deep(code) {
  background: rgba(255, 255, 255, 0.2);
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

.message-no-evidence {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.message-degraded {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #fdf6ec;
  font-size: 12px;
  color: #e6a23c;
}
</style>

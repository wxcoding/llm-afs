<template>
  <div
    class="knowledge-item"
    :class="{ expanded: expanded }"
  >
    <div class="knowledge-item-header" @click="$emit('toggle')">
      <div class="knowledge-item-left">
        <el-tag :type="categoryTagType" size="small">{{ item.category }}</el-tag>
        <h3 class="knowledge-item-title">{{ item.title }}</h3>
        <el-tag :type="statusTagType" size="small">{{ displayStatus }}</el-tag>
        <el-tag v-if="isAdmin && item.vectorStatus" :type="vectorStatusTagType" size="small" effect="plain">
          {{ vectorStatusText }}
        </el-tag>
      </div>
      <div class="knowledge-item-right">
        <template v-if="isAdmin">
          <el-button type="primary" size="small" @click.stop="$emit('edit', item)">编辑</el-button>
          <el-button type="danger" size="small" @click.stop="$emit('delete', item.id)">删除</el-button>
        </template>
        <i :class="expanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="expand-icon"></i>
      </div>
    </div>
    <div v-show="!expanded" class="knowledge-item-summary">
      {{ summary }}
    </div>
    <div v-show="expanded" class="knowledge-item-content">
      <pre>{{ item.content }}</pre>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  item: { type: Object, required: true },
  expanded: { type: Boolean, default: false },
  statusText: { type: String, default: '' }
})

defineEmits(['toggle', 'edit', 'delete'])

const isAdmin = computed(() => useUserStore().isAdmin)

const summary = computed(() => {
  if (!props.item.content) return ''
  return props.item.content.length > 150
    ? props.item.content.substring(0, 150) + '...'
    : props.item.content
})

const categoryTagType = computed(() => {
  const map = {
    '防范技巧': 'success',
    '诈骗类型': 'danger',
    '应对方法': 'warning',
    '文档资料': 'info'
  }
  return map[props.item.category] || ''
})

const statusTagType = computed(() => {
  const map = {
    'DRAFT': 'info',
    'PENDING_REVIEW': 'warning',
    'ACTIVE': 'success',
    'REJECTED': 'danger'
  }
  return map[props.item.status] || 'success'
})

const displayStatus = computed(() => {
  return props.statusText || '已发布'
})

// 向量化处理状态（管理员可见）：异步入库任务的进度反馈
const vectorStatusTagType = computed(() => {
  const map = {
    'PENDING': 'info',
    'PROCESSING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger'
  }
  return map[props.item.vectorStatus] || 'info'
})

const vectorStatusText = computed(() => {
  const map = {
    'PENDING': '待向量化',
    'PROCESSING': '向量化中',
    'SUCCESS': '已索引',
    'FAILED': '索引失败'
  }
  return map[props.item.vectorStatus] || props.item.vectorStatus
})
</script>

<style scoped>
.knowledge-item {
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
  overflow: hidden;
}

.knowledge-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.knowledge-item.expanded {
  border-color: #409eff;
}

.knowledge-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s ease;
}

.knowledge-item-header:hover {
  background: #f0f7ff;
}

.knowledge-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.knowledge-item-title {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.expand-icon {
  font-size: 14px;
  color: #909399;
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.knowledge-item-summary {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.knowledge-item-content {
  padding: 16px;
  background: white;
  border-top: 1px solid #f0f0f0;
}

.knowledge-item-content pre {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #262626;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}
</style>

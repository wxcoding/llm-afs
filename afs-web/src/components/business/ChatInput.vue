<template>
  <div class="chat-input">
    <div class="input-card" :class="{ 'is-disabled': loading }">
      <el-input
        v-model="inputValue"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 6 }"
        resize="none"
        placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
        :disabled="loading"
        @keydown.enter.exact.prevent="handleSend"
      />
      <div class="input-bar">
        <span class="input-tip">基于防诈知识库检索增强</span>
        <button
          class="send-btn"
          :disabled="!inputValue.trim() || loading"
          @click="handleSend"
        >
          <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
          <el-icon v-else :size="16"><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Loading, Promotion } from '@element-plus/icons-vue'

const props = defineProps({
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])

const inputValue = ref('')

const handleSend = () => {
  if (!inputValue.value.trim() || props.loading) return
  emit('send', inputValue.value.trim())
  inputValue.value = ''
}
</script>

<style scoped>
.chat-input {
  padding: 12px 24px 20px;
  background: #ffffff;
}

.input-card {
  max-width: 800px;
  margin: 0 auto;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  background: #ffffff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-card:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.input-card.is-disabled {
  background: #fafafa;
}

.input-card :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 14px 16px 4px;
  font-size: 14px;
  line-height: 1.6;
  background: transparent;
}

.input-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px 10px;
}

.input-tip {
  font-size: 12px;
  color: #c0c4cc;
}

.send-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #409eff;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.send-btn:disabled {
  background: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
}
</style>

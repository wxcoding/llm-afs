<template>
  <div class="chat-input">
    <div class="input-row">
      <el-input
        v-model="inputValue"
        type="textarea"
        :rows="3"
        placeholder="请输入您的问题... (Ctrl+Enter 发送)"
        :disabled="loading"
        @keydown.enter.ctrl="handleSend"
      />
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!inputValue.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

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
  padding: 12px 16px;
  border-top: 1px solid #eee;
  background: white;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-row .el-input {
  flex: 1;
}
</style>

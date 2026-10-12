<template>
  <div class="chat-input">
    <el-input
      v-model="inputValue"
      type="textarea"
      :rows="3"
      placeholder="请输入您的问题... (Ctrl+Enter 发送)"
      @keydown.enter.ctrl="handleSend"
      :disabled="loading"
    />
    <el-button
      type="primary"
      @click="handleSend"
      :loading="loading"
      :disabled="!inputValue.trim()"
      class="send-btn"
    >
      发送
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'send'])

const inputValue = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  inputValue.value = val
})

watch(inputValue, (val) => {
  emit('update:modelValue', val)
})

const handleSend = () => {
  if (!inputValue.value.trim() || props.loading) return
  emit('send', inputValue.value.trim())
  inputValue.value = ''
}
</script>

<style scoped>
.chat-input {
  padding: 15px;
  border-top: 1px solid #eee;
  background: white;
  position: relative;
}

.chat-input .el-input {
  margin-bottom: 10px;
}

.send-btn {
  position: absolute;
  right: 25px;
  bottom: 25px;
  width: auto;
}
</style>

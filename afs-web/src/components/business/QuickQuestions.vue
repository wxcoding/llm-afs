<template>
  <div class="welcome-tip">
    <h2>🛡️ 欢迎使用防诈骗问答助手</h2>
    <p>我可以帮您解答关于各类诈骗手法的问题，请直接输入您的问题开始咨询。</p>
    <p class="rag-tip">💡 本助手已接入知识库，回答将基于专业防诈骗知识进行检索增强</p>
    <div class="quick-questions">
      <el-tag
        v-for="q in questions"
        :key="q"
        class="quick-tag"
        @click="$emit('select', q)"
      >
        {{ q }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
defineProps({
  questions: { type: Array, default: () => [] }
})

defineEmits(['select'])
</script>

<style scoped>
.welcome-tip {
  text-align: center;
  padding: 60px 20px;
}

.welcome-tip h2 {
  font-size: 24px;
  margin-bottom: 16px;
  color: #303133;
}

.welcome-tip p {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.rag-tip {
  color: #409eff !important;
}

.quick-questions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  transform: translateY(-2px);
}
</style>

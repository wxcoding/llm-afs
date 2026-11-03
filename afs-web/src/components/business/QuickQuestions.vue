<template>
  <div class="welcome-tip">
    <div class="welcome-hero">
      <h2 class="welcome-title">
        <ShieldIcon :size="24" class="title-icon" />
        防诈骗问答助手
      </h2>
      <p class="welcome-sub">基于专业反诈知识库，为你解答各类诈骗手法与应对方法</p>
    </div>

    <div class="urgent-banner">
      <el-icon :size="18" class="urgent-icon"><WarningFilled /></el-icon>
      <div class="urgent-text">
        <div class="urgent-title">正在遭遇诈骗？</div>
        <div class="urgent-sub">立即停止转账并保存证据，拨打反诈专线</div>
      </div>
      <span class="urgent-num">96110</span>
    </div>

    <div class="question-groups">
      <div v-for="group in groups" :key="group.label" class="question-group">
        <div class="group-label">{{ group.label }}</div>
        <div class="group-cards">
          <div
            v-for="q in group.questions"
            :key="q"
            class="q-card"
            @click="$emit('select', q)"
          >
            <el-icon :size="14" class="q-icon"><component :is="group.icon" /></el-icon>
            <span class="q-text">{{ q }}</span>
            <el-icon class="q-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { WarningFilled, ArrowRight } from '@element-plus/icons-vue'
import ShieldIcon from '@/components/common/ShieldIcon.vue'

defineProps({
  groups: { type: Array, default: () => [] }
})

defineEmits(['select'])
</script>

<style scoped>
.welcome-tip {
  max-width: 640px;
  margin: 0 auto;
  padding: 48px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* 顶部品牌区 */
.welcome-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.welcome-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.title-icon {
  color: #409eff;
}

.welcome-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 紧急求助横幅 */
.urgent-banner {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #f56c6c;
  border-radius: 8px;
  background: #fef0f0;
}

.urgent-icon {
  color: #f56c6c;
  flex-shrink: 0;
}

.urgent-text {
  flex: 1;
  min-width: 0;
}

.urgent-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.urgent-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.urgent-num {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
  white-space: nowrap;
}

/* 分组快捷问题 */
.question-groups {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-label {
  font-size: 12px;
  font-weight: 500;
  color: #909399;
  margin-bottom: 8px;
}

.group-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.q-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
  transition: all 0.2s;
}

.q-card:hover {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}

.q-icon {
  color: #409eff;
  flex-shrink: 0;
}

.q-arrow {
  color: #c0c4cc;
  font-size: 12px;
  flex-shrink: 0;
  transition: color 0.2s;
}

.q-card:hover .q-arrow {
  color: #409eff;
}
</style>

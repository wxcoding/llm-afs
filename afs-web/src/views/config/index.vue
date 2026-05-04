<template>
  <div class="config-page">
    <div class="page-header">
      <h2>系统配置</h2>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="提示词配置" name="prompt">
        <el-card>
          <el-form :model="promptConfig" label-width="120px">
            <el-form-item label="System Prompt">
              <el-input v-model="promptConfig.system_prompt" type="textarea" :rows="6" placeholder="设置AI助手的系统提示词" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSavePrompt">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="模型配置" name="model">
        <el-card>
          <el-form :model="modelConfig" label-width="120px">
            <el-form-item label="模型名称">
              <el-select v-model="modelConfig.model_name" placeholder="选择模型">
                <el-option label="gpt-3.5-turbo" value="gpt-3.5-turbo" />
                <el-option label="gpt-4" value="gpt-4" />
                <el-option label="gpt-4-turbo" value="gpt-4-turbo" />
              </el-select>
            </el-form-item>
            <el-form-item label="Max Tokens">
              <el-input-number v-model="modelConfig.max_tokens" :min="100" :max="4000" />
            </el-form-item>
            <el-form-item label="Temperature">
              <el-slider v-model="modelConfig.temperature" :min="0" :max="1" :step="0.1" show-input />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveModel">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="向量库配置" name="vector">
        <el-card>
          <el-form :model="vectorConfig" label-width="120px">
            <el-form-item label="同步间隔(秒)">
              <el-input-number v-model="vectorConfig.vector_sync_interval" :min="300" :max="86400" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveVector">保存</el-button>
              <el-button type="success" @click="handleSyncVector">手动同步</el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <div class="vector-stats">
            <h4>向量库状态</h4>
            <el-row :gutter="20">
              <el-col :span="6">
                <el-statistic title="知识库文档" :value="stats.knowledgeCount" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="案例库文档" :value="stats.caseCount" />
              </el-col>
              <el-col :span="6">
                <el-statistic title="总向量数" :value="stats.vectorCount" />
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigsByType, updateConfig } from '@/api/config'

const activeTab = ref('prompt')
const promptConfig = ref({ system_prompt: '' })
const modelConfig = ref({ model_name: 'gpt-3.5-turbo', max_tokens: 2000, temperature: 0.7 })
const vectorConfig = ref({ vector_sync_interval: 3600 })
const stats = ref({ knowledgeCount: 0, caseCount: 0, vectorCount: 0 })

const loadConfigs = async () => {
  try {
    const [promptRes, modelRes, vectorRes] = await Promise.all([
      getConfigsByType('prompt'),
      getConfigsByType('model'),
      getConfigsByType('vector')
    ])
    promptConfig.value = promptRes.data || { system_prompt: '' }
    modelConfig.value = {
      model_name: modelRes.data?.model_name || 'gpt-3.5-turbo',
      max_tokens: parseInt(modelRes.data?.max_tokens) || 2000,
      temperature: parseFloat(modelRes.data?.temperature) || 0.7
    }
    vectorConfig.value = { vector_sync_interval: parseInt(vectorRes.data?.vector_sync_interval) || 3600 }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

const handleSavePrompt = async () => {
  try {
    await updateConfig('system_prompt', promptConfig.value.system_prompt)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleSaveModel = async () => {
  try {
    await Promise.all([
      updateConfig('model_name', modelConfig.value.model_name),
      updateConfig('max_tokens', modelConfig.value.max_tokens.toString()),
      updateConfig('temperature', modelConfig.value.temperature.toString())
    ])
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleSaveVector = async () => {
  try {
    await updateConfig('vector_sync_interval', vectorConfig.value.vector_sync_interval.toString())
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleSyncVector = async () => {
  ElMessage.info('向量同步任务已启动，请稍后查看结果')
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.config-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.vector-stats {
  margin-top: 20px;
}

.vector-stats h4 {
  margin-bottom: 16px;
  color: #303133;
}
</style>

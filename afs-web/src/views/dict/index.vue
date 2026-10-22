<template>
  <div class="dict-container">
    <el-card class="dict-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="5">
          <div class="type-list">
            <div class="type-list-header">
              <span class="type-list-title">字典类型</span>
              <div class="type-list-actions">
                <el-button type="primary" size="small" text @click="handleAddType">
                  <el-icon style="margin-right: 2px"><Plus /></el-icon>新增
                </el-button>
                <el-button size="small" text @click="handleRefresh">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="type-list-content">
              <div
                v-for="type in dictTypes"
                :key="type.id"
                class="type-item"
                :class="{ active: currentType?.id === type.id }"
                @click="selectType(type)"
              >
                <div class="type-name">{{ type.dictName }}</div>
                <div v-if="type.dictCode" class="type-code">{{ type.dictCode }}</div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="19">
          <div class="item-list">
            <div class="item-list-header">
              <div class="item-list-title">
                <span class="title">{{ currentType ? currentType.dictName : '请选择字典类型' }}</span>
                <span v-if="currentType" class="code">({{ currentType.dictCode }})</span>
              </div>
              <el-button type="primary" size="small" :disabled="!currentType" @click="handleAddItem">新增字典项</el-button>
            </div>

            <el-table v-loading="itemLoading" :data="dictItems" stripe style="width: 100%">
              <el-table-column prop="itemCode" label="编码" width="150"></el-table-column>
              <el-table-column prop="itemName" label="名称" width="150"></el-table-column>
              <el-table-column prop="itemValue" label="值" width="150"></el-table-column>
              <el-table-column prop="sort" label="排序" width="80"></el-table-column>
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" show-overflow-tooltip></el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handleEditItem(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click="handleDeleteItem(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 字典类型对话框 -->
    <el-dialog v-model="typeDialogVisible" :title="isEditType ? '编辑字典类型' : '新增字典类型'" width="500px">
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="typeForm.dictCode" :disabled="isEditType" placeholder="请输入字典编码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典项对话框 -->
    <el-dialog v-model="itemDialogVisible" :title="isEditItem ? '编辑字典项' : '新增字典项'" width="500px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="itemForm.itemCode" :disabled="isEditItem" placeholder="请输入字典项编码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="itemForm.itemName" placeholder="请输入字典项名称" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="itemForm.itemValue" placeholder="请输入字典项值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import request from '@/utils/request'
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'

const dictTypes = ref([])
const dictItems = ref([])
const currentType = ref(null)
const typeDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const isEditType = ref(false)
const isEditItem = ref(false)
const saving = ref(false)
const itemLoading = ref(false)

const typeForm = ref({
  dictCode: '',
  dictName: '',
  description: '',
  status: 1
})

const itemForm = ref({
  itemCode: '',
  itemName: '',
  itemValue: '',
  sort: 0,
  status: 1,
  remark: ''
})

const loadDictTypes = async () => {
  try {
    const res = await request.get('/sys/dict/types')
    if (res.success) {
      dictTypes.value = res.data
      if (dictTypes.value.length > 0 && !currentType.value) {
        currentType.value = dictTypes.value[0]
        loadDictItems()
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载字典类型失败')
  }
}

const loadDictItems = async () => {
  if (!currentType.value) return
  itemLoading.value = true
  try {
    const res = await request.get(`/sys/dict/${currentType.value.dictCode}`)
    if (res.success) {
      dictItems.value = res.data
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载字典项失败')
  } finally {
    itemLoading.value = false
  }
}

const handleAddType = () => {
  isEditType.value = false
  typeForm.value = {
    dictCode: '',
    dictName: '',
    description: '',
    status: 1
  }
  typeDialogVisible.value = true
}

const handleAddItem = () => {
  if (!currentType.value) return
  isEditItem.value = false
  itemForm.value = {
    itemCode: '',
    itemName: '',
    itemValue: '',
    sort: 0,
    status: 1,
    remark: ''
  }
  itemDialogVisible.value = true
}

const handleEditItem = (row) => {
  isEditItem.value = true
  itemForm.value = { ...row, id: row.id }
  itemDialogVisible.value = true
}

const handleDeleteItem = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个字典项吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/sys/dict/item/${row.id}`)
    ElMessage.success('删除成功')
    loadDictItems()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSaveType = async () => {
  if (!typeForm.value.dictCode || !typeForm.value.dictName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  saving.value = true
  try {
    if (isEditType.value) {
      await request.put(`/sys/dict/type/${typeForm.value.id}`, typeForm.value)
    } else {
      await request.post('/sys/dict/type', typeForm.value)
    }
    ElMessage.success('保存成功')
    typeDialogVisible.value = false
    loadDictTypes()
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleSaveItem = async () => {
  if (!itemForm.value.itemCode || !itemForm.value.itemName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  saving.value = true
  try {
    const data = {
      ...itemForm.value,
      dictCode: currentType.value.dictCode
    }
    if (isEditItem.value) {
      await request.put(`/sys/dict/item/${itemForm.value.id}`, data)
    } else {
      await request.post('/sys/dict/item', data)
    }
    ElMessage.success('保存成功')
    itemDialogVisible.value = false
    loadDictItems()
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const selectType = (type) => {
  currentType.value = type
  loadDictItems()
}

const handleRefresh = () => {
  loadDictTypes()
}

onMounted(() => {
  loadDictTypes()
})
</script>

<style scoped>
.dict-container {
  padding: 0;
}

.type-list {
  border-right: 1px solid #ebeef5;
  height: calc(100vh - 240px);
  display: flex;
  flex-direction: column;
}

.type-list-header {
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.type-list-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.type-list-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.type-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.type-item {
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
  border: 1px solid transparent;
}

.type-item:hover {
  background: #f5f7fa;
}

.type-item.active {
  background: #ecf5ff;
  border-color: #d9ecff;
  color: #409eff;
}

.type-item.active .type-name {
  font-weight: 600;
}

.type-item.active .type-code {
  color: #79bbff;
}

.type-name {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
}

.type-code {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.item-list {
  height: calc(100vh - 240px);
  display: flex;
  flex-direction: column;
}

.item-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.item-list-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-list-header .title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.item-list-header .code {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 2px;
}

.item-list :deep(.el-table) {
  flex: 1;
}

:deep(.el-table__body-wrapper) {
  max-height: calc(100vh - 360px);
  overflow-y: auto;
}
</style>

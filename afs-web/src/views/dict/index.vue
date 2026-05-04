<template>
  <div class="dict-container">
    <el-card class="dict-card">
      <template #header>
        <div class="card-header">
          <span class="title">数据字典</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAddType">新增字典类型</el-button>
            <el-button @click="handleRefresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="5">
          <div class="type-list">
            <div class="type-list-header">
              <span class="type-list-title">字典类型</span>
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
                <div class="type-code" v-if="type.dictCode">{{ type.dictCode }}</div>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="19">
          <div class="item-list">
            <div class="item-list-header">
              <div class="item-list-title">
                <span class="title">{{ currentType ? currentType.dictName : '请选择字典类型' }}</span>
                <span class="code" v-if="currentType">({{ currentType.dictCode }})</span>
              </div>
              <el-button type="primary" size="small" @click="handleAddItem" :disabled="!currentType">新增字典项</el-button>
            </div>

            <el-table :data="dictItems" stripe style="width: 100%" v-loading="itemLoading">
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
        <el-button type="primary" @click="handleSaveType" :loading="saving">保存</el-button>
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
        <el-button type="primary" @click="handleSaveItem" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'Dict',
  setup() {
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
        const res = await axios.get('/api/sys/dict/types')
        if (res.data.success) {
          dictTypes.value = res.data.data
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
        const res = await axios.get(`/api/sys/dict/${currentType.value.dictCode}`)
        if (res.data.success) {
          dictItems.value = res.data.data
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
        await axios.delete(`/api/sys/dict/item/${row.id}`)
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
          await axios.put(`/api/sys/dict/type/${typeForm.value.id}`, typeForm.value)
        } else {
          await axios.post('/api/sys/dict/type', typeForm.value)
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
          await axios.put(`/api/sys/dict/item/${itemForm.value.id}`, data)
        } else {
          await axios.post('/api/sys/dict/item', data)
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

    return {
      dictTypes,
      dictItems,
      currentType,
      typeDialogVisible,
      itemDialogVisible,
      isEditType,
      isEditItem,
      saving,
      itemLoading,
      typeForm,
      itemForm,
      loadDictItems,
      handleAddType,
      handleAddItem,
      handleEditItem,
      handleDeleteItem,
      handleSaveType,
      handleSaveItem,
      handleRefresh,
      selectType
    }
  }
}
</script>

<style scoped>
.dict-container {
  padding: 20px;
}

.dict-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.type-list {
  border-right: 1px solid #e5e7eb;
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

.type-list-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  background: #f9fafb;
  flex-shrink: 0;
}

.type-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.type-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.type-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.type-item:hover {
  background: #f3f4f6;
}

.type-item.active {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.type-item.active .type-name {
  font-weight: 600;
}

.type-item.active .type-code {
  color: #60a5fa;
}

.type-name {
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
}

.type-code {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.item-list {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

.item-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.item-list-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-list-header .title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.item-list-header .code {
  font-size: 13px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.item-list :deep(.el-table) {
  flex: 1;
}

:deep(.el-table__body-wrapper) {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}
</style>

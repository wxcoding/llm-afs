<template>
  <div class="user-management">
    <template v-if="user">
      <el-config-provider :locale="zhCn">
        <div class="filter-card">
          <el-input
            v-model="searchText"
            placeholder="搜索用户名、昵称、手机号或邮箱"
            clearable
            style="width: 350px"
            @clear="loadUsers"
            @keyup.enter="searchUsers"
          >
            <template #append>
              <el-button @click="searchUsers">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon style="margin-right: 4px"><Plus /></el-icon>添加用户
          </el-button>
        </div>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="loading" :data="filteredUsers" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="头像" width="80">
              <template #default="{ row }">
                <el-avatar :size="36" :src="row.avatar || defaultAvatar">
                  {{ row.nickname ? row.nickname[0] : row.username[0] }}
                </el-avatar>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
                  {{ row.role === 'admin' ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="昵称" width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="email" label="邮箱" width="200" />
            <el-table-column prop="createTime" label="创建时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="170">
              <template #default="{ row }">
                {{ formatTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="160">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="showEditDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :pager-count="5"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-config-provider>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
        <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" :disabled="isEdit" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-else>
      <el-card shadow="never" class="not-logged-in-card">
        <el-empty description="请先登录后再访问用户管理页面">
          <el-button type="primary" @click="$router.push('/login')">立即登录</el-button>
        </el-empty>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getAllUsers, createUser, updateUserByAdmin, deleteUser } from '@/api/user'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
const { user } = storeToRefs(useUserStore())
const loading = ref(false)
const users = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const isEdit = ref(false)
const formRef = ref(null)
const defaultAvatar = '/avatars/default.jpg'

const formData = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 'user'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: false, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const filteredUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return users.value.slice(start, end)
})

const loadUsers = async () => {
  if (!user.value) {
    return
  }
  loading.value = true
  try {
    const res = await getAllUsers()
    if (res.success) {
      users.value = res.data || []
      total.value = users.value.length
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const searchUsers = () => {
  if (!searchText.value.trim()) {
    loadUsers()
    return
  }
  const keyword = searchText.value.toLowerCase()
  users.value = users.value.filter(user =>
    user.username.toLowerCase().includes(keyword) ||
    (user.nickname && user.nickname.toLowerCase().includes(keyword)) ||
    (user.phone && user.phone.includes(keyword)) ||
    (user.email && user.email.toLowerCase().includes(keyword))
  )
  total.value = users.value.length
}

const showCreateDialog = () => {
  isEdit.value = false
  dialogTitle.value = '添加用户'
  Object.assign(formData, {
    id: null,
    username: '',
    password: '',
    nickname: '',
    phone: '',
    email: '',
    role: 'user'
  })
  dialogVisible.value = true
}

const showEditDialog = (user) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: user.id,
    username: user.username,
    password: '',
    nickname: user.nickname,
    phone: user.phone,
    email: user.email,
    role: user.role || 'user'
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let res
        if (isEdit.value) {
          res = await updateUserByAdmin(formData.id, formData)
        } else {
          if (!formData.password) {
            ElMessage.warning('请输入密码')
            return
          }
          res = await createUser(formData)
        }

        if (res.success) {
          ElMessage.success(res.message)
          dialogVisible.value = false
          loadUsers()
        }
      } catch (error) {
        // 拦截器已统一提示错误
      }
    }
  })
}

const handleDelete = (user) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${user.username}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteUser(user.id)
      if (res.success) {
        ElMessage.success('删除成功')
        loadUsers()
      }
    } catch (error) {
      // 拦截器已统一提示错误
    }
  }).catch(() => {})
}

const handleSizeChange = (val) => {
  pageSize.value = val
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}

.filter-card {
  background: #fff;
  padding: 12px 16px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.not-logged-in-card {
  max-width: 480px;
  margin: 60px auto;
}
</style>

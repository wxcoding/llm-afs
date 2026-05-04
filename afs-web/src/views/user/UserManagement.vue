<template>
  <div class="user-management">
    <template v-if="user">
      <el-config-provider :locale="zhCn">
        <div class="page-header">
          <div class="header-content">
            <h2 class="page-title">用户管理</h2>
          </div>
          <el-button type="primary" @click="showCreateDialog" class="add-user-btn">
            <el-icon><Plus /></el-icon>
            添加用户
          </el-button>
        </div>

      <div class="search-bar">
        <el-input
          v-model="searchText"
          placeholder="搜索用户名、昵称、手机号或邮箱"
          clearable
          style="width: 350px"
          @clear="loadUsers"
          @keyup.enter="searchUsers"
          class="search-input"
        >
          <template #append>
            <el-button @click="searchUsers" class="search-btn">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </template>
        </el-input>
      </div>

      <div class="table-container">
        <el-table :data="filteredUsers" style="width: 100%" v-loading="loading" class="user-table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-avatar :size="40" :src="row.avatar || defaultAvatar" class="user-avatar">
                {{ row.nickname ? row.nickname[0] : row.username[0] }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.updateTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="showEditDialog(row)" class="edit-btn">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)" class="delete-btn">删除</el-button>
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
            :prev-text="'上一页'"
            :next-text="'下一页'"
            :total-text="'共 {total} 条'"
            :page-size-text="'每页条数'"
            :goto-text="'前往'"
            :pager-count="5"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="pagination-component"
          />
        </div>

        <div class="table-footer">
          <span class="table-info">共 {{ total }} 条记录</span>
        </div>
      </div>
      </el-config-provider>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="user-dialog">
        <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px" class="user-form">
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
          <el-form-item label="头像" prop="avatar">
            <el-input v-model="formData.avatar" placeholder="请输入头像URL" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-else>
      <div class="not-logged-in">
        <div class="not-logged-in-card">
          <div class="not-logged-in-icon">🔒</div>
          <h3>需要登录</h3>
          <p>请先登录后再访问用户管理页面</p>
          <el-button type="primary" size="large" @click="$router.push('/login')">
            立即登录
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

export default {
  name: 'UserManagement',
  components: {
    Plus,
    Search
  },
  setup() {
    const user = inject('user')
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
    const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

    const formData = reactive({
      id: null,
      username: '',
      password: '',
      nickname: '',
      phone: '',
      email: '',
      avatar: ''
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
        const response = await axios.get('/api/user/list')
        if (response.data.success) {
          users.value = response.data.users
          total.value = response.data.total
        } else {
          ElMessage.error(response.data.message)
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
        avatar: ''
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
        avatar: user.avatar
      })
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      if (!formRef.value) return
      
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            let response
            if (isEdit.value) {
              response = await axios.put(`/api/user/admin/update/${formData.id}`, formData)
            } else {
              if (!formData.password) {
                ElMessage.warning('请输入密码')
                return
              }
              response = await axios.post('/api/user/create', formData)
            }
            
            if (response.data.success) {
              ElMessage.success(response.data.message)
              dialogVisible.value = false
              loadUsers()
            } else {
              ElMessage.error(response.data.message)
            }
          } catch (error) {
            ElMessage.error('操作失败')
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
          const response = await axios.delete(`/api/user/delete/${user.id}`)
          if (response.data.success) {
            ElMessage.success('删除成功')
            loadUsers()
          } else {
            ElMessage.error(response.data.message)
          }
        } catch (error) {
          ElMessage.error('删除失败')
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

    return {
      user,
      loading,
      users,
      searchText,
      currentPage,
      pageSize,
      total,
      dialogVisible,
      dialogTitle,
      isEdit,
      formRef,
      formData,
      rules,
      filteredUsers,
      defaultAvatar,
      zhCn,
      loadUsers,
      searchUsers,
      showCreateDialog,
      showEditDialog,
      handleSubmit,
      handleDelete,
      handleSizeChange,
      handleCurrentChange,
      formatTime
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.header-content {
  flex: 1;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.page-description {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.add-user-btn {
  font-size: 14px;
  padding: 10px 20px;
}

.search-bar {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
}

.search-input {
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
}

.search-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.search-btn {
  border-radius: 0 8px 8px 0;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  padding: 24px;
  margin-bottom: 24px;
}

.user-table {
  border-radius: 8px;
  overflow: hidden;
}

.user-avatar {
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.edit-btn {
  margin-right: 8px;
  border-radius: 4px;
}

.delete-btn {
  border-radius: 4px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.pagination-component {
  font-size: 14px;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.user-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.user-form {
  padding: 0 16px;
}

.not-logged-in {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}

.not-logged-in-card {
  text-align: center;
  padding: 60px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  max-width: 400px;
  transition: all 0.3s ease;
}

.not-logged-in-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.not-logged-in-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.not-logged-in-card h3 {
  margin: 0 0 12px 0;
  font-size: 24px;
  color: #303133;
}

.not-logged-in-card p {
  margin: 0 0 32px 0;
  font-size: 16px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-management {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .search-input {
    width: 100% !important;
  }
  
  .table-container {
    padding: 16px;
  }
  
  .pagination {
    justify-content: center;
  }
}
</style>

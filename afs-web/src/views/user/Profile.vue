<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <el-button class="back-btn" circle @click="goBack">←</el-button>
          <span>{{ editing ? '修改信息' : '个人中心' }}</span>
        </div>
      </template>
      
      <!-- 查看模式 -->
      <div v-if="!editing" class="profile-view">
        <div class="profile-header">
          <el-avatar 
            :size="100" 
            :src="form.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
            class="profile-avatar"
          />
        </div>
        
        <el-descriptions :column="1" border class="profile-info">
          <el-descriptions-item label="用户名">{{ form.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ form.nickname || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ form.phone || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ form.email || '未设置' }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="profile-actions">
          <el-button type="primary" @click="editing = true">修改信息</el-button>
        </div>
      </div>
      
      <!-- 编辑模式 -->
      <div v-else class="profile-edit">
        <el-form :model="form" label-width="80px" class="profile-form">
          <el-form-item label="头像">
            <div class="avatar-section">
              <el-avatar 
                :size="100" 
                :src="form.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                class="preview-avatar"
              />
              <div class="avatar-actions">
                <el-upload
                  class="avatar-upload"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="uploadAvatar"
                >
                  <el-button type="primary" size="small">更换头像</el-button>
                </el-upload>
                <el-input 
                  v-model="form.avatar" 
                  placeholder="或输入头像URL"
                  size="small"
                  class="avatar-url-input"
                />
              </div>
            </div>
          </el-form-item>
          
          <el-form-item label="用户名">
            <el-input v-model="form.username" disabled />
          </el-form-item>
          
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </el-form-item>
          
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, inject, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Profile',
  setup() {
    const router = useRouter()
    const user = inject('user')
    const saving = ref(false)
    const editing = ref(false)
    
    const form = ref({
      id: null,
      username: '',
      nickname: '',
      avatar: '',
      phone: '',
      email: ''
    })
    
    const originalForm = ref({})
    
    const goBack = () => {
      if (editing.value) {
        form.value = { ...originalForm.value }
        editing.value = false
      } else {
        router.back()
      }
    }

    const loadUserInfo = async () => {
      if (!user.value) {
        router.push('/login')
        return
      }
      form.value.id = user.value.id
      form.value.username = user.value.username
      
      try {
        const res = await axios.get(`/api/user/${user.value.id}`)
        if (res.data.success) {
          const userData = res.data.user
          form.value.nickname = userData.nickname || ''
          form.value.avatar = userData.avatar || ''
          form.value.phone = userData.phone || ''
          form.value.email = userData.email || ''
          originalForm.value = { ...form.value }
        }
      } catch (e) {
        ElMessage.error('获取用户信息失败')
      }
    }

    const beforeAvatarUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('只能上传图片文件!')
        return false
      }
      if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB!')
        return false
      }
      return true
    }

    const uploadAvatar = async ({ file }) => {
      const reader = new FileReader()
      reader.onload = (e) => {
        form.value.avatar = e.target.result
      }
      reader.readAsDataURL(file)
    }

    const saveProfile = async () => {
      if (!form.value.id) return
      
      saving.value = true
      try {
        const res = await axios.put('/api/user/update', {
          id: form.value.id,
          nickname: form.value.nickname,
          phone: form.value.phone,
          email: form.value.email,
          avatar: form.value.avatar
        })
        
        if (res.data.success) {
          ElMessage.success('保存成功')
          user.value = { ...user.value, ...res.data.user }
          localStorage.setItem('user', JSON.stringify(user.value))
          editing.value = false
        } else {
          ElMessage.error(res.data.message || '保存失败')
        }
      } catch (e) {
        ElMessage.error('保存失败，请检查网络')
      } finally {
        saving.value = false
      }
    }
    
    const cancelEdit = () => {
      form.value = { ...originalForm.value }
      editing.value = false
    }

    onMounted(() => {
      loadUserInfo()
    })

    return {
      form,
      saving,
      editing,
      goBack,
      beforeAvatarUpload,
      uploadAvatar,
      saveProfile,
      cancelEdit
    }
  }
}
</script>

<style scoped>
.profile-page {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.profile-card {
  width: 600px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  margin-right: 8px;
}

.profile-view {
  padding: 20px;
}

.profile-header {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.profile-avatar {
  cursor: default;
}

.profile-info {
  margin-bottom: 20px;
}

.profile-actions {
  display: flex;
  justify-content: center;
}

.profile-form {
  padding: 20px;
}

.avatar-section {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.preview-avatar {
  flex-shrink: 0;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.avatar-url-input {
  width: 250px;
}
</style>

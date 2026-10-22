<template>
  <div class="profile-page">
    <el-card class="profile-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-button class="back-btn" circle size="small" @click="goBack">←</el-button>
          <span>{{ editing ? '修改信息' : '个人信息' }}</span>
        </div>
      </template>

      <!-- 查看模式 -->
      <div v-if="!editing" class="profile-view">
        <div class="profile-header">
          <el-avatar
            :size="90"
            :src="form.avatar || '/avatars/default.jpg'"
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
              <el-upload
                class="avatar-upload-wrapper"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="uploadAvatar"
                accept="image/*"
              >
                <div class="avatar-hover-wrapper">
                  <el-avatar
                    :size="90"
                    :src="form.avatar || '/avatars/default.jpg'"
                    class="preview-avatar"
                  />
                  <div class="avatar-hover-mask">
                    <el-icon><Camera /></el-icon>
                    <span>更换头像</span>
                  </div>
                </div>
              </el-upload>
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
            <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import { getUserById, updateUser } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const { user } = storeToRefs(userStore)
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
    const res = await getUserById(user.value.id)
    if (res.success) {
      const userData = res.data
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
  const isLt500K = file.size / 1024 < 500

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt500K) {
    ElMessage.error('图片大小不能超过 500KB!')
    return false
  }
  return true
}

/** canvas 压缩为 200×200 的 base64 */
const compressImage = (dataUrl, maxSize = 200) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      canvas.width = maxSize
      canvas.height = maxSize
      // 居中裁剪为正方形
      const size = Math.min(img.width, img.height)
      const sx = (img.width - size) / 2
      const sy = (img.height - size) / 2
      ctx.drawImage(img, sx, sy, size, size, 0, 0, maxSize, maxSize)
      resolve(canvas.toDataURL('image/jpeg', 0.85))
    }
    img.src = dataUrl
  })
}

const uploadAvatar = async ({ file }) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    const compressed = await compressImage(e.target.result)
    form.value.avatar = compressed
  }
  reader.readAsDataURL(file)
}

const saveProfile = async () => {
  if (!form.value.id) return

  saving.value = true
  try {
    const res = await updateUser({
      id: form.value.id,
      nickname: form.value.nickname,
      phone: form.value.phone,
      email: form.value.email,
      avatar: form.value.avatar
    })

    if (res.success) {
      ElMessage.success('保存成功')
      userStore.updateUser(res.data)
      editing.value = false
    }
  } catch (e) {
    // 拦截器已统一提示错误
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
</script>

<style scoped>
.profile-page {
  padding: 0;
}

.profile-card {
  max-width: 720px;
}

.card-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-view {
  padding: 8px;
}

.profile-header {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.profile-info {
  margin-bottom: 16px;
}

.profile-actions {
  display: flex;
  justify-content: center;
}

.profile-form {
  padding: 8px;
}

.avatar-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.avatar-upload-wrapper {
  cursor: pointer;
}

.avatar-hover-wrapper {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  overflow: hidden;
}

.preview-avatar {
  flex-shrink: 0;
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-hover-mask .el-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.avatar-hover-wrapper:hover .avatar-hover-mask {
  opacity: 1;
}
</style>

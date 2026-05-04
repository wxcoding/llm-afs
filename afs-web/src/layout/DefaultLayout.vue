<template>
  <div class="default-layout">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <span class="logo-icon">🛡️</span>
            <span class="logo-text">防诈骗知识问答系统</span>
          </div>
          <el-menu
            mode="horizontal"
            :default-active="activeIndex"
            @select="handleMenuSelect"
            :ellipsis="false"
            class="nav-menu"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/chat">智能问答</el-menu-item>
            <el-menu-item index="/knowledge">知识库</el-menu-item>
            <el-menu-item index="/cases">案例分析</el-menu-item>
            <el-menu-item index="/favorites">我的收藏</el-menu-item>
            <el-menu-item index="/templates">对话模板</el-menu-item>
            <el-menu-item index="/config">系统配置</el-menu-item>
            <el-menu-item index="/audit" v-if="user">知识审核</el-menu-item>
            <el-menu-item index="/search-history">搜索历史</el-menu-item>
            <el-menu-item index="/users" v-if="user">用户管理</el-menu-item>
          </el-menu>
          <div class="user-area">
            <template v-if="user">
              <div class="user-info" @click="$router.push('/profile')">
                <el-avatar
                  :size="36"
                  :src="user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                  class="user-avatar"
                />
                <span class="username">{{ user.nickname || user.username }}</span>
              </div>
              <el-dropdown @command="handleCommand">
                <el-button type="primary" size="small">
                  更多
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="logout" divided>退出</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
            </template>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { computed, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'DefaultLayout',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const user = inject('user')

    const activeIndex = computed(() => route.path)

    const handleMenuSelect = (index) => {
      router.push(index)
    }

    const handleCommand = (command) => {
      if (command === 'profile') {
        router.push('/profile')
      } else if (command === 'logout') {
        logout()
      }
    }

    const logout = () => {
      localStorage.removeItem('user')
      user.value = null
      router.push('/login')
    }

    return {
      user,
      activeIndex,
      handleMenuSelect,
      handleCommand,
      logout
    }
  }
}
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 40px;
  cursor: pointer;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.nav-menu {
  flex: 1;
  border: none !important;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-avatar {
  cursor: pointer;
}

.username {
  color: #666;
}

.el-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}
</style>
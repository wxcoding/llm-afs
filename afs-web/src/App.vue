<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <div class="logo">
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
            <el-menu-item index="/cases">案例分析</el-menu-item>
            <el-menu-item index="/knowledge">知识库</el-menu-item>
            <el-menu-item index="/users">用户管理</el-menu-item>
          </el-menu>
          <div class="user-area">
            <template v-if="user">
              <el-popover
                placement="bottom"
                :width="200"
                trigger="hover"
              >
                <template #reference>
                  <div class="user-info" @click="$router.push('/')">
                    <el-avatar 
                      :size="36" 
                      :src="user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                      class="user-avatar"
                    />
                    <span class="username">{{ user.nickname || user.username }}</span>
                  </div>
                </template>
                <div class="user-popover-simple">
                  <div class="popover-user-info">
                    <el-avatar :size="40" :src="user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                    <div>
                      <div class="popover-name">{{ user.nickname || user.username }}</div>
                      <div class="popover-username">@{{ user.username }}</div>
                    </div>
                  </div>
                </div>
              </el-popover>
              <el-dropdown @command="handleCommand">
                <el-button type="primary" size="small">
                  更多<i class="el-icon-arrow-down el-icon--right"></i>
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
import { ref, computed, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'App',
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

<style>
#app {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
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

.user-popover-simple {
  padding: 8px;
}

.popover-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.popover-user-info > div {
  flex: 1;
}

.popover-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.popover-username {
  font-size: 12px;
  color: #999;
}
</style>

<template>
  <div id="app">
    <el-container class="app-container">
      <el-header class="header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <span class="logo-icon">🛡️</span>
            <span class="logo-text">防诈骗知识问答系统</span>
          </div>
          <div class="header-right">
            <template v-if="user">
              <el-popover
                placement="bottom"
                :width="200"
                trigger="hover"
              >
                <template #reference>
                  <div class="user-info">
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
      <el-container>
        <el-aside class="sidebar">
          <el-menu
            mode="vertical"
            :default-active="activeIndex"
            @select="handleMenuSelect"
            class="side-menu"
          >
            <el-menu-item index="/">
              <i class="el-icon-s-home"></i>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/chat">
              <i class="el-icon-chat-dot-round"></i>
              <span>智能问答</span>
            </el-menu-item>
            <el-menu-item index="/knowledge">
              <i class="el-icon-book-open"></i>
              <span>知识库</span>
            </el-menu-item>
            <el-menu-item index="/cases">
              <i class="el-icon-folder-opened"></i>
              <span>案例分析</span>
            </el-menu-item>
            <el-menu-item index="/favorites">
              <i class="el-icon-star"></i>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="/templates">
              <i class="el-icon-file-text"></i>
              <span>对话模板</span>
            </el-menu-item>
            <el-menu-item index="/config">
              <i class="el-icon-setting"></i>
              <span>系统配置</span>
            </el-menu-item>
            <el-menu-item index="/search-history">
              <i class="el-icon-search"></i>
              <span>搜索历史</span>
            </el-menu-item>
            <el-menu-item index="/audit" v-if="user">
              <i class="el-icon-check-circle"></i>
              <span>知识审核</span>
            </el-menu-item>
            <el-menu-item index="/users" v-if="user">
              <i class="el-icon-user"></i>
              <span>用户管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, inject } from 'vue'
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
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.app-container {
  height: 100vh;
}

.header {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.1);
  padding: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(64, 158, 255, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 30px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.logo:hover {
  background: rgba(64, 158, 255, 0.08);
  transform: scale(1.02);
}

.logo-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 8px rgba(64, 158, 255, 0.3));
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: 25px;
  transition: all 0.3s ease;
  background: rgba(64, 158, 255, 0.05);
}

.user-info:hover {
  background: rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.user-avatar {
  cursor: pointer;
  border: 2px solid rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.user-info:hover .user-avatar {
  border-color: #409eff;
  transform: scale(1.1);
}

.username {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  border-right: 1px solid rgba(64, 158, 255, 0.1);
  padding: 20px 0;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.05);
}

.side-menu {
  border: none;
  height: 100%;
  padding: 0 10px;
}

.side-menu .el-menu-item {
  margin: 6px 8px;
  border-radius: 12px;
  padding: 12px 20px;
  transition: all 0.35s ease;
  border: 1px solid transparent;
}

.side-menu .el-menu-item:hover {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(103, 194, 58, 0.08) 100%);
  transform: translateX(4px);
  border-color: rgba(64, 158, 255, 0.2);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.side-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4);
}

.side-menu .el-menu-item.is-active:hover {
  transform: translateX(0);
  box-shadow: 0 6px 25px rgba(64, 158, 255, 0.5);
}

.side-menu .el-menu-item.is-active i {
  filter: brightness(1.1);
}

.side-menu .el-menu-item i {
  margin-right: 12px;
  font-size: 18px;
  transition: all 0.3s ease;
}

.side-menu .el-menu-item:hover i {
  transform: scale(1.15);
}

.side-menu .el-menu-item span {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.user-popover-simple {
  padding: 12px;
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
  font-weight: 600;
  color: #2d3748;
}

.popover-username {
  font-size: 12px;
  color: #718096;
}
</style>
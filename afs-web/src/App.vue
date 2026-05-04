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
            <el-sub-menu index="knowledge">
              <template #title>
                <i class="el-icon-book-open"></i>
                <span>知识库管理</span>
              </template>
              <el-menu-item index="/knowledge">
                <span>知识库</span>
              </el-menu-item>
              <el-menu-item index="/knowledge/audit" v-if="user">
                <span>知识审核</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/cases">
              <i class="el-icon-folder-opened"></i>
              <span>案例分析</span>
            </el-menu-item>
            <el-sub-menu index="config">
              <template #title>
                <i class="el-icon-setting"></i>
                <span>系统配置</span>
              </template>
              <el-menu-item index="/config">
                <span>模型配置</span>
              </el-menu-item>
              <el-menu-item index="/config/dict">
                <span>数据字典</span>
              </el-menu-item>
            </el-sub-menu>
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
  background: #f0f2f5;
}

.app-container {
  height: 100vh;
}

.header {
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0;
  z-index: 100;
  border-bottom: 1px solid #e6e6e6;
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
  border-radius: 6px;
  transition: all 0.3s ease;
}

.logo:hover {
  background: #f0f7ff;
}

.logo-icon {
  font-size: 28px;
  color: #1890ff;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
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
  border-radius: 4px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  cursor: pointer;
  border: 2px solid #d9d9d9;
  transition: all 0.3s ease;
}

.user-info:hover .user-avatar {
  border-color: #1890ff;
}

.username {
  color: #303133;
  font-weight: 500;
  font-size: 14px;
}

.sidebar {
  width: 200px;
  background: #304156;
  border-right: none;
  padding: 0;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
}

.side-menu {
  border: none;
  height: 100%;
  padding: 0;
  background: #304156 !important;
}

/* 一级菜单项（无子菜单） */
.side-menu > .el-menu-item {
  margin: 0;
  border-radius: 0;
  padding: 0 20px;
  transition: all 0s;
  border: none;
  height: 50px;
  line-height: 50px;
  color: #bfcbd9;
  font-weight: 400;
  background: #304156;
}

.side-menu > .el-menu-item:hover {
  color: #fff;
  background: #263445;
}

/* 子菜单标题 */
.side-menu .el-sub-menu__title {
  margin: 0;
  border-radius: 0;
  padding: 0 20px;
  transition: all 0s;
  border: none;
  height: 50px;
  line-height: 50px;
  color: #bfcbd9;
  font-weight: 400;
  background: #304156;
}

.side-menu .el-sub-menu__title:hover {
  color: #fff;
  background: #263445;
}

/* 子菜单项 */
.side-menu .el-sub-menu .el-menu-item {
  margin: 0;
  border-radius: 0;
  padding: 0 50px !important;
  transition: all 0s;
  border: none;
  height: 45px;
  line-height: 45px;
  color: #bfcbd9;
  background: #1f2d3d !important;
  font-size: 14px;
  font-weight: 400;
}

.side-menu .el-sub-menu .el-menu-item:hover {
  color: #fff;
  background: #1f2d3d !important;
}

/* 激活状态 */
.side-menu .el-menu-item.is-active {
  color: #fff;
  background: #1890ff;
  font-weight: 400;
  border-right: 3px solid #1890ff;
}

.side-menu .el-sub-menu .el-menu-item.is-active {
  color: #fff;
  background: #1890ff !important;
  font-weight: 400;
  border-right: 3px solid #1890ff;
}

/* 图标样式 */
.side-menu .el-menu-item i,
.side-menu .el-sub-menu__title i {
  margin-right: 10px;
  font-size: 16px;
  width: 16px;
  text-align: center;
}

/* 文字样式 */
.side-menu .el-menu-item span,
.side-menu .el-sub-menu__title span {
  font-size: 14px;
  font-weight: 400;
}

/* 子菜单展开/收起图标 */
.side-menu .el-sub-menu__icon-arrow {
  font-size: 12px;
  transition: transform 0.3s ease;
}

.side-menu .el-sub-menu.is-opened .el-sub-menu__icon-arrow {
  transform: rotate(180deg);
}

/* 子菜单容器 */
.side-menu .el-sub-menu,
.side-menu .el-menu--inline {
  background: #1f2d3d !important;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f0f2f5;
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
  color: #303133;
}

.popover-username {
  font-size: 12px;
  color: #909399;
}
</style>
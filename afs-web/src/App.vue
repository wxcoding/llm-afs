<template>
  <div id="app">
    <!-- 全屏独立页面（如登录页）：不渲染主布局 -->
    <router-view v-if="isBlankRoute" />

    <!-- 主布局：左侧侧栏（含 logo） + 右侧（header + main） -->
    <el-container v-else class="app-container">
      <el-aside class="sidebar" :width="isCollapse ? '64px' : '200px'">
        <div class="sidebar-logo" @click="$router.push('/')">
          <ShieldIcon :size="26" class="logo-icon" />
          <span v-show="!isCollapse" class="logo-text">智能反诈系统</span>
        </div>
        <el-menu
          mode="vertical"
          :default-active="activeIndex"
          :collapse="isCollapse"
          class="side-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-sub-menu index="knowledge">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>知识库管理</span>
            </template>
            <el-menu-item index="/knowledge">
              <el-icon><Collection /></el-icon>
              <span>知识库</span>
            </el-menu-item>
            <el-menu-item v-if="isAdmin" index="/audit">
              <el-icon><DocumentChecked /></el-icon>
              <span>知识审核</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/cases">
            <el-icon><FolderOpened /></el-icon>
            <span>案例分析</span>
          </el-menu-item>
          <el-sub-menu v-if="isAdmin" index="config">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </template>
            <el-menu-item index="/config">
              <el-icon><Cpu /></el-icon>
              <span>模型配置</span>
            </el-menu-item>
            <el-menu-item index="/config/dict">
              <el-icon><Notebook /></el-icon>
              <span>数据字典</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-if="isAdmin" index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container class="right-container">
        <el-header class="header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="toggleCollapse">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
            <el-breadcrumb separator="/" class="breadcrumb">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentTitle && currentTitle !== '首页'">
                {{ currentTitle }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="header-right">
            <template v-if="user">
              <el-dropdown @command="handleCommand">
                <div class="user-info">
                  <el-avatar
                    :size="32"
                    :src="user.avatar || '/avatars/default.jpg'"
                    class="user-avatar"
                  />
                  <span class="username">{{ user.nickname || user.username }}</span>
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="$router.push('/login')">登录</el-button>
            </template>
          </div>
        </el-header>

        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import {
  House,
  ChatDotRound,
  Reading,
  Collection,
  DocumentChecked,
  FolderOpened,
  Setting,
  Cpu,
  Notebook,
  User,
  Expand,
  Fold,
  ArrowDown
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import ShieldIcon from '@/components/common/ShieldIcon.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { user, isAdmin } = storeToRefs(userStore)

const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const activeIndex = computed(() => route.path)
const isBlankRoute = computed(() => route.meta?.blank === true)
const currentTitle = computed(() => route.meta?.title || '')

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
  userStore.clearUser()
  router.push('/login')
}
</script>

<style>
#app {
  min-height: 100vh;
  background: var(--brand-bg-page, #f5f6fa);
}

.app-container {
  height: 100vh;
}

.right-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* ========== 侧栏 ========== */
.sidebar {
  background: #304156;
  border-right: none;
  padding: 0;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  transition: width 0.28s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #2b2f3a;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.28s;
}

.logo-icon {
  flex-shrink: 0;
  color: #ffffff;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.side-menu {
  border: none;
  flex: 1;
  padding: 0;
  background: #304156 !important;
}

/* 一级菜单项（无子菜单） */
.side-menu > .el-menu-item {
  margin: 0;
  border-radius: 0;
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
  padding: 0 20px 0 50px !important;
  transition: all 0s;
  border: none;
  height: 45px;
  line-height: 45px;
  color: #bfcbd9;
  background: #1f2d3d !important;
  font-size: 14px;
  font-weight: 400;
  min-width: 0;
}

.side-menu .el-sub-menu .el-menu-item:hover {
  color: #fff;
  background: #1f2d3d !important;
}

/* 激活状态 */
.side-menu .el-menu-item.is-active {
  color: #fff;
  background: var(--brand-primary) !important;
  font-weight: 500;
  border-right: 3px solid rgba(255, 255, 255, 0.75);
}

.side-menu .el-sub-menu .el-menu-item.is-active {
  color: #fff;
  background: var(--brand-primary) !important;
  font-weight: 500;
  border-right: 3px solid rgba(255, 255, 255, 0.75);
}

/* 图标样式 */
.side-menu .el-menu-item .el-icon,
.side-menu .el-sub-menu__title .el-icon {
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

/* 子菜单容器 */
.side-menu .el-sub-menu,
.side-menu .el-menu--inline {
  background: #1f2d3d !important;
}

/* 折叠时菜单不显示 border-right */
.side-menu.el-menu--collapse {
  border-right: none;
}

/* ========== Header ========== */
.header {
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 16px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.collapse-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
  outline: none;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  flex-shrink: 0;
}

.username {
  color: #303133;
  font-weight: 500;
  font-size: 14px;
}

.arrow-icon {
  font-size: 12px;
  color: #909399;
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: var(--brand-bg-page, #f5f6fa);
}
</style>

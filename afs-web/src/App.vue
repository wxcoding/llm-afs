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
          </el-menu>
          <div class="user-area">
            <template v-if="user">
              <span class="username">{{ user.username }}</span>
              <el-button type="danger" size="small" @click="logout">退出</el-button>
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
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

    const activeIndex = computed(() => route.path)

    const handleMenuSelect = (index) => {
      router.push(index)
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

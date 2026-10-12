import { defineStore } from 'pinia'
import storage from '@/utils/storage'

/**
 * 用户状态管理
 * 统一管理登录用户信息与 localStorage 持久化，禁止在组件中直接读写 localStorage
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    user: storage.get('user', null)
  }),

  getters: {
    isLoggedIn: (state) => !!state.user,
    userId: (state) => state.user?.id ?? null,
    displayName: (state) => state.user?.nickname || state.user?.username || ''
  },

  actions: {
    setUser(userData) {
      this.user = userData
      storage.set('user', userData)
    },

    updateUser(partial) {
      if (!this.user) return
      this.setUser({ ...this.user, ...partial })
    },

    clearUser() {
      this.user = null
      storage.remove('user')
    }
  }
})

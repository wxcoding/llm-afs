import { reactive, provide, inject } from 'vue'
import storage from '@/utils/storage'

const storeKey = Symbol('store')

export const createStore = () => {
  const state = reactive({
    user: storage.get('user', null)
  })
  
  const setUser = (user) => {
    state.user = user
    storage.set('user', user)
  }
  
  const clearUser = () => {
    state.user = null
    storage.remove('user')
  }
  
  return {
    state,
    setUser,
    clearUser
  }
}

export const provideStore = (app, store) => {
  app.provide(storeKey, store)
  app.provide('user', store.state.user)
}

export const useStore = () => {
  const store = inject(storeKey)
  if (!store) {
    throw new Error('Store not provided')
  }
  return store
}

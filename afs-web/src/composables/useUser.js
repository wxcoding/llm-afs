import { ref, inject } from 'vue'
import storage from '@/utils/storage'

export const useUser = () => {
  const user = inject('user')
  
  const setUser = (userData) => {
    user.value = userData
    storage.set('user', userData)
  }
  
  const clearUser = () => {
    user.value = null
    storage.remove('user')
  }
  
  const isLoggedIn = () => {
    return !!user.value
  }
  
  return {
    user,
    setUser,
    clearUser,
    isLoggedIn
  }
}

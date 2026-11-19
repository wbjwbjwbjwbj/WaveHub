import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import http, { request, resetCsrf } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const username = ref('')
  const loggingOut = ref(false)
  const beforeLogout = shallowRef<(() => Promise<boolean>) | null>(null)
  async function check() {
    try {
      username.value = (await request<{ username: string }>('get', '/auth/session')).username
      return true
    } catch {
      username.value = ''
      return false
    }
  }
  async function login(name: string, password: string) {
    const body = new URLSearchParams({ username: name, password })
    const response = await http.post('/auth/login', body)
    username.value = response.data.data.username
    resetCsrf()
  }
  async function logout() {
    if (loggingOut.value) return false
    loggingOut.value = true
    try {
      if (beforeLogout.value && !(await beforeLogout.value())) return false
      await request('post', '/auth/logout')
      username.value = ''
      resetCsrf()
      return true
    } finally {
      loggingOut.value = false
    }
  }
  return { username, loggingOut, beforeLogout, check, login, logout }
})

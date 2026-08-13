import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const authError = ref<string | null>(null)
  const authLoading = ref(false)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    localStorage.setItem('access_token', access)
    localStorage.setItem('refresh_token', refresh)
  }

  function clearTokens() {
    accessToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }

  async function login(email: string, password: string): Promise<boolean> {
    authError.value = null
    authLoading.value = true
    try {
      const data = await authApi.login(email, password)
      setTokens(data.accessToken, data.refreshToken)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: { message?: string } } } }
      authError.value = err.response?.data?.error?.message ?? 'Login failed. Please try again.'
      return false
    } finally {
      authLoading.value = false
    }
  }

  async function register(firstName: string, lastName: string, email: string, password: string): Promise<boolean> {
    authError.value = null
    authLoading.value = true
    try {
      const data = await authApi.register(firstName, lastName, email, password)
      setTokens(data.accessToken, data.refreshToken)
      return true
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: { message?: string } } } }
      authError.value = err.response?.data?.error?.message ?? 'Registration failed. Please try again.'
      return false
    } finally {
      authLoading.value = false
    }
  }

  function logout() {
    clearTokens()
  }

  return { accessToken, isAuthenticated, authError, authLoading, setTokens, clearTokens, login, register, logout }
})

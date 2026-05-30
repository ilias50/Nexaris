import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/api/auth'
import { normalizeRoleList } from '@/utils/roles'
import {
  clearAuthSession,
  getStoredRefreshToken,
  getStoredToken,
  getStoredUser,
  persistAuthSession,
  setStoredUser,
} from '@/utils/authStorage'

function normalizeRoles(roles: Array<string | { name?: string }> | undefined, fallback: string[]) {
  if (!roles?.length) return fallback
  return normalizeRoleList(roles)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const refreshToken = ref<string | null>(getStoredRefreshToken())
  const user = ref<LoginResponse['user'] | null>(getStoredUser<LoginResponse['user']>())

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.roles.includes('ROLE_ADMIN') ?? false)
  const fullName = computed(() =>
    user.value ? `${user.value.firstName} ${user.value.lastName}` : '',
  )

  async function login(payload: LoginRequest) {
    const { data } = await authApi.login(payload)
    _persist(data)
  }

  async function logout() {
    if (token.value) {
      try {
        await authApi.logout(token.value)
      } catch {
        // ignore si le serveur est injoignable
      }
    }
    _clear()
  }

  async function refresh() {
    const { data } = await authApi.refreshToken()
    _persist(data)
  }

  async function syncUser() {
    if (!user.value?.id) return
    const { data } = await authApi.getUser(user.value.id)
    user.value = {
      ...user.value,
      ...data,
      roles: normalizeRoles(data.roles, user.value.roles),
    }
    setStoredUser(user.value)
  }

  function setProfileImageUrl(profileImageUrl: string | null) {
    if (!user.value) return
    user.value = {
      ...user.value,
      profileImageUrl,
    }
    setStoredUser(user.value)
  }

  function _persist(data: LoginResponse) {
    token.value = data.token
    refreshToken.value = data.refreshToken
    user.value = data.user
    persistAuthSession({
      token: data.token,
      refreshToken: data.refreshToken,
      user: data.user,
    })
  }

  function _clear() {
    token.value = null
    refreshToken.value = null
    user.value = null
    clearAuthSession()
  }

  return {
    token,
    refreshToken,
    user,
    isAuthenticated,
    isAdmin,
    fullName,
    login,
    logout,
    refresh,
    syncUser,
    setProfileImageUrl,
  }
})

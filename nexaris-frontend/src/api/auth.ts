import apiClient from './index'
import { getStoredRefreshToken, getStoredToken } from '@/utils/authStorage'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  expiresIn: number
  user: {
    id: number
    firstName: string
    lastName: string
    email: string
    profileImageUrl?: string | null
    countryCode: string
    languageCode: string
    roles: string[]
  }
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  profileImageUrl?: string
  fkCountry?: number
  fkLanguage?: number
  countryCode?: string
  languageCode?: string
}

export interface RegistrationStatusResponse {
  enabled: boolean
}

export interface AdminUser {
  id: number
  firstName: string
  lastName: string
  email: string
  enabled?: boolean
  profileImageUrl?: string | null
  countryCode?: string
  languageCode?: string
  roles?: Array<string | { name?: string }>
}

export interface CountryOption {
  code: string
  name: string
}

export const authApi = {
  login(payload: LoginRequest) {
    return apiClient.post<LoginResponse>('/api/v1/auth/login', payload)
  },

  register(payload: RegisterRequest) {
    return apiClient.post<string>('/api/v1/auth/register', payload)
  },

  getRegistrationStatus() {
    return apiClient.get<RegistrationStatusResponse>('/api/v1/auth/registration-enabled')
  },

  getCountries() {
    return apiClient.get<CountryOption[]>('/api/v1/auth/countries')
  },

  updateRegistrationStatus(enabled: boolean) {
    return apiClient.patch<string>(`/api/v1/auth/admin/registration-toggle?enabled=${enabled}`)
  },

  createUserByAdmin(payload: RegisterRequest) {
    return apiClient.post<string>('/api/v1/auth/admin/users', payload)
  },

  assignRoleToUser(id: number, roleName: string) {
    return apiClient.post<AdminUser>(`/api/v1/auth/admin/${id}/roles/${encodeURIComponent(roleName)}`)
  },

  revokeRoleFromUser(id: number, roleName: string) {
    return apiClient.delete<AdminUser>(`/api/v1/auth/admin/${id}/roles/${encodeURIComponent(roleName)}`)
  },

  logout(token: string) {
    return apiClient.post('/api/v1/auth/logout', null, {
      headers: { Authorization: `Bearer ${token}` },
    })
  },

  refreshToken() {
    const refreshToken = getStoredRefreshToken()
    return apiClient.post<LoginResponse>('/api/v1/auth/refresh-token', null, {
      headers: {
        ...(refreshToken ? { Authorization: `Bearer ${refreshToken}` } : {}),
      },
    })
  },

  getUser(id: number) {
    return apiClient.get<AdminUser>(`/api/v1/auth/user/${id}`)
  },

  getAllUsers() {
    return apiClient.get<AdminUser[]>('/api/v1/auth/users')
  },

  async listUsers() {
    const { data } = await apiClient.get<AdminUser[]>('/api/v1/auth/users')
    return data
  },

  async listUsersSafe() {
    try {
      return await authApi.listUsers()
    } catch {
      return [] as AdminUser[]
    }
  },

  async listEnabledUsers() {
    const users = await authApi.listUsers()
    return users.filter((user) => user.enabled !== false)
  },

  async listEnabledUsersSafe() {
    const users = await authApi.listUsersSafe()
    return users.filter((user) => user.enabled !== false)
  },

  updateUser(id: number, payload: Partial<RegisterRequest>) {
    return apiClient.put(`/api/v1/auth/user/${id}`, payload)
  },

  deleteUser(id: number) {
    return apiClient.delete(`/api/v1/auth/user/${id}`)
  },

  anonymizeUser(id: number) {
    return apiClient.patch(`/api/v1/auth/user/${id}/anonymize`)
  },

  resetUserPassword(id: number, newPassword: string) {
    return apiClient.put(`/api/v1/auth/user/${id}`, { password: newPassword })
  },

  uploadProfileImage(id: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const token = getStoredToken()

    return apiClient.post<{ profileImageUrl: string }>(`/api/v1/auth/user/${id}/profile-image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  },

  deleteProfileImage(id: number) {
    const token = getStoredToken()
    return apiClient.delete(`/api/v1/auth/user/${id}/profile-image`, {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
  },

  changePassword(id: number, currentPassword: string, newPassword: string) {
    return apiClient.post(`/api/v1/auth/user/${id}/password`, {
      currentPassword,
      newPassword,
    })
  },
}

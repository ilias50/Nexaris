import { authApi, type RegisterRequest } from '@/api/auth'

export function useRegisterApiAccess() {
  async function getRegistrationEnabled() {
    const { data } = await authApi.getRegistrationStatus()
    return !!data.enabled
  }

  async function register(payload: RegisterRequest) {
    await authApi.register(payload)
  }

  return {
    getRegistrationEnabled,
    register,
  }
}
import { authApi } from '@/api/auth'

export function useLoginApiAccess() {
  async function getRegistrationEnabled() {
    const { data } = await authApi.getRegistrationStatus()
    return !!data.enabled
  }

  return {
    getRegistrationEnabled,
  }
}

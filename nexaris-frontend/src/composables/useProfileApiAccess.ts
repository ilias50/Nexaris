import { authApi, type CountryOption, type RegisterRequest } from '@/api/auth'

export function useProfileApiAccess() {
  async function getSupportedCountries(): Promise<CountryOption[]> {
    const { data } = await authApi.getCountries()
    const countries = Array.isArray(data) ? data : []

    return countries
      .filter((country) => country?.code && country?.name)
      .map((country) => ({
        code: country.code.trim().toUpperCase(),
        name: country.name.trim(),
      }))
      .sort((a, b) => a.name.localeCompare(b.name))
  }

  async function updateUser(id: number, payload: Partial<RegisterRequest>) {
    await authApi.updateUser(id, payload)
  }

  async function uploadProfileImage(id: number, file: File) {
    const { data } = await authApi.uploadProfileImage(id, file)
    return data.profileImageUrl
  }

  async function deleteProfileImage(id: number) {
    await authApi.deleteProfileImage(id)
  }

  return {
    getSupportedCountries,
    updateUser,
    uploadProfileImage,
    deleteProfileImage,
  }
}
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import type { TranslationKey } from '@/i18n/messages'

export function useAdminRegistrationSettings(t: (key: TranslationKey) => string) {
  const registrationEnabled = ref(false)
  const loadingRegistration = ref(false)
  const savingRegistration = ref(false)
  const registrationMessage = ref('')
  const registrationError = ref('')

  async function loadRegistrationStatus() {
    loadingRegistration.value = true
    registrationError.value = ''
    try {
      const { data } = await authApi.getRegistrationStatus()
      registrationEnabled.value = !!data.enabled
    } catch {
      registrationError.value = t('adminUsers.errors.loadRegistration')
    } finally {
      loadingRegistration.value = false
    }
  }

  async function saveRegistrationStatus() {
    savingRegistration.value = true
    registrationMessage.value = ''
    registrationError.value = ''

    try {
      await authApi.updateRegistrationStatus(registrationEnabled.value)
      registrationMessage.value = registrationEnabled.value
        ? t('adminUsers.registration.enabledMessage')
        : t('adminUsers.registration.disabledMessage')
    } catch {
      registrationError.value = t('adminUsers.errors.saveRegistration')
    } finally {
      savingRegistration.value = false
    }
  }

  return {
    registrationEnabled,
    loadingRegistration,
    savingRegistration,
    registrationMessage,
    registrationError,
    loadRegistrationStatus,
    saveRegistrationStatus,
  }
}
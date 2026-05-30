import { notificationsApi, type EmailSettingsDto } from '@/api/notifications'

export function useAdminEmailSettingsApiAccess() {
  async function getEmailSettings() {
    return notificationsApi.getEmailSettings()
  }

  async function saveEmailSettings(payload: EmailSettingsDto) {
    await notificationsApi.saveEmailSettings(payload)
  }

  return {
    getEmailSettings,
    saveEmailSettings,
  }
}

export type { EmailSettingsDto }

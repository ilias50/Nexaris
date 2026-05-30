import { notificationsApi, type ChannelPreferenceDto } from '@/api/notifications'

export function useNotificationPreferencesApiAccess() {
  async function getPreferencesWorkspace() {
    return notificationsApi.getPreferencesWorkspace()
  }

  function buildPreferencesPayload(
    preferences: ChannelPreferenceDto,
    channelPreferences: Record<string, boolean>,
    eventTypePreferences: Record<string, boolean>,
  ) {
    return notificationsApi.buildPreferencesPayload(preferences, channelPreferences, eventTypePreferences)
  }

  async function savePreferences(payload: ChannelPreferenceDto) {
    await notificationsApi.savePreferences(payload)
  }

  return {
    getPreferencesWorkspace,
    buildPreferencesPayload,
    savePreferences,
  }
}

export type { ChannelPreferenceDto }

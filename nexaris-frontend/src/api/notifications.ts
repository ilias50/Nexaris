import apiClient from './index'

export interface SendNotificationRequest {
  recipientEmail: string
  subject: string
  body: string
}

export interface EmailSettingsDto {
  host: string
  port: number
  username: string
  /** Null when reading (password is never returned by the API). */
  password: string | null
  fromAddress: string
  smtpAuth: boolean
  starttls: boolean
  sslTrust: string | null
}

export interface InAppNotificationDto {
  id: number
  title: string
  message: string
  link?: string
  read: boolean
  createdAt: string
}

export interface InboxResponse {
  notifications: InAppNotificationDto[]
  unreadCount: number
}

export interface ChannelPreferenceDto {
  notificationsEnabled: boolean
  externalEnabled: boolean
  emailEnabled: boolean
  inAppEnabled: boolean
  channels?: Record<string, boolean>
  eventTypes?: Record<string, boolean>
}

export interface AvailableChannelsResponse {
  available_channels: string[]
  count: number
}

export interface AvailableEventTypesResponse {
  available_event_types: string[]
  count: number
}

export interface NotificationPreferencesWorkspace {
  preferences: ChannelPreferenceDto
  availableChannels: string[]
  availableEventTypes: string[]
}

export const notificationsApi = {
  send(payload: SendNotificationRequest) {
    return apiClient.post('/api/v1/notifications/send', payload)
  },

  getEmailSettings() {
    return apiClient.get<EmailSettingsDto>('/api/v1/notifications/settings/email')
  },

  saveEmailSettings(payload: EmailSettingsDto) {
    return apiClient.put<EmailSettingsDto>('/api/v1/notifications/settings/email', payload)
  },

  // In-app notifications
  getInbox() {
    return apiClient.get<InboxResponse>('/api/v1/notifications/inbox')
  },

  markRead(notificationId: number) {
    return apiClient.patch(`/api/v1/notifications/inbox/${notificationId}/read`)
  },

  markAllRead() {
    return apiClient.patch('/api/v1/notifications/inbox/read-all')
  },

  // Channel preferences
  getPreferences() {
    return apiClient.get<ChannelPreferenceDto>('/api/v1/notifications/preferences')
  },

  savePreferences(payload: ChannelPreferenceDto) {
    return apiClient.put<ChannelPreferenceDto>('/api/v1/notifications/preferences', payload)
  },

  buildPreferencesPayload(
    preferences: ChannelPreferenceDto,
    channels: Record<string, boolean>,
    eventTypes: Record<string, boolean>,
  ): ChannelPreferenceDto {
    return {
      ...preferences,
      channels,
      eventTypes,
    }
  },

  async getPreferencesWorkspace() {
    const [preferencesRes, channelsRes, eventTypesRes] = await Promise.all([
      notificationsApi.getPreferences(),
      notificationsApi.getAvailableChannels(),
      notificationsApi.getAvailableEventTypes(),
    ])

    return {
      preferences: preferencesRes.data,
      availableChannels: channelsRes.data.available_channels,
      availableEventTypes: eventTypesRes.data.available_event_types,
    } as NotificationPreferencesWorkspace
  },

  // Available channels
  getAvailableChannels() {
    return apiClient.get<AvailableChannelsResponse>('/api/v1/notifications/channels')
  },

  getAvailableEventTypes() {
    return apiClient.get<AvailableEventTypesResponse>('/api/v1/notifications/event-types')
  },
}

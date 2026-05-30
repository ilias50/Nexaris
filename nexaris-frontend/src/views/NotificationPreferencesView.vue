<template>
  <AppLayout :title="t('nav.notificationPreferences')">
    <div class="preferences-container">
      <div class="preferences-card">
        <h2>{{ t('notifications.preferences.title') }}</h2>
        <p class="subtitle">{{ t('notifications.preferences.subtitle') }}</p>

        <div class="preferences-section">
          <div v-if="isLoading" class="loading-spinner">
            {{ t('common.loading') }}
          </div>
          <div v-else>
            <h3 class="section-title">{{ t('notifications.preferences.quickModeTitle') }}</h3>
            <p class="section-subtitle">{{ t('notifications.preferences.quickModeSubtitle') }}</p>

            <div class="quick-modes">
              <label class="quick-mode-item">
                <input
                  type="radio"
                  name="quick-mode"
                  value="ALL_OFF"
                  :checked="quickMode === 'ALL_OFF'"
                  @change="applyQuickMode('ALL_OFF')"
                />
                <span class="quick-mode-text">
                  <strong>{{ t('notifications.preferences.modes.allOffLabel') }}</strong>
                  <small>{{ t('notifications.preferences.modes.allOffDesc') }}</small>
                </span>
              </label>

              <label class="quick-mode-item">
                <input
                  type="radio"
                  name="quick-mode"
                  value="SITE_ONLY"
                  :checked="quickMode === 'SITE_ONLY'"
                  @change="applyQuickMode('SITE_ONLY')"
                />
                <span class="quick-mode-text">
                  <strong>{{ t('notifications.preferences.modes.siteOnlyLabel') }}</strong>
                  <small>{{ t('notifications.preferences.modes.siteOnlyDesc') }}</small>
                </span>
              </label>

              <label class="quick-mode-item">
                <input
                  type="radio"
                  name="quick-mode"
                  value="CUSTOM"
                  :checked="quickMode === 'CUSTOM'"
                  @change="applyQuickMode('CUSTOM')"
                />
                <span class="quick-mode-text">
                  <strong>{{ t('notifications.preferences.modes.customLabel') }}</strong>
                  <small>{{ t('notifications.preferences.modes.customDesc') }}</small>
                </span>
              </label>
            </div>

            <div v-if="quickMode !== 'ALL_OFF'" class="master-switches">
              <label class="master-switch-item">
                <span class="master-switch-label">{{ t('notifications.preferences.siteNotificationsLabel') }}</span>
                <input
                  type="checkbox"
                  class="toggle-checkbox"
                  :checked="preferences.inAppEnabled"
                  @change="(e) => setInAppEnabled((e.target as HTMLInputElement).checked)"
                />
              </label>
              <label class="master-switch-item">
                <span class="master-switch-label">{{ t('notifications.preferences.externalNotificationsLabel') }}</span>
                <input
                  type="checkbox"
                  class="toggle-checkbox"
                  :checked="preferences.externalEnabled"
                  @change="(e) => setExternalEnabled((e.target as HTMLInputElement).checked)"
                />
              </label>
            </div>

            <h3 class="section-title">{{ t('notifications.preferences.channelsTitle') }}</h3>
            <p class="section-subtitle">{{ t('notifications.preferences.channelsSubtitle') }}</p>

            <div v-if="quickMode !== 'ALL_OFF' && availableChannels.length > 0">
              <div
                v-for="channel in availableChannels"
                :key="channel"
                class="preference-item"
              >
                <div class="preference-info">
                  <label class="preference-label">
                    {{ getChannelLabel(channel) }}
                  </label>
                  <p class="preference-description">
                    {{ getChannelDescription(channel) }}
                  </p>
                </div>
                <div class="preference-toggle">
                  <input
                    :checked="getChannelEnabled(channel)"
                    @change="(e) => setChannelEnabled(channel, (e.target as HTMLInputElement).checked)"
                    type="checkbox"
                    class="toggle-checkbox"
                  />
                </div>
              </div>
            </div>
            <div v-else-if="quickMode !== 'ALL_OFF'" class="no-channels">
              {{ t('notifications.preferences.noChannels') }}
            </div>

            <h3 class="section-title">{{ t('notifications.preferences.reasonsTitle') }}</h3>
            <p class="section-subtitle">{{ t('notifications.preferences.reasonsSubtitle') }}</p>

            <div v-if="quickMode !== 'ALL_OFF' && availableEventTypes.length > 0">
              <div
                v-for="eventType in availableEventTypes"
                :key="eventType"
                class="preference-item"
              >
                <div class="preference-info">
                  <label class="preference-label">
                    {{ getEventTypeLabel(eventType) }}
                  </label>
                  <p class="preference-description">
                    {{ getEventTypeDescription(eventType) }}
                  </p>
                </div>
                <div class="preference-toggle">
                  <input
                    :checked="getEventTypeEnabled(eventType)"
                    @change="(e) => setEventTypeEnabled(eventType, (e.target as HTMLInputElement).checked)"
                    type="checkbox"
                    class="toggle-checkbox"
                  />
                </div>
              </div>
            </div>
            <div v-else-if="quickMode !== 'ALL_OFF'" class="no-channels">
              {{ t('notifications.preferences.noReasons') }}
            </div>
          </div>
        </div>

        <div class="actions">
          <button
            class="btn btn-primary"
            @click="savePreferences"
            :disabled="isSaving"
          >
            {{ isSaving ? t('common.saving') : t('common.save') }}
          </button>
          <span v-if="message.text" :class="['message', message.type]">
            {{ message.text }}
          </span>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useNotificationPreferencesApiAccess, type ChannelPreferenceDto } from '@/composables/useNotificationPreferencesApiAccess'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const notificationsPreferencesApi = useNotificationPreferencesApiAccess()
const preferences = ref<ChannelPreferenceDto>({
  notificationsEnabled: true,
  externalEnabled: true,
  emailEnabled: true,
  inAppEnabled: true,
})
const availableChannels = ref<string[]>([])
const availableEventTypes = ref<string[]>([])
const isSaving = ref(false)
const isLoading = ref(true)
const message = ref<{ text: string; type: 'success' | 'error' }>({ text: '', type: 'success' })

const channelPreferences = ref<Record<string, boolean>>({})
const eventTypePreferences = ref<Record<string, boolean>>({})
type QuickMode = 'ALL_OFF' | 'SITE_ONLY' | 'CUSTOM'

const quickMode = ref<QuickMode>('CUSTOM')

function inferQuickModeFromPreferences(): QuickMode {
  if (!preferences.value.notificationsEnabled) {
    return 'ALL_OFF'
  }
  return 'CUSTOM'
}

function applyQuickMode(mode: QuickMode) {
  quickMode.value = mode

  if (mode === 'ALL_OFF') {
    preferences.value.notificationsEnabled = false
    preferences.value.inAppEnabled = false
    preferences.value.externalEnabled = false
    preferences.value.emailEnabled = false
    return
  }

  if (mode === 'SITE_ONLY') {
    preferences.value.notificationsEnabled = true
    preferences.value.inAppEnabled = true
    preferences.value.externalEnabled = false
    preferences.value.emailEnabled = false
    return
  }

  preferences.value.notificationsEnabled = true
  if (!preferences.value.inAppEnabled && !preferences.value.externalEnabled && !preferences.value.emailEnabled) {
    preferences.value.inAppEnabled = true
  }
}

const CHANNEL_LABEL_KEYS: Record<string, string> = {
  email: 'notifications.preferences.emailLabel',
  inApp: 'notifications.preferences.inAppLabel',
  sms: 'notifications.preferences.smsLabel',
  discord: 'notifications.preferences.discordLabel',
  push: 'notifications.preferences.pushLabel',
}

const CHANNEL_DESCRIPTION_KEYS: Record<string, string> = {
  email: 'notifications.preferences.emailDesc',
  inApp: 'notifications.preferences.inAppDesc',
  sms: 'notifications.preferences.smsDesc',
  discord: 'notifications.preferences.discordDesc',
  push: 'notifications.preferences.pushDesc',
}

const EVENT_TYPE_LABEL_KEYS: Record<string, string> = {
  GENERAL: 'notifications.preferences.reasons.generalLabel',
  PLANNING_MEETING_CREATED: 'notifications.preferences.reasons.slotEventCreatedLabel',
  PLANNING_MEETING_UPDATED: 'notifications.preferences.reasons.slotEventUpdatedLabel',
  PLANNING_MEETING_CANCELLED: 'notifications.preferences.reasons.slotEventCancelledLabel',
  PLANNING_ENTRY_CREATED: 'notifications.preferences.reasons.slotCreatedLabel',
  PLANNING_ENTRY_UPDATED: 'notifications.preferences.reasons.slotUpdatedLabel',
  PLANNING_ENTRY_DELETED: 'notifications.preferences.reasons.slotDeletedLabel',
  PLANNING_ENTRY_ASSIGNED: 'notifications.preferences.reasons.slotAssignedLabel',
  ORG_ANNOUNCEMENT_CREATED: 'notifications.preferences.reasons.orgAnnouncementCreatedLabel',
  ORG_ANNOUNCEMENT_UPDATED: 'notifications.preferences.reasons.orgAnnouncementUpdatedLabel',
  ORG_ANNOUNCEMENT_DELETED: 'notifications.preferences.reasons.orgAnnouncementDeletedLabel',
  ORG_ANNOUNCEMENT: 'notifications.preferences.reasons.orgAnnouncementLabel',
}

const EVENT_TYPE_DESCRIPTION_KEYS: Record<string, string> = {
  GENERAL: 'notifications.preferences.reasons.generalDesc',
  PLANNING_MEETING_CREATED: 'notifications.preferences.reasons.slotEventCreatedDesc',
  PLANNING_MEETING_UPDATED: 'notifications.preferences.reasons.slotEventUpdatedDesc',
  PLANNING_MEETING_CANCELLED: 'notifications.preferences.reasons.slotEventCancelledDesc',
  PLANNING_ENTRY_CREATED: 'notifications.preferences.reasons.slotCreatedDesc',
  PLANNING_ENTRY_UPDATED: 'notifications.preferences.reasons.slotUpdatedDesc',
  PLANNING_ENTRY_DELETED: 'notifications.preferences.reasons.slotDeletedDesc',
  PLANNING_ENTRY_ASSIGNED: 'notifications.preferences.reasons.slotAssignedDesc',
  ORG_ANNOUNCEMENT_CREATED: 'notifications.preferences.reasons.orgAnnouncementCreatedDesc',
  ORG_ANNOUNCEMENT_UPDATED: 'notifications.preferences.reasons.orgAnnouncementUpdatedDesc',
  ORG_ANNOUNCEMENT_DELETED: 'notifications.preferences.reasons.orgAnnouncementDeletedDesc',
  ORG_ANNOUNCEMENT: 'notifications.preferences.reasons.orgAnnouncementDesc',
}

function normalizeChannel(channel: string): string {
  return channel
    .trim()
    .toLowerCase()
    .replace(/[-_](\w)/g, (_, c: string) => c.toUpperCase())
}

function normalizeEventType(eventType: string): string {
  return eventType.trim().toUpperCase().replace(/[\s-]+/g, '_')
}

const getChannelLabel = (channel: string): string => {
  const key = CHANNEL_LABEL_KEYS[normalizeChannel(channel)]
  return key ? t(key as never) : channel
}

const getChannelDescription = (channel: string): string => {
  const key = CHANNEL_DESCRIPTION_KEYS[normalizeChannel(channel)]
  return key ? t(key as never) : t('notifications.preferences.defaultChannelDesc')
}

const getChannelEnabled = (channel: string): boolean => {
  const normalized = normalizeChannel(channel)
  if (normalized === 'email') return preferences.value.emailEnabled
  if (normalized === 'inApp') return preferences.value.inAppEnabled
  return channelPreferences.value[normalized]
    ?? channelPreferences.value[normalized.toUpperCase()]
    ?? true
}

const setChannelEnabled = (channel: string, enabled: boolean) => {
  const normalized = normalizeChannel(channel)
  if (quickMode.value !== 'ALL_OFF') {
    quickMode.value = 'CUSTOM'
  }
  if (normalized === 'email') {
    preferences.value.emailEnabled = enabled
  } else if (normalized === 'inApp') {
    preferences.value.inAppEnabled = enabled
  } else {
    channelPreferences.value[normalized] = enabled
  }
}

const getEventTypeLabel = (eventType: string): string => {
  const normalized = normalizeEventType(eventType)
  const key = EVENT_TYPE_LABEL_KEYS[normalized]
  return key ? t(key as never) : eventType
}

const getEventTypeDescription = (eventType: string): string => {
  const normalized = normalizeEventType(eventType)
  const key = EVENT_TYPE_DESCRIPTION_KEYS[normalized]
  return key ? t(key as never) : t('notifications.preferences.defaultReasonDesc')
}

const getEventTypeEnabled = (eventType: string): boolean => {
  const normalized = normalizeEventType(eventType)
  return eventTypePreferences.value[normalized] ?? true
}

const setEventTypeEnabled = (eventType: string, enabled: boolean) => {
  if (quickMode.value !== 'ALL_OFF') {
    quickMode.value = 'CUSTOM'
  }
  const normalized = normalizeEventType(eventType)
  eventTypePreferences.value[normalized] = enabled
}

const setInAppEnabled = (enabled: boolean) => {
  if (quickMode.value !== 'ALL_OFF') {
    quickMode.value = 'CUSTOM'
  }
  preferences.value.inAppEnabled = enabled
}

const setExternalEnabled = (enabled: boolean) => {
  if (quickMode.value !== 'ALL_OFF') {
    quickMode.value = 'CUSTOM'
  }
  preferences.value.externalEnabled = enabled
}

const loadPreferences = async () => {
  try {
    const workspace = await notificationsPreferencesApi.getPreferencesWorkspace()
    preferences.value = workspace.preferences
    quickMode.value = inferQuickModeFromPreferences()
    availableChannels.value = workspace.availableChannels
    availableEventTypes.value = workspace.availableEventTypes
    channelPreferences.value = { ...(workspace.preferences.channels ?? {}) }
    eventTypePreferences.value = { ...(workspace.preferences.eventTypes ?? {}) }

    for (const rawChannel of workspace.availableChannels) {
      const channel = normalizeChannel(rawChannel)
      if (channel === 'email' || channel === 'inApp') continue
      if (!(channel in channelPreferences.value)) {
        channelPreferences.value[channel] = true
      }
    }

    for (const rawEventType of workspace.availableEventTypes) {
      const eventType = normalizeEventType(rawEventType)
      if (!(eventType in eventTypePreferences.value)) {
        eventTypePreferences.value[eventType] = true
      }
    }
  } catch {
    message.value = {
      text: t('notifications.preferences.loadError'),
      type: 'error',
    }
  }
}

const savePreferences = async () => {
  isSaving.value = true
  try {
    const payload = notificationsPreferencesApi.buildPreferencesPayload(
      preferences.value,
      channelPreferences.value,
      eventTypePreferences.value,
    )
    await notificationsPreferencesApi.savePreferences(payload)
    message.value = {
      text: t('notifications.preferences.saved'),
      type: 'success',
    }
    setTimeout(() => {
      message.value = { text: '', type: 'success' }
    }, 3000)
  } catch {
    message.value = {
      text: t('notifications.preferences.saveError'),
      type: 'error',
    }
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  try {
    await loadPreferences()
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.preferences-container {
  max-width: 600px;
  margin: 0 auto;
}

.preferences-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 2rem;
}

.preferences-card h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--color-text);
}

.subtitle {
  color: var(--color-text-secondary);
  margin-bottom: 2rem;
  font-size: 0.95rem;
}

.preferences-section {
  margin-bottom: 2rem;
}

.section-title {
  margin: 0 0 0.35rem;
  color: var(--color-text);
  font-size: 1rem;
  font-weight: 650;
}

.section-subtitle {
  margin: 0 0 1rem;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
}

.quick-modes {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.quick-mode-item {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  cursor: pointer;
}

.quick-mode-item input[type='radio'] {
  margin-top: 0.15rem;
}

.quick-mode-text {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.quick-mode-text strong {
  color: var(--color-text);
  font-size: 0.94rem;
}

.quick-mode-text small {
  color: var(--color-text-secondary);
  font-size: 0.82rem;
}

.master-switches {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.master-switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
}

.master-switch-label {
  color: var(--color-text);
  font-size: 0.9rem;
  font-weight: 600;
}

.preference-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  margin-bottom: 1rem;
  background: var(--color-bg);
}

.preference-info {
  flex: 1;
}

.preference-label {
  display: block;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.25rem;
  cursor: pointer;
}

.preference-description {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.preference-toggle {
  margin-left: 1.5rem;
}

.toggle-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #3b82f6;
}

.actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1d4ed8;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.message {
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
  border-radius: 4px;
}

.message.success {
  background-color: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}

.message.error {
  background-color: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}

@media (max-width: 768px) {
  .preferences-card {
    padding: 1.5rem;
  }

  .preference-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .preference-toggle {
    margin-left: 0;
    margin-top: 1rem;
    align-self: flex-start;
  }
}

.loading-spinner {
  text-align: center;
  padding: 2rem;
  color: var(--color-text-secondary);
}

.no-channels {
  text-align: center;
  padding: 2rem;
  color: var(--color-text-secondary);
  font-size: 0.95rem;
}
</style>

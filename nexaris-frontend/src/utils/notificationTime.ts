import type { TranslationKey } from '@/i18n/messages'

export function formatNotificationTime(
  dateString: string,
  t: (key: TranslationKey) => string,
): string {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return t('notifications.inbox.time.justNow')
  if (diffMins < 60) return `${diffMins}${t('notifications.inbox.time.minuteSuffix')}`
  if (diffHours < 24) return `${diffHours}${t('notifications.inbox.time.hourSuffix')}`
  if (diffDays < 7) return `${diffDays}${t('notifications.inbox.time.daySuffix')}`

  return date.toLocaleDateString()
}

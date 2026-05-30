import { ref } from 'vue'
import { notificationsApi, type InAppNotificationDto } from '@/api/notifications'

export type { InAppNotificationDto }

export function useNotificationsInbox() {
  const notifications = ref<InAppNotificationDto[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const markAllLoading = ref(false)

  async function fetchInbox(options?: { silent?: boolean }) {
    const silent = options?.silent ?? false
    if (!silent) loading.value = true
    try {
      const { data } = await notificationsApi.getInbox()
      notifications.value = data.notifications
      unreadCount.value = data.unreadCount
      return true
    } catch {
      if (!silent) {
        notifications.value = []
        unreadCount.value = 0
      }
      return false
    } finally {
      if (!silent) loading.value = false
    }
  }

  async function markAllAsRead() {
    markAllLoading.value = true
    try {
      await notificationsApi.markAllRead()
      unreadCount.value = 0
      notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
    } finally {
      markAllLoading.value = false
    }
  }

  async function markNotificationAsRead(item: InAppNotificationDto) {
    if (item.read) return true
    try {
      await notificationsApi.markRead(item.id)
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      return true
    } catch {
      return false
    }
  }

  return {
    notifications,
    unreadCount,
    loading,
    markAllLoading,
    fetchInbox,
    markAllAsRead,
    markNotificationAsRead,
  }
}

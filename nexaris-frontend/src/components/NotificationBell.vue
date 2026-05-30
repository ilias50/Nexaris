<template>
  <div ref="bellRoot" class="notification-bell">
    <button
      class="bell-button"
      @click="toggleDropdown"
      :aria-label="t('notifications.bell')"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <span v-if="unreadCount > 0" class="badge">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <div v-if="showDropdown" class="dropdown">
      <div class="dropdown-header">
        <h3>{{ t('notifications.inbox.title') }}</h3>
        <button
          v-if="unreadCount > 0"
          class="mark-all-btn"
          @click="markAllAsRead"
        >
          {{ t('notifications.inbox.markAll') }}
        </button>
      </div>

      <div class="dropdown-content">
        <div v-if="notifications.length === 0" class="empty-state">
          {{ t('notifications.inbox.empty') }}
        </div>
        <div
          v-for="notif in notifications.slice(0, 10)"
          :key="notif.id"
          :class="['notification-item', { unread: !notif.read }]"
          @click="handleNotificationClick(notif)"
        >
          <div class="notif-title">{{ notif.title }}</div>
          <div class="notif-message">{{ notif.message }}</div>
          <div class="notif-time">{{ formatTime(notif.createdAt) }}</div>
        </div>
      </div>

      <div class="dropdown-footer">
        <router-link to="/notifications/inbox" class="see-all-btn">
          {{ t('notifications.inbox.seeAll') }}
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import type { InAppNotificationDto } from '@/types/domain'
import { useI18n } from '@/i18n'
import { useNotificationsInbox } from '@/composables/useNotificationsInbox'
import { formatNotificationTime } from '@/utils/notificationTime'

const { t } = useI18n()

const showDropdown = ref(false)
const bellRoot = ref<HTMLElement | null>(null)
const { notifications, unreadCount, fetchInbox, markAllAsRead, markNotificationAsRead } =
  useNotificationsInbox()
const router = useRouter()
let pollInterval: ReturnType<typeof setInterval> | null = null

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    fetchInbox()
  }
}

const handleNotificationClick = async (notif: InAppNotificationDto) => {
  await markNotificationAsRead(notif)

  if (notif.link) {
    showDropdown.value = false
    router.push(notif.link)
  }
}

const formatTime = (dateString: string): string => {
  return formatNotificationTime(dateString, t)
}

onMounted(() => {
  fetchInbox({ silent: true })
  // Poll every 30 seconds
  pollInterval = setInterval(() => {
    fetchInbox({ silent: true })
  }, 30000)

  // Close dropdown when clicking outside
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  if (pollInterval) clearInterval(pollInterval)
  document.removeEventListener('click', handleClickOutside)
})

const handleClickOutside = (event: MouseEvent) => {
  if (bellRoot.value && !bellRoot.value.contains(event.target as Node)) {
    showDropdown.value = false
  }
}
</script>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-block;
}

.bell-button {
  background: none;
  border: none;
  padding: 8px;
  cursor: pointer;
  color: currentColor;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.bell-button:hover {
  opacity: 0.7;
}

.badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background-color: var(--color-notif-badge);
  color: white;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--color-notif-surface);
  border: 1px solid var(--color-notif-border);
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  width: 380px;
  max-height: 500px;
  display: flex;
  flex-direction: column;
  z-index: 50;
}

.dropdown-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-notif-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dropdown-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.mark-all-btn {
  background: none;
  border: none;
  color: var(--color-notif-link);
  cursor: pointer;
  font-size: 12px;
  text-decoration: underline;
  padding: 0;
}

.mark-all-btn:hover {
  color: var(--color-notif-link-hover);
}

.dropdown-content {
  flex: 1;
  overflow-y: auto;
  max-height: 350px;
}

.empty-state {
  padding: 32px 16px;
  text-align: center;
  color: var(--color-notif-empty);
  font-size: 14px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-notif-item-border);
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: var(--color-notif-hover-bg);
}

.notification-item.unread {
  background-color: var(--color-notif-unread-bg);
  font-weight: 500;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-notif-title);
  margin-bottom: 4px;
}

.notif-message {
  font-size: 13px;
  color: var(--color-notif-body);
  line-height: 1.4;
  margin-bottom: 4px;
}

.notif-time {
  font-size: 11px;
  color: var(--color-notif-time);
}

.dropdown-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-notif-border);
  text-align: center;
}

.see-all-btn {
  color: var(--color-notif-link);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  display: inline-block;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.see-all-btn:hover {
  background-color: var(--color-notif-footer-hover);
  color: var(--color-notif-link-hover);
}

@media (max-width: 768px) {
  .dropdown {
    position: fixed;
    top: 56px;
    right: 0.5rem;
    left: 0.5rem;
    width: auto;
    max-height: min(70vh, 500px);
  }

  .dropdown-content {
    max-height: min(52vh, 350px);
  }
}
</style>

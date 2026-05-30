<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useI18n } from '@/i18n'
import { useNotificationsInbox, type InAppNotificationDto } from '@/composables/useNotificationsInbox'
import { formatNotificationTime } from '@/utils/notificationTime'

const { t } = useI18n()
const router = useRouter()

const { notifications, unreadCount, loading, markAllLoading, fetchInbox, markAllAsRead, markNotificationAsRead } =
  useNotificationsInbox()

async function openNotification(item: InAppNotificationDto) {
  await markNotificationAsRead(item)

  if (item.link) {
    router.push(item.link)
  }
}

function formatTime(dateString: string): string {
  return formatNotificationTime(dateString, t)
}

onMounted(fetchInbox)
</script>

<template>
  <AppLayout :title="t('notifications.inbox.title')">
    <section class="inbox-card">
      <header class="inbox-card__header">
        <div>
          <h2>{{ t('notifications.inbox.title') }}</h2>
          <p class="inbox-card__subtitle">
            {{ unreadCount }} {{ t('notifications.inbox.unreadLabel') }}
          </p>
        </div>
        <div class="inbox-card__actions">
          <BaseButton variant="ghost" size="sm" @click="fetchInbox">
            {{ t('common.refresh') }}
          </BaseButton>
          <BaseButton
            variant="primary"
            size="sm"
            :loading="markAllLoading"
            :disabled="unreadCount === 0"
            @click="markAllAsRead"
          >
            {{ t('notifications.inbox.markAll') }}
          </BaseButton>
        </div>
      </header>

      <div v-if="loading" class="inbox-state">
        {{ t('common.loading') }}
      </div>

      <div v-else-if="notifications.length === 0" class="inbox-state">
        {{ t('notifications.inbox.empty') }}
      </div>

      <ul v-else class="inbox-list">
        <li
          v-for="item in notifications"
          :key="item.id"
          class="inbox-item"
          :class="{ 'inbox-item--unread': !item.read, 'inbox-item--clickable': !!item.link }"
          @click="openNotification(item)"
        >
          <div class="inbox-item__top">
            <h3>{{ item.title }}</h3>
            <span>{{ formatTime(item.createdAt) }}</span>
          </div>
          <p>{{ item.message }}</p>
        </li>
      </ul>
    </section>
  </AppLayout>
</template>

<style scoped>
.inbox-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.inbox-card__header {
  padding: 1rem 1rem 0.85rem;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.inbox-card__header h2 {
  font-size: 1rem;
  margin: 0;
}

.inbox-card__subtitle {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.inbox-card__actions {
  display: flex;
  gap: 0.45rem;
}

.inbox-state {
  padding: 1.25rem;
  color: var(--color-text-muted);
}

.inbox-list {
  list-style: none;
}

.inbox-item {
  padding: 0.9rem 1rem;
  border-top: 1px solid var(--color-border);
  transition: background var(--transition);
}

.inbox-item--clickable {
  cursor: pointer;
}

.inbox-item--clickable:hover {
  background: #f8fafc;
}

.inbox-item--unread {
  background: #eff6ff;
}

.inbox-item__top {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.3rem;
}

.inbox-item__top h3 {
  font-size: 0.92rem;
  margin: 0;
}

.inbox-item__top span {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.inbox-item p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.45;
}

@media (max-width: 768px) {
  .inbox-card__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .inbox-card__actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>

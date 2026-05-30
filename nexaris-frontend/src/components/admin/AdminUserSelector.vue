<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'
import type { AdminUser } from '@/types/domain'
import { formatUserDisplayName } from '@/utils/users'

const props = withDefaults(defineProps<{
  title: string
  users: AdminUser[]
  selectedUserId?: number | null
  loadingUsers?: boolean
  loadingText: string
  emptyText: string
  refreshLabel?: string
  refreshLoading?: boolean
  showRefresh?: boolean
}>(), {
  selectedUserId: null,
  loadingUsers: false,
  refreshLoading: false,
  showRefresh: true,
})

const emit = defineEmits<{
  (event: 'select-user', userId: number): void
  (event: 'refresh'): void
}>()

function userDisplayName(user: AdminUser) {
  return formatUserDisplayName(user)
}
</script>

<template>
  <div class="aus">
    <div class="aus__head">
      <span>{{ title }}</span>
      <BaseButton
        v-if="showRefresh"
        type="button"
        variant="ghost"
        :loading="refreshLoading"
        @click="emit('refresh')"
      >
        {{ refreshLabel }}
      </BaseButton>
    </div>

    <p v-if="loadingUsers" class="aus__hint">{{ loadingText }}</p>
    <p v-else-if="!users.length" class="aus__hint">{{ emptyText }}</p>
    <ul v-else class="aus__list">
      <li v-for="user in users" :key="user.id">
        <button
          type="button"
          class="aus__item"
          :class="{ 'aus__item--active': selectedUserId === user.id }"
          @click="emit('select-user', user.id)"
        >
          <strong>{{ userDisplayName(user) }}</strong>
          <span>{{ user.email }}</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.aus__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.aus__hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.aus__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.45rem;
  max-height: 450px;
  overflow-y: auto;
}

.aus__item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-sm);
  background: #fff;
  padding: 0.55rem 0.65rem;
  display: grid;
  gap: 0.2rem;
  cursor: pointer;
}

.aus__item span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.aus__item:hover {
  border-color: #93c5fd;
}

.aus__item--active {
  border-color: #2563eb;
  background: #eff6ff;
}
</style>
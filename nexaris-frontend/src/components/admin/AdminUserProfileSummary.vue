<script setup lang="ts">
import { computed } from 'vue'
import type { AdminUser } from '@/types/domain'
import { useI18n } from '@/i18n'
import { formatRoleLabel } from '@/utils/roles'
import { formatUserDisplayName } from '@/utils/users'

const { t } = useI18n()

const props = defineProps<{
  user: AdminUser
  authRoles: string[]
  profileImageSrc?: string | null
}>()

const displayName = computed(() => formatUserDisplayName(props.user))

const roleSummary = computed(() => {
  if (!props.authRoles.length) return '-'
  return props.authRoles.map(formatRoleLabel).join(', ')
})

const profileRows = computed(() => [
  {
    label: t('adminUsers.management.labels.status'),
    value: props.user.enabled === false ? t('adminUsers.management.status.disabled') : t('adminUsers.management.status.enabled'),
  },
  {
    label: t('adminUsers.management.labels.country'),
    value: props.user.countryCode || '-',
  },
  {
    label: t('adminUsers.management.labels.language'),
    value: props.user.languageCode || '-',
  },
  {
    label: t('adminUsers.management.labels.roles'),
    value: roleSummary.value,
  },
])
</script>

<template>
  <div class="aups">
    <div class="aups__details">
      <p><strong>{{ t('adminUsers.management.labels.fullName') }}:</strong> {{ displayName }}</p>
      <p><strong>{{ t('adminUsers.management.labels.email') }}:</strong> {{ user.email }}</p>
      <p v-for="row in profileRows" :key="row.label">
        <strong>{{ row.label }}:</strong> {{ row.value }}
      </p>
    </div>

    <aside class="aups__photo" :aria-label="t('adminUsers.management.profileTitle')">
      <img
        v-if="profileImageSrc"
        :src="profileImageSrc"
        :alt="`${t('adminUsers.management.labels.fullName')}: ${displayName}`"
      />
      <div v-else class="aups__photo-fallback">
        {{ displayName.charAt(0).toUpperCase() }}
      </div>
      <p class="aups__photo-name">{{ displayName }}</p>
    </aside>
  </div>
</template>

<style scoped>
.aups {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 0.9rem;
  align-items: start;
}

.aups p {
  margin: 0;
}

.aups__details {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.aups__photo {
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.08), rgba(59, 130, 246, 0.02));
}

.aups__photo img,
.aups__photo-fallback {
  width: 128px;
  height: 128px;
  border-radius: 999px;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.aups__photo-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #60a5fa);
  color: #fff;
  font-size: 3rem;
  font-weight: 700;
}

.aups__photo-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-align: center;
}

@media (max-width: 768px) {
  .aups {
    grid-template-columns: 1fr;
  }

  .aups__photo {
    order: -1;
  }
}
</style>
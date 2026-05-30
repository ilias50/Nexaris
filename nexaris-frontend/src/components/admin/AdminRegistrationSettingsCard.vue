<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  enabled: boolean
  loading?: boolean
  saving?: boolean
  message?: string
  error?: string
}>(), {
  loading: false,
  saving: false,
  message: '',
  error: '',
})

const emit = defineEmits<{
  (event: 'update:enabled', value: boolean): void
  (event: 'save'): void
}>()

function updateEnabled(event: Event) {
  emit('update:enabled', (event.target as HTMLInputElement).checked)
}
</script>

<template>
  <section class="nx-admin-card">
    <h2 class="nx-admin-title">{{ t('adminUsers.registration.title') }}</h2>
    <p class="nx-admin-subtitle">{{ t('adminUsers.registration.description') }}</p>

    <div v-if="loading" class="nx-admin-info">{{ t('adminUsers.registration.loading') }}</div>

    <div v-else class="arsc__toggle-row">
      <label class="arsc__toggle" for="registration-enabled">
        <input id="registration-enabled" :checked="props.enabled" type="checkbox" @change="updateEnabled" />
        <span>{{ t('adminUsers.registration.toggleLabel') }}</span>
      </label>
      <BaseButton :loading="saving" @click="emit('save')">
        {{ t('adminUsers.registration.saveButton') }}
      </BaseButton>
    </div>

    <p v-if="message" class="nx-admin-success">{{ message }}</p>
    <p v-if="error" class="nx-admin-error">{{ error }}</p>
  </section>
</template>

<style scoped>
.arsc__toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.arsc__toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
}

.arsc__toggle input {
  width: 16px;
  height: 16px;
}
</style>
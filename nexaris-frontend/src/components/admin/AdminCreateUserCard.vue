<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  firstName: string
  lastName: string
  email: string
  password: string
  confirmPassword: string
  loading?: boolean
  message?: string
  error?: string
}>(), {
  loading: false,
  message: '',
  error: '',
})

const emit = defineEmits<{
  (event: 'update:firstName', value: string): void
  (event: 'update:lastName', value: string): void
  (event: 'update:email', value: string): void
  (event: 'update:password', value: string): void
  (event: 'update:confirmPassword', value: string): void
  (event: 'submit'): void
}>()
</script>

<template>
  <section class="nx-admin-card">
    <h2 class="nx-admin-title">{{ t('adminUsers.create.title') }}</h2>
    <p class="nx-admin-subtitle">{{ t('adminUsers.create.description') }}</p>

    <form class="acuc__form" @submit.prevent="emit('submit')">
      <BaseInput
        :model-value="firstName"
        :label="t('adminUsers.create.firstNameLabel')"
        type="text"
        :placeholder="t('adminUsers.create.firstNamePlaceholder')"
        @update:model-value="emit('update:firstName', $event)"
      />
      <BaseInput
        :model-value="lastName"
        :label="t('adminUsers.create.lastNameLabel')"
        type="text"
        :placeholder="t('adminUsers.create.lastNamePlaceholder')"
        @update:model-value="emit('update:lastName', $event)"
      />
      <BaseInput
        :model-value="email"
        :label="t('adminUsers.create.emailLabel')"
        type="email"
        placeholder="name@example.com"
        @update:model-value="emit('update:email', $event)"
      />
      <BaseInput
        :model-value="password"
        :label="t('adminUsers.create.passwordLabel')"
        type="password"
        placeholder="********"
        @update:model-value="emit('update:password', $event)"
      />
      <BaseInput
        :model-value="confirmPassword"
        :label="t('adminUsers.create.confirmPasswordLabel')"
        type="password"
        placeholder="********"
        @update:model-value="emit('update:confirmPassword', $event)"
      />

      <p v-if="message" class="nx-admin-success">{{ message }}</p>
      <p v-if="error" class="nx-admin-error">{{ error }}</p>

      <BaseButton type="submit" :loading="loading">
        {{ t('adminUsers.create.submit') }}
      </BaseButton>
    </form>
  </section>
</template>

<style scoped>
.acuc__form {
  display: grid;
  gap: 0.8rem;
  max-width: 520px;
}
</style>
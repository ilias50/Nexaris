<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  resettingPassword?: boolean
  deletingUser?: boolean
  isDeletingSelf?: boolean
  resetPasswordMessage?: string
  resetPasswordError?: string
  deleteUserMessage?: string
  deleteUserError?: string
}>(), {
  resettingPassword: false,
  deletingUser: false,
  isDeletingSelf: false,
  resetPasswordMessage: '',
  resetPasswordError: '',
  deleteUserMessage: '',
  deleteUserError: '',
})

const emit = defineEmits<{
  (event: 'reset-password', payload: { newPassword: string; confirmNewPassword: string }): void
  (event: 'delete-user'): void
}>()

const newPassword = ref('')
const confirmNewPassword = ref('')

watch(
  () => props.resetPasswordMessage,
  (value) => {
    if (value) {
      newPassword.value = ''
      confirmNewPassword.value = ''
    }
  },
)

function submitResetPassword() {
  emit('reset-password', {
    newPassword: newPassword.value,
    confirmNewPassword: confirmNewPassword.value,
  })
}
</script>

<template>
  <div class="aupa">
    <h4 class="aupa__subtitle">{{ t('adminUsers.management.actionsTitle') }}</h4>

    <form class="aupa__reset-form" @submit.prevent="submitResetPassword">
      <BaseInput
        v-model="newPassword"
        :label="t('adminUsers.management.newPasswordLabel')"
        type="password"
        :placeholder="t('adminUsers.management.newPasswordPlaceholder')"
      />
      <BaseInput
        v-model="confirmNewPassword"
        :label="t('adminUsers.management.confirmPasswordLabel')"
        type="password"
        :placeholder="t('adminUsers.management.newPasswordPlaceholder')"
      />
      <BaseButton type="submit" variant="secondary" :loading="resettingPassword">
        {{ t('adminUsers.management.resetPasswordButton') }}
      </BaseButton>
    </form>

    <BaseButton variant="danger" :loading="deletingUser" :disabled="isDeletingSelf" @click="emit('delete-user')">
      {{ t('adminUsers.management.deleteUserButton') }}
    </BaseButton>
    <p v-if="isDeletingSelf" class="aupa__info">
      {{ t('adminUsers.management.errors.cannotDeleteSelf') }}
    </p>

    <p v-if="resetPasswordMessage" class="aupa__success">{{ resetPasswordMessage }}</p>
    <p v-if="resetPasswordError" class="aupa__error">{{ resetPasswordError }}</p>
    <p v-if="deleteUserMessage" class="aupa__success">{{ deleteUserMessage }}</p>
    <p v-if="deleteUserError" class="aupa__error">{{ deleteUserError }}</p>
  </div>
</template>

<style scoped>
.aupa {
  border-top: 1px solid var(--color-border);
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  display: grid;
  gap: 0.6rem;
}

.aupa__subtitle {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin-bottom: 0.55rem;
}

.aupa__reset-form {
  display: grid;
  gap: 0.65rem;
  max-width: 360px;
}

.aupa__info {
  font-size: 0.9rem;
  color: #1e40af;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.65rem;
}

.aupa__success {
  margin-top: 0.7rem;
  font-size: 0.85rem;
  color: #065f46;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.6rem;
}

.aupa__error {
  margin-top: 0.7rem;
  font-size: 0.85rem;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.6rem;
}
</style>
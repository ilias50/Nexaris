<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useAdminEmailSettingsApiAccess, type EmailSettingsDto } from '@/composables/useAdminEmailSettingsApiAccess'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const adminEmailApi = useAdminEmailSettingsApiAccess()

const host = ref('')
const port = ref(587)
const username = ref('')
const password = ref('')
const fromAddress = ref('')
const smtpAuth = ref(true)
const starttls = ref(true)
const sslTrust = ref('')

const loading = ref(false)
const saving = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await adminEmailApi.getEmailSettings()
    if (res.status === 204) return // no config yet
    const data: EmailSettingsDto = res.data
    host.value = data.host ?? ''
    port.value = data.port ?? 587
    username.value = data.username ?? ''
    password.value = '' // never pre-fill
    fromAddress.value = data.fromAddress ?? ''
    smtpAuth.value = data.smtpAuth ?? true
    starttls.value = data.starttls ?? true
    sslTrust.value = data.sslTrust ?? ''
  } catch {
    errorMessage.value = t('adminEmail.messages.loadError')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  successMessage.value = ''
  errorMessage.value = ''
  try {
    await adminEmailApi.saveEmailSettings({
      host: host.value.trim(),
      port: port.value,
      username: username.value.trim(),
      password: password.value || null,
      fromAddress: fromAddress.value.trim(),
      smtpAuth: smtpAuth.value,
      starttls: starttls.value,
      sslTrust: sslTrust.value.trim() || null,
    })
    successMessage.value = t('adminEmail.messages.saved')
    password.value = ''
  } catch {
    errorMessage.value = t('adminEmail.messages.saveError')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppLayout :title="t('adminEmail.title')">
    <div class="email-panel">
      <div class="email-panel__header">
        <h3>{{ t('adminEmail.title') }}</h3>
        <p>{{ t('adminEmail.subtitle') }}</p>
      </div>

      <div v-if="successMessage" class="email-panel__alert email-panel__alert--success">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="email-panel__alert email-panel__alert--error">
        {{ errorMessage }}
      </div>

      <form v-if="!loading" class="email-form" @submit.prevent="save">
        <div class="email-form__row">
          <BaseInput
            v-model="host"
            :label="t('adminEmail.hostLabel')"
            type="text"
            :placeholder="t('adminEmail.hostPlaceholder')"
            required
          />
          <BaseInput
            v-model.number="port"
            :label="t('adminEmail.portLabel')"
            type="number"
            min="1"
            max="65535"
            required
          />
        </div>

        <div class="email-form__row">
          <BaseInput
            v-model="username"
            :label="t('adminEmail.usernameLabel')"
            type="text"
            :placeholder="t('adminEmail.usernamePlaceholder')"
            required
          />
          <BaseInput
            v-model="password"
            :label="t('adminEmail.passwordLabel')"
            type="password"
            :placeholder="t('adminEmail.passwordPlaceholder')"
          />
        </div>

        <BaseInput
          v-model="fromAddress"
          :label="t('adminEmail.fromLabel')"
          type="email"
          :placeholder="t('adminEmail.fromPlaceholder')"
          required
        />

        <BaseInput
          v-model="sslTrust"
          :label="t('adminEmail.sslTrustLabel')"
          type="text"
          :placeholder="t('adminEmail.sslTrustPlaceholder')"
        />

        <div class="email-form__checkboxes">
          <label class="email-form__checkbox">
            <input type="checkbox" v-model="smtpAuth" />
            {{ t('adminEmail.smtpAuthLabel') }}
          </label>
          <label class="email-form__checkbox">
            <input type="checkbox" v-model="starttls" />
            {{ t('adminEmail.starttlsLabel') }}
          </label>
        </div>

        <div class="email-form__actions">
          <BaseButton type="submit" :loading="saving">{{ t('adminEmail.save') }}</BaseButton>
        </div>
      </form>
    </div>
  </AppLayout>
</template>

<style scoped>
.email-panel {
  max-width: 680px;
}
.email-panel__header {
  margin-bottom: 1.5rem;
}
.email-panel__header h3 {
  font-size: 1.15rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.email-panel__header p {
  font-size: 0.875rem;
  color: var(--color-text-muted, #666);
}
.email-panel__alert {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}
.email-panel__alert--success {
  background: var(--color-feedback-success-bg);
  color: var(--color-feedback-success-text);
  border: 1px solid var(--color-feedback-success-border);
}
.email-panel__alert--error {
  background: var(--color-feedback-error-bg);
  color: var(--color-feedback-error-text);
  border: 1px solid var(--color-feedback-error-border);
}
.email-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.email-form__row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1rem;
}
.email-form__checkboxes {
  display: flex;
  gap: 1.5rem;
}
.email-form__checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  cursor: pointer;
}
.email-form__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

@media (max-width: 900px) {
  .email-panel {
    max-width: 100%;
  }

  .email-form__row {
    grid-template-columns: 1fr;
  }

  .email-form__checkboxes {
    flex-wrap: wrap;
    gap: 0.75rem 1rem;
  }

  .email-form__actions {
    justify-content: stretch;
  }

  .email-form__actions :deep(button) {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .email-panel__header h3 {
    font-size: 1.02rem;
  }

  .email-panel__alert {
    padding: 0.65rem 0.75rem;
  }
}
</style>

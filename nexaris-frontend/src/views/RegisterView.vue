<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import { getApiErrorStatus, getApiValidationErrors } from '@/utils/apiError'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import { useRegisterApiAccess } from '@/composables/useRegisterApiAccess'
import { passwordsMatch } from '@/utils/validation'

const router = useRouter()
const auth = useAuthStore()
const { t } = useI18n()
const registerApi = useRegisterApiAccess()

const firstName = ref('')
const lastName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')

const loading = ref(false)
const loadingStatus = ref(true)
const registrationEnabled = ref(false)
const success = ref('')
const error = ref('')
const fieldErrors = ref<Partial<Record<'firstName' | 'lastName' | 'email' | 'password' | 'confirmPassword', string>>>({})

function clearErrors() {
  error.value = ''
  success.value = ''
  fieldErrors.value = {}
}

function getFirstFieldError() {
  const order: Array<keyof typeof fieldErrors.value> = ['firstName', 'lastName', 'email', 'password', 'confirmPassword']
  for (const key of order) {
    const message = fieldErrors.value[key]
    if (message) return message
  }
  return ''
}

function validateRegistrationForm() {
  fieldErrors.value = {}

  if (!firstName.value.trim()) {
    fieldErrors.value.firstName = t('register.firstNameRequired')
  }
  if (!lastName.value.trim()) {
    fieldErrors.value.lastName = t('register.lastNameRequired')
  }
  if (!email.value.trim()) {
    fieldErrors.value.email = t('register.emailRequired')
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    fieldErrors.value.email = t('register.emailInvalid')
  }
  if (!password.value) {
    fieldErrors.value.password = t('register.passwordRequired')
  } else if (password.value.length < 8) {
    fieldErrors.value.password = t('register.passwordTooShort')
  }
  if (!confirmPassword.value) {
    fieldErrors.value.confirmPassword = t('register.confirmPasswordRequired')
  }
  if (password.value && confirmPassword.value && !passwordsMatch(password.value, confirmPassword.value)) {
    fieldErrors.value.confirmPassword = t('register.passwordMismatch')
  }

  const firstError = getFirstFieldError()
  if (firstError) {
    error.value = firstError
    return false
  }

  return true
}

async function loadRegistrationStatus() {
  loadingStatus.value = true
  try {
    registrationEnabled.value = await registerApi.getRegistrationEnabled()
  } catch {
    registrationEnabled.value = false
  } finally {
    loadingStatus.value = false
  }
}

async function handleRegister() {
  clearErrors()

  if (!registrationEnabled.value) {
    error.value = t('register.closed')
    return
  }

  if (!validateRegistrationForm()) {
    return
  }

  loading.value = true

  try {
    await registerApi.register({
      firstName: firstName.value.trim(),
      lastName: lastName.value.trim(),
      email: email.value.trim(),
      password: password.value,
    })
    await auth.login({ email: email.value.trim(), password: password.value })
    await router.push('/dashboard')
  } catch (apiError: unknown) {
    const validationErrors = getApiValidationErrors(apiError)
    if (validationErrors) {
      fieldErrors.value = {
        firstName: validationErrors.firstName,
        lastName: validationErrors.lastName,
        email: validationErrors.email,
        password: validationErrors.password,
      }
      error.value = getFirstFieldError() || t('register.genericError')
      return
    }

    const status = getApiErrorStatus(apiError)
    if (status === 403) {
      error.value = t('register.closed')
      registrationEnabled.value = false
    } else {
      error.value = resolveApiErrorMessage(apiError, t, {
        conflict: 'register.emailExists',
        unauthorized: 'register.autoLoginFailed',
        server: 'register.serviceUnavailable',
        generic: 'register.genericError',
      })
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadRegistrationStatus)
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-card__brand">
        <span class="register-card__logo">N</span>
        <span class="register-card__app-name">{{ t('app.name') }}</span>
      </div>
      <p class="register-card__subtitle">{{ t('register.subtitle') }}</p>

      <div v-if="loadingStatus" class="register-card__status">{{ t('register.loadingStatus') }}</div>

      <div v-else-if="!registrationEnabled" class="register-card__status register-card__status--closed">
        {{ t('register.closed') }}
      </div>

      <form v-else class="register-card__form" @submit.prevent="handleRegister">
        <BaseInput
          v-model="firstName"
          :label="t('register.firstNameLabel')"
          type="text"
          :placeholder="t('register.firstNamePlaceholder')"
          :error="fieldErrors.firstName"
        />
        <BaseInput
          v-model="lastName"
          :label="t('register.lastNameLabel')"
          type="text"
          :placeholder="t('register.lastNamePlaceholder')"
          :error="fieldErrors.lastName"
        />
        <BaseInput
          v-model="email"
          :label="t('register.emailLabel')"
          type="email"
          :placeholder="t('register.emailPlaceholder')"
          :error="fieldErrors.email"
        />
        <BaseInput
          v-model="password"
          :label="t('register.passwordLabel')"
          type="password"
          :placeholder="t('register.passwordPlaceholder')"
          :error="fieldErrors.password"
        />
        <BaseInput
          v-model="confirmPassword"
          :label="t('register.confirmPasswordLabel')"
          type="password"
          :placeholder="t('register.passwordPlaceholder')"
          :error="fieldErrors.confirmPassword"
        />

        <p v-if="error" class="register-card__error">{{ error }}</p>
        <p v-if="success" class="register-card__success">{{ success }}</p>

        <BaseButton type="submit" size="lg" :loading="loading" style="width: 100%">
          {{ t('register.submit') }}
        </BaseButton>
      </form>

      <button class="register-card__back" type="button" @click="router.push('/login')">
        {{ t('register.backToLogin') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 1rem;
}

.register-card {
  background: var(--color-surface);
  padding: 2.25rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 460px;
}

.register-card__brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.5rem;
}

.register-card__logo {
  width: 36px;
  height: 36px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.15rem;
}

.register-card__app-name {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
}

.register-card__subtitle {
  color: var(--color-text-muted);
  margin-bottom: 1.25rem;
  font-size: 0.9rem;
}

.register-card__form {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}

.register-card__status {
  padding: 0.65rem 0.8rem;
  border-radius: var(--radius-sm);
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e3a8a;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.register-card__status--closed {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.register-card__error {
  font-size: 0.875rem;
  color: var(--color-danger);
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
}

.register-card__success {
  font-size: 0.875rem;
  color: #065f46;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
}

.register-card__back {
  margin-top: 1rem;
  width: 100%;
  border: none;
  background: transparent;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 0.4rem;
}

.register-card__back:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .register-card {
    padding: 1.5rem 1rem;
  }
}
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import BaseInput from '@/components/BaseInput.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useLoginApiAccess } from '@/composables/useLoginApiAccess'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const auth = useAuthStore()
const { t } = useI18n()
const loginApi = useLoginApiAccess()
const { theme, toggleTheme } = useTheme()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const registrationEnabled = ref(false)

async function loadRegistrationStatus() {
  try {
    registrationEnabled.value = await loginApi.getRegistrationEnabled()
  } catch {
    registrationEnabled.value = false
  }
}

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login({ email: email.value, password: password.value })
    router.push('/dashboard')
  } catch (apiError: unknown) {
    error.value = resolveApiErrorMessage(
      apiError,
      t,
      {
        unauthorized: 'login.invalidCredentials',
        server: 'login.serviceUnavailable',
        generic: 'login.genericError',
      },
      { useApiMessageForUnauthorized: true },
    )
  } finally {
    loading.value = false
  }
}

onMounted(loadRegistrationStatus)
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-card__topbar">
        <div class="login-card__brand">
          <span class="login-card__logo">N</span>
          <span class="login-card__app-name">{{ t('app.name') }}</span>
        </div>

        <BaseButton type="button" variant="ghost" size="sm" class="login-card__theme-toggle" @click="toggleTheme">
          <svg v-if="theme === 'dark'" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
          </svg>
          <svg v-else width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
          <span>{{ t('nav.themeToggle') }}</span>
        </BaseButton>
      </div>
      <p class="login-card__subtitle">{{ t('login.subtitle') }}</p>

      <form class="login-card__form" @submit.prevent="handleLogin">
        <BaseInput
          v-model="email"
          :label="t('login.emailLabel')"
          type="email"
          :placeholder="t('login.emailPlaceholder')"
        />
        <BaseInput
          v-model="password"
          :label="t('login.passwordLabel')"
          type="password"
          :placeholder="t('login.passwordPlaceholder')"
        />

        <p v-if="error" class="login-card__error">{{ error }}</p>

        <BaseButton type="submit" size="lg" :loading="loading" style="width: 100%">
          {{ t('login.submit') }}
        </BaseButton>
      </form>

      <RouterLink v-if="registrationEnabled" to="/register" class="login-card__register-link">
        {{ t('login.registerLink') }}
      </RouterLink>

      <p v-else class="login-card__register-closed">{{ t('login.registerClosed') }}</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.login-card {
  background: var(--color-surface);
  padding: 2.5rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 420px;
}

.login-card__topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.login-card__brand {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.login-card__logo {
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

.login-card__app-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
}

.login-card__theme-toggle {
  flex-shrink: 0;
}

.login-card__subtitle {
  color: var(--color-text-muted);
  margin-bottom: 1.75rem;
  font-size: 0.9rem;
}

.login-card__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.login-card__error {
  font-size: 0.875rem;
  color: var(--color-danger);
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
}

.login-card__register-link {
  margin-top: 0.9rem;
  display: inline-block;
  font-size: 0.87rem;
  color: var(--color-primary);
}

.login-card__register-link:hover {
  text-decoration: underline;
}

.login-card__register-closed {
  margin-top: 0.9rem;
  font-size: 0.84rem;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .login-page {
    padding: 1rem;
  }

  .login-card {
    padding: 1.5rem 1rem;
  }

  .login-card__topbar {
    align-items: center;
  }

  .login-card__app-name {
    font-size: 1.3rem;
  }

  .login-card__theme-toggle span {
    display: none;
  }
}
</style>


<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useI18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'
import { resolveProfileImageUrl } from '@/utils/profileImage'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useProfileApiAccess } from '@/composables/useProfileApiAccess'
import { getApiBaseUrl } from '@/config/runtime'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import { hasAllRequired, trimOrEmpty } from '@/utils/validation'
import type { CountryOption } from '@/api/auth'

const { t, setLocaleFromLanguageCode } = useI18n()
const auth = useAuthStore()
const profileApi = useProfileApiAccess()

const uploading = ref(false)
const deleting = ref(false)
const savingInfo = ref(false)
const photoMessage = ref('')
const photoError = ref('')
const infoMessage = ref('')
const infoError = ref('')
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const apiBaseUrl = getApiBaseUrl()

const firstName = ref('')
const lastName = ref('')
const email = ref('')
const countryCode = ref('')
const languageCode = ref('')
const countryOptions = ref<CountryOption[]>([])

const languageOptions = [
  { code: 'FR', labelKey: 'locale.fr' as const },
  { code: 'EN', labelKey: 'locale.en' as const },
  { code: 'NL', labelKey: 'locale.nl' as const },
  { code: 'DE', labelKey: 'locale.de' as const },
]

watch(
  () => auth.user,
  (user) => {
    firstName.value = user?.firstName ?? ''
    lastName.value = user?.lastName ?? ''
    email.value = user?.email ?? ''
    countryCode.value = (user?.countryCode ?? '').toUpperCase()
    languageCode.value = (user?.languageCode ?? '').toUpperCase()
  },
  { immediate: true },
)

async function loadCountryOptions() {
  try {
    countryOptions.value = await profileApi.getSupportedCountries()
  } catch {
    countryOptions.value = [
      { code: 'BE', name: 'Belgique' },
      { code: 'FR', name: 'France' },
    ]
  }
}

onMounted(() => {
  loadCountryOptions()
})

const profileImageSrc = computed(() => {
  if (previewUrl.value) return previewUrl.value
  return resolveProfileImageUrl(auth.user?.profileImageUrl, apiBaseUrl)
})

const selectedFileName = computed(() => selectedFile.value?.name ?? '')

function openFilePicker() {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  photoMessage.value = ''
  photoError.value = ''

  if (!file.type.startsWith('image/')) {
    photoError.value = t('profile.errors.invalidType')
    return
  }

  if (file.size > 2_000_000) {
    photoError.value = t('profile.errors.tooLarge')
    return
  }

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

async function saveProfileInfo() {
  if (!auth.user?.id) return

  infoMessage.value = ''
  infoError.value = ''

  if (!hasAllRequired([firstName.value, lastName.value, email.value])) {
    infoError.value = t('profile.errors.requiredFields')
    return
  }

  savingInfo.value = true
  try {
    await profileApi.updateUser(auth.user.id, {
      firstName: trimOrEmpty(firstName.value),
      lastName: trimOrEmpty(lastName.value),
      email: trimOrEmpty(email.value),
      countryCode: countryCode.value || undefined,
      languageCode: languageCode.value || undefined,
    })
    await auth.syncUser()
    setLocaleFromLanguageCode(languageCode.value)
    infoMessage.value = t('profile.messages.infoUpdated')
  } catch (error: unknown) {
    infoError.value = resolveApiErrorMessage(error, t, {
      conflict: 'profile.errors.emailExists',
      server: 'profile.errors.serviceUnavailable',
      generic: 'profile.errors.infoUpdateFailed',
    })
  } finally {
    savingInfo.value = false
  }
}

async function uploadImage() {
  if (!auth.user?.id || !selectedFile.value) return

  uploading.value = true
  photoError.value = ''
  photoMessage.value = ''

  try {
    const profileImageUrl = await profileApi.uploadProfileImage(auth.user.id, selectedFile.value)
    auth.setProfileImageUrl(profileImageUrl)
    photoMessage.value = t('profile.messages.uploadSuccess')
    selectedFile.value = null
    previewUrl.value = null
  } catch {
    photoError.value = t('profile.errors.uploadFailed')
  } finally {
    uploading.value = false
  }
}

async function deleteImage() {
  if (!auth.user?.id) return

  deleting.value = true
  photoError.value = ''
  photoMessage.value = ''

  try {
    await profileApi.deleteProfileImage(auth.user.id)
    auth.setProfileImageUrl(null)
    previewUrl.value = null
    selectedFile.value = null
    photoMessage.value = t('profile.messages.deleteSuccess')
  } catch {
    photoError.value = t('profile.errors.deleteFailed')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <AppLayout :title="t('profile.title')">
    <div class="profile-panel">
      <div class="profile-panel__header">
        <h3>{{ t('profile.info.title') }}</h3>
        <p>{{ t('profile.info.subtitle') }}</p>
      </div>

      <div class="profile-grid">
        <aside class="profile-photo-card">
          <div class="profile-avatar">
            <img v-if="profileImageSrc" :src="profileImageSrc" alt="Profile" />
            <span v-else>{{ auth.fullName?.charAt(0).toUpperCase() }}</span>
          </div>

          <div class="profile-photo-card__meta">
            <p class="profile-photo-card__name">{{ auth.fullName }}</p>
            <p class="profile-photo-card__subtitle">{{ t('profile.photo.subtitle') }}</p>
          </div>

          <div class="profile-photo-card__actions">
            <input ref="fileInputRef" class="profile-photo-card__file-input" type="file" accept="image/*" @change="handleFileChange" />

            <div class="profile-actions__buttons">
              <BaseButton type="button" variant="ghost" @click="openFilePicker">
                {{ t('profile.photo.choose') }}
              </BaseButton>
              <BaseButton type="button" :loading="uploading" :disabled="!selectedFile" @click="uploadImage">
                {{ t('profile.photo.upload') }}
              </BaseButton>
              <BaseButton
                type="button"
                variant="ghost"
                :loading="deleting"
                :disabled="!auth.user?.profileImageUrl && !previewUrl"
                @click="deleteImage"
              >
                {{ t('profile.photo.delete') }}
              </BaseButton>
            </div>

            <p v-if="selectedFileName" class="profile-photo-card__filename">{{ selectedFileName }}</p>

            <p v-if="photoMessage" class="profile-actions__message profile-actions__message--success">
              {{ photoMessage }}
            </p>
            <p v-if="photoError" class="profile-actions__message profile-actions__message--error">
              {{ photoError }}
            </p>
          </div>
        </aside>

        <form class="profile-form-card" @submit.prevent="saveProfileInfo">
          <h4 class="profile-form-card__title">{{ t('profile.info.title') }}</h4>

          <BaseInput
            v-model="firstName"
            :label="t('profile.info.firstNameLabel')"
            type="text"
            :placeholder="t('profile.info.firstNamePlaceholder')"
          />
          <BaseInput
            v-model="lastName"
            :label="t('profile.info.lastNameLabel')"
            type="text"
            :placeholder="t('profile.info.lastNamePlaceholder')"
          />
          <BaseInput
            v-model="email"
            :label="t('profile.info.emailLabel')"
            type="email"
            :placeholder="t('profile.info.emailPlaceholder')"
          />

          <div class="profile-select-field">
            <label class="profile-select-field__label" for="profile-country">{{ t('profile.info.countryLabel') }}</label>
            <select id="profile-country" v-model="countryCode" class="profile-select-field__control">
              <option value="">{{ t('profile.info.countryPlaceholder') }}</option>
              <option v-for="country in countryOptions" :key="country.code" :value="country.code">
                {{ country.name }} ({{ country.code }})
              </option>
            </select>
          </div>

          <div class="profile-select-field">
            <label class="profile-select-field__label" for="profile-language">{{ t('profile.info.languageLabel') }}</label>
            <select id="profile-language" v-model="languageCode" class="profile-select-field__control">
              <option value="">{{ t('profile.info.languagePlaceholder') }}</option>
              <option v-for="option in languageOptions" :key="option.code" :value="option.code">
                {{ t(option.labelKey) }}
              </option>
            </select>
          </div>

          <div class="profile-form-card__footer">
            <div>
              <p v-if="infoMessage" class="profile-actions__message profile-actions__message--success">
                {{ infoMessage }}
              </p>
              <p v-if="infoError" class="profile-actions__message profile-actions__message--error">
                {{ infoError }}
              </p>
            </div>

            <BaseButton type="submit" :loading="savingInfo">
              {{ t('profile.info.save') }}
            </BaseButton>
          </div>
        </form>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.profile-panel {
  background: linear-gradient(180deg, var(--color-soft, rgba(248, 250, 252, 0.8)) 0%, var(--color-surface) 35%);
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: var(--radius-md);
  padding: 1.35rem;
  max-width: 980px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.profile-panel__header h3 {
  font-size: 1.18rem;
  margin-bottom: 0.2rem;
}

.profile-panel__header p {
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.profile-grid {
  margin-top: 1.1rem;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.profile-photo-card {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: var(--color-surface);
}

.profile-form-card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: var(--color-surface);
}

.profile-select-field {
  display: grid;
  gap: 0.35rem;
}

.profile-select-field__label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
}

.profile-select-field__control {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-sm);
  padding: 0.6rem 0.75rem;
  font: inherit;
  color: var(--color-text);
  background: var(--color-surface);
}

.profile-select-field__control:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.75);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.profile-form-card__title {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.1rem;
}

.profile-form-card__footer {
  margin-top: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.profile-photo-card__meta {
  display: grid;
  gap: 0.2rem;
}

.profile-photo-card__name {
  font-size: 1rem;
  font-weight: 700;
}

.profile-photo-card__subtitle {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.profile-avatar {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--color-primary), #0f4aa6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.2rem;
  font-weight: 700;
  overflow: hidden;
  box-shadow: 0 10px 22px rgba(15, 74, 166, 0.3);
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-photo-card__actions {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.profile-photo-card__file-input {
  display: none;
}

.profile-photo-card__filename {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  word-break: break-all;
}

.profile-actions__buttons {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.profile-actions__message {
  font-size: 0.82rem;
  line-height: 1.35;
}

.profile-actions__message--success {
  color: var(--color-success);
}

.profile-actions__message--error {
  color: var(--color-danger);
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-form-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-avatar {
    width: 100px;
    height: 100px;
  }
}
</style>

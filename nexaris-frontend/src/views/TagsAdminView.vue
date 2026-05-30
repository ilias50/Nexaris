<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useI18n } from '@/i18n'
import { useTagsAdminApiAccess, type AdminUser, type PlanningTag } from '@/composables/useTagsAdminApiAccess'
import { formatUserDisplayName } from '@/utils/users'
import { isBlank, trimOrEmpty, trimOrNull } from '@/utils/validation'

const { t } = useI18n()
const tagsApi = useTagsAdminApiAccess()

const tags = ref<PlanningTag[]>([])
const users = ref<AdminUser[]>([])
const loading = ref(false)
const error = ref('')
const creating = ref(false)

const name = ref('')
const description = ref('')
const color = ref('#3b82f6')
const blocking = ref(false)

function normalizeColor(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return null
  return trimmed.startsWith('#') ? trimmed : `#${trimmed}`
}

async function loadTags() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await tagsApi.getTags()
    tags.value = [...data].sort((left, right) => left.name.localeCompare(right.name))
  } catch {
    error.value = t('tagsAdmin.messages.loadError')
  } finally {
    loading.value = false
  }
}

async function createTag() {
  if (isBlank(name.value)) return
  creating.value = true
  error.value = ''
  try {
    await tagsApi.createTag({
      name: trimOrEmpty(name.value),
      description: trimOrNull(description.value),
      color: normalizeColor(color.value),
      blocking: blocking.value,
    })
    name.value = ''
    description.value = ''
    color.value = '#3b82f6'
    blocking.value = false
    await loadTags()
  } catch {
    error.value = t('tagsAdmin.messages.saveError')
  } finally {
    creating.value = false
  }
}

async function changeTagColor(tag: PlanningTag, nextColor: string) {
  const normalized = normalizeColor(nextColor)
  if (!normalized) return
  try {
    const { data } = await tagsApi.updateTagColor(tag.id, normalized)
    const index = tags.value.findIndex((entry) => entry.id === tag.id)
    if (index !== -1) tags.value[index] = data
  } catch {
    error.value = t('tagsAdmin.messages.saveError')
  }
}

function colorStyle(tag: PlanningTag) {
  if (!tag.color) return {}
  return {
    background: `${tag.color}22`,
    borderColor: `${tag.color}88`,
    color: tag.color,
  }
}

function flagLabel(value: boolean, yesLabel: string, noLabel: string) {
  return value ? yesLabel : noLabel
}

function creatorName(userId: number | null) {
  if (userId == null) return 'system'
  const user = users.value.find((entry) => entry.id === userId)
  if (!user) return 'user'
  return formatUserDisplayName(user)
}

async function loadUsers() {
  users.value = await tagsApi.listUsersSafe()
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadTags()])
})
</script>

<template>
  <AppLayout :title="t('tagsAdmin.title')">
    <div class="tag-admin">
      <div class="tag-admin__header">
        <h3 class="tag-admin__title">{{ t('tagsAdmin.title') }}</h3>
        <p class="tag-admin__subtitle">{{ t('tagsAdmin.subtitle') }}</p>
      </div>

      <section class="tag-admin__card">
        <h4 class="tag-admin__section-title">{{ t('tagsAdmin.createTitle') }}</h4>
        <div class="tag-admin__form">
          <BaseInput v-model="name" :label="t('tagsAdmin.nameLabel')" type="text" :placeholder="t('tagsAdmin.namePlaceholder')" />
          <BaseInput v-model="description" :label="t('tagsAdmin.descriptionLabel')" type="text" :placeholder="t('tagsAdmin.descriptionPlaceholder')" />
          <div class="tag-admin__field">
            <label class="tag-admin__label">{{ t('tagsAdmin.colorLabel') }}</label>
            <input v-model="color" type="color" class="tag-admin__color-input" />
          </div>
          <div class="tag-admin__toggles">
            <label class="tag-admin__toggle">
              <input v-model="blocking" type="checkbox" />
              {{ t('tagsAdmin.blockingLabel') }}
            </label>
            <p class="tag-admin__help">{{ t('tagsAdmin.blockingHelp') }}</p>
          </div>
        </div>
        <div class="tag-admin__actions">
          <BaseButton type="button" :loading="creating" @click="createTag">
            {{ t('tagsAdmin.createButton') }}
          </BaseButton>
        </div>
      </section>

      <p v-if="loading" class="tag-admin__hint">{{ t('tagsAdmin.loading') }}</p>
      <p v-else-if="error" class="tag-admin__error">{{ error }}</p>
      <p v-else-if="tags.length === 0" class="tag-admin__hint">{{ t('tagsAdmin.empty') }}</p>

      <section v-else class="tag-admin__list">
        <div v-for="tag in tags" :key="tag.id" class="tag-admin__item">
          <div class="tag-admin__item-main">
            <div class="tag-admin__item-row">
              <span class="tag-admin__badge" :style="colorStyle(tag)">{{ tag.name }}</span>
              <span class="tag-admin__flag" :class="{ 'tag-admin__flag--warning': tag.blocking }">
                {{ flagLabel(tag.blocking, t('tagsAdmin.blockingYes'), t('tagsAdmin.blockingNo')) }}
              </span>
            </div>
            <p v-if="tag.description" class="tag-admin__desc">{{ tag.description }}</p>
            <p class="tag-admin__meta">{{ t('tagsAdmin.createdBy') }} {{ creatorName(tag.createdByUserId) }}</p>
          </div>

          <div class="tag-admin__color-box">
            <label class="tag-admin__label">{{ t('tagsAdmin.changeColorLabel') }}</label>
            <input
              :value="tag.color ?? '#94a3b8'"
              type="color"
              class="tag-admin__color-input"
              :title="t('tagsAdmin.changeColorLabel')"
              @change="changeTagColor(tag, ($event.target as HTMLInputElement).value)"
            />
          </div>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<style scoped>
.tag-admin {
  max-width: 980px;
}

.tag-admin__header {
  margin-bottom: 1rem;
}

.tag-admin__title {
  margin: 0 0 0.2rem;
}

.tag-admin__subtitle {
  margin: 0;
  color: var(--color-text-muted);
}

.tag-admin__card {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 1rem;
  margin-bottom: 1rem;
}

.tag-admin__section-title {
  margin: 0 0 0.85rem;
}

.tag-admin__form {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 0.75rem;
  align-items: end;
}

.tag-admin__field {
  display: grid;
  gap: 0.25rem;
}

.tag-admin__label {
  font-size: 0.85rem;
  font-weight: 600;
}

.tag-admin__color-input {
  width: 2.65rem;
  height: 2.4rem;
  padding: 0;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-sm);
  background: #fff;
  cursor: pointer;
}

.tag-admin__toggles {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.tag-admin__toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
  color: var(--color-text);
}

.tag-admin__help {
  margin: 0.15rem 0 0;
  grid-column: 1 / -1;
  font-size: 0.82rem;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.tag-admin__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.85rem;
}

.tag-admin__list {
  display: grid;
  gap: 0.75rem;
}

.tag-admin__item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 0.9rem 1rem;
}

.tag-admin__item-main {
  min-width: 0;
}

.tag-admin__item-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  align-items: center;
}

.tag-admin__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.18rem 0.55rem;
  border-radius: 999px;
  border: 1px solid;
  font-size: 0.82rem;
  font-weight: 700;
}

.tag-admin__flag {
  display: inline-flex;
  align-items: center;
  padding: 0.16rem 0.5rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: var(--color-text-muted);
  font-size: 0.76rem;
}

.tag-admin__flag--warning {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.tag-admin__flag--muted {
  opacity: 0.7;
}

.tag-admin__desc {
  margin: 0.45rem 0 0;
  color: var(--color-text-muted);
}

.tag-admin__meta {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.tag-admin__color-box {
  display: grid;
  gap: 0.25rem;
  justify-items: end;
}

.tag-admin__hint {
  color: var(--color-text-muted);
}

.tag-admin__error {
  color: var(--color-error, #dc2626);
}

@media (max-width: 900px) {
  .tag-admin__form {
    grid-template-columns: 1fr;
  }

  .tag-admin__item {
    flex-direction: column;
  }

  .tag-admin__color-box {
    justify-items: start;
  }
}
</style>

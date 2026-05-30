<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import type { CatalogEntry } from '@/types/domain'
import { useI18n } from '@/i18n'
import { formatRoleLabel } from '@/utils/roles'

const { t } = useI18n()

const props = defineProps<{
  title: string
  hint: string
  inputLabel: string
  inputPlaceholder: string
  inputValue: string
  entries: CatalogEntry[]
  manageLinkTo?: string
  manageLinkLabel?: string
  displayAsRoleLabel?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:inputValue', value: string): void
  (event: 'add'): void
  (event: 'remove', value: string): void
  (event: 'color-change', payload: { value: string; color: string }): void
}>()

const normalizedEntries = computed(() => {
  return props.entries.map((entry) => ({
    ...entry,
    displayLabel: props.displayAsRoleLabel ? formatRoleLabel(entry.value) : entry.value,
  }))
})

function updateInput(value: string | number) {
  emit('update:inputValue', String(value ?? ''))
}

function changeColor(value: string, event: Event) {
  const color = (event.target as HTMLInputElement).value
  emit('color-change', { value, color })
}
</script>

<template>
  <section class="ccm">
    <div class="ccm__head">
      <h4>{{ title }}</h4>
      <RouterLink v-if="manageLinkTo && manageLinkLabel" class="ccm__manage-link" :to="manageLinkTo">
        {{ manageLinkLabel }}
      </RouterLink>
    </div>
    <p class="ccm__hint">{{ hint }}</p>
    <div class="ccm__row">
      <BaseInput
        :model-value="inputValue"
        :label="inputLabel"
        type="text"
        :placeholder="inputPlaceholder"
        @update:model-value="updateInput"
        @keyup.enter="emit('add')"
      />
      <BaseButton type="button" @click="emit('add')">{{ t('adminOrgCatalogs.add') }}</BaseButton>
    </div>
    <div class="ccm__chips">
      <div
        v-for="entry in normalizedEntries"
        :key="entry.value"
        class="ccm__chip"
        :style="entry.color ? { background: entry.color + '22', borderColor: entry.color + '88', color: entry.color } : {}"
      >
        <input
          type="color"
          class="ccm__color-swatch"
          :value="entry.color ?? '#94a3b8'"
          :title="t('adminOrgCatalogs.colorLabel')"
          @change="changeColor(entry.value, $event)"
        />
        <span>{{ entry.displayLabel }}</span>
        <button class="ccm__chip-del" @click="emit('remove', entry.value)">✕</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ccm {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 1rem;
}

.ccm__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}

.ccm h4 {
  margin: 0 0 0.25rem;
}

.ccm__manage-link {
  font-size: 0.82rem;
  color: var(--color-primary);
  text-decoration: none;
}

.ccm__manage-link:hover {
  text-decoration: underline;
}

.ccm__hint {
  margin: 0 0 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.ccm__row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.6rem;
  align-items: end;
  margin-bottom: 0.8rem;
}

.ccm__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.ccm__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid rgba(148, 163, 184, 0.3);
  background: rgba(148, 163, 184, 0.08);
  padding: 0.2rem 0.55rem 0.2rem 0.35rem;
  border-radius: 999px;
  font-size: 0.82rem;
  transition: background 0.12s, border-color 0.12s, color 0.12s;
}

.ccm__color-swatch {
  width: 1.1rem;
  height: 1.1rem;
  border: none;
  border-radius: 50%;
  padding: 0;
  cursor: pointer;
  background: none;
  flex-shrink: 0;
  overflow: hidden;
  appearance: none;
  -webkit-appearance: none;
}

.ccm__color-swatch::-webkit-color-swatch-wrapper {
  padding: 0;
  border-radius: 50%;
}

.ccm__color-swatch::-webkit-color-swatch {
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 50%;
}

.ccm__chip-del {
  border: none;
  background: none;
  cursor: pointer;
  color: inherit;
  opacity: 0.6;
  padding: 0 0.1rem;
  line-height: 1;
}

.ccm__chip-del:hover {
  opacity: 1;
  color: #dc2626 !important;
}
</style>
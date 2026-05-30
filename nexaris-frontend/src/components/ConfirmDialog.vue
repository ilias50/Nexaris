<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    message: string
    details?: string
    confirmText: string
    cancelText: string
    loading?: boolean
    closeOnBackdrop?: boolean
    confirmVariant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  }>(),
  {
    details: '',
    loading: false,
    closeOnBackdrop: true,
    confirmVariant: 'danger',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

function close() {
  emit('update:modelValue', false)
  emit('cancel')
}

function handleBackdropClick() {
  if (!props.closeOnBackdrop) return
  close()
}

function handleConfirm() {
  emit('confirm')
}
</script>

<template>
  <div v-if="props.modelValue" class="confirm-dialog__backdrop" @click="handleBackdropClick">
    <div class="confirm-dialog" role="dialog" aria-modal="true" @click.stop>
      <h3 class="confirm-dialog__title">{{ props.title }}</h3>
      <p class="confirm-dialog__text">{{ props.message }}</p>
      <p v-if="props.details" class="confirm-dialog__details">{{ props.details }}</p>
      <slot />
      <div class="confirm-dialog__actions">
        <BaseButton variant="ghost" type="button" @click="close">{{ props.cancelText }}</BaseButton>
        <BaseButton :variant="props.confirmVariant" type="button" :loading="props.loading" @click="handleConfirm">
          {{ props.confirmText }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-dialog__backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(1px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}

.confirm-dialog {
  width: min(460px, 100%);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
  box-shadow: var(--shadow-lg);
  display: grid;
  gap: 0.75rem;
}

.confirm-dialog__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.confirm-dialog__text {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.confirm-dialog__details {
  margin: 0;
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--color-text);
  background: rgba(148, 163, 184, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.55rem;
}

.confirm-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>

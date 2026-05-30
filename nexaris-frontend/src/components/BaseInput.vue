<script setup lang="ts">
defineProps<{
  label?: string
  type?: 'text' | 'email' | 'password' | 'number' | 'date' | 'time' | 'datetime-local'
  placeholder?: string
  error?: string
  disabled?: boolean
  modelValue: string | number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()
</script>

<template>
  <div class="field">
    <label v-if="label" class="field__label">{{ label }}</label>
    <input
      :type="type ?? 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :class="['field__input', { 'field__input--error': error }]"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <p v-if="error" class="field__error">{{ error }}</p>
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field__label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
}

.field__input {
  padding: 0.55rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.95rem;
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
  transition: border-color var(--transition), box-shadow var(--transition);
}

.field__input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(26, 86, 219, 0.1);
}

.field__input--error {
  border-color: var(--color-danger);
}

.field__input:disabled {
  background: var(--color-input-disabled);
  cursor: not-allowed;
}

.field__error {
  font-size: 0.8rem;
  color: var(--color-danger);
}
</style>

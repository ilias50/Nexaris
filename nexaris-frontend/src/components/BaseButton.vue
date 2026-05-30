<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
    size?: 'sm' | 'md' | 'lg'
    loading?: boolean
    disabled?: boolean
    type?: 'button' | 'submit' | 'reset'
  }>(),
  {
    variant: 'primary',
    size: 'md',
    loading: false,
    disabled: false,
    type: 'button',
  },
)
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="['btn', `btn--${variant}`, `btn--${size}`, { 'btn--loading': loading }]"
  >
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 500;
  transition: background var(--transition), opacity var(--transition);
  cursor: pointer;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Tailles */
.btn--sm { padding: 0.35rem 0.7rem; font-size: 0.8rem; }
.btn--md { padding: 0.55rem 1.1rem; font-size: 0.95rem; }
.btn--lg { padding: 0.75rem 1.5rem; font-size: 1.05rem; }

/* Variantes */
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--primary:hover:not(:disabled) { background: var(--color-primary-dark); }

.btn--secondary { background: var(--color-border); color: var(--color-text); }
.btn--secondary:hover:not(:disabled) { background: var(--color-secondary-hover); }

.btn--danger { background: var(--color-danger); color: #fff; }
.btn--danger:hover:not(:disabled) { background: var(--color-danger-dark); }

.btn--ghost {
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
.btn--ghost:hover:not(:disabled) { background: var(--color-border); color: var(--color-text); }

/* Spinner */
.btn__spinner {
  width: 0.9em;
  height: 0.9em;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

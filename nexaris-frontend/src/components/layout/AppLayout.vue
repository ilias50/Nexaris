<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import NotificationBell from '../NotificationBell.vue'
import { useAuthStore } from '@/stores/auth'

defineProps<{
  title?: string
}>()

const auth = useAuthStore()
</script>

<template>
  <div class="app-layout">
    <AppSidebar />
    <main class="app-layout__content">
      <header v-if="$slots.header || title || auth.isAuthenticated" class="app-layout__header">
        <slot name="header">
          <div class="header-left">
            <h1 class="app-layout__title">{{ title }}</h1>
          </div>
          <div class="header-right">
            <NotificationBell v-if="auth.isAuthenticated" />
          </div>
        </slot>
      </header>
      <div class="app-layout__body">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

.app-layout__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-layout__header {
  padding: 1.25rem 2rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.app-layout__title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.app-layout__body {
  flex: 1;
  padding: 1.75rem 2rem;
  overflow-y: auto;
}

@media (max-width: 1024px) {
  .app-layout__header {
    padding: 1rem 1.25rem;
  }

  .app-layout__body {
    padding: 1.25rem;
  }
}

@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
  }

  .app-layout__header {
    padding: 0.85rem 1rem;
  }

  .app-layout__title {
    font-size: 1.05rem;
  }

  .app-layout__body {
    padding: 1rem;
  }
}
</style>

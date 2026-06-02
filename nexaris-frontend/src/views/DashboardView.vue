<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import AppLayout from '@/components/layout/AppLayout.vue'

const auth = useAuthStore()
const { t } = useI18n()
</script>

<template>
  <AppLayout :title="t('dashboard.title')">
    <div class="dashboard">
      <div class="dashboard__welcome">
        <h2>{{ t('dashboard.welcomePrefix') }}, {{ auth.fullName }}</h2>
        <p>{{ t('dashboard.welcomeMessage') }}</p>
      </div>

      <div class="dashboard__cards">
        <div class="card">
          <div class="card__icon card__icon--blue">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.agendaCardTitle') }}</span>
            <RouterLink to="/agenda" class="card__action">{{ t('dashboard.agendaCardAction') }}</RouterLink>
          </div>
        </div>

        <div class="card">
          <div class="card__icon card__icon--green">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.profileCardTitle') }}</span>
            <RouterLink to="/profile" class="card__action">{{ t('dashboard.profileCardAction') }}</RouterLink>
          </div>
        </div>

        <div class="card">
          <div class="card__icon card__icon--teal">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M3 6h18M6 12h12M10 18h4" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.orgCardTitle') }}</span>
            <RouterLink to="/org" class="card__action">{{ t('dashboard.orgCardAction') }}</RouterLink>
          </div>
        </div>

        <div class="card">
          <div class="card__icon card__icon--slate">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 7h18s-3 0-3-7" /><path d="M13.7 21a2 2 0 0 1-3.4 0" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.notificationPreferencesCardTitle') }}</span>
            <RouterLink to="/notifications/preferences" class="card__action">{{ t('dashboard.notificationPreferencesCardAction') }}</RouterLink>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="card">
          <div class="card__icon card__icon--purple">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><circle cx="7" cy="7" r="1.5" fill="currentColor" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.tagsCardTitle') }}</span>
            <RouterLink to="/admin/tags" class="card__action">{{ t('dashboard.tagsCardAction') }}</RouterLink>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="card">
          <div class="card__icon card__icon--orange">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3" /><path d="M2 20c0-3.3 2.7-6 6-6h2" /><circle cx="17" cy="9" r="2" /><path d="M19 17h3M20.5 15.5v3" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.adminUsersCardTitle') }}</span>
            <RouterLink to="/admin/users" class="card__action">{{ t('dashboard.adminUsersCardAction') }}</RouterLink>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="card">
          <div class="card__icon card__icon--amber">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.adminOrgCatalogsCardTitle') }}</span>
            <RouterLink to="/admin/org-catalogs" class="card__action">{{ t('dashboard.adminOrgCatalogsCardAction') }}</RouterLink>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="card">
          <div class="card__icon card__icon--indigo">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="14" rx="2" /><path d="M8 3v4M16 3v4M4 10h16" /><path d="M12 13v4M10 15h4" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.adminPlanningRolesCardTitle') }}</span>
            <RouterLink to="/admin/planning-role-permissions" class="card__action">{{ t('dashboard.adminPlanningRolesCardAction') }}</RouterLink>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="card">
          <div class="card__icon card__icon--cyan">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></svg>
          </div>
          <div class="card__body">
            <span class="card__label">{{ t('dashboard.adminEmailCardTitle') }}</span>
            <RouterLink to="/admin/email-settings" class="card__action">{{ t('dashboard.adminEmailCardAction') }}</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.dashboard__welcome {
  margin-bottom: 2rem;
}

.dashboard__welcome h2 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.dashboard__welcome p {
  color: var(--color-text-muted);
}

.dashboard__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1.25rem;
}

.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition);
}

.card:hover {
  box-shadow: var(--shadow-md);
}

.card__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card__icon--blue { background: #dbeafe; color: #1d4ed8; }
.card__icon--green { background: #dcfce7; color: #15803d; }
.card__icon--purple { background: #ede9fe; color: #7c3aed; }
.card__icon--orange { background: #ffedd5; color: #c2410c; }
.card__icon--teal { background: #ccfbf1; color: #0f766e; }
.card__icon--slate { background: #e2e8f0; color: #334155; }
.card__icon--rose { background: #ffe4e6; color: #be123c; }
.card__icon--indigo { background: #e0e7ff; color: #4338ca; }
.card__icon--amber { background: #fef3c7; color: #b45309; }
.card__icon--cyan { background: #cffafe; color: #0e7490; }

.card__body {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.card__label {
  font-weight: 600;
  font-size: 0.95rem;
}

.card__action {
  font-size: 0.82rem;
  color: var(--color-primary);
}

.card__action:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .dashboard__welcome {
    margin-bottom: 1.25rem;
  }

  .dashboard__welcome h2 {
    font-size: 1.25rem;
  }

  .dashboard__cards {
    grid-template-columns: 1fr;
    gap: 0.9rem;
  }

  .card {
    padding: 1rem;
  }
}
</style>


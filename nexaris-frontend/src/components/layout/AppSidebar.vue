<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import { useRouter } from 'vue-router'
import { getApiBaseUrl } from '@/config/runtime'
import { resolveProfileImageUrl } from '@/utils/profileImage'
import { useTheme } from '@/composables/useTheme'

const auth = useAuthStore()
const router = useRouter()
const { t, locale, setLocale, setLocaleFromLanguageCode, availableLocales, getLocaleLabel } =
  useI18n()
const apiBaseUrl = getApiBaseUrl()
const baseUrl = import.meta.env.BASE_URL ?? '/'
const brandLogoSrc = `${baseUrl}logo-nexaris.png`
const hasBrandLogo = ref(true)

const selectedLocale = computed({
  get: () => locale.value,
  set: (next: string) => setLocale(next),
})

const profileImageSrc = computed(() => {
  return resolveProfileImageUrl(auth.user?.profileImageUrl, apiBaseUrl)
})

watch(
  () => auth.user?.languageCode,
  (languageCode: string | undefined) => {
    if (languageCode) {
      setLocaleFromLanguageCode(languageCode)
    }
  },
  { immediate: true },
)

const { theme, toggleTheme, init } = useTheme()
onMounted(init)

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

function handleBrandLogoError() {
  hasBrandLogo.value = false
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__brand">
      <img
        v-if="hasBrandLogo"
        :src="brandLogoSrc"
        alt="Nexaris"
        class="sidebar__logo"
        @error="handleBrandLogoError"
      />
      <span v-else class="sidebar__logo sidebar__logo--fallback">N</span>
      <span class="sidebar__name">{{ t('app.name') }}</span>
    </div>

    <nav class="sidebar__nav">
      <RouterLink to="/dashboard" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
        {{ t('nav.dashboard') }}
      </RouterLink>

      <RouterLink to="/agenda" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
        {{ t('nav.agenda') }}
      </RouterLink>

      <RouterLink to="/profile" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" /></svg>
        {{ t('nav.profile') }}
      </RouterLink>

      <RouterLink to="/notifications/preferences" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
        {{ t('nav.notificationPreferences') }}
      </RouterLink>

      <RouterLink to="/notifications/inbox" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M3 7h18" /><path d="M5 7l1.5 11a2 2 0 0 0 2 1.7h7a2 2 0 0 0 2-1.7L19 7" /><path d="M10 11h4" /></svg>
        {{ t('nav.notificationsInbox') }}
      </RouterLink>

      <RouterLink to="/org" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="9" y="2" width="6" height="4" rx="1" /><rect x="2" y="17" width="6" height="4" rx="1" /><rect x="16" y="17" width="6" height="4" rx="1" /><path d="M12 6v4M5 17v-3a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3" /></svg>
        {{ t('nav.org') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/tags" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" /><circle cx="7" cy="7" r="1.5" fill="currentColor" /></svg>
        {{ t('nav.adminTags') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/users" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3" /><path d="M2 20c0-3.3 2.7-6 6-6h2" /><circle cx="17" cy="9" r="2" /><path d="M19 17h3M20.5 15.5v3" /></svg>
        {{ t('nav.adminUsers') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/org-catalogs" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h10" /><circle cx="18" cy="18" r="2" /></svg>
        {{ t('nav.adminOrgCatalogs') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/org-membership-role-permissions" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M8 7a4 4 0 1 0 0 8" /><path d="M16 7a4 4 0 1 1 0 8" /><path d="M8 11h8" /><path d="M12 2v4M12 18v4" /></svg>
        {{ t('nav.adminMembershipRolePermissions') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/planning-role-permissions" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M8 3v4M16 3v4M4 10h16" /><path d="M12 13v4M10 15h4" /></svg>
        {{ t('nav.adminPlanningRolePermissions') }}
      </RouterLink>

      <RouterLink v-if="auth.isAdmin" to="/admin/email-settings" class="sidebar__link">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m2 7 10 7 10-7" /></svg>
        {{ t('nav.adminEmail') }}
      </RouterLink>
    </nav>

    <div class="sidebar__language">
      <label for="language-select" class="sidebar__language-label">{{ t('nav.language') }}</label>
      <div class="sidebar__language-control">
        <select id="language-select" v-model="selectedLocale" class="sidebar__language-select">
          <option v-for="localeCode in availableLocales" :key="localeCode" :value="localeCode">
            {{ getLocaleLabel(localeCode) }}
          </option>
        </select>
        <span class="sidebar__language-chevron" aria-hidden="true">▾</span>
      </div>
    </div>

    <div class="sidebar__footer">
      <div class="sidebar__user">
        <img
          v-if="profileImageSrc"
          :src="profileImageSrc"
          alt="Profile"
          class="sidebar__avatar sidebar__avatar--image"
        />
        <div v-else class="sidebar__avatar">{{ auth.fullName?.charAt(0).toUpperCase() }}</div>
        <div class="sidebar__user-info">
          <span class="sidebar__user-name">{{ auth.fullName }}</span>
          <span v-if="auth.isAdmin" class="sidebar__badge">{{ t('nav.adminBadge') }}</span>
        </div>
      </div>
      <button class="sidebar__theme-btn" @click="toggleTheme" :title="t('nav.themeToggle')">
        <svg v-if="theme === 'dark'" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="4"/>
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
        </svg>
        <svg v-else width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
      </button>
      <button class="sidebar__logout" @click="handleLogout" :title="t('nav.logout')">
        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" /></svg>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  width: 240px;
  min-height: 100vh;
  background: var(--color-sidebar-bg);
  padding: 1.25rem 0.75rem;
  flex-shrink: 0;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0 0.5rem 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  margin-bottom: 1rem;
}

.sidebar__logo {
  width: 32px;
  height: 32px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  object-fit: contain;
  border: 1px solid rgba(255, 255, 255, 0.25);
}

.sidebar__logo--fallback {
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
  font-size: 1.1rem;
}

.sidebar__name {
  font-weight: 700;
  font-size: 1.05rem;
  color: #f1f5f9;
  letter-spacing: 0.02em;
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.sidebar__link {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.6rem 0.75rem;
  border-radius: var(--radius-sm);
  color: var(--color-sidebar-text);
  font-size: 0.9rem;
  transition: background var(--transition), color var(--transition);
}

.sidebar__link:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #f1f5f9;
}

.sidebar__link.router-link-active {
  background: var(--color-sidebar-active);
  color: #f1f5f9;
}

.sidebar__language {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin: 0.65rem 0 1rem;
  padding: 0.55rem;
  border-radius: calc(var(--radius-sm) + 2px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.45);
}

.sidebar__language-label {
  font-size: 0.75rem;
  color: #cbd5e1;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.sidebar__language-control {
  position: relative;
}

.sidebar__language-select {
  width: 100%;
  appearance: none;
  background: linear-gradient(180deg, #111c33 0%, #0f172a 100%);
  color: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-sm);
  padding: 0.5rem 1.8rem 0.5rem 0.6rem;
  font-size: 0.82rem;
  transition: border-color var(--transition), box-shadow var(--transition);
}

.sidebar__language-select:focus {
  outline: none;
  border-color: rgba(96, 165, 250, 0.75);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.sidebar__language-chevron {
  position: absolute;
  right: 0.6rem;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 0.82rem;
  pointer-events: none;
}

.sidebar__footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  margin-top: auto;
}

.sidebar__user {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex: 1;
  min-width: 0;
}

.sidebar__avatar {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 600;
  flex-shrink: 0;
}

.sidebar__avatar--image {
  object-fit: cover;
  background: transparent;
}

.sidebar__user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar__user-name {
  font-size: 0.82rem;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar__badge {
  font-size: 0.68rem;
  color: #93c5fd;
  font-weight: 500;
}

.sidebar__theme-btn {
  background: transparent;
  border: none;
  padding: 0.35rem;
  border-radius: var(--radius-sm);
  color: var(--color-sidebar-text);
  transition: color var(--transition), background var(--transition);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar__theme-btn:hover {
  color: #f1f5f9;
  background: rgba(255, 255, 255, 0.08);
}

.sidebar__logout {
  background: transparent;
  border: none;
  padding: 0.35rem;
  border-radius: var(--radius-sm);
  color: var(--color-sidebar-text);
  transition: color var(--transition), background var(--transition);
  flex-shrink: 0;
}

.sidebar__logout:hover {
  color: var(--color-danger);
  background: rgba(220, 38, 38, 0.1);
}

@media (max-width: 1024px) {
  .sidebar {
    width: 210px;
    padding: 1rem 0.65rem;
  }

  .sidebar__name {
    font-size: 0.98rem;
  }

  .sidebar__link {
    font-size: 0.84rem;
    padding: 0.55rem 0.65rem;
  }

  .sidebar__language-select {
    font-size: 0.78rem;
  }
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    min-height: auto;
    padding: 0.8rem 0.75rem;
    gap: 0.8rem;
  }

  .sidebar__brand {
    border-bottom: none;
    margin-bottom: 0;
    padding: 0;
  }

  .sidebar__nav {
    flex-direction: row;
    flex-wrap: nowrap;
    overflow-x: auto;
    gap: 0.45rem;
    padding-bottom: 0.2rem;
  }

  .sidebar__link {
    white-space: nowrap;
    border: 1px solid rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.02);
  }

  .sidebar__footer {
    margin-top: 0;
    padding-top: 0;
    border-top: none;
    justify-content: space-between;
  }

  .sidebar__language {
    margin: 0.15rem 0 0.45rem;
    padding: 0.45rem;
  }

  .sidebar__user-name {
    max-width: 150px;
  }
}

@media (max-width: 460px) {
  .sidebar__name,
  .sidebar__badge,
  .sidebar__user-name {
    display: none;
  }

  .sidebar__language-label {
    display: none;
  }

  .sidebar__user {
    flex: 0;
  }

  .sidebar__link {
    padding: 0.5rem 0.6rem;
    font-size: 0.8rem;
  }
}
</style>

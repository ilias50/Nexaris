import { ref } from 'vue'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'nexaris-theme'
const colorSchemeQuery = window.matchMedia('(prefers-color-scheme: dark)')

// Singleton partagé entre toutes les instances du composable
const theme = ref<Theme>('light')
let hasExplicitPreference = false
let systemListenerAttached = false

function applyTheme(t: Theme): void {
  if (t === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

function resolveSystemTheme(): Theme {
  return colorSchemeQuery.matches ? 'dark' : 'light'
}

function syncWithSystemTheme(): void {
  if (hasExplicitPreference) {
    return
  }

  theme.value = resolveSystemTheme()
  applyTheme(theme.value)
}

export function bootstrapTheme(): void {
  const stored = localStorage.getItem(STORAGE_KEY) as Theme | null
  hasExplicitPreference = stored !== null
  theme.value = stored ?? resolveSystemTheme()
  applyTheme(theme.value)

  if (!hasExplicitPreference && !systemListenerAttached) {
    colorSchemeQuery.addEventListener('change', syncWithSystemTheme)
    systemListenerAttached = true
  }
}

export function useTheme() {
  function init(): void {
    bootstrapTheme()
  }

  function toggleTheme(): void {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    hasExplicitPreference = true
    localStorage.setItem(STORAGE_KEY, theme.value)
    applyTheme(theme.value)
  }

  return { theme, toggleTheme, init }
}

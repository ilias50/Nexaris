import { ref } from 'vue'
import { messages, type Locale, type TranslationKey } from './messages'

const STORAGE_KEY = 'nexaris_locale'
const defaultLocale: Locale = 'fr'

const supportedLocales = Object.keys(messages)

function isLocale(value: string): value is Locale {
  return value in messages
}

function normalizeLocale(value: string | null): Locale {
  if (value && isLocale(value)) return value
  return defaultLocale
}

const locale = ref<Locale>(normalizeLocale(localStorage.getItem(STORAGE_KEY)))

function mapLanguageCodeToLocale(languageCode?: string | null): Locale | null {
  if (!languageCode) return null

  const normalized = languageCode.toLowerCase()
  if (normalized.startsWith('fr')) return 'fr'
  if (normalized.startsWith('en')) return 'en'
  if (normalized.startsWith('nl')) return 'nl'
  if (normalized.startsWith('de')) return 'de'

  return null
}

function getFromPath(target: Record<string, unknown>, path: string): string | undefined {
  return path.split('.').reduce<unknown>((acc, segment) => {
    if (typeof acc === 'object' && acc !== null && segment in acc) {
      return (acc as Record<string, unknown>)[segment]
    }
    return undefined
  }, target) as string | undefined
}

export function useI18n() {
  function t(key: TranslationKey): string {
    const currentLocale = locale.value as Locale
    const dictionary = messages[currentLocale] ?? messages[defaultLocale]
    const current = getFromPath(dictionary as unknown as Record<string, unknown>, key)
    if (current) return current

    const fallback = getFromPath(messages[defaultLocale] as unknown as Record<string, unknown>, key)
    return fallback ?? key
  }

  function setLocale(nextLocale: Locale) {
    if (!isLocale(nextLocale)) {
      return
    }
    locale.value = nextLocale
    localStorage.setItem(STORAGE_KEY, nextLocale)
  }

  function setLocaleFromLanguageCode(languageCode?: string | null) {
    const mapped = mapLanguageCodeToLocale(languageCode)
    if (mapped) setLocale(mapped)
  }

  function getLocaleLabel(localeCode: string): string {
    const currentDictionary = messages[locale.value] ?? messages[defaultLocale]
    const localeSection = (currentDictionary as { locale?: Record<string, string> }).locale

    return localeSection?.[localeCode] ?? localeCode.toUpperCase()
  }

  return {
    locale,
    t,
    setLocale,
    setLocaleFromLanguageCode,
    availableLocales: supportedLocales,
    getLocaleLabel,
  }
}

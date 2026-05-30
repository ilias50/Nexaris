import fr from './locales/fr'
type LocaleDictionary = typeof fr
type LocaleModule = { default: LocaleDictionary } | LocaleDictionary

const localeModules = import.meta.glob('./locales/*.{ts,json}', {
  eager: true,
}) as Record<string, LocaleModule>

function getLocaleCodeFromPath(path: string): string {
  const fileName = path.split('/').pop() ?? ''
  return fileName.replace(/\.(ts|json)$/i, '').toLowerCase()
}

function getLocaleDictionary(mod: LocaleModule): LocaleDictionary {
  if (typeof mod === 'object' && mod !== null && 'default' in mod) {
    return mod.default
  }
  return mod as LocaleDictionary
}

const loadedMessages: Record<string, LocaleDictionary> = {}

for (const [path, mod] of Object.entries(localeModules)) {
  const localeCode = getLocaleCodeFromPath(path)
  loadedMessages[localeCode] = getLocaleDictionary(mod)
}

// Keep French as guaranteed fallback if auto-loading ever misses it.
if (!loadedMessages.fr) {
  loadedMessages.fr = fr
}

export const messages = loadedMessages

export type Locale = string

type Primitive = string | number | boolean | null | undefined
type DotPath<T> = {
  [K in Extract<keyof T, string>]: T[K] extends Primitive
    ? K
    : T[K] extends Record<string, unknown>
      ? `${K}.${DotPath<T[K]>}`
      : K
}[Extract<keyof T, string>]

export type TranslationKey = DotPath<LocaleDictionary>

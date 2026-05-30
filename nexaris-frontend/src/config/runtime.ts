export interface NexarisRuntimeConfig {
  apiBaseUrl?: string
}

declare global {
  interface Window {
    __NEXARIS_RUNTIME_CONFIG__?: NexarisRuntimeConfig
  }
}

export function getRuntimeConfig(): NexarisRuntimeConfig {
  if (typeof window === 'undefined') {
    return {}
  }

  return window.__NEXARIS_RUNTIME_CONFIG__ ?? {}
}

export function getApiBaseUrl() {
  return getRuntimeConfig().apiBaseUrl ?? import.meta.env.VITE_API_BASE_URL ?? ''
}
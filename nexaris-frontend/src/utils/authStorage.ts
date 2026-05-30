const TOKEN_KEY = 'nexaris_token'
const REFRESH_TOKEN_KEY = 'nexaris_refresh_token'
const USER_KEY = 'nexaris_user'

type PersistedAuthSession = {
  token: string
  refreshToken: string
  user: unknown
}

function getItem(key: string): string | null {
  return sessionStorage.getItem(key) ?? localStorage.getItem(key)
}

function setItem(key: string, value: string) {
  sessionStorage.setItem(key, value)
  localStorage.removeItem(key)
}

function removeItem(key: string) {
  sessionStorage.removeItem(key)
  localStorage.removeItem(key)
}

function parseJsonSafe<T>(raw: string | null): T | null {
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export function getStoredToken() {
  return getItem(TOKEN_KEY)
}

export function getStoredRefreshToken() {
  return getItem(REFRESH_TOKEN_KEY)
}

export function getStoredUser<T>() {
  return parseJsonSafe<T>(getItem(USER_KEY))
}

export function setStoredUser(user: unknown) {
  setItem(USER_KEY, JSON.stringify(user))
}

export function persistAuthSession(session: PersistedAuthSession) {
  setItem(TOKEN_KEY, session.token)
  setItem(REFRESH_TOKEN_KEY, session.refreshToken)
  setItem(USER_KEY, JSON.stringify(session.user))
}

export function clearAuthSession() {
  removeItem(TOKEN_KEY)
  removeItem(REFRESH_TOKEN_KEY)
  removeItem(USER_KEY)
}

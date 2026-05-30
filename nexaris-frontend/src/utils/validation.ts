export function trimOrEmpty(value: string | null | undefined) {
  return (value ?? '').trim()
}

export function toUpperTrimmed(value: string | null | undefined) {
  return trimOrEmpty(value).toUpperCase()
}

export function isBlank(value: string | null | undefined) {
  return trimOrEmpty(value) === ''
}

export function hasAllRequired(values: Array<string | null | undefined>) {
  return values.every((value) => !isBlank(value))
}

export function trimOrNull(value: string | null | undefined) {
  const trimmed = trimOrEmpty(value)
  return trimmed || null
}

export function passwordsMatch(password: string, confirmPassword: string) {
  return password === confirmPassword
}

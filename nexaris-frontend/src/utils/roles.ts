export const DEFAULT_AUTH_ROLES = ['ROLE_USER', 'ROLE_ADMIN'] as const

export const DEFAULT_ACCESS_RULE_ROLES = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_USER'] as const

export function normalizeRoleCode(raw: string) {
  const cleaned = raw.trim().toUpperCase().replace(/^ROLE_/, '')
  return cleaned ? `ROLE_${cleaned}` : ''
}

export function normalizeRoleList(values: Array<string | { name?: string } | null | undefined> | undefined) {
  if (!values?.length) return []
  return Array.from(
    new Set(
      values
        .map((value) => (typeof value === 'string' ? value : value?.name ?? ''))
        .map((value) => normalizeRoleCode(value))
        .filter(Boolean),
    ),
  )
}

export function formatRoleLabel(role: string) {
  const cleaned = role
    .trim()
    .replace(/^ROLE_/i, '')
    .replace(/[_\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()

  if (!cleaned) return ''

  return cleaned.charAt(0).toUpperCase() + cleaned.slice(1)
}

export function isDefaultUserRole(roleName: string) {
  return normalizeRoleCode(roleName) === 'ROLE_USER'
}
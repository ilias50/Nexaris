import type { TranslationKey } from '@/i18n/messages'

const PERMISSION_LABEL_KEYS: Record<string, TranslationKey> = {
  READ: 'permissionLabels.READ',
  EDIT_CONTENT: 'permissionLabels.EDIT_CONTENT',
  EDIT_LINKS: 'permissionLabels.EDIT_LINKS',
  MANAGE_MEMBERS: 'permissionLabels.MANAGE_MEMBERS',
  MANAGE_ACCESS: 'permissionLabels.MANAGE_ACCESS',
  MANAGE_ANNOUNCEMENTS: 'permissionLabels.MANAGE_ANNOUNCEMENTS',
  CREATE_CHILD: 'permissionLabels.CREATE_CHILD',
  DELETE_NODE: 'permissionLabels.DELETE_NODE',
  CREATE_ANY_ENTRY: 'permissionLabels.CREATE_ANY_ENTRY',
  CREATE_MEETING: 'permissionLabels.CREATE_MEETING',
  VIEW_ANY_CALENDAR: 'permissionLabels.VIEW_ANY_CALENDAR',
  WRITE: 'permissionLabels.WRITE',
  MANAGE: 'permissionLabels.MANAGE',
}

const EFFECT_LABEL_KEYS: Record<string, TranslationKey> = {
  ALLOW: 'effectLabels.ALLOW',
  DENY: 'effectLabels.DENY',
}

function humanizePermissionCode(value: string) {
  const cleaned = value
    .trim()
    .replace(/[_\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .toLowerCase()

  if (!cleaned) return ''
  return cleaned.charAt(0).toUpperCase() + cleaned.slice(1)
}

export function formatPermissionLabel(permission: string, t: (key: TranslationKey) => string) {
  const normalized = permission.trim().toUpperCase()
  const key = PERMISSION_LABEL_KEYS[normalized]

  if (key) {
    return t(key)
  }

  return humanizePermissionCode(normalized)
}

export function formatEffectLabel(effect: string, t: (key: TranslationKey) => string) {
  const normalized = effect.trim().toUpperCase()
  const key = EFFECT_LABEL_KEYS[normalized]

  if (key) {
    return t(key)
  }

  return humanizePermissionCode(normalized)
}

export function formatUserDisplayName(user: { firstName?: string; lastName?: string; email?: string }) {
  const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
  return fullName || user.email || ''
}

export function extractRoleNames(roles: Array<string | { name?: string }> | undefined) {
  if (!roles) return []
  return roles.map((role) => (typeof role === 'string' ? role : role?.name ?? '')).filter((role) => !!role)
}
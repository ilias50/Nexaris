export function resolveProfileImageUrl(profileImageUrl: string | null | undefined, apiBaseUrl: string) {
  if (!profileImageUrl) return null
  if (profileImageUrl.startsWith('http://') || profileImageUrl.startsWith('https://')) {
    return profileImageUrl
  }
  return `${apiBaseUrl}${profileImageUrl}`
}
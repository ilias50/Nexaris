import type { CatalogEntry } from '@/api/org'

export function toCatalogColorMap(entries: CatalogEntry[]) {
  const map: Record<string, string> = {}
  for (const entry of entries) {
    if (entry.color) map[entry.value.toUpperCase()] = entry.color
  }
  return map
}

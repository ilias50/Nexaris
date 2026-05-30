import type { ComputedRef, Ref } from 'vue'
import type { AgendaEntry, PlanningTag } from '@/api/planning'
import type { TranslationKey } from '@/i18n/messages'

interface UseAgendaEntryHelpersOptions {
  currentUserRoles: ComputedRef<string[]>
  userId: ComputedRef<number | null>
  selectedTagIds: Ref<number[]>
  t: (key: TranslationKey) => string
}

export function useAgendaEntryHelpers({ currentUserRoles, userId, selectedTagIds, t }: UseAgendaEntryHelpersOptions) {
  function toggleTag(tagId: number) {
    if (selectedTagIds.value.includes(tagId)) {
      selectedTagIds.value = selectedTagIds.value.filter((current) => current !== tagId)
      return
    }

    selectedTagIds.value = [...selectedTagIds.value, tagId]
  }

  function isTagSelected(tagId: number) {
    return selectedTagIds.value.includes(tagId)
  }

  function tagChipStyle(tag: PlanningTag) {
    const color = tag.color || 'var(--color-primary)'
    return { backgroundColor: color, borderColor: color, color: '#fff' }
  }

  function formatEntrySource(source: string) {
    const normalized = source?.trim().toUpperCase()
    if (normalized === 'MEETING') return t('agenda.sources.meeting')
    if (normalized === 'MANUAL') return t('agenda.sources.manual')
    return source || t('agenda.sources.default')
  }

  function canDeleteEntry(entry: AgendaEntry) {
    const isAdmin = currentUserRoles.value.includes('ROLE_ADMIN')
    const isOwnManualEntry = entry.source?.toUpperCase() === 'MANUAL' && entry.userId === userId.value
    return isAdmin || isOwnManualEntry
  }

  function combineDateTime(dateValue: string, timeValue: string) {
    return new Date(`${dateValue}T${timeValue}:00`)
  }

  return {
    toggleTag,
    isTagSelected,
    tagChipStyle,
    formatEntrySource,
    canDeleteEntry,
    combineDateTime,
  }
}
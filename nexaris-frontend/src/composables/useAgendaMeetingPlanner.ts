import { ref, watch, type Ref } from 'vue'
import { authApi } from '@/api/auth'
import { planningApi, type MeetingSuggestion } from '@/api/planning'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import type { TranslationKey } from '@/i18n/messages'
import { isBlank, trimOrEmpty } from '@/utils/validation'

interface AgendaMeetingPlannerDeps {
  t: (key: TranslationKey) => string
  locale: Ref<string>
  canManageMeetings: Ref<boolean>
  selectedDayDate: Ref<Date>
  userId: Ref<number | null>
  onSuggestionDatePicked: (date: Date) => void
  refreshEntries: () => Promise<void>
}

export function useAgendaMeetingPlanner(deps: AgendaMeetingPlannerDeps) {
  const meetingTitle = ref('')
  const meetingWindowStart = ref('')
  const meetingWindowEnd = ref('')
  const meetingWindowStartDateFallback = ref('')
  const meetingWindowStartTimeFallback = ref('09:00')
  const meetingWindowEndDateFallback = ref('')
  const meetingWindowEndTimeFallback = ref('18:00')
  const supportsDateTimeLocal = ref(true)
  const meetingDurationMinutes = ref(60)
  const meetingMaxSuggestions = ref(8)
  const meetingParticipantIds = ref<number[]>([])
  const meetingTagIds = ref<number[]>([])
  const meetingLoading = ref(false)
  const meetingError = ref('')
  const meetingSuccess = ref('')
  const meetingSuggestions = ref<MeetingSuggestion[]>([])
  const meetingScannedSlots = ref<number | null>(null)
  const creatingMeetingKey = ref<string | null>(null)
  const meetingUsers = ref<Array<{ id: number; firstName: string; lastName: string; email: string }>>([])

  if (typeof document !== 'undefined') {
    const probe = document.createElement('input')
    probe.setAttribute('type', 'datetime-local')
    supportsDateTimeLocal.value = probe.type === 'datetime-local'
  }

  function pad(value: number) {
    return String(value).padStart(2, '0')
  }

  function toDateTimeLocalInputValue(date: Date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
  }

  function toFallbackDateInputValue(date: Date) {
    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`
  }

  function normalizeDateTimeInput(value: string) {
    if (isBlank(value)) return ''
    return value.length === 16 ? `${value}:00` : value
  }

  function normalizeFallbackDateInput(value: string) {
    const trimmed = trimOrEmpty(value)
    const slashMatch = trimmed.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
    if (slashMatch) {
      const day = Number(slashMatch[1])
      const month = Number(slashMatch[2])
      const year = Number(slashMatch[3])
      const parsed = new Date(year, month - 1, day)
      const isValid = parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day
      if (!isValid) return ''
      return `${year}-${pad(month)}-${pad(day)}`
    }

    const isoMatch = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (!isoMatch) return ''

    const year = Number(isoMatch[1])
    const month = Number(isoMatch[2])
    const day = Number(isoMatch[3])
    const parsed = new Date(year, month - 1, day)
    const isValid = parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day
    if (!isValid) return ''

    return `${year}-${pad(month)}-${pad(day)}`
  }

  function normalizeFallbackTimeInput(value: string) {
    const trimmed = trimOrEmpty(value)
    const match = trimmed.match(/^(\d{1,2}):(\d{2})$/)
    if (!match) return ''

    const hours = Number(match[1])
    const minutes = Number(match[2])
    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return ''

    return `${pad(hours)}:${pad(minutes)}`
  }

  function buildFallbackDateTime(dateValue: string, timeValue: string) {
    const normalizedDate = normalizeFallbackDateInput(dateValue)
    const normalizedTime = normalizeFallbackTimeInput(timeValue)
    if (!normalizedDate || !normalizedTime) return ''
    return `${normalizedDate}T${normalizedTime}:00`
  }

  function formatDateTimeRange(startAt: string, endAt: string) {
    const formatter = new Intl.DateTimeFormat(deps.locale.value, {
      weekday: 'short',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })

    return `${formatter.format(new Date(startAt))} - ${formatter.format(new Date(endAt))}`
  }

  function toggleMeetingTag(tagId: number) {
    if (meetingTagIds.value.includes(tagId)) {
      meetingTagIds.value = meetingTagIds.value.filter((current) => current !== tagId)
      return
    }
    meetingTagIds.value = [...meetingTagIds.value, tagId]
  }

  function isMeetingTagSelected(tagId: number) {
    return meetingTagIds.value.includes(tagId)
  }

  function toggleMeetingParticipant(userIdValue: number) {
    if (meetingParticipantIds.value.includes(userIdValue)) {
      meetingParticipantIds.value = meetingParticipantIds.value.filter((current) => current !== userIdValue)
      return
    }
    meetingParticipantIds.value = [...meetingParticipantIds.value, userIdValue]
  }

  function isMeetingParticipantSelected(userIdValue: number) {
    return meetingParticipantIds.value.includes(userIdValue)
  }

  function ensureMeetingDefaults() {
    if (isBlank(meetingTitle.value)) {
      meetingTitle.value = deps.t('agenda.meeting.defaultTitle')
    }
    if (!meetingWindowStart.value || !meetingWindowEnd.value) {
      const base = deps.selectedDayDate.value
      const from = new Date(base.getFullYear(), base.getMonth(), base.getDate(), 9, 0, 0)
      const to = new Date(base.getFullYear(), base.getMonth(), base.getDate(), 18, 0, 0)
      meetingWindowStart.value = toDateTimeLocalInputValue(from)
      meetingWindowEnd.value = toDateTimeLocalInputValue(to)
      meetingWindowStartDateFallback.value = toFallbackDateInputValue(from)
      meetingWindowStartTimeFallback.value = `${pad(from.getHours())}:${pad(from.getMinutes())}`
      meetingWindowEndDateFallback.value = toFallbackDateInputValue(to)
      meetingWindowEndTimeFallback.value = `${pad(to.getHours())}:${pad(to.getMinutes())}`
    }
  }

  async function loadMeetingUsers() {
    if (!deps.canManageMeetings.value) {
      meetingUsers.value = []
      meetingParticipantIds.value = []
      return
    }
    const users = await authApi.listEnabledUsersSafe()
    meetingUsers.value = users
    if (deps.userId.value != null && !meetingParticipantIds.value.includes(deps.userId.value)) {
      meetingParticipantIds.value = [deps.userId.value]
    }
  }

  watch([deps.canManageMeetings, deps.userId], async () => {
    await loadMeetingUsers()
    ensureMeetingDefaults()
  }, { immediate: true })

  async function suggestMeetingSlots() {
    meetingError.value = ''
    meetingSuccess.value = ''
    meetingSuggestions.value = []
    meetingScannedSlots.value = null

    if (!deps.canManageMeetings.value) {
      meetingError.value = deps.t('agenda.meeting.errors.forbidden')
      return
    }

    const normalizedStart = supportsDateTimeLocal.value
      ? normalizeDateTimeInput(meetingWindowStart.value)
      : buildFallbackDateTime(meetingWindowStartDateFallback.value, meetingWindowStartTimeFallback.value)
    const normalizedEnd = supportsDateTimeLocal.value
      ? normalizeDateTimeInput(meetingWindowEnd.value)
      : buildFallbackDateTime(meetingWindowEndDateFallback.value, meetingWindowEndTimeFallback.value)
    const start = new Date(normalizedStart)
    const end = new Date(normalizedEnd)

    if (isBlank(meetingTitle.value)) {
      meetingError.value = deps.t('agenda.meeting.errors.titleRequired')
      return
    }
    if (meetingParticipantIds.value.length === 0) {
      meetingError.value = deps.t('agenda.meeting.errors.participantsRequired')
      return
    }
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || !(start.getTime() < end.getTime())) {
      meetingError.value = deps.t('agenda.meeting.errors.invalidWindow')
      return
    }

    meetingLoading.value = true
    try {
      const { data } = await planningApi.suggestMeetingSlots({
        participantUserIds: meetingParticipantIds.value,
        durationMinutes: meetingDurationMinutes.value,
        windowStart: normalizedStart,
        windowEnd: normalizedEnd,
        maxSuggestions: meetingMaxSuggestions.value,
      })
      meetingSuggestions.value = data.suggestions
      meetingScannedSlots.value = data.scannedSlots
      if (data.suggestions.length > 0) {
        meetingSuccess.value = deps.t('agenda.meeting.messages.suggestionsLoaded')
      }
    } catch (error: unknown) {
      meetingError.value = resolveApiErrorMessage(error, deps.t, {
        server: 'agenda.meeting.errors.suggestFailed',
        generic: 'agenda.meeting.errors.suggestFailed',
      })
    } finally {
      meetingLoading.value = false
    }
  }

  async function createMeetingFromSuggestion(suggestion: MeetingSuggestion) {
    meetingError.value = ''
    meetingSuccess.value = ''

    if (!deps.canManageMeetings.value) {
      meetingError.value = deps.t('agenda.meeting.errors.forbidden')
      return
    }

    creatingMeetingKey.value = `${suggestion.startAt}-${suggestion.endAt}`
    try {
      await planningApi.createMeeting({
        title: trimOrEmpty(meetingTitle.value),
        participantUserIds: meetingParticipantIds.value,
        startAt: suggestion.startAt,
        endAt: suggestion.endAt,
        tagIds: meetingTagIds.value,
      })
      meetingSuccess.value = `${deps.t('agenda.meeting.messages.created')} (${formatDateTimeRange(suggestion.startAt, suggestion.endAt)})`

      const suggestionDate = new Date(suggestion.startAt)
      deps.onSuggestionDatePicked(suggestionDate)
      await deps.refreshEntries()
    } catch (error: unknown) {
      meetingError.value = resolveApiErrorMessage(error, deps.t, {
        server: 'agenda.meeting.errors.createFailed',
        generic: 'agenda.meeting.errors.createFailed',
      })
    } finally {
      creatingMeetingKey.value = null
    }
  }

  return {
    meetingTitle,
    meetingWindowStart,
    meetingWindowEnd,
    meetingWindowStartDateFallback,
    meetingWindowStartTimeFallback,
    meetingWindowEndDateFallback,
    meetingWindowEndTimeFallback,
    supportsDateTimeLocal,
    meetingDurationMinutes,
    meetingMaxSuggestions,
    meetingParticipantIds,
    meetingTagIds,
    meetingLoading,
    meetingError,
    meetingSuccess,
    meetingSuggestions,
    meetingScannedSlots,
    creatingMeetingKey,
    meetingUsers,
    toggleMeetingTag,
    isMeetingTagSelected,
    toggleMeetingParticipant,
    isMeetingParticipantSelected,
    suggestMeetingSlots,
    createMeetingFromSuggestion,
  }
}

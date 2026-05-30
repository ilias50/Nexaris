<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useAuthStore } from '@/stores/auth'
import { useAgendaStore } from '@/stores/agenda'
import AgendaCalendarPanel from '@/components/agenda/AgendaCalendarPanel.vue'
import { normalizeRoleList } from '@/utils/roles'
import { useI18n } from '@/i18n'
import { useAgendaApiAccess, type AdminUser, type HolidayDay } from '@/composables/useAgendaApiAccess'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import { useAgendaCalendar, type CalendarHoliday } from '@/composables/useAgendaCalendar'
import { useAgendaRecurrence } from '@/composables/useAgendaRecurrence'
import { useAgendaMeetingPlanner } from '@/composables/useAgendaMeetingPlanner'
import { useAgendaEntryHelpers } from '@/composables/useAgendaEntryHelpers'
import { formatUserDisplayName } from '@/utils/users'
import { isBlank, toUpperTrimmed, trimOrEmpty } from '@/utils/validation'
import { planningApi } from '@/api/planning'
import { orgApi, type OrgTreeNode, type NodeMembership } from '@/api/org'

const auth = useAuthStore()
const agenda = useAgendaStore()
const { t, locale } = useI18n()
const agendaApi = useAgendaApiAccess()

const selectedMonth = ref(new Date())
const title = ref('')
const agendaEntries = computed(() => agenda.entries)
const holidaysByDay = ref(new Map<string, CalendarHoliday>())

const {
  monthLabel,
  todayKey,
  selectedDayKey,
  weekdayLabels,
  calendarCells,
  selectedDayDate,
  selectedDayLabel,
  selectedDayEntries,
  selectedDayHoliday,
  toDateInputValue,
  toDateKey,
  toLocalDateTime,
  buildMonthRange,
  shiftMonth,
  resetToCurrentMonth,
  formatEntryRange,
  formatDateTimeRange,
  formatEntryStart,
  pickDate,
} = useAgendaCalendar(locale, selectedMonth, agendaEntries, holidaysByDay)

const entryDate = ref(toDateInputValue(new Date()))
const startTime = ref('09:00')
const endTime = ref('10:00')

const {
  recurrenceFrequency,
  recurrenceUntil,
  recurrenceWeekdays,
  recurrenceSkipHolidays,
  recurrenceWeekdayOptions,
  toggleRecurrenceWeekday,
  isRecurrenceWeekdaySelected,
  applyRecurrenceToPayload,
  resetRecurrence,
} = useAgendaRecurrence()

const selectedEntryTargetUserIds = ref<number[]>([])
const entryUserToAddId = ref<number | null>(null)
const entryNodeToAddId = ref<number | null>(null)
const selectedCalendarUserId = ref<number | null>(null)
const entryUsers = ref<AdminUser[]>([])
const selectedTagIds = ref<number[]>([])
const formError = ref('')
const formSuccess = ref('')
const submitting = ref(false)

const userId = computed(() => auth.user?.id ?? null)
const countryCode = computed(() => auth.user?.countryCode ?? '')
const languageCode = computed(() => auth.user?.languageCode ?? '')
const monthRange = computed(() => buildMonthRange(selectedMonth.value))
const currentUserRoles = computed(() => normalizeRoles(auth.user?.roles))
const planningPermissions = ref<string[]>([])
const canManageMeetings = computed(() => planningPermissions.value.includes('CREATE_MEETING'))
const canAssignEntriesToOthers = computed(() => planningPermissions.value.includes('CREATE_ANY_ENTRY'))
const canViewCalendarOfOthers = computed(() => planningPermissions.value.includes('VIEW_ANY_CALENDAR'))
type MeetingOrgNodeOption = { id: number; name: string; nodeType: string }
const meetingUserToAddId = ref<number | null>(null)
const meetingNodeToAddId = ref<number | null>(null)
const meetingOrgNodes = ref<MeetingOrgNodeOption[]>([])
const meetingNodeMembersCache = new Map<number, number[]>()

const {
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
  suggestMeetingSlots,
  createMeetingFromSuggestion,
} = useAgendaMeetingPlanner({
  t,
  locale,
  canManageMeetings,
  selectedDayDate,
  userId,
  onSuggestionDatePicked: (date: Date) => {
    selectedDayKey.value = toDateKey(date)
    entryDate.value = toDateInputValue(date)
  },
  refreshEntries: async () => {
    await refreshVisibleEntries()
  },
})

async function refreshVisibleEntries() {
  if (selectedCalendarUserId.value == null) return
  await agenda.fetchEntries(selectedCalendarUserId.value, monthRange.value.from, monthRange.value.to)
}

// ── Holidays ────────────────────────────────────────────────────────────────
const loadedHolidayYears = new Set<string>()

async function fetchHolidaysForYear(year: number, code: string, userLanguageCode?: string) {
  if (!code) return
  const normalizedLanguageCode = toUpperTrimmed(userLanguageCode)
  const cacheKey = `${year}-${code}-${normalizedLanguageCode}`
  if (loadedHolidayYears.has(cacheKey)) return
  try {
    const data = await agendaApi.getHolidayCalendar(year, code, true, normalizedLanguageCode || undefined)
    loadedHolidayYears.add(cacheKey)
    const merged = new Map(holidaysByDay.value)

    const addDays = (days: HolidayDay[], isPublic: boolean) => {
      for (const h of days) {
        if (h.date) merged.set(h.date, { name: h.name, isPublic })
      }
    }

    addDays(data.holidays ?? [], true)
    addDays(data.schoolVacations ?? [], false)
    holidaysByDay.value = merged
  } catch {
    // holidays are not critical — fail silently
  }
}

watch(
  [selectedMonth, countryCode, languageCode],
  async ([month, code, currentLanguageCode]) => {
    const year = (month as Date).getFullYear()
    await fetchHolidaysForYear(year, code as string, currentLanguageCode as string)
    // pre-fetch next year when browsing December
    if ((month as Date).getMonth() === 11) {
      await fetchHolidaysForYear(year + 1, code as string, currentLanguageCode as string)
    }
  },
  { immediate: true },
)

watch(selectedMonth, (nextMonth) => {
  const firstDay = new Date(nextMonth.getFullYear(), nextMonth.getMonth(), 1)
  selectedDayKey.value = toDateKey(firstDay)
  entryDate.value = toDateInputValue(firstDay)
})

watch(userId, async () => {
  if (userId.value == null) return
  selectedEntryTargetUserIds.value = [userId.value]
  selectedCalendarUserId.value = userId.value
  try {
    const { data } = await planningApi.getMyPlanningPermissions()
    planningPermissions.value = data.map(toUpperTrimmed).filter(Boolean)
  } catch {
    planningPermissions.value = []
  }
  await agenda.fetchTags()
}, { immediate: true })

watch([canAssignEntriesToOthers, canViewCalendarOfOthers, userId], async () => {
  if ((!canAssignEntriesToOthers.value && !canViewCalendarOfOthers.value) || userId.value == null) {
    entryUsers.value = []
    selectedEntryTargetUserIds.value = userId.value != null ? [userId.value] : []
    return
  }

  try {
    entryUsers.value = await agendaApi.listAssignableUsers()
  } catch {
    entryUsers.value = []
  }
  if (canViewCalendarOfOthers.value) {
    const canKeepCurrentSelection = selectedCalendarUserId.value != null
      && entryUsers.value.some((member: AdminUser) => member.id === selectedCalendarUserId.value)
    if (!canKeepCurrentSelection) {
      selectedCalendarUserId.value = userId.value
    }
  }

  const allowedUserIds = new Set(entryUsers.value.map((member: AdminUser) => member.id))
  const filteredTargets = selectedEntryTargetUserIds.value.filter((value: number) => allowedUserIds.has(value))
  selectedEntryTargetUserIds.value = filteredTargets.length > 0
    ? filteredTargets
    : (userId.value != null ? [userId.value] : [])
}, { immediate: true })

watch([selectedMonth, selectedCalendarUserId], async () => {
  if (selectedCalendarUserId.value == null) return
  await refreshVisibleEntries()
}, { immediate: true })

watch([canViewCalendarOfOthers, userId], () => {
  if (!canViewCalendarOfOthers.value) {
    selectedCalendarUserId.value = userId.value
  }
}, { immediate: true })

function flattenOrgNodes(nodes: OrgTreeNode[]) {
  const result: MeetingOrgNodeOption[] = []
  const visit = (node: OrgTreeNode) => {
    result.push({ id: node.id, name: node.name, nodeType: node.nodeType })
    for (const child of node.children ?? []) {
      visit(child)
    }
  }

  for (const node of nodes) {
    visit(node)
  }

  return result
}

async function loadMeetingOrgNodes() {
  if (!canManageMeetings.value && !canAssignEntriesToOthers.value) {
    meetingOrgNodes.value = []
    meetingNodeMembersCache.clear()
    return
  }

  try {
    const { data } = await orgApi.getTree()
    meetingOrgNodes.value = flattenOrgNodes(data)
  } catch {
    meetingOrgNodes.value = []
  }
}

watch([canManageMeetings, canAssignEntriesToOthers], async () => {
  await loadMeetingOrgNodes()
}, { immediate: true })

function formatMeetingNodeLabel(node: MeetingOrgNodeOption) {
  return `${node.name} (${node.nodeType})`
}

function selectAllMeetingUsers() {
  const allUserIds = meetingUsers.value.map((member: AdminUser) => member.id)
  meetingParticipantIds.value = Array.from(new Set([...meetingParticipantIds.value, ...allUserIds]))
}

function addMeetingUser() {
  const userIdValue = meetingUserToAddId.value
  if (userIdValue == null) return
  meetingParticipantIds.value = Array.from(new Set([...meetingParticipantIds.value, userIdValue]))
  meetingUserToAddId.value = null
}

function removeMeetingUser(userIdValue: number) {
  meetingParticipantIds.value = meetingParticipantIds.value.filter((current: number) => current !== userIdValue)
}

async function addMeetingNodeMembers(nodeId: number) {
  try {
    let memberIds = meetingNodeMembersCache.get(nodeId)
    if (!memberIds) {
      const { data } = await orgApi.getNodeMemberships(nodeId)
      memberIds = Array.from(new Set(data.map((membership: NodeMembership) => membership.userId)))
      meetingNodeMembersCache.set(nodeId, memberIds)
    }

    if (memberIds.length === 0) {
      return
    }

    meetingParticipantIds.value = Array.from(new Set([...meetingParticipantIds.value, ...memberIds]))
  } catch {
    // Keep meeting panel resilient if org-service is unavailable.
  }
}

async function addMeetingNodeMembersFromSelection() {
  if (meetingNodeToAddId.value == null) return
  await addMeetingNodeMembers(meetingNodeToAddId.value)
  meetingNodeToAddId.value = null
}

function formatMeetingParticipantLabel(participantId: number) {
  const member = meetingUsers.value.find((item: AdminUser) => item.id === participantId)
  return member ? formatUserDisplayName(member) : `#${participantId}`
}

function selectAllEntryUsers() {
  const allUserIds = entryUsers.value.map((member: AdminUser) => member.id)
  selectedEntryTargetUserIds.value = Array.from(new Set([...selectedEntryTargetUserIds.value, ...allUserIds]))
}

function addEntryTargetUser() {
  const userIdValue = entryUserToAddId.value
  if (userIdValue == null) return
  selectedEntryTargetUserIds.value = Array.from(new Set([...selectedEntryTargetUserIds.value, userIdValue]))
  entryUserToAddId.value = null
}

function removeEntryTargetUser(userIdValue: number) {
  selectedEntryTargetUserIds.value = selectedEntryTargetUserIds.value.filter((current: number) => current !== userIdValue)
  if (selectedEntryTargetUserIds.value.length === 0 && userId.value != null) {
    selectedEntryTargetUserIds.value = [userId.value]
  }
}

async function addEntryNodeMembersFromSelection() {
  if (entryNodeToAddId.value == null) return

  try {
    let memberIds = meetingNodeMembersCache.get(entryNodeToAddId.value)
    if (!memberIds) {
      const { data } = await orgApi.getNodeMemberships(entryNodeToAddId.value)
      memberIds = Array.from(new Set(data.map((membership: NodeMembership) => membership.userId)))
      meetingNodeMembersCache.set(entryNodeToAddId.value, memberIds)
    }

    selectedEntryTargetUserIds.value = Array.from(new Set([...selectedEntryTargetUserIds.value, ...memberIds]))
  } catch {
    // Keep entry creation resilient if org-service is unavailable.
  } finally {
    entryNodeToAddId.value = null
  }
}

function formatEntryTargetUserLabel(targetUserId: number) {
  const member = entryUsers.value.find((item: AdminUser) => item.id === targetUserId)
  return member ? formatUserDisplayName(member) : `#${targetUserId}`
}

function normalizeRoles(roles: Array<string | { name?: string }> | undefined) {
  return normalizeRoleList(roles)
}

async function ensureHolidayCoverage(fromDate: Date, toDate: Date) {
  const code = countryCode.value
  if (!code) return

  const fromYear = fromDate.getFullYear()
  const toYear = toDate.getFullYear()
  for (let year = fromYear; year <= toYear; year += 1) {
    await fetchHolidaysForYear(year, code, languageCode.value)
  }
}

function buildExcludedHolidayDates(fromDate: Date, toDate: Date) {
  const fromKey = toDateKey(fromDate)
  const toKey = toDateKey(toDate)

  return [...holidaysByDay.value.entries()]
    .filter(([key, holiday]) => holiday.isPublic && key >= fromKey && key <= toKey)
    .map(([key]) => key)
}

const {
  toggleTag,
  isTagSelected,
  tagChipStyle,
  formatEntrySource,
  canDeleteEntry,
  combineDateTime,
} = useAgendaEntryHelpers({
  currentUserRoles,
  userId,
  selectedTagIds,
  t,
})

function formatGroupCount(count: number) {
  return count === 1 ? t('agenda.entrySingular') : t('agenda.entryPlural')
}

function pickDateAndSyncEntryDate(date: Date) {
  pickDate(date)
  entryDate.value = toDateInputValue(date)
}

async function deleteEntry(entryId: number) {
  if (!userId.value) return
  try {
    await agenda.deleteEntry(entryId)
    await refreshVisibleEntries()
  } catch {
    // Keep UI responsive if deletion fails.
  }
}

async function submitEntry() {
  formError.value = ''
  formSuccess.value = ''

  if (userId.value == null) {
    formError.value = t('agenda.errors.noUser')
    return
  }

  if (isBlank(title.value)) {
    formError.value = t('agenda.errors.titleRequired')
    return
  }

  const startAt = combineDateTime(entryDate.value, startTime.value)
  const endAt = combineDateTime(entryDate.value, endTime.value)

  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime()) || !(startAt.getTime() < endAt.getTime())) {
    formError.value = t('agenda.errors.invalidRange')
    return
  }

  submitting.value = true
  try {
    const payloadBase: {
      title: string
      startAt: string
      endAt: string
      tagIds: number[]
      recurrenceFrequency?: 'NONE' | 'DAILY' | 'WEEKLY' | 'WEEKDAYS' | 'MONTHLY'
      recurrenceUntil?: string
      recurrenceWeekdays?: string[]
      recurrenceSkipHolidays?: boolean
      recurrenceExcludedDates?: string[]
    } = {
      title: trimOrEmpty(title.value),
      startAt: toLocalDateTime(startAt),
      endAt: toLocalDateTime(endAt),
      tagIds: selectedTagIds.value,
    }

    const recurrenceApplied = await applyRecurrenceToPayload(
      payloadBase,
      entryDate.value,
      t,
      ensureHolidayCoverage,
      buildExcludedHolidayDates,
      (message) => { formError.value = message },
    )
    if (!recurrenceApplied) {
      submitting.value = false
      return
    }

    const targetUserIds = canAssignEntriesToOthers.value
      ? selectedEntryTargetUserIds.value
      : (userId.value != null ? [userId.value] : [])

    if (targetUserIds.length === 0) {
      formError.value = t('agenda.errors.noUser')
      submitting.value = false
      return
    }

    for (const targetUserId of targetUserIds) {
      await agenda.addEntry({
        ...payloadBase,
        userId: targetUserId,
      })
    }

    formSuccess.value = t('agenda.messages.created')
    title.value = ''
    selectedTagIds.value = []
    resetRecurrence()
    await refreshVisibleEntries()
  } catch (error: unknown) {
    formError.value = resolveApiErrorMessage(error, t, {
      server: 'agenda.errors.createFailed',
      generic: 'agenda.errors.createFailed',
    })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AppLayout :title="t('agenda.title')">
    <div class="agenda-page">
      <div class="agenda-month-bar">
        <BaseButton variant="ghost" :aria-label="t('agenda.previousMonth')" @click="shiftMonth(-1)"><span aria-hidden="true">←</span></BaseButton>
        <span class="agenda-month-label">{{ monthLabel }}</span>
        <BaseButton variant="ghost" :aria-label="t('agenda.nextMonth')" @click="shiftMonth(1)"><span aria-hidden="true">→</span></BaseButton>
      </div>
      <div class="agenda-layout">
        <section class="agenda-panel agenda-panel--form">
          <div class="agenda-panel__header">
            <h3>{{ t('agenda.createTitle') }}</h3>
            <p>{{ t('agenda.createSubtitle') }}</p>
          </div>

          <p v-if="formError" class="agenda-message agenda-message--error">{{ formError }}</p>
          <p v-if="formSuccess" class="agenda-message agenda-message--success">{{ formSuccess }}</p>

          <div v-if="canViewCalendarOfOthers" class="agenda-form__field">
            <label class="agenda-form__label">{{ t('agenda.calendarUserLabel') }}</label>
            <select v-model.number="selectedCalendarUserId" class="agenda-form__select">
              <option v-for="member in entryUsers" :key="`calendar-${member.id}`" :value="member.id">
                {{ formatUserDisplayName(member) }}
              </option>
            </select>
          </div>

          <form class="agenda-form" @submit.prevent="submitEntry">
            <BaseInput v-model="title" :label="t('agenda.titleLabel')" :placeholder="t('agenda.titlePlaceholder')" />

            <div v-if="canAssignEntriesToOthers" class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.targetUserLabel') }}</label>
              <div class="agenda-form__selection-row">
                <select v-model.number="entryUserToAddId" class="agenda-form__select" @change="addEntryTargetUser">
                  <option :value="null">{{ t('agenda.selectUserPlaceholder') }}</option>
                  <option v-for="member in entryUsers" :key="`entry-target-${member.id}`" :value="member.id">
                    {{ formatUserDisplayName(member) }}
                  </option>
                </select>
                <BaseButton type="button" size="sm" variant="ghost" @click="selectAllEntryUsers">{{ t('agenda.selectAllUsers') }}</BaseButton>
              </div>

              <select v-model.number="entryNodeToAddId" class="agenda-form__select" @change="addEntryNodeMembersFromSelection">
                <option :value="null">{{ t('agenda.selectNodePlaceholder') }}</option>
                <option v-for="node in meetingOrgNodes" :key="`entry-node-${node.id}`" :value="node.id">
                  {{ formatMeetingNodeLabel(node) }}
                </option>
              </select>

              <p v-if="selectedEntryTargetUserIds.length === 0" class="agenda-form__hint">{{ t('agenda.noSelectedUsers') }}</p>
              <div v-else class="agenda-selected-users">
                <button
                  v-for="targetUserId in selectedEntryTargetUserIds"
                  :key="`entry-selected-${targetUserId}`"
                  type="button"
                  class="agenda-selected-user"
                  @click="removeEntryTargetUser(targetUserId)"
                >
                  <span>{{ formatEntryTargetUserLabel(targetUserId) }}</span>
                  <span class="agenda-selected-user__remove">×</span>
                </button>
              </div>
            </div>

            <div class="agenda-form__row">
              <BaseInput v-model="entryDate" type="date" :label="t('agenda.dateLabel')" />
              <BaseInput v-model="startTime" type="time" :label="t('agenda.startTimeLabel')" />
              <BaseInput v-model="endTime" type="time" :label="t('agenda.endTimeLabel')" />
            </div>

            <div class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.recurrence.label') }}</label>
              <select v-model="recurrenceFrequency" class="agenda-form__select">
                <option value="NONE">{{ t('agenda.recurrence.none') }}</option>
                <option value="DAILY">{{ t('agenda.recurrence.daily') }}</option>
                <option value="WEEKDAYS">{{ t('agenda.recurrence.weekdays') }}</option>
                <option value="WEEKLY">{{ t('agenda.recurrence.weekly') }}</option>
                <option value="MONTHLY">{{ t('agenda.recurrence.monthly') }}</option>
              </select>
            </div>

            <div v-if="recurrenceFrequency !== 'NONE'" class="agenda-form__row agenda-form__row--recurrence">
              <BaseInput v-model="recurrenceUntil" type="date" :label="t('agenda.recurrence.untilLabel')" />
            </div>

            <div v-if="recurrenceFrequency !== 'NONE'" class="agenda-form__field agenda-form__field--checkbox">
              <label class="agenda-checkbox-label">
                <input v-model="recurrenceSkipHolidays" type="checkbox">
                <span>{{ t('agenda.recurrence.skipHolidays') }}</span>
              </label>
            </div>

            <div v-if="recurrenceFrequency === 'WEEKLY'" class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.recurrence.weekdaysLabel') }}</label>
              <div class="agenda-weekdays">
                <button
                  v-for="day in recurrenceWeekdayOptions"
                  :key="day.value"
                  type="button"
                  class="agenda-weekday"
                  :class="{ 'agenda-weekday--selected': isRecurrenceWeekdaySelected(day.value) }"
                  @click="toggleRecurrenceWeekday(day.value)"
                >
                  {{ t(day.labelKey) }}
                </button>
              </div>
            </div>

            <div class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.tagsLabel') }}</label>
              <p v-if="!agenda.tags.length" class="agenda-form__hint">{{ t('agenda.noTags') }}</p>
              <div v-else class="agenda-tags">
                <button
                  v-for="tag in agenda.tags"
                  :key="tag.id"
                  type="button"
                  class="agenda-tag"
                  :class="{ 'agenda-tag--selected': isTagSelected(tag.id) }"
                  :style="tagChipStyle(tag)"
                  @click="toggleTag(tag.id)"
                >
                  <span>{{ tag.name }}</span>
                </button>
              </div>
            </div>

            <div class="agenda-form__footer">
              <BaseButton type="submit" :loading="submitting">{{ t('agenda.saveButton') }}</BaseButton>
            </div>
          </form>

          <template v-if="canManageMeetings">
            <div class="agenda-divider" />

            <div class="agenda-panel__header agenda-panel__header--compact">
              <h3>{{ t('agenda.meeting.title') }}</h3>
              <p>{{ t('agenda.meeting.subtitle') }}</p>
            </div>

            <p v-if="meetingError" class="agenda-message agenda-message--error">{{ meetingError }}</p>
            <p v-if="meetingSuccess" class="agenda-message agenda-message--success">{{ meetingSuccess }}</p>

            <form class="agenda-form" @submit.prevent="suggestMeetingSlots">
            <BaseInput
              v-model="meetingTitle"
              :label="t('agenda.meeting.titleLabel')"
              :placeholder="t('agenda.meeting.titlePlaceholder')"
            />

            <div class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.meeting.participantsLabel') }}</label>
              <p v-if="!meetingUsers.length" class="agenda-form__hint">{{ t('agenda.meeting.noUsers') }}</p>
              <div v-else>
                <div class="agenda-form__selection-row">
                  <select v-model.number="meetingUserToAddId" class="agenda-form__select" @change="addMeetingUser">
                    <option :value="null">{{ t('agenda.selectUserPlaceholder') }}</option>
                    <option v-for="member in meetingUsers" :key="`meeting-user-${member.id}`" :value="member.id">
                      {{ formatUserDisplayName(member) }}
                    </option>
                  </select>
                  <BaseButton type="button" size="sm" variant="ghost" @click="selectAllMeetingUsers">{{ t('agenda.meeting.selectAllUsers') }}</BaseButton>
                </div>

                <div class="agenda-selected-users">
                  <button
                    v-for="participantUserId in meetingParticipantIds"
                    :key="`meeting-selected-${participantUserId}`"
                    type="button"
                    class="agenda-selected-user"
                    @click="removeMeetingUser(participantUserId)"
                  >
                    <span>{{ formatMeetingParticipantLabel(participantUserId) }}</span>
                    <span class="agenda-selected-user__remove">×</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.meeting.orgNodesLabel') }}</label>
              <p v-if="!meetingOrgNodes.length" class="agenda-form__hint">{{ t('agenda.meeting.noOrgNodes') }}</p>
              <select v-if="meetingOrgNodes.length" v-model.number="meetingNodeToAddId" class="agenda-form__select" @change="addMeetingNodeMembersFromSelection">
                <option :value="null">{{ t('agenda.selectNodePlaceholder') }}</option>
                <option v-for="node in meetingOrgNodes" :key="`meeting-node-${node.id}`" :value="node.id">
                  {{ formatMeetingNodeLabel(node) }}
                </option>
              </select>
              <p v-else class="agenda-form__hint">{{ t('agenda.meeting.noOrgNodes') }}</p>
            </div>

            <div class="agenda-form__row agenda-form__row--meeting-window">
              <template v-if="supportsDateTimeLocal">
                <BaseInput v-model="meetingWindowStart" type="datetime-local" :label="t('agenda.meeting.windowStartLabel')" />
                <BaseInput v-model="meetingWindowEnd" type="datetime-local" :label="t('agenda.meeting.windowEndLabel')" />
              </template>
              <template v-else>
                <BaseInput
                  v-model="meetingWindowStartDateFallback"
                  type="text"
                  :label="`${t('agenda.meeting.windowStartLabel')} (JJ/MM/AAAA)`"
                  :placeholder="t('agenda.meeting.datePlaceholder')"
                />
                <BaseInput
                  v-model="meetingWindowStartTimeFallback"
                  type="text"
                  :label="`${t('agenda.meeting.windowStartLabel')} (HH:MM)`"
                  :placeholder="t('agenda.meeting.timePlaceholder')"
                />
                <BaseInput
                  v-model="meetingWindowEndDateFallback"
                  type="text"
                  :label="`${t('agenda.meeting.windowEndLabel')} (JJ/MM/AAAA)`"
                  :placeholder="t('agenda.meeting.datePlaceholder')"
                />
                <BaseInput
                  v-model="meetingWindowEndTimeFallback"
                  type="text"
                  :label="`${t('agenda.meeting.windowEndLabel')} (HH:MM)`"
                  :placeholder="t('agenda.meeting.timePlaceholder')"
                />
              </template>
            </div>

            <div class="agenda-form__row agenda-form__row--meeting-numbers">
              <BaseInput v-model="meetingDurationMinutes" type="number" min="15" max="480" :label="t('agenda.meeting.durationLabel')" />
              <BaseInput v-model="meetingMaxSuggestions" type="number" min="1" max="50" :label="t('agenda.meeting.maxSuggestionsLabel')" />
            </div>

            <div class="agenda-form__field">
              <label class="agenda-form__label">{{ t('agenda.meeting.tagsLabel') }}</label>
              <p v-if="!agenda.tags.length" class="agenda-form__hint">{{ t('agenda.noTags') }}</p>
              <div v-else class="agenda-tags">
                <button
                  v-for="tag in agenda.tags"
                  :key="`meeting-${tag.id}`"
                  type="button"
                  class="agenda-tag"
                  :class="{ 'agenda-tag--selected': isMeetingTagSelected(tag.id) }"
                  :style="tagChipStyle(tag)"
                  @click="toggleMeetingTag(tag.id)"
                >
                  <span>{{ tag.name }}</span>
                </button>
              </div>
            </div>

            <div class="agenda-form__footer">
              <BaseButton type="submit" :loading="meetingLoading">{{ t('agenda.meeting.suggestButton') }}</BaseButton>
            </div>
            </form>

            <div v-if="meetingSuggestions.length" class="meeting-suggestions">
              <div class="agenda-panel__header agenda-panel__header--compact">
                <h3>{{ t('agenda.meeting.suggestionsTitle') }}</h3>
                <p v-if="meetingScannedSlots != null">{{ t('agenda.meeting.scannedSlots') }}: {{ meetingScannedSlots }}</p>
              </div>
              <div class="meeting-suggestions__list">
                <article v-for="suggestion in meetingSuggestions" :key="`${suggestion.startAt}-${suggestion.endAt}`" class="meeting-suggestion">
                  <div class="meeting-suggestion__main">
                    <strong>{{ formatDateTimeRange(suggestion.startAt, suggestion.endAt) }}</strong>
                    <p>{{ suggestion.rationale }}</p>
                  </div>
                  <div class="meeting-suggestion__side">
                    <span>{{ t('agenda.meeting.scoreLabel') }}: {{ suggestion.score }}</span>
                    <BaseButton
                      type="button"
                      size="sm"
                      :loading="creatingMeetingKey === `${suggestion.startAt}-${suggestion.endAt}`"
                      @click="createMeetingFromSuggestion(suggestion)"
                    >
                      {{ t('agenda.meeting.createButton') }}
                    </BaseButton>
                  </div>
                </article>
              </div>
            </div>
          </template>
        </section>

        <AgendaCalendarPanel
          :entries-count="agenda.entries.length"
          :entries-title="t('agenda.entriesTitle')"
          :entry-group-label="formatGroupCount(agenda.entries.length)"
          :loading="agenda.loading"
          :loading-text="t('common.loading')"
          :error="agenda.error"
          :weekday-labels="weekdayLabels"
          :calendar-cells="calendarCells"
          :today-key="todayKey"
          :selected-day-key="selectedDayKey"
          :selected-day-label="selectedDayLabel"
          :selected-day-title="t('agenda.selectedDayTitle')"
          :selected-day-holiday="selectedDayHoliday"
          :selected-day-entries="selectedDayEntries"
          :no-entries-for-day-text="t('agenda.noEntriesForDay')"
          :manual-locked-text="t('agenda.manualLocked')"
          :delete-label="t('agenda.deleteLabel')"
          :delete-aria-label="t('agenda.deleteAriaLabel')"
          :format-entry-start="formatEntryStart"
          :format-entry-range="formatEntryRange"
          :format-entry-source="formatEntrySource"
          :tag-chip-style="tagChipStyle"
          :can-delete-entry="canDeleteEntry"
          :on-delete-entry="deleteEntry"
          :on-pick-date="pickDateAndSyncEntryDate"
        />
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.agenda-page {
  --agenda-bg: #f6f8f7;
  --agenda-surface: #ffffff;
  --agenda-soft: #eef3ef;
  --agenda-border: #d8dfdc;
  --agenda-text: #17201c;
  --agenda-muted: #64706a;
  --agenda-accent: #0f766e;
  --agenda-accent-soft: #dff6f1;
  background: radial-gradient(circle at 100% 0%, #e4f5ea 0%, #f6f8f7 40%, #f6f8f7 100%);
  border-radius: 18px;
  padding: 1rem;
}

.agenda-month-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin: 0 0 1rem;
}

.agenda-month-label {
  min-width: 210px;
  text-align: center;
  font-size: 1.3rem;
  text-transform: capitalize;
  letter-spacing: 0.02em;
  color: var(--agenda-text);
}

.agenda-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.agenda-panel {
  background: var(--agenda-surface);
  border: 1px solid var(--agenda-border);
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(22, 40, 32, 0.06);
  padding: 1.1rem;
}

.agenda-panel__header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: 1rem;
}

.agenda-panel__header h3 {
  font-size: 1.03rem;
  color: var(--agenda-text);
}

.agenda-panel__header p,
.agenda-state,
.agenda-form__hint,
.meeting-suggestion__side,
.meeting-suggestion__main p,
.agenda-entry__meta,
.calendar-day-detail__header p,
.calendar__weekday,
.calendar__more {
  color: var(--agenda-muted);
}

.agenda-form {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.agenda-form__row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
}

.agenda-form__row--meeting-window {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.agenda-form__row--recurrence {
  grid-template-columns: 1fr;
}

.agenda-form__row--meeting-numbers {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.agenda-form__row--meeting-numbers :deep(.field) {
  display: grid;
  grid-template-rows: 2.35rem auto;
}

.agenda-form__row--meeting-numbers :deep(.field__label) {
  min-height: 2.35rem;
  line-height: 1.2;
  display: block;
}

.agenda-form__field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.agenda-form__selection-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.45rem;
  align-items: center;
}

.agenda-form__select {
  width: 100%;
  border: 1px solid var(--agenda-border);
  border-radius: 10px;
  background: var(--agenda-surface);
  color: var(--agenda-text);
  min-height: 2.35rem;
  padding: 0 0.65rem;
}

.agenda-selected-users {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.agenda-selected-user {
  border: 1px solid #a6d4cc;
  border-radius: 999px;
  background: var(--agenda-accent-soft);
  color: var(--agenda-text);
  padding: 0.35rem 0.6rem;
  font-size: 0.78rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
}

.agenda-selected-user__remove {
  display: inline-flex;
  width: 1.05rem;
  height: 1.05rem;
  border-radius: 999px;
  align-items: center;
  justify-content: center;
  background: #bfe8df;
}

.agenda-form__field--checkbox {
  gap: 0;
}

.agenda-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  color: var(--agenda-text);
  cursor: pointer;
}

.agenda-checkbox-label input {
  width: 1rem;
  height: 1rem;
}

.agenda-weekdays {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.agenda-weekday {
  border: 1px solid var(--agenda-border);
  border-radius: 999px;
  background: #f2f7f4;
  color: var(--agenda-text);
  padding: 0.3rem 0.65rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.agenda-weekday--selected {
  background: #dff6f1;
  border-color: #4ab5a9;
  color: #0f766e;
}

.agenda-form__label {
  font-size: 0.84rem;
}

.agenda-form__footer {
  display: flex;
  justify-content: flex-end;
}

.agenda-divider {
  border-top: 1px dashed var(--agenda-border);
  margin: 1rem 0;
}

.agenda-panel__header--compact {
  margin-bottom: 0.6rem;
}

.agenda-message {
  border-radius: 10px;
  padding: 0.65rem 0.8rem;
  font-size: 0.88rem;
  margin-bottom: 0.8rem;
}

.agenda-message--error {
  background: #fef2f2;
  color: #b91c1c;
}

.agenda-message--success {
  background: #ecfdf3;
  color: #166534;
}

.agenda-tags,
.agenda-entry__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.agenda-tag,
.agenda-chip {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.38rem 0.7rem;
  font-size: 0.79rem;
  font-weight: 600;
}

.agenda-tag {
  cursor: pointer;
  opacity: 0.84;
}

.agenda-tag--selected {
  opacity: 1;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.15);
}

.agenda-tag--participant {
  background: #eef3ef;
  color: #1f2f2a;
  border-color: #d8dfdc;
}

.meeting-suggestions {
  margin-top: 0.9rem;
}

.meeting-suggestions__list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.meeting-suggestion {
  border: 1px solid var(--agenda-border);
  background: #fafcfb;
  border-radius: 12px;
  padding: 0.7rem;
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
}

.calendar {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.calendar__weekdays,
.calendar__grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.38rem;
}

.calendar__weekday {
  text-transform: uppercase;
  font-size: 0.68rem;
  letter-spacing: 0.09em;
  font-weight: 700;
  padding: 0.25rem 0.35rem;
}

.calendar__cell {
  min-height: 116px;
  border: 1px solid var(--agenda-border);
  border-radius: 12px;
  background: var(--agenda-surface);
  padding: 0.45rem;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  transition: transform 0.14s ease, border-color 0.14s ease, box-shadow 0.14s ease;
}

.calendar__cell:hover {
  border-color: #9ccac3;
  transform: translateY(-1px);
}

.calendar__cell--today {
  border-color: var(--agenda-accent);
  box-shadow: 0 0 0 1px rgba(15, 118, 110, 0.18) inset;
}

.calendar__cell--selected {
  background: var(--agenda-accent-soft);
  border-color: #4ab5a9;
}

.calendar__cell--placeholder {
  background: transparent;
  border: 1px dashed #dce4e0;
  pointer-events: none;
}

.calendar__cell--holiday {
  background: #fff4f2;
}

.calendar__cell--school {
  background: #fffbea;
}

.calendar__cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.calendar__day-number {
  color: var(--agenda-text);
  font-size: 0.84rem;
  font-weight: 700;
}

.calendar__count {
  min-width: 1.3rem;
  height: 1.3rem;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.14);
  color: var(--agenda-accent);
  font-size: 0.72rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.calendar-holiday-label {
  font-size: 0.67rem;
  font-weight: 600;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-radius: 4px;
  padding: 0.14rem 0.35rem;
  background: #fee2e2;
  color: #991b1b;
}

.calendar-holiday-label:not(.calendar-holiday-label--public) {
  background: #fef3c7;
  color: #92400e;
}

.calendar__events {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  overflow: hidden;
}

.calendar-event {
  border-radius: 8px;
  background: #ecf9f5;
  border: 1px solid #ccede5;
  padding: 0.24rem 0.36rem;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.calendar-event__time {
  color: #0f766e;
  font-size: 0.68rem;
  font-weight: 700;
}

.calendar-event__title {
  color: #1f2f2a;
  font-size: 0.72rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-day-detail {
  margin-top: 0.75rem;
  border: 1px solid var(--agenda-border);
  border-radius: 14px;
  background: var(--agenda-surface);
  padding: 0.82rem;
}

.calendar-day-detail__header {
  margin-bottom: 0.65rem;
}

.calendar-day-detail__header h4 {
  color: var(--agenda-text);
  font-size: 0.94rem;
}

.calendar-holiday-banner {
  display: flex;
  align-items: center;
  gap: 0.58rem;
  border-radius: 10px;
  margin-bottom: 0.75rem;
  padding: 0.6rem 0.75rem;
  background: #fff4f2;
  border: 1px solid #fecaca;
}

.calendar-holiday-banner:not(.calendar-holiday-banner--public) {
  background: #fffbea;
  border-color: #fde68a;
}

.agenda-state--empty-day {
  text-align: center;
  padding: 1rem 0;
}

.agenda-day__entries {
  display: flex;
  flex-direction: column;
}

.agenda-entry {
  padding: 0.9rem 0;
  border-top: 1px solid var(--agenda-border);
}

.agenda-entry:first-child {
  border-top: none;
}

.agenda-entry__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}

.agenda-entry__delete {
  margin-left: 0.35rem;
  min-width: 92px;
}

.agenda-chip {
  color: #fff;
}

@media (max-width: 1024px) {
  .agenda-layout {
    grid-template-columns: 1fr;
  }

  .calendar__cell {
    min-height: 100px;
  }
}

@media (max-width: 768px) {
  .agenda-page {
    padding: 0.75rem;
  }

  .agenda-panel {
    padding: 0.9rem;
  }

  .agenda-form__row,
  .agenda-form__row--meeting-window {
    grid-template-columns: 1fr;
  }

  .agenda-form__selection-row {
    grid-template-columns: 1fr;
  }

  .meeting-suggestion {
    flex-direction: column;
    align-items: flex-start;
  }

  .meeting-suggestion__side {
    align-items: flex-start;
  }

  .calendar__weekdays,
  .calendar__grid {
    min-width: 0;
    gap: 0.25rem;
  }

  .calendar__weekday {
    padding: 0.22rem 0.15rem;
    font-size: 0.58rem;
    letter-spacing: 0.06em;
    text-align: center;
  }

  .calendar__cell {
    min-height: 92px;
    padding: 0.3rem;
    border-radius: 10px;
  }

  .calendar__day-number {
    font-size: 0.75rem;
  }

  .calendar__count {
    min-width: 1.1rem;
    height: 1.1rem;
    font-size: 0.62rem;
  }

  .calendar-event {
    padding: 0.18rem 0.28rem;
  }

  .calendar-event__time,
  .calendar-event__title,
  .calendar-holiday-label {
    font-size: 0.62rem;
  }
}
</style>

<!-- Dark mode overrides — sélecteur html[data-theme="dark"] pour battre les styles scopés Vue -->
<style>
html[data-theme="dark"] .agenda-page {
  --agenda-bg: #111827;
  --agenda-surface: #1e293b;
  --agenda-soft: #243447;
  --agenda-border: #334155;
  --agenda-text: #e2e8f0;
  --agenda-muted: #94a3b8;
  --agenda-accent: #2dd4bf;
  --agenda-accent-soft: #134e4a;
  background: #111827;
}

html[data-theme="dark"] .agenda-selected-user {
  border-color: #2d5a52;
  background: #134e4a;
}
html[data-theme="dark"] .agenda-selected-user__remove {
  background: #1a5a52;
}

html[data-theme="dark"] .agenda-weekday {
  background: #1e293b;
}
html[data-theme="dark"] .agenda-weekday--selected {
  background: #134e4a;
  border-color: #2dd4bf;
  color: #5eead4;
}

html[data-theme="dark"] .agenda-message--error {
  background: #2d0a0a;
  color: #fca5a5;
}
html[data-theme="dark"] .agenda-message--success {
  background: #052e16;
  color: #86efac;
}

html[data-theme="dark"] .agenda-panel {
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.35);
}

html[data-theme="dark"] .calendar__cell:hover {
  border-color: #475569;
}
html[data-theme="dark"] .calendar__cell--placeholder {
  border-color: #283548;
}
html[data-theme="dark"] .calendar__cell--holiday {
  background: #2d1515;
}
html[data-theme="dark"] .calendar__cell--school {
  background: #2d2a0a;
}

html[data-theme="dark"] .calendar__count {
  background: rgba(45, 212, 191, 0.15);
  color: #2dd4bf;
}

html[data-theme="dark"] .calendar-holiday-label {
  background: #3d1515;
  color: #fca5a5;
}
html[data-theme="dark"] .calendar-holiday-label:not(.calendar-holiday-label--public) {
  background: #3d350a;
  color: #fde68a;
}

html[data-theme="dark"] .calendar-event {
  background: #134e4a;
  border-color: #1a6b62;
}
html[data-theme="dark"] .calendar-event__time {
  color: #5eead4;
}
html[data-theme="dark"] .calendar-event__title {
  color: #cffafe;
}

html[data-theme="dark"] .calendar-holiday-banner {
  background: #2d1515;
  border-color: #7f1d1d;
}
html[data-theme="dark"] .calendar-holiday-banner:not(.calendar-holiday-banner--public) {
  background: #2d2a0a;
  border-color: #78350f;
}

html[data-theme="dark"] .agenda-tag--participant {
  background: #1e293b;
  color: #cbd5e1;
  border-color: #334155;
}

html[data-theme="dark"] .meeting-suggestion {
  background: #1a2535;
}
</style>

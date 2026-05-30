
import apiClient from './index'


// ---- Types ----

export interface AgendaEntry {
  id: number
  userId: number
  title: string
  source: string
  manualLocked: boolean
  startAt: string
  endAt: string
  tags: PlanningTag[]
}

export interface CreateEntryRequest {
  userId: number
  title: string
  startAt: string
  endAt: string
  tagIds?: number[]
  recurrenceFrequency?: 'NONE' | 'DAILY' | 'WEEKLY' | 'WEEKDAYS' | 'MONTHLY'
  recurrenceUntil?: string
  recurrenceWeekdays?: string[]
  recurrenceSkipHolidays?: boolean
  recurrenceExcludedDates?: string[]
}

export interface PlanningTag {
  id: number
  name: string
  description: string | null
  color: string | null
  active: boolean
  blocking: boolean
  createdByUserId: number | null
}

export interface CreateTagRequest {
  name: string
  description?: string | null
  color?: string | null
  blocking?: boolean
  active?: boolean
}

export interface UserPreference {
  userId: number
  workDayStart: string
  workDayEnd: string
  preferredMeetingBlockMinutes: number
}

export interface MeetingSuggestion {
  startAt: string
  endAt: string
  score: number
  rationale: string
}

export interface MeetingSuggestionResult {
  requestedDurationMinutes: number
  scannedSlots: number
  suggestions: MeetingSuggestion[]
}

export interface MeetingSlotsRequest {
  participantUserIds: number[]
  durationMinutes: number
  windowStart: string
  windowEnd: string
  maxSuggestions?: number
}

export interface CreateMeetingRequest {
  title: string
  participantUserIds: number[]
  startAt: string
  endAt: string
  tagIds?: number[]
}

export interface UpdateEntryTagsRequest {
  tagIds: number[]
}

export interface PlanningRole {
  roleName: string
  description: string | null
  active: boolean
  permissions: string[]
}

export interface CreatePlanningRoleRequest {
  roleName: string
  description?: string | null
  active?: boolean
}

export interface ReplacePlanningRolePermissionsRequest {
  permissions: string[]
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function toLocalDateTime(value: Date) {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

function defaultEntriesRange() {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59)
  return { from: toLocalDateTime(from), to: toLocalDateTime(to) }
}

// ---- API calls ----

export const planningApi = {
  getEntries(userId: number, from?: string, to?: string) {
    const range = from && to ? { from, to } : defaultEntriesRange()
    return apiClient.get<AgendaEntry[]>(`/api/v1/planning/users/${userId}/entries`, {
      params: range,
    })
  },

  createEntry(payload: CreateEntryRequest) {
    return apiClient.post<AgendaEntry>('/api/v1/planning/entries/manual', payload)
  },

  updateEntryTags(entryId: number, payload: UpdateEntryTagsRequest) {
    return apiClient.put<AgendaEntry>(`/api/v1/planning/entries/${entryId}/tags`, payload)
  },

  getTags() {
    return apiClient.get<PlanningTag[]>('/api/v1/planning/tags')
  },

  createTag(payload: CreateTagRequest) {
    return apiClient.post<PlanningTag>('/api/v1/planning/tags', payload)
  },

  updateTagColor(tagId: number, color: string | null) {
    return apiClient.put<PlanningTag>(`/api/v1/planning/tags/${tagId}/color`, color ?? '', {
      headers: { 'Content-Type': 'text/plain' },
    })
  },

  getPreferences(userId: number) {
    return apiClient.get<UserPreference>(`/api/v1/planning/users/${userId}/preferences`)
  },

  getMyPlanningPermissions() {
    return apiClient.get<string[]>('/api/v1/planning/me/permissions')
  },

  updatePreferences(userId: number, payload: Partial<UserPreference>) {
    return apiClient.put<UserPreference>(`/api/v1/planning/users/${userId}/preferences`, payload)
  },

  suggestMeetingSlots(payload: MeetingSlotsRequest) {
    return apiClient.post<MeetingSuggestionResult>('/api/v1/planning/meeting-slots/suggestions', payload)
  },

  createMeeting(payload: CreateMeetingRequest) {
    return apiClient.post<AgendaEntry[]>('/api/v1/planning/meetings', payload)
  },

  deleteEntry(entryId: number) {
    return apiClient.delete(`/api/v1/planning/entries/${entryId}`)
  },

  listPlanningRoles() {
    return apiClient.get<PlanningRole[]>('/api/v1/planning/admin/roles')
  },

  listAvailablePlanningPermissions() {
    return apiClient.get<string[]>('/api/v1/planning/admin/permissions')
  },

  createPlanningRole(payload: CreatePlanningRoleRequest) {
    return apiClient.post<PlanningRole>('/api/v1/planning/admin/roles', payload)
  },

  replacePlanningRolePermissions(roleName: string, payload: ReplacePlanningRolePermissionsRequest) {
    return apiClient.put<PlanningRole>(`/api/v1/planning/admin/roles/${encodeURIComponent(roleName)}/permissions`, payload)
  },

  assignPlanningRoleToUser(targetUserId: number, roleName: string) {
    return apiClient.post<void>(`/api/v1/planning/admin/users/${targetUserId}/roles/${encodeURIComponent(roleName)}`)
  },

  getUserPlanningRoles(targetUserId: number) {
    return apiClient.get<string[]>(`/api/v1/planning/admin/users/${targetUserId}/roles`)
  },

  revokePlanningRoleFromUser(targetUserId: number, roleName: string) {
    return apiClient.delete<void>(`/api/v1/planning/admin/users/${targetUserId}/roles/${encodeURIComponent(roleName)}`)
  },
}

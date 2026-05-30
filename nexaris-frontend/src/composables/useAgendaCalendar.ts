import { computed, ref, type ComputedRef, type Ref } from 'vue'
import type { AgendaEntry } from '@/api/planning'

export interface CalendarHoliday {
  name: string
  isPublic: boolean
}

export function useAgendaCalendar(
  locale: Ref<string>,
  selectedMonth: Ref<Date>,
  agendaEntries: ComputedRef<AgendaEntry[]>,
  holidaysByDay: Ref<Map<string, CalendarHoliday>>,
) {
  function pad(value: number) {
    return String(value).padStart(2, '0')
  }

  function toDateInputValue(date: Date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }

  function dateFromKey(key: string) {
    const [year = 1970, month = 1, day = 1] = key.split('-').map((value) => Number(value))
    return new Date(year, month - 1, day)
  }

  function toDateKey(date: Date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  }

  function toLocalDateTime(date: Date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }

  function buildMonthRange(anchor: Date) {
    const from = new Date(anchor.getFullYear(), anchor.getMonth(), 1, 0, 0, 0)
    const to = new Date(anchor.getFullYear(), anchor.getMonth() + 1, 0, 23, 59, 59)
    return { from: toLocalDateTime(from), to: toLocalDateTime(to) }
  }

  function shiftMonth(delta: number) {
    const next = new Date(selectedMonth.value)
    next.setMonth(next.getMonth() + delta)
    selectedMonth.value = next
  }

  function resetToCurrentMonth() {
    selectedMonth.value = new Date()
  }

  function formatEntryRange(startAt: string, endAt: string) {
    const formatter = new Intl.DateTimeFormat(locale.value, {
      hour: '2-digit',
      minute: '2-digit',
    })

    return `${formatter.format(new Date(startAt))} - ${formatter.format(new Date(endAt))}`
  }

  function formatDateTimeRange(startAt: string, endAt: string) {
    const formatter = new Intl.DateTimeFormat(locale.value, {
      weekday: 'short',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })

    return `${formatter.format(new Date(startAt))} - ${formatter.format(new Date(endAt))}`
  }

  function formatEntryStart(startAt: string) {
    return new Intl.DateTimeFormat(locale.value, {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(startAt))
  }

  const monthLabel = computed(() =>
    new Intl.DateTimeFormat(locale.value, { month: 'long', year: 'numeric' }).format(selectedMonth.value),
  )

  const todayKey = computed(() => toDateKey(new Date()))
  const selectedDayKey = ref(todayKey.value)

  const entriesByDay = computed(() => {
    const map = new Map<string, AgendaEntry[]>()
    const sortedEntries = [...agendaEntries.value].sort(
      (left, right) => new Date(left.startAt).getTime() - new Date(right.startAt).getTime(),
    )

    for (const entry of sortedEntries) {
      const key = toDateKey(new Date(entry.startAt))
      if (!map.has(key)) {
        map.set(key, [])
      }
      map.get(key)?.push(entry)
    }

    return map
  })

  const weekdayLabels = computed(() => {
    const monday = new Date(2024, 0, 1)
    return Array.from({ length: 7 }, (_, index) => {
      const day = new Date(monday)
      day.setDate(monday.getDate() + index)
      return new Intl.DateTimeFormat(locale.value, { weekday: 'short' }).format(day)
    })
  })

  const calendarCells = computed(() => {
    const firstOfMonth = new Date(selectedMonth.value.getFullYear(), selectedMonth.value.getMonth(), 1)
    const mondayBasedOffset = (firstOfMonth.getDay() + 6) % 7
    const daysInMonth = new Date(selectedMonth.value.getFullYear(), selectedMonth.value.getMonth() + 1, 0).getDate()
    const totalCells = Math.ceil((mondayBasedOffset + daysInMonth) / 7) * 7

    return Array.from({ length: totalCells }, (_, index) => {
      const dayInMonth = index - mondayBasedOffset + 1

      if (dayInMonth < 1 || dayInMonth > daysInMonth) {
        return {
          key: `empty-${selectedMonth.value.getFullYear()}-${selectedMonth.value.getMonth()}-${index}`,
          date: null,
          dayNumber: null,
          entries: [],
          holiday: null,
          isPlaceholder: true,
        }
      }

      const date = new Date(selectedMonth.value.getFullYear(), selectedMonth.value.getMonth(), dayInMonth)
      const key = toDateKey(date)

      return {
        key,
        date,
        dayNumber: dayInMonth,
        entries: entriesByDay.value.get(key) ?? [],
        holiday: holidaysByDay.value.get(key) ?? null,
        isPlaceholder: false,
      }
    })
  })

  const selectedDayDate = computed(() => dateFromKey(selectedDayKey.value))
  const selectedDayLabel = computed(() =>
    new Intl.DateTimeFormat(locale.value, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).format(selectedDayDate.value),
  )
  const selectedDayEntries = computed(() => entriesByDay.value.get(selectedDayKey.value) ?? [])
  const selectedDayHoliday = computed(() => holidaysByDay.value.get(selectedDayKey.value) ?? null)

  function pickDate(date: Date) {
    selectedDayKey.value = toDateKey(date)
  }

  return {
    monthLabel,
    todayKey,
    selectedDayKey,
    weekdayLabels,
    calendarCells,
    selectedDayDate,
    selectedDayLabel,
    selectedDayEntries,
    selectedDayHoliday,
    entriesByDay,
    pad,
    toDateInputValue,
    dateFromKey,
    toDateKey,
    toLocalDateTime,
    buildMonthRange,
    shiftMonth,
    resetToCurrentMonth,
    formatEntryRange,
    formatDateTimeRange,
    formatEntryStart,
    pickDate,
  }
}

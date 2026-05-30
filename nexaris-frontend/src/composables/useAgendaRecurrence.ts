import { ref, watch } from 'vue'
import type { TranslationKey } from '@/i18n/messages'

type RecurrenceFrequency = 'NONE' | 'DAILY' | 'WEEKLY' | 'WEEKDAYS' | 'MONTHLY'

type RecurrencePayload = {
  recurrenceFrequency?: RecurrenceFrequency
  recurrenceUntil?: string
  recurrenceWeekdays?: string[]
  recurrenceSkipHolidays?: boolean
  recurrenceExcludedDates?: string[]
}

export function useAgendaRecurrence() {
  const recurrenceFrequency = ref<RecurrenceFrequency>('NONE')
  const recurrenceUntil = ref('')
  const recurrenceWeekdays = ref<string[]>([])
  const recurrenceSkipHolidays = ref(false)

  const recurrenceWeekdayOptions: Array<{ value: string; labelKey: TranslationKey }> = [
    { value: 'MONDAY', labelKey: 'agenda.recurrence.weekdaysShort.monday' },
    { value: 'TUESDAY', labelKey: 'agenda.recurrence.weekdaysShort.tuesday' },
    { value: 'WEDNESDAY', labelKey: 'agenda.recurrence.weekdaysShort.wednesday' },
    { value: 'THURSDAY', labelKey: 'agenda.recurrence.weekdaysShort.thursday' },
    { value: 'FRIDAY', labelKey: 'agenda.recurrence.weekdaysShort.friday' },
    { value: 'SATURDAY', labelKey: 'agenda.recurrence.weekdaysShort.saturday' },
    { value: 'SUNDAY', labelKey: 'agenda.recurrence.weekdaysShort.sunday' },
  ]

  watch(recurrenceFrequency, (nextValue) => {
    if (nextValue === 'NONE') {
      recurrenceSkipHolidays.value = false
    }
  })

  function toggleRecurrenceWeekday(day: string) {
    if (recurrenceWeekdays.value.includes(day)) {
      recurrenceWeekdays.value = recurrenceWeekdays.value.filter((current) => current !== day)
      return
    }
    recurrenceWeekdays.value = [...recurrenceWeekdays.value, day]
  }

  function isRecurrenceWeekdaySelected(day: string) {
    return recurrenceWeekdays.value.includes(day)
  }

  async function applyRecurrenceToPayload(
    payload: RecurrencePayload,
    entryDate: string,
    t: (key: TranslationKey) => string,
    ensureHolidayCoverage: (fromDate: Date, toDate: Date) => Promise<void>,
    buildExcludedHolidayDates: (fromDate: Date, toDate: Date) => string[],
    setError: (message: string) => void,
  ) {
    if (recurrenceFrequency.value === 'NONE') return true

    if (!recurrenceUntil.value) {
      setError(t('agenda.errors.recurrenceUntilRequired'))
      return false
    }

    const untilDate = new Date(`${recurrenceUntil.value}T00:00:00`)
    const entryDateValue = new Date(`${entryDate}T00:00:00`)
    if (Number.isNaN(untilDate.getTime()) || untilDate < entryDateValue) {
      setError(t('agenda.errors.recurrenceUntilInvalid'))
      return false
    }

    payload.recurrenceFrequency = recurrenceFrequency.value
    payload.recurrenceUntil = recurrenceUntil.value
    if (recurrenceFrequency.value === 'WEEKLY' && recurrenceWeekdays.value.length > 0) {
      payload.recurrenceWeekdays = recurrenceWeekdays.value
    }

    payload.recurrenceSkipHolidays = recurrenceSkipHolidays.value
    if (recurrenceSkipHolidays.value) {
      await ensureHolidayCoverage(entryDateValue, untilDate)
      payload.recurrenceExcludedDates = buildExcludedHolidayDates(entryDateValue, untilDate)
    }

    return true
  }

  function resetRecurrence() {
    recurrenceFrequency.value = 'NONE'
    recurrenceUntil.value = ''
    recurrenceWeekdays.value = []
    recurrenceSkipHolidays.value = false
  }

  return {
    recurrenceFrequency,
    recurrenceUntil,
    recurrenceWeekdays,
    recurrenceSkipHolidays,
    recurrenceWeekdayOptions,
    toggleRecurrenceWeekday,
    isRecurrenceWeekdaySelected,
    applyRecurrenceToPayload,
    resetRecurrence,
  }
}

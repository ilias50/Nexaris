<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'
import type { AgendaEntry, PlanningTag } from '@/types/domain'
import type { CalendarHoliday } from '@/composables/useAgendaCalendar'

interface CalendarCell {
  key: string
  date: Date | null
  dayNumber: number | null
  entries: AgendaEntry[]
  holiday: CalendarHoliday | null
  isPlaceholder: boolean
}

const props = defineProps<{
  entriesCount: number
  entriesTitle: string
  entryGroupLabel: string
  loading: boolean
  loadingText: string
  error: string | null
  weekdayLabels: string[]
  calendarCells: CalendarCell[]
  todayKey: string
  selectedDayKey: string
  selectedDayLabel: string
  selectedDayTitle: string
  selectedDayHoliday: CalendarHoliday | null
  selectedDayEntries: AgendaEntry[]
  noEntriesForDayText: string
  manualLockedText: string
  deleteLabel: string
  deleteAriaLabel: string
  formatEntryStart: (startAt: string) => string
  formatEntryRange: (startAt: string, endAt: string) => string
  formatEntrySource: (source: string) => string
  tagChipStyle: (tag: PlanningTag) => Record<string, string>
  canDeleteEntry: (entry: AgendaEntry) => boolean
  onDeleteEntry: (entryId: number) => void | Promise<void>
  onPickDate: (date: Date) => void
}>()
</script>

<template>
  <section class="agenda-panel agenda-panel--list">
    <div class="agenda-panel__header">
      <h3>{{ entriesTitle }}</h3>
      <p>{{ entriesCount }} {{ entryGroupLabel }}</p>
    </div>

    <p v-if="loading" class="agenda-state agenda-state--inline">{{ loadingText }}</p>
    <p v-else-if="error" class="agenda-message agenda-message--error">{{ error }}</p>

    <div class="calendar">
      <div class="calendar__weekdays">
        <div v-for="label in weekdayLabels" :key="label" class="calendar__weekday">{{ label }}</div>
      </div>

      <div class="calendar__grid">
        <template v-for="cell in calendarCells" :key="cell.key">
          <div v-if="cell.isPlaceholder" class="calendar__cell calendar__cell--placeholder" aria-hidden="true" />
          <button
            v-else
            type="button"
            class="calendar__cell"
            :class="{
              'calendar__cell--today': cell.key === todayKey,
              'calendar__cell--selected': cell.key === selectedDayKey,
              'calendar__cell--holiday': !!cell.holiday && cell.holiday.isPublic,
              'calendar__cell--school': !!cell.holiday && !cell.holiday.isPublic,
            }"
            @click="onPickDate(cell.date!)"
          >
            <div class="calendar__cell-head">
              <span class="calendar__day-number">{{ cell.dayNumber }}</span>
              <span v-if="cell.entries.length" class="calendar__count">{{ cell.entries.length }}</span>
            </div>

            <div v-if="cell.holiday" class="calendar-holiday-label" :class="{ 'calendar-holiday-label--public': cell.holiday.isPublic }">
              {{ cell.holiday.name }}
            </div>

            <div class="calendar__events">
              <article v-for="entry in cell.entries.slice(0, 2)" :key="entry.id" class="calendar-event">
                <div class="calendar-event__time">{{ formatEntryStart(entry.startAt) }}</div>
                <div class="calendar-event__title">{{ entry.title }}</div>
              </article>
              <p v-if="cell.entries.length > 2" class="calendar__more">+{{ cell.entries.length - 2 }}</p>
            </div>
          </button>
        </template>
      </div>

      <div class="calendar-day-detail">
        <div class="calendar-day-detail__header">
          <h4>{{ selectedDayTitle }}</h4>
          <p>{{ selectedDayLabel }}</p>
        </div>

        <div v-if="selectedDayHoliday" class="calendar-holiday-banner" :class="{ 'calendar-holiday-banner--public': selectedDayHoliday.isPublic }">
          <span class="calendar-holiday-banner__icon">{{ selectedDayHoliday.isPublic ? '🏛️' : '📅' }}</span>
          <div class="calendar-holiday-banner__text">
            <strong>{{ selectedDayHoliday.name }}</strong>
          </div>
        </div>

        <p v-if="!selectedDayEntries.length" class="agenda-state agenda-state--empty-day">
          {{ noEntriesForDayText }}
        </p>

        <div v-else class="agenda-day__entries">
          <div v-for="entry in selectedDayEntries" :key="entry.id" class="agenda-entry">
            <div class="agenda-entry__top agenda-entry__top--row">
              <strong>{{ entry.title }}</strong>
              <BaseButton
                v-if="canDeleteEntry(entry)"
                variant="danger"
                size="sm"
                class="agenda-entry__delete"
                @click="onDeleteEntry(entry.id)"
                :aria-label="deleteAriaLabel"
              >{{ deleteLabel }}</BaseButton>
            </div>
            <div class="agenda-entry__meta">
              <span>{{ formatEntryRange(entry.startAt, entry.endAt) }}</span>
              <span>• {{ formatEntrySource(entry.source) }}</span>
              <span v-if="entry.manualLocked">• {{ manualLockedText }}</span>
            </div>
            <div v-if="entry.tags.length" class="agenda-entry__tags">
              <span v-for="tag in entry.tags" :key="tag.id" class="agenda-chip" :style="tagChipStyle(tag)">
                {{ tag.name }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
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
.agenda-entry__meta,
.calendar-day-detail__header p,
.calendar__weekday,
.calendar__more {
  color: var(--agenda-muted);
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

.agenda-chip {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.38rem 0.7rem;
  font-size: 0.79rem;
  font-weight: 600;
  color: #fff;
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

.agenda-entry__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

@media (max-width: 1024px) {
  .calendar__cell {
    min-height: 100px;
  }
}

@media (max-width: 768px) {
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

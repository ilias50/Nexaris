import { ref } from 'vue'
import { defineStore } from 'pinia'
import { planningApi, type AgendaEntry, type PlanningTag, type CreateEntryRequest } from '@/api/planning'
import { useI18n } from '@/i18n'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'

export const useAgendaStore = defineStore('agenda', () => {
  const { t } = useI18n()
  const entries = ref<AgendaEntry[]>([])
  const tags = ref<PlanningTag[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function deleteEntry(entryId: number) {
    await planningApi.deleteEntry(entryId)
    entries.value = entries.value.filter((e: AgendaEntry) => e.id !== entryId)
  }

  async function fetchEntries(userId: number, from?: string, to?: string) {
    loading.value = true
    error.value = null
    try {
      const res = await planningApi.getEntries(userId, from, to)
      entries.value = res.data
    } catch (errorResponse: unknown) {
      error.value = resolveApiErrorMessage(errorResponse, t, {
        server: 'agenda.errors.loadFailed',
        generic: 'agenda.errors.loadFailed',
      })
    } finally {
      loading.value = false
    }
  }

  async function fetchTags() {
    try {
      const res = await planningApi.getTags()
      tags.value = res.data
    } catch {
      // non-bloquant
    }
  }

  async function addEntry(data: CreateEntryRequest) {
    const res = await planningApi.createEntry(data)
    entries.value.push(res.data)
    return res.data
  }

  function reset() {
    entries.value = []
    tags.value = []
    error.value = null
  }

  return { entries, tags, loading, error, fetchEntries, fetchTags, addEntry, deleteEntry, reset }
})

import { authApi } from '@/api/auth'
import { planningApi, type CreateTagRequest } from '@/api/planning'
import type { AdminUser } from '@/api/auth'
import type { PlanningTag } from '@/api/planning'

export function useTagsAdminApiAccess() {
  async function getTags() {
    return planningApi.getTags()
  }

  async function createTag(payload: CreateTagRequest) {
    await planningApi.createTag(payload)
  }

  async function updateTagColor(tagId: number, color: string | null) {
    return planningApi.updateTagColor(tagId, color)
  }

  async function listUsersSafe() {
    return authApi.listUsersSafe()
  }

  return {
    getTags,
    createTag,
    updateTagColor,
    listUsersSafe,
  }
}

export type { AdminUser, PlanningTag }

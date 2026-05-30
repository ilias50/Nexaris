import { orgApi } from '@/api/org'
import type { MyOrgTreeNode } from '@/api/org'

export function useOrgTreeApiAccess() {
  async function getMyTreeWorkspace() {
    return orgApi.getMyTreeWorkspace()
  }

  async function getNodeLinks(nodeId: number, activeOnly = true) {
    return orgApi.getNodeLinks(nodeId, activeOnly)
  }

  async function getNodeAnnouncements(nodeId: number) {
    return orgApi.getNodeAnnouncements(nodeId)
  }

  return {
    getMyTreeWorkspace,
    getNodeLinks,
    getNodeAnnouncements,
  }
}

export type { MyOrgTreeNode }
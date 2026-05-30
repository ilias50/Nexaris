import { orgApi } from '@/api/org'
import type { Announcement, NodeContent, NodeLink, OrgNode } from '@/api/org'

export function useOrgNodeReadApiAccess() {
  async function getNodeReadWorkspace(nodeId: number) {
    return orgApi.getNodeReadWorkspace(nodeId)
  }

  return {
    getNodeReadWorkspace,
  }
}

export type { Announcement, NodeContent, NodeLink, OrgNode }

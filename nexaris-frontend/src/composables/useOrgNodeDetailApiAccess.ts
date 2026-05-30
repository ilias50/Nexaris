import { orgApi, type UpdateNodeRequest } from '@/api/org'
import type { OrgNode } from '@/api/org'

export function useOrgNodeDetailApiAccess() {
  async function getNodeDetailWorkspace(nodeId: number) {
    return orgApi.getNodeDetailWorkspace(nodeId)
  }

  async function updateNode(nodeId: number, payload: UpdateNodeRequest) {
    return orgApi.updateNode(nodeId, payload)
  }

  return {
    getNodeDetailWorkspace,
    updateNode,
  }
}

export type { OrgNode }

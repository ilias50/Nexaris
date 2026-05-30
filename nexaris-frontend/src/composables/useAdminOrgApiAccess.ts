import {
  orgApi,
  type CatalogEntry,
  type CreateMembershipRequest,
  type CreateNodeRequest,
  type OrgNode,
  type OrgTreeNode,
  type UpdateNodeRequest,
} from '@/api/org'

export function useAdminOrgApiAccess() {
  async function getNodeTypeCatalogMeta(optional = true) {
    return orgApi.getNodeTypeCatalogMeta({ optional })
  }

  async function getTree() {
    const { data } = await orgApi.getTree()
    return data
  }

  async function updateNode(nodeId: number, payload: UpdateNodeRequest) {
    await orgApi.updateNode(nodeId, payload)
  }

  async function createNode(payload: CreateNodeRequest) {
    const { data } = await orgApi.createNode(payload)
    return data
  }

  async function createMembership(nodeId: number, payload: CreateMembershipRequest) {
    await orgApi.createMembership(nodeId, payload)
  }

  async function deleteNode(nodeId: number) {
    await orgApi.deleteNode(nodeId)
  }

  return {
    getNodeTypeCatalogMeta,
    getTree,
    updateNode,
    createNode,
    createMembership,
    deleteNode,
  }
}

export type { CatalogEntry, OrgNode, OrgTreeNode }
import apiClient from './index'

export interface OrgNode {
  id: number
  parentId: number | null
  nodeType: string
  name: string
  slug: string
  path: string
  depth: number
  sortOrder: number
  isActive: boolean
}

export interface OrgTreeNode extends OrgNode {
  children: OrgTreeNode[]
}

export interface MyOrgTreeNode extends OrgNode {
  canEdit: boolean
  children: MyOrgTreeNode[]
}

export interface NodeMembership {
  id: number
  nodeId: number
  userId: number
  membershipRole: string
  isPrimary: boolean
  activeFrom: string | null
  activeTo: string | null
}

export interface NodeAccessRule {
  id: number
  nodeId: number
  effect: string
  subjectType: string
  subjectValue: string
  permission: string
  appliesToChildren: boolean
}

export interface CreateNodeRequest {
  parentId?: number | null
  nodeType: string
  name: string
  slug?: string
}

export interface UpdateNodeRequest {
  name?: string
  isActive?: boolean
}

export interface CreateMembershipRequest {
  userId: number
  membershipRole: string
  isPrimary?: boolean
}

export interface CreateAccessRuleRequest {
  effect: string
  subjectType: string
  subjectValue: string
  permission: string
  appliesToChildren?: boolean
}

export interface NodeContent {
  id: number
  nodeId: number
  summary: string | null
  description: string | null
  contactEmail: string | null
  location: string | null
  metadataJson: string | null
}

export interface NodeContentRequest {
  summary?: string | null
  description?: string | null
  contactEmail?: string | null
  location?: string | null
  metadataJson?: string | null
}

export interface NodeLink {
  id: number
  nodeId: number
  label: string
  url: string
  category: string
  icon: string | null
  visibility: string
  sortOrder: number
  isActive: boolean
}

export interface NodeLinkRequest {
  label: string
  url: string
  category?: string
  icon?: string | null
  visibility?: string
  sortOrder?: number
  isActive?: boolean
}

export interface Announcement {
  id: number
  nodeId: number
  scopeType: string
  title: string
  body: string
  severity: string
  startAt: string | null
  endAt: string | null
  isActive: boolean
  createdByUserId: number | null
}

export interface AnnouncementRequest {
  nodeId: number
  scopeType: string
  title: string
  body: string
  severity?: string
  startAt?: string | null
  endAt?: string | null
  isActive?: boolean
  createdByUserId?: number
}

export interface PermissionsResponse {
  nodeId: number
  userId: number
  permissions: string[]
}

export interface CatalogEntry {
  value: string
  color: string | null
}

export interface OrgUserRole {
  id: number
  userId: number
  roleName: string
}

export interface OrgNodeReadWorkspaceResponse {
  node: OrgNode
  content: NodeContent | null
  links: NodeLink[]
  announcements: Announcement[]
  permissions: string[]
  catalogs: {
    nodeTypes: CatalogEntry[]
    linkCategories: CatalogEntry[]
    severities: CatalogEntry[]
  }
}

export interface OrgNodeDetailWorkspaceResponse {
  node: OrgNode
  content: NodeContent | null
  links: NodeLink[]
  announcements: Announcement[]
  permissions: string[]
  catalogs: {
    linkCategories: CatalogEntry[]
    severities: CatalogEntry[]
    membershipRoles: string[]
  }
}

export interface OrgCatalogWorkspaceResponse {
  accessRuleRoles: CatalogEntry[]
  membershipRoles: CatalogEntry[]
  nodeTypes: CatalogEntry[]
  announcementSeverities: CatalogEntry[]
  linkCategories: CatalogEntry[]
}

const catalogMetaCache = new Map<string, CatalogEntry[]>()

async function getCatalogMetaCachedInternal(catalogType: string, force = false, optional = false) {
  if (!force && catalogMetaCache.has(catalogType)) {
    return catalogMetaCache.get(catalogType) ?? []
  }

  try {
    const { data } = await apiClient.get<CatalogEntry[]>(`/api/v1/org/catalogs/${catalogType}/meta`)
    catalogMetaCache.set(catalogType, data)
    return data
  } catch (error) {
    if (optional) return []
    throw error
  }
}

/** Convert a display name to a URL-safe slug (backend normalizes anyway). */
export function toSlug(name: string): string {
  return name
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

export const orgApi = {
  getTree() {
    return apiClient.get<OrgTreeNode[]>('/api/v1/org/tree')
  },

  getMyTree() {
    return apiClient.get<MyOrgTreeNode[]>('/api/v1/org/tree/my-tree')
  },

  getNode(nodeId: number) {
    return apiClient.get<OrgNode>(`/api/v1/org/nodes/${nodeId}`)
  },

  createNode(payload: CreateNodeRequest) {
    const body = { ...payload, slug: payload.slug ?? toSlug(payload.name) }
    return apiClient.post<OrgNode>('/api/v1/org/nodes', body)
  },

  updateNode(nodeId: number, payload: UpdateNodeRequest) {
    return apiClient.patch<OrgNode>(`/api/v1/org/nodes/${nodeId}`, payload)
  },

  deleteNode(nodeId: number) {
    return apiClient.delete(`/api/v1/org/nodes/${nodeId}`)
  },

  getNodeMemberships(nodeId: number) {
    return apiClient.get<NodeMembership[]>(`/api/v1/org/nodes/${nodeId}/memberships`)
  },

  createMembership(nodeId: number, payload: CreateMembershipRequest) {
    return apiClient.post<NodeMembership>(`/api/v1/org/nodes/${nodeId}/memberships`, payload)
  },

  deleteMembership(membershipId: number) {
    return apiClient.delete(`/api/v1/org/memberships/${membershipId}`)
  },

  getAccessRules(nodeId: number) {
    return apiClient.get<NodeAccessRule[]>(`/api/v1/org/nodes/${nodeId}/access-rules`)
  },

  createAccessRule(nodeId: number, payload: CreateAccessRuleRequest) {
    return apiClient.post<NodeAccessRule>(`/api/v1/org/nodes/${nodeId}/access-rules`, payload)
  },

  deleteAccessRule(ruleId: number) {
    return apiClient.delete(`/api/v1/org/access-rules/${ruleId}`)
  },

  getNodeContent(nodeId: number) {
    return apiClient.get<NodeContent | null>(`/api/v1/org/nodes/${nodeId}/content`)
  },

  upsertNodeContent(nodeId: number, payload: NodeContentRequest) {
    return apiClient.put<NodeContent>(`/api/v1/org/nodes/${nodeId}/content`, payload)
  },

  getNodeLinks(nodeId: number, activeOnly = true) {
    return apiClient.get<NodeLink[]>(`/api/v1/org/nodes/${nodeId}/links`, {
      params: { activeOnly },
    })
  },

  createNodeLink(nodeId: number, payload: NodeLinkRequest) {
    return apiClient.post<NodeLink>(`/api/v1/org/nodes/${nodeId}/links`, payload)
  },

  updateNodeLink(linkId: number, payload: Partial<NodeLinkRequest>) {
    return apiClient.patch<NodeLink>(`/api/v1/org/links/${linkId}`, payload)
  },

  deleteNodeLink(linkId: number) {
    return apiClient.delete(`/api/v1/org/links/${linkId}`)
  },

  getNodeAnnouncements(nodeId: number) {
    return apiClient.get<Announcement[]>(`/api/v1/org/nodes/${nodeId}/announcements`)
  },

  createAnnouncement(payload: AnnouncementRequest) {
    return apiClient.post<Announcement>('/api/v1/org/announcements', payload)
  },

  updateAnnouncement(announcementId: number, payload: Partial<AnnouncementRequest>) {
    return apiClient.patch<Announcement>(`/api/v1/org/announcements/${announcementId}`, payload)
  },

  deleteAnnouncement(announcementId: number) {
    return apiClient.delete(`/api/v1/org/announcements/${announcementId}`)
  },

  getMyPermissions(nodeId: number) {
    return apiClient.get<PermissionsResponse>(`/api/v1/org/nodes/${nodeId}/my-permissions`)
  },

  getUserGlobalRoles(userId: number) {
    return apiClient.get<OrgUserRole[]>(`/api/v1/org/users/${userId}/roles`)
  },

  assignUserGlobalRole(userId: number, roleName: string) {
    return apiClient.post<OrgUserRole[]>(`/api/v1/org/users/${userId}/roles`, roleName, {
      headers: { 'Content-Type': 'text/plain' },
    })
  },

  removeUserGlobalRole(userId: number, roleName: string) {
    return apiClient.delete<OrgUserRole[]>(`/api/v1/org/users/${userId}/roles/${encodeURIComponent(roleName)}`)
  },

  getCatalogValues(catalogType: string) {
    return apiClient.get<string[]>(`/api/v1/org/catalogs/${catalogType}`)
  },

  addCatalogValue(catalogType: string, value: string) {
    return apiClient.post<string[]>(`/api/v1/org/catalogs/${catalogType}`, value, {
      headers: { 'Content-Type': 'text/plain' },
    })
  },

  removeCatalogValue(catalogType: string, value: string) {
    return apiClient.delete(`/api/v1/org/catalogs/${catalogType}/${encodeURIComponent(value)}`)
  },

  getCatalogMeta(catalogType: string) {
    return apiClient.get<CatalogEntry[]>(`/api/v1/org/catalogs/${catalogType}/meta`)
  },

  async getCatalogMetaCached(catalogType: string, options?: { force?: boolean; optional?: boolean }) {
    const force = options?.force ?? false
    const optional = options?.optional ?? false
    return getCatalogMetaCachedInternal(catalogType, force, optional)
  },

  clearCatalogMetaCache(catalogType?: string) {
    if (!catalogType) {
      catalogMetaCache.clear()
      return
    }
    catalogMetaCache.delete(catalogType)
  },

  async getOrgCatalogWorkspace(options?: { force?: boolean }) {
    const force = options?.force ?? false
    const [accessRuleRoles, membershipRoles, nodeTypes, announcementSeverities, linkCategories] = await Promise.all([
      getCatalogMetaCachedInternal('ACCESS_RULE_ROLE', force, false),
      getCatalogMetaCachedInternal('MEMBERSHIP_ROLE', force, false),
      getCatalogMetaCachedInternal('NODE_TYPE', force, false),
      getCatalogMetaCachedInternal('ANNOUNCEMENT_SEVERITY', force, false),
      getCatalogMetaCachedInternal('LINK_CATEGORY', force, false),
    ])

    return {
      accessRuleRoles,
      membershipRoles,
      nodeTypes,
      announcementSeverities,
      linkCategories,
    } as OrgCatalogWorkspaceResponse
  },

  async getNodeTypeCatalogMeta(options?: { force?: boolean; optional?: boolean }) {
    return getCatalogMetaCachedInternal('NODE_TYPE', options?.force ?? false, options?.optional ?? false)
  },

  async getMyTreeWorkspace() {
    const [treeRes, nodeTypeMeta] = await Promise.all([
      apiClient.get<MyOrgTreeNode[]>('/api/v1/org/tree/my-tree'),
      getCatalogMetaCachedInternal('NODE_TYPE', false, true),
    ])

    return {
      tree: treeRes.data,
      nodeTypeMeta,
    }
  },

  async getNodeReadWorkspace(nodeId: number) {
    const [nodeRes, contentRes, linksRes, announcementsRes, permsRes, nodeTypes, linkCategories, severities] =
      await Promise.all([
        apiClient.get<OrgNode>(`/api/v1/org/nodes/${nodeId}`),
        apiClient.get<NodeContent | null>(`/api/v1/org/nodes/${nodeId}/content`),
        apiClient.get<NodeLink[]>(`/api/v1/org/nodes/${nodeId}/links`, { params: { activeOnly: true } }),
        apiClient.get<Announcement[]>(`/api/v1/org/nodes/${nodeId}/announcements`),
        apiClient.get<PermissionsResponse>(`/api/v1/org/nodes/${nodeId}/my-permissions`).catch(() => null),
        getCatalogMetaCachedInternal('NODE_TYPE', false, true),
        getCatalogMetaCachedInternal('LINK_CATEGORY', false, true),
        getCatalogMetaCachedInternal('ANNOUNCEMENT_SEVERITY', false, true),
      ])

    return {
      node: nodeRes.data,
      content: contentRes.data,
      links: linksRes.data,
      announcements: announcementsRes.data,
      permissions: permsRes?.data?.permissions ?? [],
      catalogs: {
        nodeTypes,
        linkCategories,
        severities,
      },
    } as OrgNodeReadWorkspaceResponse
  },

  async getNodeDetailWorkspace(nodeId: number) {
    const [nodeRes, contentRes, linksRes, announcementsRes, permissionsRes, linkCategories, severities, membershipRoles] =
      await Promise.all([
        apiClient.get<OrgNode>(`/api/v1/org/nodes/${nodeId}`),
        apiClient.get<NodeContent | null>(`/api/v1/org/nodes/${nodeId}/content`),
        apiClient.get<NodeLink[]>(`/api/v1/org/nodes/${nodeId}/links`, { params: { activeOnly: false } }),
        apiClient.get<Announcement[]>(`/api/v1/org/nodes/${nodeId}/announcements`),
        apiClient.get<PermissionsResponse>(`/api/v1/org/nodes/${nodeId}/my-permissions`).catch(() => null),
        getCatalogMetaCachedInternal('LINK_CATEGORY', false, false),
        getCatalogMetaCachedInternal('ANNOUNCEMENT_SEVERITY', false, false),
        apiClient.get<string[]>('/api/v1/org/catalogs/MEMBERSHIP_ROLE'),
      ])

    return {
      node: nodeRes.data,
      content: contentRes.data,
      links: linksRes.data,
      announcements: announcementsRes.data,
      permissions: permissionsRes?.data?.permissions ?? [],
      catalogs: {
        linkCategories,
        severities,
        membershipRoles: membershipRoles.data,
      },
    } as OrgNodeDetailWorkspaceResponse
  },

  getAvailablePermissions() {
    return apiClient.get<string[]>('/api/v1/org/permissions/available')
  },

  getMembershipRolePermissions(membershipRole: string) {
    return apiClient.get<string[]>(`/api/v1/org/membership-roles/${encodeURIComponent(membershipRole)}/permissions`)
  },

  replaceMembershipRolePermissions(membershipRole: string, permissions: string[]) {
    return apiClient.put<string[]>(
      `/api/v1/org/membership-roles/${encodeURIComponent(membershipRole)}/permissions`,
      permissions,
    )
  },

  updateCatalogColor(catalogType: string, value: string, color: string | null) {
    return apiClient.patch<CatalogEntry>(
      `/api/v1/org/catalogs/${catalogType}/${encodeURIComponent(value)}/color`,
      color ?? '',
      { headers: { 'Content-Type': 'text/plain' } },
    )
  },
}


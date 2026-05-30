import { ref, type Ref } from 'vue'
import { orgApi, type Announcement, type CatalogEntry, type NodeLink, type NodeContent } from '@/api/org'
import { toCatalogColorMap } from '@/utils/catalogTransform'
import { isBlank, toUpperTrimmed, trimOrEmpty } from '@/utils/validation'

function badgeStyle(color?: string) {
  if (!color) return {}
  return {
    background: `${color}22`,
    borderColor: `${color}88`,
    color,
  }
}

function parseMetadataJson(json: string | null | undefined): { key: string; value: string }[] {
  if (!json) return []
  try {
    const obj = JSON.parse(json)
    if (typeof obj === 'object' && obj !== null && !Array.isArray(obj)) {
      return Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
    }
  } catch {
    return []
  }
  return []
}

function serializeMetadataEntries(entries: Array<{ key: string; value: string }>): string | null {
  const filtered = entries.filter((entry) => !isBlank(entry.key))
  if (!filtered.length) return null
  return JSON.stringify(Object.fromEntries(filtered.map((entry) => [trimOrEmpty(entry.key), entry.value])))
}

const DEFAULT_SEVERITY_DESCRIPTIONS: Record<string, string> = {
  INFO: 'orgNodeDetail.severityDescriptions.INFO',
  WARNING: 'orgNodeDetail.severityDescriptions.WARNING',
  CRITICAL: 'orgNodeDetail.severityDescriptions.CRITICAL',
  MAINTENANCE: 'orgNodeDetail.severityDescriptions.MAINTENANCE',
  INCIDENT: 'orgNodeDetail.severityDescriptions.INCIDENT',
}

const DEFAULT_SCOPE_DESCRIPTIONS: Record<string, string> = {
  NODE: 'orgNodeDetail.scopeDescriptions.NODE',
  SUBTREE: 'orgNodeDetail.scopeDescriptions.SUBTREE',
  GLOBAL: 'orgNodeDetail.scopeDescriptions.GLOBAL',
}

export function useOrgNodeContentManager(nodeId: Ref<number>, getCurrentUserId: () => number | null) {
  const contentForm = ref({
    summary: '',
    description: '',
    contactEmail: '',
    location: '',
  })
  const metadataEntries = ref<Array<{ key: string; value: string }>>([])
  const contentSaving = ref(false)

  const links = ref<NodeLink[]>([])
  const linkForm = ref({
    label: '',
    url: '',
    category: 'GENERAL',
  })
  const linkSaving = ref(false)

  const announcements = ref<Announcement[]>([])
  const announcementForm = ref({
    title: '',
    body: '',
    severity: 'INFO',
    scopeType: 'NODE',
    startAt: '',
    endAt: '',
  })
  const announcementSaving = ref(false)

  const categoryList = ref<string[]>([])
  const categoryColorMap = ref<Record<string, string>>({})
  const newCategoryInput = ref('')

  const severityList = ref<string[]>([])
  const severityColorMap = ref<Record<string, string>>({})
  const newSeverityInput = ref('')

  const SEVERITY_DESCRIPTIONS = DEFAULT_SEVERITY_DESCRIPTIONS
  const SCOPE_DESCRIPTIONS = DEFAULT_SCOPE_DESCRIPTIONS

  function addMetadataEntry() {
    metadataEntries.value.push({ key: '', value: '' })
  }

  function removeMetadataEntry(index: number) {
    metadataEntries.value.splice(index, 1)
  }

  function categoryBadgeStyle(category: string) {
    return badgeStyle(categoryColorMap.value[category?.toUpperCase()])
  }

  function severityBadgeStyle(severity: string) {
    return badgeStyle(severityColorMap.value[severity?.toUpperCase()])
  }

  function setWorkspaceData(payload: {
    content: NodeContent | null
    links: NodeLink[]
    announcements: Announcement[]
    categoriesMeta: CatalogEntry[]
    severitiesMeta: CatalogEntry[]
  }) {
    contentForm.value = {
      summary: payload.content?.summary ?? '',
      description: payload.content?.description ?? '',
      contactEmail: payload.content?.contactEmail ?? '',
      location: payload.content?.location ?? '',
    }
    metadataEntries.value = parseMetadataJson(payload.content?.metadataJson)

    links.value = payload.links
    announcements.value = payload.announcements

    categoryList.value = payload.categoriesMeta.map((entry) => entry.value)
    severityList.value = payload.severitiesMeta.map((entry) => entry.value)
    categoryColorMap.value = toCatalogColorMap(payload.categoriesMeta)
    severityColorMap.value = toCatalogColorMap(payload.severitiesMeta)
  }

  async function saveContent() {
    if (!nodeId.value) return
    contentSaving.value = true
    try {
      await orgApi.upsertNodeContent(nodeId.value, {
        summary: contentForm.value.summary,
        description: contentForm.value.description,
        contactEmail: contentForm.value.contactEmail,
        location: contentForm.value.location,
        metadataJson: serializeMetadataEntries(metadataEntries.value),
      })
    } finally {
      contentSaving.value = false
    }
  }

  async function addLink() {
    if (!nodeId.value || isBlank(linkForm.value.label) || isBlank(linkForm.value.url)) return

    linkSaving.value = true
    try {
      await orgApi.createNodeLink(nodeId.value, {
        label: trimOrEmpty(linkForm.value.label),
        url: trimOrEmpty(linkForm.value.url),
        category: linkForm.value.category,
        visibility: 'INHERIT',
        isActive: true,
      })
      linkForm.value = { label: '', url: '', category: 'GENERAL' }
      links.value = (await orgApi.getNodeLinks(nodeId.value, false)).data
    } finally {
      linkSaving.value = false
    }
  }

  async function removeLink(linkId: number) {
    if (!nodeId.value) return
    await orgApi.deleteNodeLink(linkId)
    links.value = (await orgApi.getNodeLinks(nodeId.value, false)).data
  }

  async function addCategory() {
    const value = toUpperTrimmed(newCategoryInput.value)
    if (!value) return

    newCategoryInput.value = ''
    try {
      await orgApi.addCatalogValue('LINK_CATEGORY', value)
      orgApi.clearCatalogMetaCache('LINK_CATEGORY')
      const metaEntries = await orgApi.getCatalogMetaCached('LINK_CATEGORY', { force: true })
      categoryList.value = metaEntries.map((entry) => entry.value)
      categoryColorMap.value = toCatalogColorMap(metaEntries)
    } catch {
      if (!categoryList.value.includes(value)) categoryList.value.push(value)
    }
    linkForm.value.category = value
  }

  async function addSeverity() {
    const value = toUpperTrimmed(newSeverityInput.value)
    if (!value) return

    newSeverityInput.value = ''
    try {
      await orgApi.addCatalogValue('ANNOUNCEMENT_SEVERITY', value)
      orgApi.clearCatalogMetaCache('ANNOUNCEMENT_SEVERITY')
      const metaEntries = await orgApi.getCatalogMetaCached('ANNOUNCEMENT_SEVERITY', { force: true })
      severityList.value = metaEntries.map((entry) => entry.value)
      severityColorMap.value = toCatalogColorMap(metaEntries)
    } catch {
      if (!severityList.value.includes(value)) severityList.value.push(value)
    }
    announcementForm.value.severity = value
  }

  async function addAnnouncement() {
    if (!nodeId.value || isBlank(announcementForm.value.title) || isBlank(announcementForm.value.body)) return

    announcementSaving.value = true
    try {
      await orgApi.createAnnouncement({
        nodeId: nodeId.value,
        scopeType: announcementForm.value.scopeType,
        title: trimOrEmpty(announcementForm.value.title),
        body: trimOrEmpty(announcementForm.value.body),
        severity: announcementForm.value.severity,
        startAt: announcementForm.value.startAt || null,
        endAt: announcementForm.value.endAt || null,
        isActive: true,
        createdByUserId: getCurrentUserId() ?? undefined,
      })
      announcementForm.value = {
        title: '',
        body: '',
        severity: 'INFO',
        scopeType: 'NODE',
        startAt: '',
        endAt: '',
      }
      announcements.value = (await orgApi.getNodeAnnouncements(nodeId.value)).data
    } finally {
      announcementSaving.value = false
    }
  }

  async function removeAnnouncement(announcementId: number) {
    if (!nodeId.value) return
    await orgApi.deleteAnnouncement(announcementId)
    announcements.value = (await orgApi.getNodeAnnouncements(nodeId.value)).data
  }

  return {
    contentForm,
    metadataEntries,
    contentSaving,
    links,
    linkForm,
    linkSaving,
    announcements,
    announcementForm,
    announcementSaving,
    categoryList,
    categoryColorMap,
    newCategoryInput,
    severityList,
    severityColorMap,
    newSeverityInput,
    SEVERITY_DESCRIPTIONS,
    SCOPE_DESCRIPTIONS,
    addMetadataEntry,
    removeMetadataEntry,
    categoryBadgeStyle,
    severityBadgeStyle,
    setWorkspaceData,
    saveContent,
    addLink,
    removeLink,
    addCategory,
    addSeverity,
    addAnnouncement,
    removeAnnouncement,
  }
}

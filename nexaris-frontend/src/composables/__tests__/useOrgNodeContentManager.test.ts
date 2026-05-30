import { describe, expect, it, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useOrgNodeContentManager } from '@/composables/useOrgNodeContentManager'

const orgMocks = vi.hoisted(() => ({
  upsertNodeContent: vi.fn(),
  createNodeLink: vi.fn(),
  getNodeLinks: vi.fn(),
  deleteNodeLink: vi.fn(),
  addCatalogValue: vi.fn(),
  getCatalogMeta: vi.fn(),
  createAnnouncement: vi.fn(),
  getNodeAnnouncements: vi.fn(),
  deleteAnnouncement: vi.fn(),
}))

vi.mock('@/api/org', () => ({
  orgApi: {
    upsertNodeContent: orgMocks.upsertNodeContent,
    createNodeLink: orgMocks.createNodeLink,
    getNodeLinks: orgMocks.getNodeLinks,
    deleteNodeLink: orgMocks.deleteNodeLink,
    addCatalogValue: orgMocks.addCatalogValue,
    getCatalogMeta: orgMocks.getCatalogMeta,
    createAnnouncement: orgMocks.createAnnouncement,
    getNodeAnnouncements: orgMocks.getNodeAnnouncements,
    deleteAnnouncement: orgMocks.deleteAnnouncement,
  },
}))

describe('useOrgNodeContentManager', () => {
  beforeEach(() => {
    orgMocks.upsertNodeContent.mockReset()
    orgMocks.createNodeLink.mockReset()
    orgMocks.getNodeLinks.mockReset()
    orgMocks.deleteNodeLink.mockReset()
    orgMocks.addCatalogValue.mockReset()
    orgMocks.getCatalogMeta.mockReset()
    orgMocks.createAnnouncement.mockReset()
    orgMocks.getNodeAnnouncements.mockReset()
    orgMocks.deleteAnnouncement.mockReset()
  })

  it('parses metadata JSON in setWorkspaceData', () => {
    const nodeId = ref(15)
    const composable = useOrgNodeContentManager(nodeId, () => 9)

    composable.setWorkspaceData({
      content: {
        id: 1,
        nodeId: 15,
        summary: 'Summary',
        description: 'Desc',
        contactEmail: 'hello@nexaris.com',
        location: 'Brussels',
        metadataJson: '{"budget":"1000","region":"BE"}',
      },
      links: [],
      announcements: [],
      categoriesMeta: [],
      severitiesMeta: [],
    })

    expect(composable.contentForm.value.summary).toBe('Summary')
    expect(composable.metadataEntries.value).toEqual([
      { key: 'budget', value: '1000' },
      { key: 'region', value: 'BE' },
    ])
  })

  it('sends undefined createdByUserId when no user id is available', async () => {
    orgMocks.createAnnouncement.mockResolvedValue({})
    orgMocks.getNodeAnnouncements.mockResolvedValue({ data: [] })

    const nodeId = ref(15)
    const composable = useOrgNodeContentManager(nodeId, () => null)
    composable.announcementForm.value.title = 'Incident'
    composable.announcementForm.value.body = 'Body'

    await composable.addAnnouncement()

    expect(orgMocks.createAnnouncement).toHaveBeenCalledTimes(1)
    const payload = orgMocks.createAnnouncement.mock.calls[0]?.[0] as { createdByUserId?: number }
    expect(payload.createdByUserId).toBeUndefined()
  })
})

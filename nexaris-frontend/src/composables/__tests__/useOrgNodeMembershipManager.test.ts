import { describe, expect, it, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useOrgNodeMembershipManager } from '@/composables/useOrgNodeMembershipManager'

const authMocks = vi.hoisted(() => ({
  getAllUsers: vi.fn(),
  listEnabledUsersSafe: vi.fn(),
}))

const orgMocks = vi.hoisted(() => ({
  addCatalogValue: vi.fn(),
  getNodeMemberships: vi.fn(),
  createMembership: vi.fn(),
  deleteMembership: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    getAllUsers: authMocks.getAllUsers,
    listEnabledUsersSafe: authMocks.listEnabledUsersSafe,
  },
}))

vi.mock('@/api/org', () => ({
  orgApi: {
    addCatalogValue: orgMocks.addCatalogValue,
    getNodeMemberships: orgMocks.getNodeMemberships,
    createMembership: orgMocks.createMembership,
    deleteMembership: orgMocks.deleteMembership,
  },
}))

describe('useOrgNodeMembershipManager', () => {
  beforeEach(() => {
    authMocks.getAllUsers.mockReset()
    authMocks.listEnabledUsersSafe.mockReset()
    orgMocks.addCatalogValue.mockReset()
    orgMocks.getNodeMemberships.mockReset()
    orgMocks.createMembership.mockReset()
    orgMocks.deleteMembership.mockReset()
  })

  it('loads enabled users and presets first user in the form', async () => {
    authMocks.listEnabledUsersSafe.mockResolvedValue([
      { id: 1, firstName: 'Alice', lastName: 'Doe', email: 'a@nexaris.com', enabled: true },
    ])

    const composable = useOrgNodeMembershipManager(ref(22))
    await composable.loadUsers()

    expect(composable.userOptions.value).toHaveLength(1)
    expect(composable.membershipForm.value.userId).toBe('1')
  })

  it('creates membership and refreshes membership list', async () => {
    orgMocks.createMembership.mockResolvedValue({})
    orgMocks.getNodeMemberships.mockResolvedValue({ data: [{ id: 10, nodeId: 22, userId: 1, membershipRole: 'ROLE_MANAGER' }] })

    const composable = useOrgNodeMembershipManager(ref(22))
    composable.membershipForm.value.userId = '1'
    composable.membershipForm.value.membershipRole = 'ROLE_MANAGER'

    await composable.addMembership()

    expect(orgMocks.createMembership).toHaveBeenCalledWith(22, {
      userId: 1,
      membershipRole: 'ROLE_MANAGER',
    })
    expect(composable.memberships.value).toEqual([{ id: 10, nodeId: 22, userId: 1, membershipRole: 'ROLE_MANAGER' }])
  })
})

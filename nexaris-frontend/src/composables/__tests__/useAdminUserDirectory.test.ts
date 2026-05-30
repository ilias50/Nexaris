import { describe, expect, it, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import type { AdminUser } from '@/api/auth'
import type { OrgUserRole } from '@/api/org'
import { useAdminUserDirectory } from '@/composables/useAdminUserDirectory'

const authMocks = vi.hoisted(() => ({
  getAllUsers: vi.fn(),
  listUsers: vi.fn(),
  getUser: vi.fn(),
}))

const orgMocks = vi.hoisted(() => ({
  getUserGlobalRoles: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    getAllUsers: authMocks.getAllUsers,
    listUsers: authMocks.listUsers,
    getUser: authMocks.getUser,
  },
}))

vi.mock('@/api/org', () => ({
  orgApi: {
    getUserGlobalRoles: orgMocks.getUserGlobalRoles,
  },
}))

describe('useAdminUserDirectory', () => {
  const t = (key: any) => String(key)

  beforeEach(() => {
    authMocks.getAllUsers.mockReset()
    authMocks.listUsers.mockReset()
    authMocks.getUser.mockReset()
    orgMocks.getUserGlobalRoles.mockReset()
  })

  it('loads users list and filters disabled users', async () => {
    authMocks.listUsers.mockResolvedValue([
      { id: 1, firstName: 'A', lastName: 'A', email: 'a@example.com', enabled: true },
      { id: 2, firstName: 'B', lastName: 'B', email: 'b@example.com', enabled: false },
    ])

    const selectedUser = ref<AdminUser | null>(null)
    const userOrgRoles = ref<OrgUserRole[]>([])
    const composable = useAdminUserDirectory(selectedUser, userOrgRoles, t)

    await composable.loadAllUsers()

    expect(composable.users.value).toHaveLength(1)
    expect(composable.users.value[0]?.id).toBe(1)
    expect(composable.usersVisible.value).toBe(true)
    expect(composable.usersMessage.value).toBe('adminUsers.management.messages.usersLoaded')
  })

  it('loads selected user profile and org roles', async () => {
    authMocks.getUser.mockResolvedValue({
      data: { id: 7, firstName: 'Jane', lastName: 'Doe', email: 'jane@example.com' },
    })
    orgMocks.getUserGlobalRoles.mockResolvedValue({
      data: [{ id: 1, userId: 7, roleName: 'ROLE_MANAGER' }],
    })

    const selectedUser = ref<AdminUser | null>(null)
    const userOrgRoles = ref<OrgUserRole[]>([])
    const composable = useAdminUserDirectory(selectedUser, userOrgRoles, t)

    await composable.selectUser(7)

    expect(selectedUser.value?.id).toBe(7)
    expect(userOrgRoles.value).toHaveLength(1)
    expect(userOrgRoles.value[0]?.roleName).toBe('ROLE_MANAGER')
  })
})

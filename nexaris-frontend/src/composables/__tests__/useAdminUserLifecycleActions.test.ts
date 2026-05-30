import { describe, expect, it, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useAdminUserLifecycleActions } from '@/composables/useAdminUserLifecycleActions'

const authMocks = vi.hoisted(() => ({
  resetUserPassword: vi.fn(),
  anonymizeUser: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    resetUserPassword: authMocks.resetUserPassword,
    anonymizeUser: authMocks.anonymizeUser,
  },
}))

describe('useAdminUserLifecycleActions', () => {
  const t = (key: any) => String(key)

  beforeEach(() => {
    authMocks.resetUserPassword.mockReset()
    authMocks.anonymizeUser.mockReset()
  })

  it('blocks delete confirmation when trying to delete self', () => {
    const selectedUser = ref({ id: 42, firstName: 'Self', lastName: 'User', email: 'self@example.com' })
    const composable = useAdminUserLifecycleActions(selectedUser as any, async () => {}, () => 42, t)

    composable.askDeleteUserConfirmation()

    expect(composable.deleteUserError.value).toBe('adminUsers.management.errors.cannotDeleteSelf')
    expect(composable.showDeleteConfirm.value).toBe(false)
  })

  it('validates reset password length before API call', async () => {
    const selectedUser = ref({ id: 10, firstName: 'A', lastName: 'B', email: 'a@example.com' })
    const composable = useAdminUserLifecycleActions(selectedUser as any, async () => {}, () => 99, t)

    await composable.handleResetPassword({ newPassword: 'short', confirmNewPassword: 'short' })

    expect(composable.resetPasswordError.value).toBe('adminUsers.management.errors.passwordTooShort')
    expect(authMocks.resetUserPassword).not.toHaveBeenCalled()
  })
})

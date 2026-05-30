import { describe, expect, it, beforeEach, vi } from 'vitest'
import { useAdminUserCreation } from '@/composables/useAdminUserCreation'

const authMocks = vi.hoisted(() => ({
  createUserByAdmin: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    createUserByAdmin: authMocks.createUserByAdmin,
  },
}))

describe('useAdminUserCreation', () => {
  const t = (key: any) => String(key)

  beforeEach(() => {
    authMocks.createUserByAdmin.mockReset()
  })

  it('validates password confirmation before API call', async () => {
    const composable = useAdminUserCreation(t)
    composable.password.value = 'password123'
    composable.confirmPassword.value = 'password124'

    await composable.handleCreateUser()

    expect(composable.createUserError.value).toBe('adminUsers.create.passwordMismatch')
    expect(authMocks.createUserByAdmin).not.toHaveBeenCalled()
  })

  it('creates user and clears form on success', async () => {
    authMocks.createUserByAdmin.mockResolvedValue({})
    const composable = useAdminUserCreation(t)

    composable.firstName.value = 'John'
    composable.lastName.value = 'Doe'
    composable.email.value = 'john@example.com'
    composable.password.value = 'password123'
    composable.confirmPassword.value = 'password123'

    await composable.handleCreateUser()

    expect(authMocks.createUserByAdmin).toHaveBeenCalledWith({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      password: 'password123',
    })
    expect(composable.createUserMessage.value).toBe('adminUsers.create.success')
    expect(composable.firstName.value).toBe('')
    expect(composable.lastName.value).toBe('')
    expect(composable.email.value).toBe('')
    expect(composable.password.value).toBe('')
    expect(composable.confirmPassword.value).toBe('')
  })
})

import { ref, type Ref } from 'vue'
import { authApi, type AdminUser } from '@/api/auth'
import { orgApi, type OrgUserRole } from '@/api/org'
import type { TranslationKey } from '@/i18n/messages'
import { isServerError } from '@/utils/apiError'

export type { AdminUser, OrgUserRole }

export function useAdminUserDirectory(
  selectedUser: Ref<AdminUser | null>,
  userOrgRoles: Ref<OrgUserRole[]>,
  t: (key: TranslationKey) => string,
) {
  const usersVisible = ref(false)
  const users = ref<AdminUser[]>([])
  const loadingUsers = ref(false)
  const usersMessage = ref('')
  const usersError = ref('')

  const loadingUserProfile = ref(false)
  const userProfileError = ref('')

  async function loadAllUsers() {
    loadingUsers.value = true
    usersError.value = ''
    usersMessage.value = ''

    try {
      const allUsers = await authApi.listUsers()
      users.value = allUsers.filter((user) => user.enabled !== false)
      usersVisible.value = true
      usersMessage.value = t('adminUsers.management.messages.usersLoaded')

      if (selectedUser.value) {
        const updatedSelection = allUsers.find((item) => item.id === selectedUser.value?.id)
        if (updatedSelection) {
          selectedUser.value = updatedSelection
        } else {
          selectedUser.value = null
        }
      }
    } catch (error: unknown) {
      if (isServerError(error)) {
        usersError.value = t('adminUsers.errors.serviceUnavailable')
      } else {
        usersError.value = t('adminUsers.management.errors.loadUsers')
      }
    } finally {
      loadingUsers.value = false
    }
  }

  async function showUsersList() {
    usersVisible.value = true
    if (!users.value.length) {
      await loadAllUsers()
    }
  }

  async function selectUser(userId: number) {
    loadingUserProfile.value = true
    userProfileError.value = ''

    try {
      const [userRes, orgRolesRes] = await Promise.all([
        authApi.getUser(userId),
        orgApi.getUserGlobalRoles(userId),
      ])
      selectedUser.value = userRes.data
      userOrgRoles.value = orgRolesRes.data
    } catch (error: unknown) {
      userOrgRoles.value = []
      if (isServerError(error)) {
        userProfileError.value = t('adminUsers.errors.serviceUnavailable')
      } else {
        userProfileError.value = t('adminUsers.management.errors.loadProfile')
      }
    } finally {
      loadingUserProfile.value = false
    }
  }

  return {
    usersVisible,
    users,
    loadingUsers,
    usersMessage,
    usersError,
    loadingUserProfile,
    userProfileError,
    loadAllUsers,
    showUsersList,
    selectUser,
  }
}
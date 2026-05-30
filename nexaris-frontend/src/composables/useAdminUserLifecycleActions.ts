import { computed, ref, type Ref } from 'vue'
import { authApi, type AdminUser } from '@/api/auth'
import { formatUserDisplayName } from '@/utils/users'
import type { TranslationKey } from '@/i18n/messages'
import { getApiErrorStatus, isServerError } from '@/utils/apiError'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'

export function useAdminUserLifecycleActions(
  selectedUser: Ref<AdminUser | null>,
  loadAllUsers: () => Promise<void>,
  getCurrentUserId: () => number | undefined,
  t: (key: TranslationKey) => string,
) {
  const resettingPassword = ref(false)
  const resetPasswordMessage = ref('')
  const resetPasswordError = ref('')

  const deletingUser = ref(false)
  const deleteUserMessage = ref('')
  const deleteUserError = ref('')
  const showDeleteConfirm = ref(false)
  const pendingDeleteUser = ref<AdminUser | null>(null)

  const isDeletingSelf = computed(() => {
    const currentUserId = getCurrentUserId()
    if (!selectedUser.value || !currentUserId) return false
    return selectedUser.value.id === currentUserId
  })

  const deleteConfirmDetails = computed(() => {
    if (!pendingDeleteUser.value) return ''
    return `${formatUserDisplayName(pendingDeleteUser.value)} (${pendingDeleteUser.value.email})`
  })

  function clearUserActionMessages() {
    resetPasswordMessage.value = ''
    resetPasswordError.value = ''
    deleteUserMessage.value = ''
    deleteUserError.value = ''
  }

  async function handleResetPassword(payload: { newPassword: string; confirmNewPassword: string }) {
    if (!selectedUser.value) return

    resetPasswordMessage.value = ''
    resetPasswordError.value = ''

    const nextPassword = payload.newPassword
    const nextPasswordConfirmation = payload.confirmNewPassword

    if (nextPassword.length < 8) {
      resetPasswordError.value = t('adminUsers.management.errors.passwordTooShort')
      return
    }

    if (nextPassword !== nextPasswordConfirmation) {
      resetPasswordError.value = t('adminUsers.management.errors.passwordMismatch')
      return
    }

    resettingPassword.value = true
    try {
      await authApi.resetUserPassword(selectedUser.value.id, nextPassword)
      resetPasswordMessage.value = t('adminUsers.management.messages.passwordReset')
    } catch (error: unknown) {
      if (isServerError(error)) {
        resetPasswordError.value = t('adminUsers.errors.serviceUnavailable')
      } else {
        resetPasswordError.value = t('adminUsers.management.errors.resetPassword')
      }
    } finally {
      resettingPassword.value = false
    }
  }

  function askDeleteUserConfirmation() {
    if (!selectedUser.value) {
      deleteUserError.value = t('adminUsers.management.selectUserHint')
      return
    }

    deleteUserMessage.value = ''
    deleteUserError.value = ''

    if (isDeletingSelf.value) {
      deleteUserError.value = t('adminUsers.management.errors.cannotDeleteSelf')
      return
    }

    pendingDeleteUser.value = selectedUser.value
    showDeleteConfirm.value = true
  }

  function cancelDeleteConfirmation() {
    showDeleteConfirm.value = false
    pendingDeleteUser.value = null
  }

  async function confirmDeleteUser() {
    if (!pendingDeleteUser.value) {
      showDeleteConfirm.value = false
      return
    }

    const targetUser = pendingDeleteUser.value
    showDeleteConfirm.value = false
    pendingDeleteUser.value = null

    deletingUser.value = true
    deleteUserMessage.value = ''
    deleteUserError.value = ''
    try {
      await authApi.anonymizeUser(targetUser.id)
      await loadAllUsers()
      selectedUser.value = null
      deleteUserMessage.value = t('adminUsers.management.messages.userDeleted')
    } catch (error: unknown) {
      if (isServerError(error)) {
        deleteUserError.value = t('adminUsers.errors.serviceUnavailable')
      } else {
        deleteUserError.value = resolveApiErrorMessage(error, t, {
          forbidden: 'adminUsers.management.errors.deleteUser',
          server: 'adminUsers.errors.serviceUnavailable',
          generic: 'adminUsers.management.errors.deleteUser',
        })
      }
    } finally {
      deletingUser.value = false
    }
  }

  return {
    resettingPassword,
    resetPasswordMessage,
    resetPasswordError,
    deletingUser,
    deleteUserMessage,
    deleteUserError,
    showDeleteConfirm,
    pendingDeleteUser,
    isDeletingSelf,
    deleteConfirmDetails,
    clearUserActionMessages,
    handleResetPassword,
    askDeleteUserConfirmation,
    cancelDeleteConfirmation,
    confirmDeleteUser,
  }
}
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import type { TranslationKey } from '@/i18n/messages'
import { resolveApiErrorMessage } from '@/utils/apiErrorMessage'
import { passwordsMatch } from '@/utils/validation'

export function useAdminUserCreation(t: (key: TranslationKey) => string) {
  const firstName = ref('')
  const lastName = ref('')
  const email = ref('')
  const password = ref('')
  const confirmPassword = ref('')

  const creatingUser = ref(false)
  const createUserMessage = ref('')
  const createUserError = ref('')

  async function handleCreateUser() {
    if (!passwordsMatch(password.value, confirmPassword.value)) {
      createUserError.value = t('adminUsers.create.passwordMismatch')
      createUserMessage.value = ''
      return
    }

    creatingUser.value = true
    createUserError.value = ''
    createUserMessage.value = ''

    try {
      await authApi.createUserByAdmin({
        firstName: firstName.value,
        lastName: lastName.value,
        email: email.value,
        password: password.value,
      })
      createUserMessage.value = t('adminUsers.create.success')
      firstName.value = ''
      lastName.value = ''
      email.value = ''
      password.value = ''
      confirmPassword.value = ''
    } catch (error: unknown) {
      createUserError.value = resolveApiErrorMessage(error, t, {
        conflict: 'adminUsers.create.emailExists',
        server: 'adminUsers.errors.serviceUnavailable',
        generic: 'adminUsers.create.genericError',
      })
    } finally {
      creatingUser.value = false
    }
  }

  return {
    firstName,
    lastName,
    email,
    password,
    confirmPassword,
    creatingUser,
    createUserMessage,
    createUserError,
    handleCreateUser,
  }
}
import type { TranslationKey } from '@/i18n/messages'
import { getApiErrorMessage, getApiErrorStatus, isServerError } from '@/utils/apiError'

type ErrorMessageMap = {
  unauthorized?: TranslationKey
  forbidden?: TranslationKey
  conflict?: TranslationKey
  server: TranslationKey
  generic: TranslationKey
}

type ErrorMessageOptions = {
  useApiMessageForUnauthorized?: boolean
}

export function resolveApiErrorMessage(
  error: unknown,
  t: (key: TranslationKey) => string,
  messages: ErrorMessageMap,
  options?: ErrorMessageOptions,
) {
  const status = getApiErrorStatus(error)
  if (status === 401 && messages.unauthorized) {
    if (options?.useApiMessageForUnauthorized) {
      return getApiErrorMessage(error) ?? t(messages.unauthorized)
    }
    return t(messages.unauthorized)
  }
  if (status === 403 && messages.forbidden) {
    return t(messages.forbidden)
  }
  if (status === 409 && messages.conflict) {
    return t(messages.conflict)
  }
  if (isServerError(error)) {
    return t(messages.server)
  }
  return t(messages.generic)
}

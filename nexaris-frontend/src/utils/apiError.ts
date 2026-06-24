export interface ApiError {
  response?: {
    status?: number
    data?: unknown
  }
}

export function getApiErrorStatus(error: unknown): number | undefined {
  return (error as ApiError)?.response?.status
}

export function getApiErrorMessage(error: unknown): string | undefined {
  const data = (error as ApiError)?.response?.data
  if (typeof data === 'string') return data
  if (data && typeof data === 'object' && 'message' in data) {
    const message = (data as { message?: unknown }).message
    return typeof message === 'string' ? message : undefined
  }
  return undefined
}

export function getApiValidationErrors(error: unknown): Record<string, string> | undefined {
  const data = (error as ApiError)?.response?.data
  if (!data || typeof data !== 'object' || !('errors' in data)) {
    return undefined
  }

  const rawErrors = (data as { errors?: unknown }).errors
  if (!rawErrors || typeof rawErrors !== 'object') {
    return undefined
  }

  const normalized: Record<string, string> = {}
  for (const [key, value] of Object.entries(rawErrors)) {
    if (typeof value === 'string' && value.trim().length > 0) {
      normalized[key] = value
    }
  }

  return Object.keys(normalized).length > 0 ? normalized : undefined
}

export function isServerError(error: unknown): boolean {
  const status = getApiErrorStatus(error)
  return typeof status === 'number' && status >= 500
}

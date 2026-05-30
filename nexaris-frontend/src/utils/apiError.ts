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

export function isServerError(error: unknown): boolean {
  const status = getApiErrorStatus(error)
  return typeof status === 'number' && status >= 500
}

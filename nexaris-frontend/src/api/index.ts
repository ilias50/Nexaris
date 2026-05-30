import axios from 'axios'
import { getApiBaseUrl } from '@/config/runtime'
import {
  clearAuthSession,
  getStoredRefreshToken,
  getStoredToken,
  persistAuthSession,
} from '@/utils/authStorage'

const apiClient = axios.create({
  // Use same-origin API by default (works on phone/LAN with reverse proxy).
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
})

function isPublicAuthEndpoint(url?: string) {
  if (!url) return false
  return (
    url.includes('/api/v1/auth/login') ||
    url.includes('/api/v1/auth/register') ||
    url.includes('/api/v1/auth/refresh-token')
  )
}

function clearSessionAndRedirectToLogin() {
  clearAuthSession()
  window.location.href = '/login'
}

// Intercepteur requête : injecte le JWT si présent
apiClient.interceptors.request.use((config) => {
  const token = getStoredToken()
  const hasAuthorization = !!config.headers?.Authorization
  if (token && !hasAuthorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Intercepteur réponse : redirige vers /login si token expiré
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config as any
    const requestUrl: string | undefined = error?.config?.url
    const status = error.response?.status

    if (status === 401 && !isPublicAuthEndpoint(requestUrl) && !originalRequest?._retry) {
      originalRequest._retry = true
      const refreshToken = getStoredRefreshToken()
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            '/api/v1/auth/refresh-token',
            null,
            {
                baseURL: getApiBaseUrl(),
              headers: {
                Authorization: `Bearer ${refreshToken}`,
                'Content-Type': 'application/json',
              },
            },
          )

          persistAuthSession({
            token: data.token,
            refreshToken: data.refreshToken,
            user: data.user,
          })

          originalRequest.headers = originalRequest.headers ?? {}
          originalRequest.headers.Authorization = `Bearer ${data.token}`
          return apiClient(originalRequest)
        } catch {
          clearSessionAndRedirectToLogin()
        }
      } else {
        clearSessionAndRedirectToLogin()
      }
    }

    // A 403 can mean "forbidden" on a specific resource, not "session invalid".
    if (status === 401 && !isPublicAuthEndpoint(requestUrl)) {
      clearSessionAndRedirectToLogin()
    }

    return Promise.reject(error)
  },
)

export default apiClient

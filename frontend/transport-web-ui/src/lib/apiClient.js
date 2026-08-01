import axios from 'axios'
import { clearSession, getSession, setSession } from './session.js'

const defaultDevelopmentGateway = import.meta.env.DEV
  ? 'http://127.0.0.1:9090'
  : ''
const baseURL = import.meta.env.VITE_API_BASE_URL || defaultDevelopmentGateway

export const apiClient = axios.create({
  baseURL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

const refreshClient = axios.create({
  baseURL,
  timeout: 12_000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null

apiClient.interceptors.request.use((config) => {
  const token = getSession()?.accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config
    const session = getSession()
    const requestUrl = request?.url || ''
    const isPublicAuthEndpoint = [
      '/api/auth/login/',
      '/api/auth/register/',
      '/api/auth/forgot-password/',
      '/api/auth/refresh',
    ].some((path) => requestUrl.includes(path))

    if (
      error.response?.status !== 401 ||
      request?._retried ||
      isPublicAuthEndpoint ||
      !session?.refreshToken
    ) {
      return Promise.reject(error)
    }

    request._retried = true
    try {
      if (!refreshPromise) {
        refreshPromise = refreshClient
          .post('/api/auth/refresh', { refreshToken: session.refreshToken })
          .then((response) => response.data?.data)
          .finally(() => {
            refreshPromise = null
          })
      }

      const refreshed = await refreshPromise
      const nextSession = {
        ...session,
        ...refreshed,
        user: refreshed?.user || session.user,
      }
      setSession(nextSession)
      request.headers.Authorization = `Bearer ${nextSession.accessToken}`
      return apiClient(request)
    } catch (refreshError) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login?reason=session-expired')
      }
      return Promise.reject(refreshError)
    }
  },
)

export function unwrap(response) {
  return response?.data?.data
}

export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const payload = error?.response?.data
  if (typeof payload?.message === 'string' && payload.message.trim()) {
    return payload.message
  }

  if (payload?.errors && typeof payload.errors === 'object') {
    const firstError = Object.values(payload.errors)[0]
    if (firstError) return String(firstError)
  }

  if (error?.code === 'ECONNABORTED') {
    return 'The request took too long. Please try again.'
  }

  if (!error?.response) {
    return 'Unable to reach the transport server. Check that the backend is running.'
  }

  return fallback
}


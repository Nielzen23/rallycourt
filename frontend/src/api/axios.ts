import axios from 'axios'
import { clearAuthSession, getAuthToken, isAuthTokenExpired } from '../utils/authStorage'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  import.meta.env.VITE_API_URL ??
  'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

function redirectToSessionExpired() {
  clearAuthSession()

  if (window.location.pathname !== '/session-expired') {
    window.location.assign('/session-expired')
  }
}

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken()
  const isRefreshRequest = config.url?.includes('/api/auth/refresh') || config.url?.includes('/auth/refresh')

  if (token && !isRefreshRequest) {
    if (isAuthTokenExpired(token)) {
      redirectToSessionExpired()
      return Promise.reject(new axios.CanceledError('Session expired'))
    }

    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      redirectToSessionExpired()
    }

    return Promise.reject(error)
  },
)

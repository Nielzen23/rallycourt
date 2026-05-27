import { apiClient } from '../../api/axios'
import type {
  AuthResponse,
  CourtOwnerApplicationResponse,
  LoginRequest,
  RegisterRequest,
  SessionTokenRequest,
} from '../types/authTypes'

export async function login(request: LoginRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/login', request)
  return response.data
}

export async function register(request: RegisterRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/register', request)
  return response.data
}

export async function renewSession(request: SessionTokenRequest) {
  const response = await apiClient.post<AuthResponse>('/api/auth/refresh', request)
  return response.data
}

export async function applyForCourtOwner() {
  const response = await apiClient.post<CourtOwnerApplicationResponse>(
    '/api/court-owner/apply',
  )
  return response.data
}

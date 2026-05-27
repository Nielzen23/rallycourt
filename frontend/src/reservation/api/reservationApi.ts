import { apiClient } from '../../api/axios'
import type {
  CourtAvailabilityItem,
  CreatedReservation,
  CreateReservationPayload,
  ReservationConfigResponse,
  ReservationPageResponse,
} from '../types/reservationTypes'

export async function fetchReservationConfig() {
  const response = await apiClient.get<ReservationConfigResponse>('/api/reservations/config')
  return response.data
}

export async function fetchCourtAvailability(courtId: number, from: string, to: string) {
  const response = await apiClient.get<CourtAvailabilityItem[]>(`/api/courts/${courtId}/availability`, {
    params: { from, to },
  })
  return response.data
}

export async function createReservation(payload: CreateReservationPayload) {
  const response = await apiClient.post<CreatedReservation>('/api/reservations', payload)
  return response.data
}

export async function fetchMyReservations(page = 0, size = 10) {
  const response = await apiClient.get<ReservationPageResponse>('/api/reservations', {
    params: { page, size },
  })
  return response.data
}

export async function fetchAllReservations(page = 0, size = 10) {
  const response = await apiClient.get<ReservationPageResponse>('/api/reservations/all', {
    params: { page, size },
  })
  return response.data
}

import { apiClient } from '../../../api/axios'
import type {
  CourtManagementCourt,
  CourtManagementDetails,
  CourtManagementPageResponse,
  CourtManagementRequest,
  CourtManagementReservation,
} from '../types/courtManagementTypes'

export async function fetchCourtManagementCourts(params?: {
  courtType?: string
  venueType?: string
  status?: string
  page?: number
  size?: number
}) {
  const response = await apiClient.get<CourtManagementPageResponse>('/api/court-management/courts', {
    params: {
      courtType: params?.courtType,
      venueType: params?.venueType,
      status: params?.status,
      page: params?.page ?? 0,
      size: params?.size ?? 10,
    },
  })
  return response.data
}

export async function fetchCourtManagementDetails(courtId: number) {
  const response = await apiClient.get<CourtManagementDetails>(`/api/court-management/courts/${courtId}`)
  return response.data
}

export async function fetchCourtManagementReservations(courtId: number) {
  const response = await apiClient.get<CourtManagementReservation[]>(
    `/api/court-management/courts/${courtId}/reservations`,
  )
  return response.data
}

export async function createCourtManagementCourt(payload: CourtManagementRequest) {
  const response = await apiClient.post<CourtManagementCourt>('/api/court-management/courts', payload)
  return response.data
}

export async function updateCourtManagementCourt(courtId: number, payload: CourtManagementRequest) {
  const response = await apiClient.put<CourtManagementCourt>(`/api/court-management/courts/${courtId}`, payload)
  return response.data
}

export async function deleteCourtManagementCourt(courtId: number) {
  await apiClient.delete(`/api/court-management/courts/${courtId}`)
}

import { apiClient } from '../../api/axios'
import type { CourtFilters, CourtPageResponse } from '../types/courtTypes'

export async function fetchCourts(filters: CourtFilters = {}) {
  const response = await apiClient.get<CourtPageResponse>('/api/courts', {
    params: {
      courtType: filters.courtType,
      venueType: filters.venueType,
      status: filters.status,
      page: filters.page ?? 0,
      size: filters.size ?? 10,
    },
  })

  return response.data
}

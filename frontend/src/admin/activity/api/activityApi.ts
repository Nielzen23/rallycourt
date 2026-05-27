import { apiClient } from '../../../api/axios'
import type {
  AdminActivityHistoryPageResponse,
  AdminUserActivitySummary,
} from '../types/activityTypes'

export async function fetchAdminUserActivitySummaries() {
  const response = await apiClient.get<AdminUserActivitySummary[]>('/api/admin/activity/users')
  return response.data
}

export async function fetchAdminUserActivityHistory(userId: number, page = 0, size = 10) {
  const response = await apiClient.get<AdminActivityHistoryPageResponse>(
    `/api/admin/activity/users/${userId}/history`,
    {
      params: {
        page,
        size,
      },
    },
  )
  return response.data
}

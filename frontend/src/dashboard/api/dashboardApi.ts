import { apiClient } from '../../api/axios'
import type { DashboardResponse } from '../types/dashboardTypes'

export async function fetchDashboard() {
  const response = await apiClient.get<DashboardResponse>('/api/dashboard')
  return response.data
}

export type AdminUserActivitySummary = {
  userId: number
  email: string
  firstName: string
  lastName: string
  role: string
  lastActivityAt: string | null
  lastActivityAction: string | null
  successfulBookings: number
  failedBookings: number
  totalBookings: number
  successRatioPercentage: number
}

export type AdminActivityHistoryItem = {
  id: string
  action: string
  status: string
  createdAt: string
}

export type AdminActivityHistoryPageResponse = {
  content: AdminActivityHistoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

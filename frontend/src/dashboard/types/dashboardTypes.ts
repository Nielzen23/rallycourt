export type DashboardCountItem = {
  label: string
  count: number
}

export type DashboardUserSummary = {
  usersWithBookings: number
  activeUsersWithoutBookings: number
  inactiveUsers: number
}

export type DashboardResponse = {
  totalBookings: number
  paidBookings: number
  unpaidBookings: number
  bookingPaymentBreakdown: DashboardCountItem[]
  popularCourts: DashboardCountItem[]
  popularSports: DashboardCountItem[]
  userSummary: DashboardUserSummary
}

export type CourtManagementCourt = {
  id: number
  name: string
  courtType: string
  venueType: string
  location: string
  latitude: number
  longitude: number
  status: string
  hourlyRate: number
  openTime?: string
  closeTime?: string
}

export type CourtManagementReservation = {
  reservationId: number
  reservationDate: string
  startTime: string
  durationMinutes: number
  paymentStatus: 'PAID' | 'PENDING'
  reservationStatus: string
  playerName: string
}

export type CourtManagementDetails = CourtManagementCourt & {
  upcomingReservations: CourtManagementReservation[]
}

export type CourtManagementRequest = {
  name: string
  courtType: string
  venueType: string
  location: string
  status: string
  hourlyRate: number
}

export type CourtManagementPageResponse = {
  content: CourtManagementCourt[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ReservationSummary = {
  id: number
  courtName: string
  status: string
  startTime: string
  endTime: string
}

export type ReservationRecord = {
  id: number
  courtId: number
  courtName: string
  courtType: string | null
  location: string | null
  latitude: number | null
  longitude: number | null
  reservedBy: string
  contactName: string | null
  contactMobileNumber: string | null
  startTime: string
  endTime: string
  expiresAt: string | null
  status: string
  paymentStatus: string | null
  amountDue: number
}

export type ReservationPageResponse = {
  content: ReservationRecord[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ReservationConfigResponse = {
  allowedDurationsMinutes: number[]
}

export type CourtAvailabilityItem = {
  reservationId: number
  startDateTime: string
  endDateTime: string
  reservationStatus: string
  paymentStatus: string
  label: string
}

export type CreateReservationPayload = {
  courtId: number
  startTime: string
  durationMinutes: number
}

export type CreatedReservation = {
  id: number
  courtId: number
  reservedBy: string
  startTime: string
  endTime: string
  expiresAt: string | null
  status: string
  amountDue: number
}

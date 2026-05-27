export type Court = {
  id: number
  name: string
  location: string
  latitude: number
  longitude: number
  courtType: string
  venueType: string
  status: string
  openTime: string
  closeTime: string
  hourlyRate: number
}

export type CourtFilters = {
  courtType?: string
  venueType?: string
  status?: string
  page?: number
  size?: number
}

export type CourtPageResponse = {
  content: Court[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

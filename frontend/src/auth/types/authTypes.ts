export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  email: string
  password: string
  firstName: string
  lastName: string
  mobileNumber: string
}

export type SessionTokenRequest = {
  sessionToken: string
}

export type AuthResponse = {
  token: string
  sessionToken: string
  email: string
  firstName: string
  lastName: string
  mobileNumber?: string
  role: string
  courtOwnerStatus: string
}

export type CourtOwnerApplicationResponse = {
  id: number
  email: string
  firstName: string
  lastName: string
  mobileNumber: string
  courtOwnerStatus: string
  role: {
    id: number
    code: string
    name: string
    description?: string
  }
}

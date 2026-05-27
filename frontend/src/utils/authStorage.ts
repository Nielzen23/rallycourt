export const AUTH_TOKEN_STORAGE_KEY = 'rallycourt.jwt'
export const SESSION_TOKEN_STORAGE_KEY = 'rallycourt.sessionToken'
export const AUTH_EMAIL_STORAGE_KEY = 'rallycourt.email'
export const AUTH_FIRST_NAME_STORAGE_KEY = 'rallycourt.firstName'
export const AUTH_LAST_NAME_STORAGE_KEY = 'rallycourt.lastName'
export const AUTH_MOBILE_NUMBER_STORAGE_KEY = 'rallycourt.mobileNumber'
export const AUTH_ROLE_STORAGE_KEY = 'rallycourt.role'

const tokenStorage = window.localStorage
const profileStorage = window.localStorage

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const segments = token.split('.')

  if (segments.length < 2) {
    return null
  }

  try {
    const normalized = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const decoded = window.atob(padded)
    return JSON.parse(decoded) as Record<string, unknown>
  } catch {
    return null
  }
}

export function getAuthToken(): string | null {
  return tokenStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
}

export function setAuthToken(token: string): void {
  tokenStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
}

export function getSessionToken(): string | null {
  return tokenStorage.getItem(SESSION_TOKEN_STORAGE_KEY)
}

export function setSessionToken(token: string): void {
  tokenStorage.setItem(SESSION_TOKEN_STORAGE_KEY, token)
}

export function clearAuthToken(): void {
  tokenStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
}

export function clearSessionToken(): void {
  tokenStorage.removeItem(SESSION_TOKEN_STORAGE_KEY)
}

export function isAuthTokenExpired(token: string): boolean {
  const payload = decodeJwtPayload(token)
  const exp = payload?.exp

  if (typeof exp !== 'number') {
    return true
  }

  return exp * 1000 <= Date.now()
}

export function getAuthTokenExpiryMs(token: string): number | null {
  const payload = decodeJwtPayload(token)
  const exp = payload?.exp

  if (typeof exp !== 'number') {
    return null
  }

  return exp * 1000
}

export function hasValidAuthToken(): boolean {
  const token = getAuthToken()

  if (!token) {
    return false
  }

  return !isAuthTokenExpired(token)
}

export function getStoredEmail(): string | null {
  return profileStorage.getItem(AUTH_EMAIL_STORAGE_KEY)
}

export function setStoredEmail(email: string): void {
  profileStorage.setItem(AUTH_EMAIL_STORAGE_KEY, email)
}

export function clearStoredEmail(): void {
  profileStorage.removeItem(AUTH_EMAIL_STORAGE_KEY)
}

export function getStoredFirstName(): string | null {
  return profileStorage.getItem(AUTH_FIRST_NAME_STORAGE_KEY)
}

export function setStoredFirstName(firstName: string): void {
  profileStorage.setItem(AUTH_FIRST_NAME_STORAGE_KEY, firstName)
}

export function clearStoredFirstName(): void {
  profileStorage.removeItem(AUTH_FIRST_NAME_STORAGE_KEY)
}

export function getStoredLastName(): string | null {
  return profileStorage.getItem(AUTH_LAST_NAME_STORAGE_KEY)
}

export function setStoredLastName(lastName: string): void {
  profileStorage.setItem(AUTH_LAST_NAME_STORAGE_KEY, lastName)
}

export function clearStoredLastName(): void {
  profileStorage.removeItem(AUTH_LAST_NAME_STORAGE_KEY)
}

export function getStoredMobileNumber(): string | null {
  return profileStorage.getItem(AUTH_MOBILE_NUMBER_STORAGE_KEY)
}

export function setStoredMobileNumber(mobileNumber: string): void {
  profileStorage.setItem(AUTH_MOBILE_NUMBER_STORAGE_KEY, mobileNumber)
}

export function clearStoredMobileNumber(): void {
  profileStorage.removeItem(AUTH_MOBILE_NUMBER_STORAGE_KEY)
}

export function getStoredRole(): string | null {
  return profileStorage.getItem(AUTH_ROLE_STORAGE_KEY)
}

export function setStoredRole(role: string): void {
  profileStorage.setItem(AUTH_ROLE_STORAGE_KEY, role)
}

export function clearStoredRole(): void {
  profileStorage.removeItem(AUTH_ROLE_STORAGE_KEY)
}

export function clearAuthSession(): void {
  clearAuthToken()
  clearSessionToken()
  clearStoredEmail()
  clearStoredFirstName()
  clearStoredLastName()
  clearStoredMobileNumber()
  clearStoredRole()
}

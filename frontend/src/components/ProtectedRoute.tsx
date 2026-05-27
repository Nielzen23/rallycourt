import { useEffect, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import { Navigate, useLocation } from 'react-router-dom'
import { renewSession } from '../auth/api/authApi'
import SessionExpiryModal from './SessionExpiryModal'
import {
  clearAuthSession,
  getAuthToken,
  getAuthTokenExpiryMs,
  getSessionToken,
  getStoredRole,
  isAuthTokenExpired,
  setAuthToken,
  setSessionToken,
  setStoredEmail,
  setStoredFirstName,
  setStoredLastName,
  setStoredMobileNumber,
  setStoredRole,
} from '../utils/authStorage'

type ProtectedRouteProps = {
  children: ReactNode
  allowedRoles?: string[]
}

function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const location = useLocation()
  const token = getAuthToken()
  const role = getStoredRole()
  const [isRenewPromptOpen, setIsRenewPromptOpen] = useState(false)
  const [remainingSeconds, setRemainingSeconds] = useState(0)
  const [isRenewing, setIsRenewing] = useState(false)
  const [renewErrorMessage, setRenewErrorMessage] = useState('')

  const expiryMs = useMemo(() => (token ? getAuthTokenExpiryMs(token) : null), [token])

  const isUnauthorized = !token
  const isExpired = !!token && isAuthTokenExpired(token)
  const isForbidden = !!allowedRoles && (!role || !allowedRoles.includes(role))

  useEffect(() => {
    if (!expiryMs || isUnauthorized || isExpired) {
      setIsRenewPromptOpen(false)
      return
    }

    const updateRemainingTime = () => {
      const nextRemainingSeconds = Math.max(0, Math.floor((expiryMs - Date.now()) / 1000))
      setRemainingSeconds(nextRemainingSeconds)
      setIsRenewPromptOpen(nextRemainingSeconds > 0 && nextRemainingSeconds <= 120)
    }

    updateRemainingTime()
    const intervalId = window.setInterval(updateRemainingTime, 1000)
    return () => window.clearInterval(intervalId)
  }, [expiryMs, isUnauthorized, isExpired])

  if (isUnauthorized) {
    return <Navigate to="/" replace state={{ from: location }} />
  }

  if (isExpired) {
    clearAuthSession()
    return <Navigate to="/session-expired" replace state={{ from: location }} />
  }

  if (isForbidden) {
    return <Navigate to="/dashboard" replace />
  }

  const handleRenewSession = async () => {
    const sessionToken = getSessionToken()
    if (!sessionToken) {
      clearAuthSession()
      window.location.assign('/session-expired')
      return
    }

    setIsRenewing(true)
    setRenewErrorMessage('')

    try {
      const response = await renewSession({ sessionToken })
      setAuthToken(response.token)
      setSessionToken(response.sessionToken)
      setStoredEmail(response.email)
      setStoredFirstName(response.firstName)
      setStoredLastName(response.lastName)
      setStoredMobileNumber(response.mobileNumber ?? '')
      setStoredRole(response.role)
      setIsRenewPromptOpen(false)
    } catch (error) {
      if (axios.isAxiosError(error)) {
        setRenewErrorMessage(error.response?.data?.message ?? 'Unable to renew session.')
      } else {
        setRenewErrorMessage('Unable to renew session.')
      }
      clearAuthSession()
      window.location.assign('/session-expired')
    } finally {
      setIsRenewing(false)
    }
  }

  return (
    <>
      {children}
      {isRenewPromptOpen ? (
        <SessionExpiryModal
          remainingSeconds={remainingSeconds}
          isRenewing={isRenewing}
          errorMessage={renewErrorMessage}
          onRenew={handleRenewSession}
          onSignOut={() => {
            clearAuthSession()
            window.location.assign('/session-expired')
          }}
        />
      ) : null}
    </>
  )
}

export default ProtectedRoute

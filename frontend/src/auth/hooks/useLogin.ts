import { useState } from 'react'
import axios from 'axios'
import { login } from '../api/authApi'
import {
  setAuthToken,
  setSessionToken,
  setStoredEmail,
  setStoredFirstName,
  setStoredLastName,
  setStoredMobileNumber,
  setStoredRole,
} from '../../utils/authStorage'
import type { ApiErrorResponse } from '../../types/api'

export function useLogin() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const submitLogin = async (email: string, password: string) => {
    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await login({
        email: email.trim(),
        password,
      })

      setAuthToken(response.token)
      setSessionToken(response.sessionToken)
      setStoredEmail(response.email)
      setStoredFirstName(response.firstName)
      setStoredLastName(response.lastName)
      setStoredMobileNumber(response.mobileNumber ?? '')
      setStoredRole(response.role)
      setSuccessMessage('Login successful. Redirecting to dashboard...')
      return true
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(
          error.response?.data?.message ??
            'Login failed. Check your email and password.',
        )
      } else {
        setErrorMessage('Unexpected error while signing in.')
      }
      return false
    } finally {
      setIsSubmitting(false)
    }
  }

  return {
    isSubmitting,
    errorMessage,
    successMessage,
    submitLogin,
  }
}

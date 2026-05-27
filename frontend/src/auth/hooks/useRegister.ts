import { useState } from 'react'
import axios from 'axios'
import { applyForCourtOwner, register } from '../api/authApi'
import type { RegisterRequest } from '../types/authTypes'
import type { ApiErrorResponse } from '../../types/api'
import {
  clearAuthSession,
  setAuthToken,
  setSessionToken,
  setStoredEmail,
  setStoredFirstName,
  setStoredLastName,
  setStoredMobileNumber,
} from '../../utils/authStorage'

export function useRegister() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const submitRegister = async (
    request: RegisterRequest,
    wantsCourtOwnerAccess: boolean,
  ) => {
    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await register(request)

      if (wantsCourtOwnerAccess) {
        setAuthToken(response.token)
        setSessionToken(response.sessionToken)
        setStoredEmail(response.email)
        setStoredFirstName(response.firstName)
        setStoredLastName(response.lastName)
        setStoredMobileNumber(request.mobileNumber)
        await applyForCourtOwner()
        clearAuthSession()
        setSuccessMessage(
          'Registration successful. Court owner application submitted for approval. Redirecting to login...',
        )
      } else {
        setSuccessMessage('Registration successful. Redirecting to login...')
      }

      return true
    } catch (error) {
      clearAuthSession()
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Registration failed.')
      } else {
        setErrorMessage('Unexpected error while creating account.')
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
    submitRegister,
  }
}
